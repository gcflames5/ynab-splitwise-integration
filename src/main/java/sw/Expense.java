package sw;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Expense {

    public String description;
    public boolean paid;
    public int id, group_id;
    public double cost;
    public Date created_at;

    public Expense(int id, int group_id, String description, boolean paid, double cost, Date created_at) {
        this.id = id;
        this.group_id = group_id;
        this.description = description;
        this.paid = paid;
        this.cost = cost;
        this.created_at = created_at;
    }

    public static Expense parseJSON(JSONObject obj, long userID) {
        int id = obj.getInt("id");
        //int group_id = obj.getBigInteger("group_id").intValue();
        String desc = obj.getString("description");
        boolean paid = obj.getBoolean("payment");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date created_at = null;
        try {
            created_at = simpleDateFormat.parse(obj.getString("created_at").replaceAll("T", " ").replaceAll("Z", " ").trim());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        double cost = 0;


        if (!obj.get("deleted_by").toString().equalsIgnoreCase("null")) {
            return null;
        }

        JSONArray repayments = obj.getJSONArray("repayments");
        for (Object repaymentObj : repayments) {
            JSONObject JSONRepaymentObj = (JSONObject) repaymentObj;
            long toUserID = JSONRepaymentObj.getLong("to");
            double amount = JSONRepaymentObj.getDouble("amount");
            if (toUserID == userID) {
                cost += amount;
            } else {
                cost -= amount;
            }
        }

        return new Expense(id, 0, desc, paid, cost, created_at);
    }

    @Override
    public String toString() {
        return String.format("sw.Expense: [desc: %s,  cost: %f, date: %s]", this.description, this.cost, this.created_at);
    }

}
