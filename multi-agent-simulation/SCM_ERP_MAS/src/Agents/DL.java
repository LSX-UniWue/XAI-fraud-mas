package Agents;

import Behaviour.DL_Behaviour;
import PassedObjects.SetUp;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class DL extends Agent {
    public SetUp setup;

    protected void setup(){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(AgentType.dl.toString());
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
        DL_Behaviour behav = new DL_Behaviour(this);
        this.addBehaviour(behav);
    }
}
