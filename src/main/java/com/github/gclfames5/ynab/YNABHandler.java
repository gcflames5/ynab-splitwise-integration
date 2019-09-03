package com.github.gclfames5.ynab;

import com.github.gclfames5.config.YAMLConfiguration;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.LocalDate;
import ynab.client.api.AccountsApi;
import ynab.client.api.BudgetsApi;
import ynab.client.api.TransactionsApi;
import ynab.client.invoker.ApiClient;
import ynab.client.invoker.ApiException;
import ynab.client.invoker.Configuration;
import ynab.client.invoker.auth.ApiKeyAuth;
import ynab.client.model.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class YNABHandler {

    /*public static void main(String[] args) throws FileNotFoundException {
        YAMLConfiguration com.github.gclfames5.config = new YAMLConfiguration("com.github.gclfames5.config.yml");
        com.github.gclfames5.config.openConfig();
        ynab.YNABHandler ynab = new ynab.YNABHandler(com.github.gclfames5.config);
        ynab.authenticate();
        ynab.addTransaction(12.31, "This is a test", new Date());
    }*/

    private String budgetName;
    private String accountName;
    private String accessToken;

    private ApiClient defaultClient;
    private BudgetsApi budgetsApi;
    private TransactionsApi transactionsApi;
    private AccountsApi accountsApi;

    private UUID budgetUUID, accountUUID;

    public YNABHandler(YAMLConfiguration config) {
        this.budgetName = config.getYNABBudgetName();
        this.accountName = config.getYNABAccountName();
        this.accessToken = config.getYNABAccessToken();

        this.defaultClient = Configuration.getDefaultApiClient();
    }

    public void authenticate() {
        // Configure API key authorization: bearer
        ApiKeyAuth bearer = (ApiKeyAuth) defaultClient.getAuthentication("bearer");
        bearer.setApiKey(this.accessToken);
        bearer.setApiKeyPrefix("Bearer");

        // Initialize API objects
        this.budgetsApi = new BudgetsApi();
        this.transactionsApi = new TransactionsApi();
        this.accountsApi = new AccountsApi();

        // Find correct budget
        try {
            BudgetSummaryResponse budgetSummaryResponse = budgetsApi.getBudgets();
            for (BudgetSummary budgetSummary : budgetSummaryResponse.getData().getBudgets()) {
                if (budgetSummary.getName().equalsIgnoreCase(budgetName)) {
                    this.budgetUUID = budgetSummary.getId();
                    break;
                }
            }
            if  (this.budgetUUID == null) throw new RuntimeException("No'" + budgetName +  "' budget found in YNAB!");

            AccountsResponse accountsResponse = accountsApi.getAccounts(budgetUUID);
            for (Account acct : accountsResponse.getData().getAccounts()) {
                if (acct.getName().equalsIgnoreCase("Splitwise")) {
                    this.accountUUID = acct.getId();
                    break;
                }
            }
            if  (this.accountUUID == null) throw new RuntimeException("No'" + accountName +  "' account found in YNAB!");

        } catch (ApiException e) {
            System.err.println("Exception raised when authenticating!");
            e.printStackTrace();
        }

    }

    public SaveTransaction toSaveTransaction(YNABTransaction transaction) {
        SaveTransaction saveTransaction = new SaveTransaction();
        saveTransaction.setAccountId(this.accountUUID);
        saveTransaction.setMemo(transaction.desc);
        saveTransaction.setAmount(new BigDecimal(transaction.cost * 1000));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        saveTransaction.setDate(LocalDate.parse(dateFormat.format(transaction.date)));
        saveTransaction.setApproved(false);
        return saveTransaction;
    }

    public void addTransactions(List<YNABTransaction> transactions) {

        List<SaveTransaction> saveTransactionList = new ArrayList<>();
        for (YNABTransaction transaction : transactions) {
            saveTransactionList.add(toSaveTransaction(transaction));
        }

        try {
            BulkTransactions bulkTransactions = new BulkTransactions();
            bulkTransactions.setTransactions(saveTransactionList);
            transactionsApi.bulkCreateTransactions(this.budgetUUID, bulkTransactions);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addTransaction(double amount, String description, Date date) {
        try {
            SaveTransaction transaction = new SaveTransaction();
            transaction.setAccountId(this.accountUUID);
            transaction.setAmount(new BigDecimal(amount * 1000));
            transaction.setApproved(false);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            transaction.setDate(LocalDate.parse(dateFormat.format(date)));
            transaction.setMemo(description);
            transactionsApi.createTransaction(this.budgetUUID, new SaveTransactionWrapper().transaction(transaction));
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    public List<TransactionDetail> getTransactionsSince(Date since) {
        try {
            LocalDate localDate = DateTimeUtils.toLocalDate(new java.sql.Date(since.getTime()));
            TransactionsResponse transactionsResponse
                    = this.transactionsApi.getTransactionsByAccount(this.budgetUUID, this.accountUUID, localDate);
            //= this.transactionsApi.getTransactions(this.budgetUUID, localDate, "");
            return transactionsResponse.getData().getTransactions();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateTransaction(UUID transactionUUID, SaveTransaction saveTransaction) {
        try {
            this.transactionsApi.updateTransaction(budgetUUID, transactionUUID, new SaveTransactionWrapper().transaction(saveTransaction));
        } catch (ApiException e) {

            e.printStackTrace();
        }
    }


}
