package Behaviour;

import Agents.AgentType;
import Agents.DL;
import CompanyData.businessProcedure;
import HelperClasses.Booking;
import HelperClasses.ControlUI;
import HelperClasses.REQ;
import PassedObjects.SetUp;
import TimeFrame.TimeFrame;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.Random;

public class DL_Behaviour extends CyclicBehaviour {

    private DL a;
    private Random rand= new Random();
    public DL_Behaviour (DL a){
        this.a=a;
    }

    @Override
    public void action() {
        ACLMessage aclm = a.receive();
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
                this.rand.setSeed(a.setup.randmap.get("DL").nextLong());

                try {
                    Thread.sleep(10); // initdays from markets
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                a.addBehaviour(new DL_Daily(a, TimeFrame.DAY.toTime()));
                ACLMessage answ = new ACLMessage(ACLMessage.CONFIRM);
                answ.addReceiver(a.setup.getAid(AgentType.erp_l));
                a.send(answ);
            }else if (aclm.getPerformative() == ACLMessage.CONFIRM && aclm.getSender().equals(a.setup.getAid(AgentType.dl))){
                // get maps from erpl
                ACLMessage ac = new ACLMessage(ACLMessage.PROPAGATE);
                ac.addReceiver(a.setup.getAid(AgentType.erp_l));
                a.send(ac);

            }else if (aclm.getPerformative() == ACLMessage.REQUEST && aclm.getSender().equals(a.setup.getAid(AgentType.erp_l))){
                // get maps
                // itemamount = storage product
                // items = ress
                // priceslist = maps of ressources
                // fixecost = fixed cost
                REQ maps=null;
                try {
                    maps = (REQ) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                double fixedCost = maps.fixedCost/5; // per day
                double storage=0.0;
                for(String s : maps.itemAmount.keySet()){
                    storage += maps.itemAmount.get(s);
                }
                fixedCost = fixedCost/storage;


                // set prices on goods
                REQ r = new REQ();
                for(String s: a.setup.products.keySet()){
                    double st = maps.itemAmount.get(s);
                    if(st != 0){
                        r.items.add(s);
                        r.itemAmount.put(s, st);
                        double price=0.0;
                        double priceRealMaps=0.0;
                        for(int j=0; j< a.setup.graphs.getStkl(s).ress.length; j++){
                            price += a.setup.graphs.getStkl(s).amount[j]*maps.pricelist.get(a.setup.graphs.getStkl(s).ress[j].identifier);
                            priceRealMaps += a.setup.graphs.getStkl(s).amount[j]*maps.maps.get(a.setup.graphs.getStkl(s).ress[j].identifier);

                        }
                        System.out.println("DL set price of "+s+" to: "+(price+fixedCost)*a.setup.margin);
                        ControlUI.Instance.UpdateProductPrices(s, String.format("%.2f",((price + fixedCost) * a.setup.margin)));

                        r.pricelist.put(s, (price+fixedCost)*a.setup.margin);
                        r.maps.put(s, (priceRealMaps+fixedCost)*a.setup.margin);

                        double theftis = rand.nextDouble();
                        //System.out.println(theftis);
                        if( theftis >= 1-( a.setup.fraudLikelihood.get("InvoiceKickback2") * a.setup.fraudLikelihood.get("AllInAll") )){
                            // fraud
                            r.Fraudulent =true;
                            r.Fraudtypes.put("InvoiceKickback2", true);
                            // lower prices arbitrarily
                            /*for(String se: r.pricelist.keySet()){
                                r.pricelist.put(se, r.pricelist.get(se)*(1-a.setup.fraudvalues.get("InvoiceKickback2")));
                            }*/
                        }

                    }
                }

                if(r.items.size() !=0) {
                    ACLMessage ac = new ACLMessage(ACLMessage.QUERY_REF);
                    try {
                        ac.setContentObject(r);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ac.addReceiver(a.setup.getAid(AgentType.markets));
                    a.send(ac);
                }

            }else if (aclm.getPerformative() == ACLMessage.INFORM_REF && aclm.getSender().equals(a.setup.getAid(AgentType.markets))){ // market
                REQ info =null;
                try {
                    info = (REQ) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }



                // book r-> booking
                Booking b = new Booking(businessProcedure.WA, true, info);

                ACLMessage ac2 = new ACLMessage(ACLMessage.REQUEST);
                ac2.addReceiver(a.setup.getAid(AgentType.erp_l));
                try {
                    ac2.setContentObject(b);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(ac2);

               /* ACLMessage ac = new ACLMessage(ACLMessage.CONFIRM);
                ac.addReceiver(a.setup.getAid(AgentType.erp_l));
                try {
                    ac.setContentObject(info);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //a.send(ac); */
            }else if (aclm.getPerformative()==ACLMessage.REQUEST && aclm.getSender().equals(a.setup.getAid(AgentType.markets))){
                Booking b = null;
                try {
                    b = (Booking) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                ACLMessage ac = new ACLMessage(ACLMessage.REQUEST);
                ac.addReceiver(a.setup.getAid(AgentType.erp_l));
                try {
                    ac.setContentObject(b);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(ac);
            }


        }
        block(20);
    }//end action

}
