package Agents;

import HelperClasses.ControlUI;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class GUIController extends Agent {
    public ControlUI controlUI;

    private AID ERP_LAID;
    protected void setup(){
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ctrl");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try{
            DFService.register(this,dfd);
        }catch (FIPAException e){
            System.err.println(getLocalName() +" registration with Agents.DF unsucceeded. Reason: "+e.getMessage());
            doDelete();
        }

        DFAgentDescription df = new DFAgentDescription();
        ServiceDescription sds = new ServiceDescription();
        sds.setType(AgentType.erp_l.toString());
        df.addServices(sds);
        DFAgentDescription[] dfres = new DFAgentDescription[1];
        try {
            dfres = DFService.search(this, df);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        ERP_LAID = dfres[0].getName();

        System.out.println("Agent :" + getAID() +" "+getAID().getLocalName()+" "+getAID().getName()+" is ready");

        controlUI = new ControlUI(this);
    }

    public void startStop(){
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(ERP_LAID);
        this.send(msg);
    }
}
