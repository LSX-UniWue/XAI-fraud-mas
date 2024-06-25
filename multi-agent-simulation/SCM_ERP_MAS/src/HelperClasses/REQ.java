package HelperClasses;

import Agents.AgentType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class REQ implements Serializable {
    public boolean done;
    public ScheduleEnum active=ScheduleEnum.NONACTIVE;
    public final String uniqueID = UUID.randomUUID().toString();
    public AgentType target;
    public ArrayList<String> items=new ArrayList<>();
    public HashMap<String, Double> itemAmount=new HashMap<>();
    public HashMap<String, Double> pricelist=new HashMap<>();
    public HashMap<String, Double> demandvolume = new HashMap<>();
    public boolean Fraudulent = false;
    public Map<String, Boolean> Fraudtypes = new HashMap<>();
    public MarketHistory eductHist;
    public MarketHistory productHist;
    public double fixedCost;
    public HashMap<String, Double> realAmounts= new HashMap<>();
    public HashMap<String, Double> maps= new HashMap<>();

    public void combineWithMarketInfo(Map<String, Double> pricelist){
        // combine here
        for(String item : this.items){
            this.pricelist.put(item,pricelist.get(item));
        }
    }

    public void addHistory(Map<String, Double> prices, Map<String, Double> volumes, MarketHistory educt, MarketHistory product){
        this.eductHist = educt.copy();
        this.productHist=product.copy();

        for(String s : prices.keySet()){
            if(eductHist.isPresent(s)){
                eductHist.addData(s, prices.get(s));
            }else if (productHist.isPresent(s)){
                productHist.addData(s, prices.get(s), volumes.get(s));
            }
        }
    }





}
