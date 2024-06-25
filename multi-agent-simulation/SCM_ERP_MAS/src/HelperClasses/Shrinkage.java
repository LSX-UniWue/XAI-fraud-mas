package HelperClasses;

import java.io.Serializable;

public class Shrinkage implements Serializable {
    private double likelihood;
    private double amount;

    public void setAmount(double amount){
        this.amount=amount;
    }
    public void setLikelihood(double likelihood){
        this.likelihood = likelihood;
    }

    public double getAmount() {
        return amount;
    }

    public double getLikelihood() {
        return likelihood;
    }
}
