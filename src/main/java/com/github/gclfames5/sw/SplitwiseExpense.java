package com.github.gclfames5.sw;

import com.github.gclfames5.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SplitwiseExpense {

    public String description;
    public boolean paid;
    public int id, group_id;
    public double cost;
    public Date created_at, updated_on, deleted_at;

    public SplitwiseExpense(int id, int group_id, String description, boolean paid, double cost, Date created_at, Date updated_on, Date deleted_at) {
        this.id = id;
        this.group_id = group_id;
        this.description = description;
        this.paid = paid;
        this.cost = cost;
        this.created_at = created_at;
        this.updated_on = updated_on;
        this.deleted_at = deleted_at;
    }

    public static SplitwiseExpense parseJSON(JSONObject obj, long userID) {
        int id = obj.getInt("id");
        //int group_id = obj.getBigInteger("group_id").intValue();
        String desc = obj.getString("description");
        boolean paid = obj.getBoolean("payment");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date created_at = null;
        Date updated_on = null;
        Date deleted_at = null;
        try {
            created_at = simpleDateFormat.parse(obj.getString("created_at").replaceAll("T", " ").replaceAll("Z", " ").trim());
            updated_on = simpleDateFormat.parse(obj.getString("updated_at").replaceAll("T", " ").replaceAll("Z", " ").trim());

            if (obj.get("deleted_at") instanceof String) {
                deleted_at = simpleDateFormat.parse(obj.getString("deleted_at").replaceAll("T", " ").replaceAll("Z", " ").trim());
            }
        } catch (ParseException e) {
            Logger.log(e);
        }

        double cost = 0;

        if (!obj.get("deleted_by").toString().equalsIgnoreCase("null")) {
            return null;
        }

        JSONArray repayments = obj.getJSONArray("repayments");
        for (Object repaymentObj : repayments) {
            JSONObject JSONRepaymentObj = (JSONObject) repaymentObj;
            long toUserID = JSONRepaymentObj.getLong("to");
            long fromUSerID = JSONRepaymentObj.getLong("from");

            // Check if user is involved in the transaction at all
            if (toUserID != userID && fromUSerID != userID) {
                continue;
            }

            // User is involved, decide whether they are giving or receiving money
            double amount = JSONRepaymentObj.getDouble("amount");
            if (toUserID == userID) {
                cost += amount;
            } else {
                cost -= amount;
            }
        }

        if (cost == 0) {
            return null;
        }

        SplitwiseExpense expense = new SplitwiseExpense(id, 0, desc, paid, cost, created_at, updated_on, deleted_at);
        Logger.log(String.format("Parsed splitwise expense: %s", expense.toString()));
        return expense;
    }

    @Override
    public String toString() {
        return String.format("Expense: [desc: %s, cost: %f, date: %s]", this.description, this.cost, this.created_at);
    }

}
