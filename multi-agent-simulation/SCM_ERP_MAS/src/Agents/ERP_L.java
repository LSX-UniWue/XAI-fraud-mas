package Agents;

import Behaviour.ERP_L_Behaviour;
import HelperClasses.REQ;
import HelperClasses.Schedule;
import PassedObjects.SetUp;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ERP_L extends Agent{
    public SetUp setup;
    public Schedule schedule;

    public HashMap<String, Integer> inventory= new HashMap<>();
    public ArrayList<String> bookkeeping= new ArrayList<>();

    protected void setup(){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(AgentType.erp_l.toString());
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
        ERP_L_Behaviour behav = new ERP_L_Behaviour(this);
        this.addBehaviour(behav);
    }

}
