package com.github.gclfames5;

import com.github.gclfames5.config.YAMLConfiguration;
import com.github.gclfames5.sw.SplitwiseExpense;
import com.github.gclfames5.sw.SplitwiseHandler;
import com.github.gclfames5.ynab.YNABHandler;
import com.github.gclfames5.ynab.YNABTransaction;
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
        System.out.println("Authenticating Splitwise...");
        SplitwiseHandler sw = new SplitwiseHandler(config);
        sw.authenticate();
        System.out.println(String.format("Fetching spltiwise transactions since %s", config.getSplitwiseLastTransactionDate().toString()));
        List<SplitwiseExpense> expensesToProcess = sw.getAllExpenses(0, config.getSplitwiseLastTransactionDate());

        // Authenticate YNAB
        System.out.println("Authenticating YNAB...");
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
        List<YNABTransaction> ynabUploadBuffer = new ArrayList<>();
        for (SplitwiseExpense newExpense : newSplitwiseExpenses) {
            System.out.println(String.format("New transaction found: %s", newExpense.toString()));
            newExpense.description += String.format(", sw_uuid:%d", newExpense.id);
            ynabUploadBuffer.add(new YNABTransaction(newExpense.description, newExpense.description, newExpense.cost, newExpense.created_at));
        }


        // For all transactions that seem to be updated:
        //  - Try to match them with a YNAB expense and perform update
        //  - Otherwise, treat as a new transaction
        if (updatedSplitwiseExpenses.size() > 0) {
            // Get the minimum number of transactions from YNAB to search through UUIDs
            List<TransactionDetail> ynabTransactions = ynab.getTransactionsSince(new Date()); //ynab.getTransactionsSince(oldestCreationDate);
            splitwiseLoop: for (SplitwiseExpense updatedExpense : updatedSplitwiseExpenses) {
                // Search for corresponding expense in YNAB
                ynabLoop: for (TransactionDetail ynabTransaction : ynabTransactions) {
                    String[] search = ynabTransaction.getMemo().split("sw_uuid:");
                    // Transactions may exist that have not been tagged with the uuid
                    // ignore those entries
                    if (search.length > 0) {
                        int uuid = Integer.valueOf(search[1]);
                        // Check if this uuid matches the splitwise expense we're looking at
                        // If it matches, update this transaction
                        if (uuid == updatedExpense.id) {
                            System.out.println(String.format("Matched updated transaction: %s with uuid %s",
                                    updatedExpense.toString(), ynabTransaction.getAccountId().toString()));
                            SaveTransaction saveTransaction = new SaveTransaction();
                            saveTransaction.accountId(ynabTransaction.getAccountId())
                                            .amount(new BigDecimal(updatedExpense.cost * 1000))
                                            .approved(ynabTransaction.isApproved());
                            if (updatedExpense.deleted_at != null) {
                                saveTransaction.amount(new BigDecimal(0));
                            }
                            // Don't bother buffering, can't bulk update
                            ynab.updateTransaction(ynabTransaction.getId(), saveTransaction);
                            continue splitwiseLoop;
                        }
                    }
                }

                // If we're here, we weren't able to find a corresponding transaction in YNAB
                // it's possible that the transaction was created & updated before this program
                // was run, add it to YNAB as a new transaction
                ynabUploadBuffer.add(new YNABTransaction(updatedExpense.description, updatedExpense.description, updatedExpense.cost, updatedExpense.created_at));
            }
        }

        // Handle deleted transactions
        for (SplitwiseExpense deletedExpense : deletedSplitwiseExpenses) {
            // TODO: implement
        }

        // Upload pending transactions to YNAB
        if (ynabUploadBuffer.size() > 0) {
            ynab.addTransactions(ynabUploadBuffer);
        }
        config.setSplitwiseLastTransactionDate(new Date()); // TODO: potential for issues here

        System.out.println("Writing config...");
        try {
            config.writeConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Exiting...");
        System.exit(0);
    }

}
