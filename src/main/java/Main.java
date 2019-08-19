import config.YAMLConfiguration;
import sw.Expense;
import ynab.YNABHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
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
        //ynab.addTransaction(12.31, "This is a test", new Date());

        if (newExpenses.size() > 0) {
            Date latestDate = new Date(0);
            for (Expense expense : newExpenses) {
                if (expense.created_at.after(latestDate)) {
                    latestDate = expense.created_at;
                }

                if (expense.description.equalsIgnoreCase("Payment") || expense.description.equalsIgnoreCase("Settle All Balances")) {
                    System.out.println("Ignoring settle up expense: " + expense.toString());
                    continue;
                }

                System.out.println("New expense found: " + expense.toString());
                ynab.addTransaction(expense.cost, expense.description, expense.created_at);
            }
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
