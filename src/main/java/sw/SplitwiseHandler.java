package sw;

import com.github.scribejava.core.model.OAuth1AccessToken;
import config.YAMLConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import splitwise.Splitwise;

import java.io.*;
import java.util.*;

public class SplitwiseHandler {

    public static void main(String[] args) throws FileNotFoundException {
        YAMLConfiguration configuration = new YAMLConfiguration("config.yml");
        configuration.openConfig();
        SplitwiseHandler sw = new SplitwiseHandler(configuration);
        sw.authenticate();
        List<sw.Expense> newExpenses = sw.getAllExpenses();
        System.out.println(newExpenses);
    }

    private Splitwise splitwise;
    private String consumerSecret, consumerKey, oauthTokenFilePath;
    private Date lastTransactionDate;
    private long userID;

    public SplitwiseHandler(YAMLConfiguration config) {
        this.consumerKey = config.getSplitwiseConsumerKey();
        this.consumerSecret = config.getSplitwiseConsumerSecret();
        this.lastTransactionDate = config.getSplitwiseLastTransactionDate();
        this.oauthTokenFilePath = config.getSplitiwiseOauthTokenFile();

        this.splitwise = new Splitwise(this.consumerKey, this.consumerSecret);
    }

    public void authenticate() {
        // Check if authentication file exists
        File oauth_token_file = new File(this.oauthTokenFilePath);
        if (!oauth_token_file.exists()) {
            System.out.println(oauth_token_file.getAbsolutePath());
            doNewAuthorization();
            authenticate();
        }

        // OAuth Token should now exist
        try {
            OAuth1AccessToken accessToken = readAccessToken();
            splitwise.util.setAccessToken(accessToken.getToken(), accessToken.getTokenSecret(), accessToken.getRawResponse());

            // Test login
            String currentUserJSON = splitwise.getCurrentUser();
            JSONObject JSONUser = (JSONObject) new JSONObject(currentUserJSON).get("user");

            this.userID = JSONUser.getLong("id");

        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("Login failed! Starting authorization process...");
            // Try again?
            oauth_token_file.delete();
            doNewAuthorization();
            authenticate();
        }
    }

    public void doNewAuthorization() {
        try {
            String authURL = splitwise.getAuthorizationUrl();
            System.out.printf("Please click the following URL to begin authorization: %s\n", authURL);

            Scanner scan = new Scanner(System.in);

            System.out.println("Please paste your oauth verifier here:");
            String oauth_verifier = scan.nextLine().replaceAll("\n", "").trim();

            //System.out.println("Please paste your oauth token here:");
            //String oauth_token = scan.nextLine().replaceAll("\n", "").trim();

            splitwise.util.setAccessToken(oauth_verifier);

            OAuth1AccessToken accessToken = (OAuth1AccessToken) splitwise.util.getAccessToken();

            writeAccessToken(accessToken);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Expense> getAllExpenses() {
        System.out.println("Fetching user");
        String expensesJSON = null;
        try {
            expensesJSON = splitwise.getExpenses(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Expense> expenses = new ArrayList<Expense>();
        JSONObject obj = new JSONObject(expensesJSON);
        JSONArray expenseArray = (JSONArray) obj.get("expenses");

        for (Object expenseObj : expenseArray) {
            JSONObject jsonExpenseObj = (JSONObject) expenseObj;
            Expense expense = Expense.parseJSON(jsonExpenseObj, this.userID);
            if (expense != null) {
                expenses.add(expense);
                //System.out.println(expense);
            }

        }

        return expenses;
    }

    public List<Expense> getNewExpenses() {
        List<Expense> newExpenses = new ArrayList<>();
        List<Expense> allExpenses = getAllExpenses();

        for (Expense e : allExpenses) {
            if (e.created_at.after(this.lastTransactionDate))
                newExpenses.add(e);
        }

        return newExpenses;
    }



    /* Token Utilities */

    private void writeAccessToken(OAuth1AccessToken token) throws IOException {
        String token_string = toString(token);
        File f = new File(this.oauthTokenFilePath);
        if (f.exists()) f.delete();
        PrintWriter out = new PrintWriter(this.oauthTokenFilePath);
        out.print(token_string);
        out.close();
    }

    private OAuth1AccessToken readAccessToken() throws IOException, ClassNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(this.oauthTokenFilePath));
        String token_string = br.readLine();
        return (OAuth1AccessToken) fromString(token_string);
    }

    /** Read the object from Base64 string. */
    private Object fromString( String s ) throws IOException,
            ClassNotFoundException {
        byte [] data = Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(  data ) );
        Object o  = ois.readObject();
        ois.close();
        return o;
    }

    /** Write the object to a Base64 string. */
    private String toString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

}

