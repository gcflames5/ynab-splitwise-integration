package com.github.gclfames5.ynab;

import java.util.Date;

public class YNABTransaction {

    public double cost;
    public String payee, desc;
    public Date date;

    public YNABTransaction(String payee, String desc, double cost, Date date) {
        this.payee = payee;
        this.desc = desc;
        this.cost = cost;
        this.date = date;
    }

}
