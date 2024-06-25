package Agents;

import Behaviour.SC_Behaviour;
import PassedObjects.SetUp;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class SC extends Agent {
    public SetUp setup;

    protected void setup(){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(AgentType.sc.toString());
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
        SC_Behaviour behav = new SC_Behaviour(this);
        this.addBehaviour(behav);
    }
}
