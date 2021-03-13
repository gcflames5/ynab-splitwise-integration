package com.github.gclfames5;

import com.github.gclfames5.config.YAMLConfiguration;
import com.github.gclfames5.log.Logger;
import com.github.gclfames5.sw.SplitwiseExpense;
import com.github.gclfames5.sw.SplitwiseHandler;
import com.github.gclfames5.ynab.YNABHandler;
import ynab.client.model.SaveTransaction;
import ynab.client.model.TransactionDetail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    public static String LOGFILE_PATH = "logfile.txt";
    public static String configPath = "<UNSPECIFIED>";

    public static void main(String[] args) {

        if (args.length <= 0) {
            System.out.println("Configuration not specified via command line. Defaulting to ./config.yml. To specify a custom path, add path as a command line argument.");
            try {
                File jarDirectory =  new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                configPath = new File(jarDirectory.getParentFile(), "config.yml").getAbsolutePath();
                System.out.println("Configuration path: " + configPath);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                System.err.println("Failed to find working directory! See error for more details.");
                System.exit(0);
            }

        } else {
            System.out.println("Configuration specified via command line: " + args[0]);
            configPath = args[0];
        }

        // Setup the log
        Logger.initLog(LOGFILE_PATH);

        // Read Config
        System.out.println("Reading configuration...");
        File f = new File(configPath);

        YAMLConfiguration config = new YAMLConfiguration(configPath);
        try {
            config.openConfig();
        } catch (FileNotFoundException e) {
            System.out.println("No com.github.gclfames5.config file found!");
            e.printStackTrace();
        }

        // Authenticate Splitwise
        Logger.log("Authenticating Splitwise...", true);
        SplitwiseHandler sw = new SplitwiseHandler(config);
        sw.authenticate(true);
        Logger.log(String.format("Fetching spltiwise transactions since %s", config.getSplitwiseLastTransactionDate().toString()), true);
        List<SplitwiseExpense> expensesToProcess = sw.getAllExpenses(0, config.getSplitwiseLastTransactionDate());

        // Authenticate YNAB
        Logger.log("Authenticating YNAB...", true);
        YNABHandler ynab = new YNABHandler(config);
        ynab.authenticate();

        // Search through all updated Spltiwise Transactions
        // If any have been updated (or deleted) determine the earliest
        // "created_on" date to determine how far back we have to search in
        // YNAB in order to fund the corresponding transaction

        List<SplitwiseExpense> newSplitwiseExpenses = new ArrayList<>();
        List<SplitwiseExpense> updatedSplitwiseExpenses = new ArrayList<>();
        List<SplitwiseExpense> deletedSplitwiseExpenses = new ArrayList<>();
        Date oldestCreationDate = new Date();

        // Sort Expenses into "new" and "updated" categories while also finding oldest "created_at" date
        for (SplitwiseExpense expense : expensesToProcess) {
            if (expense.created_at.before(oldestCreationDate))
                oldestCreationDate = expense.created_at;

            // If deleted, add to deleted expenses
            if (expense.deleted_at != null) {
                deletedSplitwiseExpenses.add(expense);
                continue;
            }

            // If "updated_on" and "created_at" match, then treat it as "new"
            if (expense.updated_on.equals(expense.created_at)) {
                newSplitwiseExpenses.add(expense);
            } else {
                updatedSplitwiseExpenses.add(expense);
            }
        }

        // Add all new transactions to YNAB
        Logger.log(String.format("Number of new expenses found: %d", newSplitwiseExpenses.size()));
        for (SplitwiseExpense newExpense : newSplitwiseExpenses) {
            Logger.log(String.format("New transaction found: %s", newExpense.toString()), true);
            newExpense.description += String.format(", sw_uuid:%d", newExpense.id);
            Logger.log(String.format("Uploading new expense to YNAB: %s", newExpense.toString()), true);
            ynab.addTransaction(newExpense.cost, newExpense.description, newExpense.created_at);
        }


        // For all transactions that seem to be updated:
        //  - Try to match them with a YNAB expense and perform update
        //  - Otherwise, treat as a new transaction
        Logger.log(String.format("Number of updated expenses found: %d", updatedSplitwiseExpenses.size()));
        if (updatedSplitwiseExpenses.size() > 0) {
            // Get the minimum number of transactions from YNAB to search through UUIDs
            List<TransactionDetail> ynabTransactions = ynab.getTransactionsSince(new Date());
            splitwiseLoop:
            for (SplitwiseExpense updatedExpense : updatedSplitwiseExpenses) {
                // Search for corresponding expense in YNAB
                ynabLoop:
                for (TransactionDetail ynabTransaction : ynabTransactions) {
                    // Ignore any transactions without memos, these can't have been
                    // generated by us anyway
                    if (ynabTransaction == null || ynabTransaction.getMemo() == null) {
                        Logger.log(String.format("Saw null transaction or null memo while searching for updated transactions"));
                        continue;
                    }

                    String[] search = ynabTransaction.getMemo().split("sw_uuid:");

                    Logger.log(String.format("Searching transaction: %s with memo: %s", ynabTransaction.toString(), ynabTransaction.getMemo()));
                    // Transactions may exist that have not been tagged with the uuid
                    // ignore those entries
                    if (search.length > 1) {
                        int uuid = Integer.valueOf(search[1]);
                        // Check if this uuid matches the splitwise expense we're looking at
                        // If it matches, update this transaction
                        if (uuid == updatedExpense.id) {
                            Logger.log(String.format("Matched updated transaction: %s with uuid %s",
                                    updatedExpense.toString(), ynabTransaction.getAccountId().toString()), true);
                            SaveTransaction saveTransaction = new SaveTransaction();
                            saveTransaction.accountId(ynabTransaction.getAccountId())
                                    .amount(new BigDecimal(updatedExpense.cost * 1000))
                                    .approved(ynabTransaction.isApproved());
                            if (updatedExpense.deleted_at != null) {
                                Logger.log(String.format("Expense was deleted! Deleted at %s", updatedExpense.deleted_at));
                                saveTransaction.amount(new BigDecimal(0));
                            }
                            // Don't bother buffering, can't bulk update
                            Logger.log(String.format("Uploading updated expense to YNAB: %s", updatedExpense.toString()), true);
                            ynab.updateTransaction(ynabTransaction.getId(), saveTransaction);
                            continue splitwiseLoop;
                        }
                    }
                }

                // If we're here, we weren't able to find a corresponding transaction in YNAB
                // it's possible that the transaction was created & updated before this program
                // was run, add it to YNAB as a new transaction
                Logger.log(String.format("Uploading new & updated expense to YNAB: %s", updatedExpense.toString()), true);
                ynab.addTransaction(updatedExpense.cost, updatedExpense.description, updatedExpense.created_at);
            }
        }

        config.setSplitwiseLastTransactionDate(new Date()); // TODO: potential for issues here

        Logger.log("Writing config...", true);
        try {
            config.writeConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Exiting...");
        System.exit(0);
    }

}
