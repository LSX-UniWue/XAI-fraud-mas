package Behaviour;

import Agents.AgentType;
import Agents.ERP_L;
import HelperClasses.REQ;
import TimeFrame.TimeFrame;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SchedulingBehaviour extends WakerBehaviour {
    private ERP_L a =null;

    public SchedulingBehaviour(ERP_L a){
        super(a, 5);
        this.a=a;
    }



    @Override
    public void onWake() {
        //System.out.println("Scheduling");
        List<REQ> run = a.schedule.getNext();
        if(run.size()!=0){
            for(REQ r: run){
                ACLMessage ac = new ACLMessage(ACLMessage.REQUEST);
                ac.addReceiver(a.setup.getAid(r.target));
                try {
                    ac.setContentObject(r);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(ac);
            }
        }

        reset();
    }
}
