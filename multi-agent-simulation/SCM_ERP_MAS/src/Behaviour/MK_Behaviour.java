package Behaviour;

import Agents.AgentType;
import Agents.MK;
import CompanyData.businessProcedure;
import HelperClasses.Booking;
import HelperClasses.ControlUI;
import HelperClasses.REQ;
import HelperClasses.REQ_MK;
import PassedObjects.SetUp;
import TimeFrame.TimeFrame;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MK_Behaviour extends CyclicBehaviour {


    private MK a;
    private int prodCounter=0;
    private String lastProduced=null;
    private Random rand = new Random();
    public MK_Behaviour (MK a){
        this.a=a;
    }

    @Override
    public void action() {
        ACLMessage aclm = getAgent().receive();
        if(aclm != null){
            if(a.setup ==null && aclm.getPerformative() == ACLMessage.INFORM){
                Object o=null;
                try {
                    o = aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                if(o instanceof SetUp){
                    a.setup= ((SetUp) o );
                }
                this.rand.setSeed(a.setup.randmap.get("MK").nextLong());
                ACLMessage answ = new ACLMessage(ACLMessage.CONFIRM);
                answ.addReceiver(a.setup.getAid(AgentType.erp_l));
                a.send(answ);


            }else if(aclm.getPerformative() == ACLMessage.REQUEST){
                REQ_MK o=null;
                try {
                    o = (REQ_MK) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                if(lastProduced==null){ //setup machines
                    lastProduced=o.items.get(0);
                    ControlUI.Instance.ChangeLastProduced(lastProduced);
                    try {
                        Thread.sleep((long) (a.setup.graphs.getStkl(lastProduced).setuptime*TimeFrame.DAY.toTime()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("lastproduced: " + lastProduced);
                    ControlUI.Instance.ChangeLastProduced(lastProduced);
                    if(!lastProduced.equals(o.items.get(0))){
                     lastProduced=o.items.get(0);
                     System.out.println("lastproduced: "+lastProduced);
                     ControlUI.Instance.ChangeLastProduced(lastProduced);
                     long f = (long) (a.setup.graphs.getStkl(lastProduced).setuptime*TimeFrame.DAY.toTime());
                     if (f != 0) {
                         try {
                             Thread.sleep(f);
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
                     }
                    }
                }

                // possibility for fluctuation : variation on wap
                // get stkl calc you much you need and what
                // write into ticket o
                o.educts = new ArrayList<>();
                o.eductamount= new HashMap<>();
                for(int i =0; i< o.stkl.ress.length; i++){
                    o.educts.add(o.stkl.ress[i].identifier);
                    o.eductamount.put(o.stkl.ress[i].identifier, o.stkl.amount[i]*o.itemAmount.get(o.items.get(0)));
                }


                // fraud theft -> acess to mainbook via a.setup.mainbook.getinven.


                Booking b = new Booking(businessProcedure.WAP, true, o);

                // book inventory wap
                ACLMessage al= new ACLMessage(ACLMessage.REQUEST);
                al.addReceiver(a.setup.getAid(AgentType.erp_l));
                try {
                    al.setContentObject(b);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(al);

            }else if(aclm.getPerformative() == ACLMessage.PROPAGATE) {
                REQ_MK o = null;
                try {
                    o = ((REQ_MK) (( (Booking) aclm.getContentObject()).getREQ()));
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

                if(o.itemAmount.get(o.items.get(0)) > 0){
                    LocalDateTime nowtime = LocalDateTime.now();
                    System.out.println("make starts producing at: "+nowtime.toString());
                    ControlUI.Instance.AddInfoLogEntry("make starts producing at: "+nowtime.toString());
                try { // actual production
                    int dailycapacity = a.setup.graphs.getStkl(o.items.get(0)).capacity; // change to machine formula
                    double timereq = (o.itemAmount.get(o.items.get(0))) / dailycapacity; // in days
                    Thread.sleep(((long) (timereq * TimeFrame.DAY.toTime())));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // book product wep
                Booking b2 = new Booking(businessProcedure.WEP, true, o);


                ACLMessage al2 = new ACLMessage(ACLMessage.REQUEST);
                al2.addReceiver(a.setup.getAid(AgentType.erp_l));
                try {
                    al2.setContentObject(b2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(al2);
            }

                // System.out.println("mk confirms at: "+LocalTime.now());
                //confirm
                ACLMessage ac = new ACLMessage(ACLMessage.CONFIRM);
                ac.addReceiver(a.setup.getAid(AgentType.erp_l));
                try {
                    ac.setContentObject(o);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(ac);


            }




        }
        block(5);
    }

}
