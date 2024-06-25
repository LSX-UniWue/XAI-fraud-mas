package Behaviour;

import Agents.AgentType;
import Agents.ERP_L;
import CompanyData.businessProcedure;
import HelperClasses.*;
import PassedObjects.SetUp;
import TimeFrame.TimeFrame;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

public class ERP_L_Behaviour extends CyclicBehaviour {
    private ERP_L a;
    private int counter=0;
    public boolean stopper=false;

    public ERP_L_Behaviour (ERP_L a){
        this.a=a;
    }

    @Override
    public void action() {
        ACLMessage aclm = getAgent().receive();
        if(aclm != null && !stopper){
           // System.out.println("erpl_l msg received at : "+ LocalTime.now()+" with type: "+aclm.getPerformative());
            if(aclm.getPerformative() == ACLMessage.CANCEL){ // killday
                stopper = true;
                System.out.println("END");
                try {
                    a.setup.mainBook.printAll();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }else if(a.setup == null && aclm.getPerformative() == ACLMessage.INFORM){ // init from GUI
                System.out.println("ERP_L inform msg from GUI...\ncreate AID list");
                SetUp setup =null;
                try {
                    setup= XMLParser.XMLtoSETUP();
                } catch (ParserConfigurationException | IOException | SAXException e) {
                    e.printStackTrace();
                }

                HashMap<AgentType, AID> aidmap = new HashMap<>();
                for(AgentType at : AgentType.values()) {
                    DFAgentDescription df = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(at.toString());
                    df.addServices(sd);
                    DFAgentDescription[] dfres = new DFAgentDescription[1];
                    try {
                        dfres = DFService.search(a, df);
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                    if(dfres.length>0)System.out.println(at.toString()+" :  "+dfres[0].getName().toString());
                    if(dfres.length>0) aidmap.put(at, dfres[0].getName());
                }
                System.out.println("ERP_L set up Setup-class");
                setup.setAidmap(aidmap);
                a.setup=setup;
                a.setup.mainBook.init();
                ACLMessage aclminit = new ACLMessage(ACLMessage.INFORM);
                try {
                    aclminit.setContentObject(setup);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (AgentType at : AgentType.values()){
                    if(at != AgentType.erp_l) aclminit.addReceiver(setup.getAid(at));
                }
                a.send(aclminit);


                System.out.println("erp_L init send...");

            }else if (aclm.getPerformative() == ACLMessage.INFORM) { // stop
                // print everything out
                boolean b = false;
                AID sender = aclm.getSender();
                for (AgentType at : AgentType.values()) {
                    b = b | sender.equals(a.setup.getAid(at));
                }
                if (!b) { // message from outside -> stop
                    stopper = true;
                    System.out.println("END");
                    try {
                        a.setup.mainBook.printAll();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);

                } else { // message from inside== agents
                    if (aclm.getSender().equals(a.setup.getAid(AgentType.markets))) {
                        // System.out.println("markets answered to erpl with quue size: " +a.getQueueSize());
                        REQ r = null;
                        try {
                            r = (REQ) aclm.getContentObject();
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        Schedule s = PlaningEngine.plan(a.setup, r, a.setup.mainBook.cash(), a.setup.mainBook.machines());
                        if (a.schedule == null) {
                            a.schedule = s;
                            a.addBehaviour(new SchedulingBehaviour(a));
                        } else {
                            a.schedule.add(s);
                        }
                    }
                }
            }else if (aclm.getPerformative() == ACLMessage.PROPAGATE && aclm.getSender().equals( a.setup.getAid(AgentType.dl))){
                REQ retu = new REQ();
                ACLMessage ac = new ACLMessage(ACLMessage.REQUEST);
                ac.addReceiver(a.setup.getAid(AgentType.dl));
                for(String s : a.setup.ressources.keySet()){
                    retu.items.add(s);
                    retu.pricelist.put(s, a.setup.mainBook.getMapsIncludingPros(s));
                    retu.maps.put(s, a.setup.mainBook.getRealMapsIncludingPros(s));
                }

                for(String p: a.setup.products.keySet()){
                    retu.itemAmount.put(p, a.setup.mainBook.getInventory(p));
                }
                retu.fixedCost = a.setup.mainBook.getFixCost();
                try {
                    ac.setContentObject(retu);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(ac);

            }else if (aclm.getPerformative() == ACLMessage.CONFIRM){ // confirmation after init
                //System.out.println("confirmation received ");
                counter++;
                if(counter==4){
                   // System.out.println("erp confirmations received ");
                    //start daily
                    a.addBehaviour(new ERP_Daily(a, TimeFrame.DAY.toTime()));
                    // start planning and make schedule
                    ACLMessage ac = new ACLMessage(ACLMessage.QUERY_REF);
                    ac.addReceiver(a.setup.getAid(AgentType.markets));
                    REQ rt = new REQ();
                    rt.items.addAll(a.setup.ressources.keySet());
                    rt.items.addAll(a.setup.products.keySet());
                    try {
                        ac.setContentObject(rt);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // a.send(ac); erp_l daily already calls planning
                }else if(counter > 4){
                    counter = 4;
                    //dostuff here
                    REQ r=null;
                    try {
                        r = (REQ) aclm.getContentObject();
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                   // System.out.println("markasDone from "+aclm.getSender()+" ");
                    a.schedule.markAsDone(r.uniqueID);
                }
            }else if (aclm.getPerformative() == ACLMessage.REQUEST && !aclm.getSender().equals( a.setup.getAid(AgentType.erp_l))){
                    // book stuff here
                Booking b =null;
                try {
                    b= (Booking) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                AgentType sender =null;
                for(AgentType agent: AgentType.values()){
                    if(a.setup.getAid(agent).equals( aclm.getSender())){
                        sender= agent;
                    }
                }
                BookingResult br = a.setup.mainBook.book(b, sender);
                if(sender == AgentType.mk && b.getGV() == businessProcedure.WAP){
                    // not enough educts in inventory -> booking is not same as real inventory ?
                    ACLMessage ac = new ACLMessage(ACLMessage.PROPAGATE);
                    ac.addReceiver(a.setup.getAid(AgentType.mk));
                    try {
                        ac.setContentObject(br.b);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                a.send(ac);
                }



                if(sender== AgentType.sc && b.getGV() == businessProcedure.S){
                    //issue payment of bills
                    a.addBehaviour(new ERPL_PAY(a, TimeFrame.DAY.toTime()*15 , b));
                }

            }else if (aclm.getSender().equals( a.setup.getAid(AgentType.erp_l) )&& aclm.getPerformative() == ACLMessage.REQUEST ){

                Object o =null;
                try {
                    o= aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

                if(!(o instanceof Booking )&& !stopper){
                   // System.out.println("erpl_message erp_l received ");
                    // restart  scheduling -> imput from erp_l daily action
                   /* REQ r =null;
                    try {
                        r = (REQ) aclm.getContentObject();
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                    a.schedule.markAsDone(r.uniqueID); */

                    ACLMessage ac = new ACLMessage(ACLMessage.QUERY_REF);
                    ac.addReceiver(a.setup.getAid(AgentType.markets));
                    REQ rt = new REQ();
                    rt.items.addAll(a.setup.ressources.keySet());
                    rt.items.addAll(a.setup.products.keySet());
                    try {
                        ac.setContentObject(rt);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    a.send(ac);
                }else{
                    Booking b = (Booking) o;
                    a.setup.mainBook.book(b, AgentType.erp_l);
                }



            }




        }
        block(5);
    }
    

}
