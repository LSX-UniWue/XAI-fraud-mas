package Behaviour;

import Agents.AgentType;
import Agents.ERP_L;
import CompanyData.businessProcedure;
import HelperClasses.Booking;
import HelperClasses.REQ_FIX;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ERP_Daily extends WakerBehaviour {
    public ERP_L erp;
    private int daycounter=0;
    private double interest =0;

    public ERP_Daily(ERP_L erp, long Period){
        super(erp, Period);
        this.erp=erp;
    }
    @Override
    public void onWake(){

        // daily planning
        ACLMessage ac=new ACLMessage(ACLMessage.REQUEST);
        ac.addReceiver(erp.setup.getAid(AgentType.erp_l));
        erp.send(ac);

        // 5 dailys are one week
        daycounter++;
        //LocalTime lt = LocalTime.now();
        //System.out.println("tic from erp_daily with : "+daycounter+" time: "+lt.toString());
        // do daily stuff here
        interest += erp.setup.mainBook.getDailyInterest();
        if(daycounter>=5){ // doweekly stuff here
           // LocalTime lt = LocalTime.now();
           // System.out.println("ERP_Daily hit at : "+lt.toString());
            daycounter=0;

            Booking b = new Booking(businessProcedure.S, false, new REQ_FIX(interest));
            ACLMessage ac2 = new ACLMessage(ACLMessage.REQUEST);
            ac2.addReceiver(erp.setup.getAid(AgentType.erp_l));
            try {
                ac2.setContentObject(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
            interest=0;
            erp.send(ac2);

        }
        reset();
    }

}
