Splitwise Integration for YNAB
====
Splitwise poses an interesting problem for YNAB, where a single settle up transaction can represent a lot of other transactions rolled into one. This makes it extremely hard to budget Splitwise transactions using YNAB. This integration solves that problem by importing Splitwise transactions into YNAB, allowing you to categorize the transactions within YNAB.

Word of Caution
----
This integration treats your Splitwise balance as an actual account balance to help categorize expenses, but be carefull about treating these items as reimbursed before they actually are. If you are not mindful, you may be relying on money that has not yet been returned to you.

Using the Integration
----

### How I Use This Tool
The tool creates transactions in YNAB for every transaction in Splitwise. I categorize each Splitwise transaction into a budget category in order to track actual spending. Then, the settle up transactions (both on the Splitwise side and the actual payment method used to fulfill the settle up) are categorized into "To Be Budgeted," which effectively ignores them in the budget but keeps the account balances correct. Here's an example to illustrate the methodology:

1. I lend Alice $25.00 for "Eating Out" (total charge: $50.00 on credit card)
2. Bob lends me $45.00 for "Household Goods"
3. I use Splitwise's debt simplification feature to pay Bob $20.00 directly

Here's what those transactions would look like in Splitwise: 

| Account     | Transaction              |  Category       | Inflow |  Outflow |
| ----------- | ------------------------ | --------------- | ------ | -------- |
| Splitwise   | Eating Out (w/ Alice)    | Eating Out      | $25    |          |
| Credit Card | Eating Out (w/ Alice)    | Eating Out      |        | $50      |
| Splitwise   | Household Goods (w/ Bob) | Household Goods |        | $45      |
| Splitwise   | Settle up                | To Be Budgeted  | $20    |          |
| Venmo       | Settle up                | To Be Budgeted  |        | $20      | 

The two settle up transactions cancel out (while still correclty updating their accounts' balances) but don't affect our budget. The Splitwise and Credit Card transactions total $25 spent on Eating Out, which is the $50 from the credit card minus the $25 reimbursement from Alice, keeping our budget correct.

### Configuration

Create a file called `config.yml` in the same directory as the .jar file with the following contents:

```yaml
splitwise:
  consumer_key: <consumer_key> # Splitwise Consumer Key
  consumer_secret: <consumer_secret> # Splitwise Consumer Secret
  oauth_token_file: splitwise_token.oauth # File to store oauth token after authorization
  last_transaction_date: never # Automatically updated by the program, last splitwie transaction parsed
ynab:
  access_token: <personal_access_token> # Personal Access Token for YNAB
  budget_name: My Budget # Name of the YNAB Budget
  account_name: Splitwise # Name of the account where Splitwise transactions will be added
```

In order to populate the configuration fields, you'll need to register an application with the Splitwise API in order to obtain a consumer key and secret pair, and request a personal access token from YNAB. 

#### Authorizing

In order to authorize the Splitwise app, you'll need to click the authorization URL. After authorizing, press "Show out of band data" and copy the verifier into the console where prompted.

### Running the Integration
Running the integration will transfer any new transactions from Splitwise to YNAB and then terminate. I suggest using cron or something like it to run the jar at regular intervals.

### Dependencies
Most dependencies will be imported via Maven and specified in the project's `pom.xml`. However, you will need to manually install [this repository](https://github.com/gcflames5/ynab-sdk) in your local maven repository by following the instlalation steps in its README.md.
