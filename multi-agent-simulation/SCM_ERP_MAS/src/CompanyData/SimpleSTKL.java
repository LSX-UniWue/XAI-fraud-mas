package CompanyData;

import java.io.Serializable;

public class SimpleSTKL implements Serializable {

    public String target;
    public Ressource[] ress;
    public Double[] amount;
    public int capacity;
    public double setuptime;

    public SimpleSTKL(int size){
        this.ress=new Ressource[size];
        this.amount= new Double[size];
    }



}
