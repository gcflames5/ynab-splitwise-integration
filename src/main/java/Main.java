import config.YAMLConfiguration;
import sw.Expense;
import sw.SplitwiseHandler;
import ynab.YNABHandler;
import ynab.YNABTransaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    public static String configPath = "<UNSPECIFIED>";

    public static void main(String[] args) {

        if (args.length <= 0) {
            System.out.println("Please specify the path of the config file as a command line argument!");
            System.exit(0);
        }
        configPath = args[0];

        // Read Config
        System.out.println("Reading configuration...");
        File f = new File(configPath);
        System.out.println(f.exists());
        YAMLConfiguration config = new YAMLConfiguration(configPath);
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
            List<YNABTransaction> transactions = new ArrayList<YNABTransaction>();

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
        System.exit(0);
    }

}
