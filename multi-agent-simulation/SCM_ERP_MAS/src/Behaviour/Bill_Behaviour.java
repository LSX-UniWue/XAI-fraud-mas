package Behaviour;

import Agents.AgentType;
import Agents.Markets;
import CompanyData.businessProcedure;
import HelperClasses.Booking;
import HelperClasses.REQ;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;

public class Bill_Behaviour extends WakerBehaviour {
    private Markets a ;
    private AgentType agent ;
    private REQ r; // combined
    private businessProcedure gv;

    public Bill_Behaviour(Markets a, long period, AgentType agent , REQ r , businessProcedure gv) {
        super(a, period);
        this.a=a;
        this.r=r;
        this.agent=agent;
        this.gv=gv;
    }

@Override
    public void onWake(){
        // make booking
    Booking b = null;
    if(gv == businessProcedure.WE){
        b = new Booking(gv, true   , r);
    }else{
        b = new Booking(gv, false, r);
    }
        // send to erpl

    ACLMessage ac = new ACLMessage(ACLMessage.REQUEST);
    ac.addReceiver(a.setup.getAid(agent));
    try {
        ac.setContentObject(b);
    } catch (IOException e) {
        e.printStackTrace();
    }
    a.send(ac);

}
}
