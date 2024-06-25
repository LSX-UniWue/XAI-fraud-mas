package HelperClasses;

import CompanyData.businessProcedure;

import java.awt.print.Book;
import java.io.Serializable;

public class Booking implements Serializable {

    private businessProcedure gv;
    private boolean inventoryChange=false;
    private REQ r ;
    public Booking(businessProcedure gv, boolean inventoryChange, REQ r ){
        this.gv=gv;
        this.inventoryChange=inventoryChange;
        this.r=r;
    }

    public businessProcedure getGV(){
        return gv;
    }

    public boolean getInventorychange(){
        return inventoryChange;
    }

    public REQ getREQ(){
        return r;
    }







}
