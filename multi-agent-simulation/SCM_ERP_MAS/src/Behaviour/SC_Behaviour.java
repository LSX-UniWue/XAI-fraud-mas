package Behaviour;

import Agents.AgentType;
import Agents.SC;
import CompanyData.businessProcedure;
import FIPA.DateTime;
import HelperClasses.Booking;
import HelperClasses.ControlUI;
import HelperClasses.REQ;
import PassedObjects.SetUp;
import TimeFrame.TimeFrame;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

public class SC_Behaviour extends CyclicBehaviour {

    private SC a;
    private REQ r ;
    private Random rand = new Random();
    public SC_Behaviour (SC a){
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
                if(o instanceof SetUp) {
                    a.setup = ((SetUp) o);
                }
                this.rand.setSeed(a.setup.randmap.get("SC").nextLong());
                ACLMessage aclmres = new ACLMessage(ACLMessage.CONFIRM);
                aclmres.addReceiver(a.setup.getAid(AgentType.erp_l));
                a.send(aclmres);
            }else if (aclm.getPerformative() == ACLMessage.REQUEST && aclm.getSender().equals(a.setup.getAid(AgentType.erp_l))){
                //LocalDateTime nowtime = LocalDateTime.now();
                //System.out.println("sc called from erp_l at: "+nowtime.toString());
                REQ o=null;
                try {
                    o = (REQ) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                ACLMessage ac = new ACLMessage(ACLMessage.QUERY_REF);
                try {
                    ac.setContentObject(o);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ac.addReceiver(a.setup.getAid(AgentType.markets));
                a.send(ac);
            }else if (aclm.getPerformative() == ACLMessage.INFORM_REF) {
                // do nothing on this message
            }else if(aclm.getPerformative() == ACLMessage.REQUEST && aclm.getSender().equals( a.setup.getAid(AgentType.markets))){
                Booking r = null;
                try {
                    r =(Booking) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

                if(r.getGV() == businessProcedure.S){
                    ACLMessage ac = new ACLMessage(ACLMessage.REQUEST);
                    ac.addReceiver(a.setup.getAid(AgentType.erp_l));
                    try {
                        ac.setContentObject(r);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    a.send(ac);
                }else{ // GV== WE
                    ACLMessage ac = new ACLMessage(ACLMessage.REQUEST);
                    ac.addReceiver(a.setup.getAid(AgentType.erp_l));
                    if(r.getREQ().Fraudtypes.containsKey("Bill") && r.getREQ().Fraudtypes.get("Bill")){
                        r.getREQ().Fraudtypes.remove("Bill");
                    }

                    double ik = rand.nextDouble();
                    if(ik >= 1- (a.setup.fraudLikelihood.get("AllInAll") * a.setup.fraudLikelihood.get("Theft1"))){
                        // steal == booking != realamounts
                        r.getREQ().Fraudulent=true;
                        r.getREQ().Fraudtypes.put("Theft1", true);
                        for(String s: r.getREQ().items) {
                            r.getREQ().realAmounts.put(s, (1 - a.setup.fraudvalues.get("Theft1")) * r.getREQ().itemAmount.get(s));
                        }
                    }
                    try {
                        ac.setContentObject(r);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }



                    a.send(ac);
                    ACLMessage ac2 = new ACLMessage(ACLMessage.CONFIRM);
                    ac2.addReceiver(a.setup.getAid(AgentType.erp_l));
                    try {
                        ac2.setContentObject(r.getREQ());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LocalDateTime nowtime = LocalDateTime.now();
                    System.out.println("sc ends at: "+nowtime.toString());
                    ControlUI.Instance.AddInfoLogEntry("sc ends at: "+nowtime.toString());
                    a.send(ac2);
                }
            }else if (aclm.getPerformative() == ACLMessage.CONFIRM ){
            }





        }
        block(5);
    }

}
