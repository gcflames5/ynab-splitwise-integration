package com.github.gclfames5.ynab;

import com.github.gclfames5.config.YAMLConfiguration;
import com.github.gclfames5.log.Logger;
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
            Logger.log("Exception raised when authenticating!");
            Logger.log(e);
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
            Logger.log(e);
        }
    }

    public List<TransactionDetail> getTransactionsSince(Date since) {
        try {
            LocalDate localDate = DateTimeUtils.toLocalDate(new java.sql.Date(since.getTime()));
            TransactionsResponse transactionsResponse
                    = this.transactionsApi.getTransactionsByAccount(this.budgetUUID, this.accountUUID, localDate);
            return transactionsResponse.getData().getTransactions();
        } catch (ApiException e) {
            Logger.log(e);
        }
        return null;
    }

    public void updateTransaction(UUID transactionUUID, SaveTransaction saveTransaction) {
        try {
            this.transactionsApi.updateTransaction(budgetUUID, transactionUUID, new SaveTransactionWrapper().transaction(saveTransaction));
        } catch (ApiException e) {
            Logger.log(e);
        }
    }


}
