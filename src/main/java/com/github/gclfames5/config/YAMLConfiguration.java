package com.github.gclfames5.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class YAMLConfiguration {

    public static void main(String[] args) throws IOException {
        YAMLConfiguration yamlConfiguration = new YAMLConfiguration("com.github.gclfames5.config.yml");
        yamlConfiguration.openConfig();
        yamlConfiguration.setSplitwiseLastTransactionDate(new Date());
        yamlConfiguration.writeConfig();
    }

    private File yaml_file;
    private Yaml yaml;
    private Map<String, Object> ynab_config;
    private Map<String , Object> splitwie_config;

    public YAMLConfiguration(String filepath) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        this.yaml_file = new File(filepath);
        this.yaml = new Yaml(options);
    }

    public void openConfig() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(this.yaml_file);
        Iterable<Object> yaml_contents = this.yaml.loadAll(inputStream);
        Map<String, Object> root = (Map<String, Object>) yaml_contents.iterator().next();
        this.ynab_config = (Map<String, Object>) root.get("ynab");
        this.splitwie_config = (Map<String, Object>) root.get("splitwise");
    }

    public String getYNABAccessToken() {
        String token = (String) this.ynab_config.get("access_token");
        if (token.equalsIgnoreCase("your_access_token")) {
            return "";
        }
        return token;
    }

    public String getYNABBudgetName() {
        return (String) this.ynab_config.get("budget_name");
    }

    public String getYNABAccountName() {
        return (String) this.ynab_config.get("account_name");
    }

    public String getSplitiwiseOauthTokenFile() {
        return (String) this.splitwie_config.get("oauth_token_file");
    }

    public String getSplitwiseConsumerKey() {
        return (String) this.splitwie_config.get("consumer_key");
    }

    public String getSplitwiseConsumerSecret() {
        return (String) this.splitwie_config.get("consumer_secret");
    }

    public Date getSplitwiseLastTransactionDate() {
        String dateString = (String) this.splitwie_config.get("last_transaction_date");
        if (dateString.equalsIgnoreCase("never")) {
            return new Date(0);
        }
        try {
            return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date(0);
        }
    }

    public void setSplitwiseLastTransactionDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        this.splitwie_config.put("last_transaction_date", dateFormat.format(date));
    }

    public void writeConfig() throws IOException {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> ynab = new HashMap<>();
        Map<String, String> splitwise = new HashMap<>();

        ynab.put("access_token", getYNABAccessToken());
        ynab.put("budget_name", getYNABBudgetName());
        ynab.put("account_name", getYNABAccountName());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

        splitwise.put("oauth_token_file", getSplitiwiseOauthTokenFile());
        splitwise.put("last_transaction_date", dateFormat.format(getSplitwiseLastTransactionDate()));
        splitwise.put("consumer_key", getSplitwiseConsumerKey());
        splitwise.put("consumer_secret", getSplitwiseConsumerSecret());

        map.put("ynab", ynab);
        map.put("splitwise", splitwise);

        this.yaml.dump(map, new FileWriter(this.yaml_file));
    }

}
