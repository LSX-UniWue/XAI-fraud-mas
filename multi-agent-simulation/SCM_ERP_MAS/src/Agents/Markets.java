package Agents;

import Behaviour.Markets_Behaviour;
import HelperClasses.MarketHistory;
import PassedObjects.SetUp;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Markets extends Agent{
    public SetUp setup;
    //public ConcurrentHashMap<String, Double> prices = new ConcurrentHashMap<>();
    public Map<String, Double> prices = new HashMap<>();
    public Map<String, Double> volumes = new HashMap<>();
    public MarketHistory historyEduct;
    public MarketHistory historyProduct;
    public Random random = new Random();

    protected void setup(){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(AgentType.markets.toString());
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try{
            DFService.register(this,dfd);
        }catch (FIPAException e){
            System.err.println(getLocalName() +" registration with Agents.DF unsucceeded. Reason: "+e.getMessage());
            doDelete();
        }
        System.out.println("Agent :" + getAID() +" "+getAID().getLocalName()+" "+getAID().getName()+" is ready");
        // new instance of behaviour
        Markets_Behaviour behav = new Markets_Behaviour(this);
        this.addBehaviour(behav);
    }

}
