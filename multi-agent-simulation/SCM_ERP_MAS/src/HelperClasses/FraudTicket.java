package HelperClasses;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FraudTicket implements Serializable {

    private Map<String, Boolean> types = new HashMap<>();


    public void copy(FraudTicket oldticket){
        for(String s : oldticket.types.keySet()){
            this.types.put(s, oldticket.types.get(s));
        }
    }












}
