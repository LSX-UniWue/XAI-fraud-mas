package PassedObjects;

import Agents.AgentType;
import CompanyData.*;
import HelperClasses.Shrinkage;
import jade.core.AID;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SetUp implements Serializable {

    private HashMap<AgentType, AID> aidmap ;
    public AID getAid(AgentType at){
        return aidmap.get(at);
    }

    public void setAidmap(HashMap<AgentType, AID> aidmap){
        this.aidmap=aidmap;
    }

    // here comes the whole company data for setup
    public AFA afa;
    public HashMap<String,Ressource> ressources = new HashMap<>();
    public HashMap<String, Vendor> vendors= new HashMap<>();
    public HashMap<String,Product> products = new HashMap<>();
    public GozintoGraphs graphs;
    public MainBook mainBook;
    public double shipmentCost;
    public int[] warehousecap;
    public Map<String, Integer> warehousetype;
    public String productDataType;
    public double storageSecurity ;
    public String productionStrategy;
    public int maxInv;
    public String forecastingMethod ;
    public double margin;
    public double marginCon;
    public Map<String, Double> fraudLikelihood = new HashMap<>();
    public Map<String, Double> fraudvalues = new HashMap<>();
    public Shrinkage shrinkage ;
    public Marketfunction marketfunction;
    public int initdays;
    public int killday;
    public double dataPriceIncrease;
    public double minlots;
    public double maxlots;
    public HashMap<String, Random> randmap;
    public double maxCash;
    public boolean printBySM;
    public double cashReserve;
    public int paymenttime;

    public long initstocks;
    public long initland;
    public long initbuildings;
    public long initmachines;
    public long initloans;
    public long baselabor ;
    public long baseoverhead ;
    public long basesga ;







    // testing stuff belongs here




}
