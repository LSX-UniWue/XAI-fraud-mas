package CompanyData;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class GozintoGraphs implements Serializable {

    private  ArrayList<SimpleSTKL> stikls = new ArrayList<>();


    public void addStkl(SimpleSTKL stkl){
        this.stikls.add(stkl);
    }

    public SimpleSTKL getStkl(String target){
        for(SimpleSTKL stkl: this.stikls){
            if(stkl.target.equals(target)){
                return stkl;
            }
        }
        return null;
    }







}
