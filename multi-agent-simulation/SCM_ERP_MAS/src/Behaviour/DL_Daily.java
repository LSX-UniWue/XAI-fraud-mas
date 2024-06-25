package Behaviour;


import Agents.AgentType;
import Agents.DL;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

public class DL_Daily extends WakerBehaviour {

    private DL a=null;

    public DL_Daily(DL a, long period){
        super(a, period);
        this.a=a;
    }

    @Override
    public void onWake(){
       // System.out.println("yehaw");
        ACLMessage acl = new ACLMessage(ACLMessage.CONFIRM);
        acl.addReceiver(a.setup.getAid(AgentType.dl));
        a.send(acl);
        reset();
    }




}
