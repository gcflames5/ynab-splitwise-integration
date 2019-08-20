import config.YAMLConfiguration;
import org.threeten.bp.LocalDate;
import sw.Expense;
import sw.SplitwiseHandler;
import ynab.YNABHandler;
import ynab.YNABTransaction;
import ynab.client.model.BulkTransactions;
import ynab.client.model.SaveTransaction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        // Read Config
        System.out.println("Reading configuration...");
        YAMLConfiguration config = new YAMLConfiguration("config.yml");
        try {
            config.openConfig();
        } catch (FileNotFoundException e) {
            System.out.println("No config file found!");
            e.printStackTrace();
        }

        // Authenticate Splitwise
        System.out.println("Authenticating Splitwise...");
        SplitwiseHandler sw = new SplitwiseHandler(config);
        sw.authenticate();
        List<Expense> newExpenses = sw.getNewExpenses();

        // Authenticate YNAB
        System.out.println("Authenticating YNAB...");
        YNABHandler ynab = new ynab.YNABHandler(config);
        ynab.authenticate();

        if (newExpenses.size() > 0) {
            Date latestDate = new Date(0);
            List<YNABTransaction> transactions = new ArrayList<>();

            for (Expense expense : newExpenses) {
                if (expense.created_at.after(latestDate)) {
                    latestDate = expense.created_at;
                }

                System.out.println("New expense found: " + expense.toString());
                transactions.add(new YNABTransaction(expense.description, expense.description, expense.cost, expense.created_at));
            }
            ynab.addTransactions(transactions);
            config.setSplitwiseLastTransactionDate(latestDate);
        }

        System.out.println("Writing config...");
        try {
            config.writeConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Exiting...");

    }

}
