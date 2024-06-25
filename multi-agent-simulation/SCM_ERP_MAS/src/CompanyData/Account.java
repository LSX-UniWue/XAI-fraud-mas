package CompanyData;

import java.io.Serializable;

public class Account implements Serializable {
    private String accountnumber;
    private String type;
    private String name;
    private double[] balance= new double[2]; //0=soll, 1=haben

    public Account(String accountnumber){
        this.accountnumber=accountnumber;
    }

    public String getAccountnumber(){
        return this.accountnumber;
    }
    public String getType(){
        return this.type;
    }
    public void setType(String type){
        this.type=type;
    }
    public void setName(String name){
        this.name=name;
    }
    public String getName(){
        return this.name;
    }



    public void book(AccountBooking book, double amount){
        if(book== AccountBooking.HABEN){
            this.balance[1] += amount;
        }else{
            this.balance[0] += amount;
        }
    }

    public double get(AccountBooking booking){
        if(booking == AccountBooking.HABEN){
            return this.balance[1];
        }else {
            return this.balance[0];
        }
    }

    public double getSaldo(){
        if(balance[0]>balance[1]){
            return balance[0]-balance[1];
        }else{
            return balance[1]-balance[0];
        }
    }




}
