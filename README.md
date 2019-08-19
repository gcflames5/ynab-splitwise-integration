Splitwise Integration for YNAB
====
Splitwise poses an interesting problem for YNAB, where a single settle up transaction can represent a lot of other transactions rolled into one. This makes it extremely hard to budget Splitwise transactions using YNAB. This integration solves that problem by importing Splitwise transactions into YNAB, allowing you to categorize the transactions within YNAB.

Word of Caution
----
This integration treats your Splitwise balance as an actual account balance to help categorize expenses, but be carefull about treating these items as reimbursed before they actually are. If you are not mindful, you may be relying on money that has not yet been returned to you.

Using the Integration
----

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
