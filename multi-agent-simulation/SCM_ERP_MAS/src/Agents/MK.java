package Agents;

import Behaviour.MK_Behaviour;
import PassedObjects.SetUp;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class MK extends Agent {
    public SetUp setup;

    protected void setup(){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(AgentType.mk.toString());
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
        MK_Behaviour behav = new MK_Behaviour(this);
        this.addBehaviour(behav);
    }
}
