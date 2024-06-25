package Behaviour;

import Agents.AgentType;
import Agents.ERP_L;
import HelperClasses.Booking;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;

public class ERPL_PAY extends WakerBehaviour {

    private ERP_L a = null;
    private Booking b=null;
    public ERPL_PAY(ERP_L a, long timeout, Booking b) {
        super(a, timeout);
        this.a=a;
        this.b=b;
    }

    @Override
    public void onWake(){
        // pay bills
        ACLMessage acl = new ACLMessage(ACLMessage.REQUEST);
        acl.addReceiver(a.setup.getAid(AgentType.erp_l));
        try {
            acl.setContentObject(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        a.send(acl);
    }






}
