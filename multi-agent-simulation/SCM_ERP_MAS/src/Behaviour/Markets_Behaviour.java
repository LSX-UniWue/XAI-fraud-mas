package Behaviour;

import Agents.AgentType;
import Agents.Markets;
import CompanyData.Product;
import CompanyData.Ressource;
import CompanyData.SimpleSTKL;
import CompanyData.businessProcedure;
import HelperClasses.ControlUI;
import HelperClasses.MarketHistory;
import HelperClasses.REQ;
import PassedObjects.SetUp;
import TimeFrame.TimeFrame;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import javax.print.attribute.standard.RequestingUserName;
import java.io.IOException;
import java.util.*;

public class Markets_Behaviour extends CyclicBehaviour {

    private Markets a;
    public Markets_Behaviour (Markets a){
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
                    int numberofEduc = 0;
                    int numberofProduct=0;
                    //set pricelist map
                    for(Ressource r : a.setup.ressources.values()){
                        numberofEduc++;
                        a.prices.put(r.identifier, r.map);
                    }
                    for(Product p: a.setup.products.values()){
                        numberofProduct++;
                        SimpleSTKL stkl = a.setup.graphs.getStkl(p.identifier);
                        double counter =0;
                        for(int i=0; i< stkl.ress.length; i++){
                            counter += stkl.amount[i]*a.prices.get(stkl.ress[i].identifier);
                        }
                        a.prices.put(p.identifier, counter*1.07);
                        a.volumes.put(p.identifier, 0.0);
                        a.historyEduct = new MarketHistory(numberofEduc, false);
                        a.historyProduct = new MarketHistory(numberofProduct, true);
                    }
                }
                int id = Math.max(a.setup.initdays, 10);
                a.addBehaviour(new MarketsInit(a, 1, id));
                a.addBehaviour(new Markets_Daily(a, TimeFrame.DAY.toTime(), id));
                ControlUI.Instance.SetEndDay(a.setup.killday);
                ControlUI.Instance.SetStartDay(a.setup.initdays);

                ACLMessage aclmres = new ACLMessage(ACLMessage.CONFIRM);
                aclmres.addReceiver(a.setup.getAid(AgentType.erp_l));
                a.send(aclmres);


            }else if(aclm.getSender().equals(a.setup.getAid(AgentType.erp_l)) && aclm.getPerformative() == ACLMessage.QUERY_REF ){
                //System.out.println("markets erpl query ref received");

                REQ r =null;
                try {
                    r =(REQ) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                r.addHistory(a.prices, a.volumes, a.historyEduct, a.historyProduct);

                ACLMessage ac = new ACLMessage(ACLMessage.INFORM);
                ac.addReceiver(a.setup.getAid(AgentType.erp_l));
                try {
                    ac.setContentObject(r);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                a.send(ac);

            }else if(aclm.getSender().equals(a.setup.getAid(AgentType.sc)) && aclm.getPerformative() == ACLMessage.QUERY_REF ){

                // getstuff
                REQ r =null;
                try {
                     r = (REQ) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                if(r.items.size() >0) { // else nothing to buy
                    r.combineWithMarketInfo(a.prices);

                    double ik = a.random.nextDouble();
                    if( ik >=1-(a.setup.fraudLikelihood.get("InvoiceKickback1")*a.setup.fraudLikelihood.get("AllInAll"))){
                        r.Fraudulent=true;
                        r.Fraudtypes.put("InvoiceKickback1", true);
                        r.Fraudtypes.put("Bill", true);
                        // increase prices arbitraily
                        for(String se: r.pricelist.keySet()){
                            r.pricelist.put(se, r.pricelist.get(se)*(1+a.setup.fraudvalues.get("InvoiceKickback1")));
                        }

                    }


                    // schedule bill
                    ACLMessage ac = new ACLMessage(ACLMessage.INFORM_REF);
                    ac.addReceiver(a.setup.getAid(AgentType.sc));
                    try {
                        ac.setContentObject(r);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    a.send(ac); // does not do anything
                    a.addBehaviour(new Bill_Behaviour(a, a.setup.ressources.get(r.items.get(0)).deliverytime * TimeFrame.DAY.toTime(), AgentType.sc, r, businessProcedure.WE));
                    a.addBehaviour(new Bill_Behaviour(a, (a.setup.ressources.get(r.items.get(0)).deliverytime + 1) * TimeFrame.DAY.toTime(), AgentType.sc, r, businessProcedure.S));
                }
            }else if(aclm.getSender().equals(a.setup.getAid(AgentType.dl)) && aclm.getPerformative() == ACLMessage.QUERY_REF && (a.setup.productDataType.equals("data") | !a.setup.marketfunction.getType().equals("nc") | !a.setup.printBySM)){

                // getstuff
                REQ r =null;
                try {
                    r = (REQ) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                REQ ranswer = new REQ();
                    ranswer.Fraudulent = r.Fraudulent;
                    ranswer.Fraudtypes = r.Fraudtypes;
                    boolean useFraudPrices = r.Fraudulent && r.Fraudtypes.containsKey("InvoiceKickback2") && r.Fraudtypes.get("InvoiceKickback2");




                if(a.setup.productDataType.equals("data")){
                    // prices and volumes
                    for (String item: r.items){
                        //System.out.println("Market decision: \n proposed item: "+ item+"\n at price: "+r.pricelist.get(item)+"\nmarket highest price poss.: "+a.prices.get(item));
                        
                        if(r.pricelist.get(item) > a.prices.get(item)*(1+a.setup.dataPriceIncrease)){
                            continue;
                        }
                        ranswer.items.add(item);
                        if (useFraudPrices){
                            ranswer.pricelist.put(item, r.pricelist.get(item)*(1-a.setup.fraudvalues.get("InvoiceKickback2")));
                        }else{
                            ranswer.pricelist.put(item, r.pricelist.get(item));
                        }
                        ranswer.itemAmount.put(item, Math.min(a.volumes.get(item), r.itemAmount.get(item)));
                    }
                }else{ // func
                    // calculate everything
                   /* double Pcl = 1;
                    double Pch = 10;
                    double nx = -1.0078;
                    double ny = 0.58;
                    double n = nx+ny;
                    int C = 2;
                    int MC1= (int)(18000.0/Pch); // hyper
                    //int MC1max = 5000;
                    int MC2 = (int)(44000/Pch); // chains
                    //int MC2max = 2000; */

                    double[] PSUBS = new double[r.items.size()];
                    int o=0;
                    for(String product: r.items) {
                        // if(r.pricelist.get(product) > Pch){continue;}
                        double standardcost = r.maps.get(product) / a.setup.margin; // actual standard cost fixedcost cons, cv without fraud

                        double mean = a.setup.marginCon * standardcost;
                        double stddev = (-1 / (1.29)) * (1 - a.setup.marginCon) * standardcost;
                        //double Psubstitute = Math.min(a.random.nextGaussian()*stddev+mean, Pch);
                        //Psubstitute = Math.max(Psubstitute, Pcl);
                        double Psubstitute = a.random.nextGaussian() * stddev + mean;
                        PSUBS[o]=Psubstitute;
                        o++;
                    }

                        HashMap<String, Integer> q= a.setup.marketfunction.getQ(r.items, r.pricelist, PSUBS);

                        for(String l: q.keySet()) {
                            if (q.get(l) == 0) {
                                continue;
                            }
                            ranswer.items.add(l);
                            if (useFraudPrices){
                                ranswer.pricelist.put(l, r.pricelist.get(l)*(1 - a.setup.fraudvalues.get("InvoiceKickback2")));
                            }else{
                                ranswer.pricelist.put(l, r.pricelist.get(l));
                            }
                            ranswer.itemAmount.put(l, Math.min(r.itemAmount.get(l), q.get(l)));
                            a.historyProduct.addData(l, r.pricelist.get(l), q.get(l));
                        }



                }

                if(ranswer.items.size() !=0) { // else we dont buy anything -> no booking
                    // schedule payment
                    ACLMessage ac = new ACLMessage(ACLMessage.INFORM_REF);
                    ac.addReceiver(a.setup.getAid(AgentType.dl));
                    try {
                        ac.setContentObject(ranswer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    a.send(ac);
                    // only if invoicekickback2 then a bill is fraudulent too
                    if(ranswer.Fraudulent && ranswer.Fraudtypes.containsKey("InvoiceKickback2") && ranswer.Fraudtypes.get("InvoiceKickback2")){
                        ranswer.Fraudtypes = new HashMap<>();
                        ranswer.Fraudtypes.put("InvoiceKickback2", true);
                    }else{
                        ranswer.Fraudulent=false;
                    }

                    ranswer.Fraudtypes.put("Bill2", true);
                    a.addBehaviour(new Bill_Behaviour(a, TimeFrame.DAY.toTime() * a.setup.paymenttime, AgentType.dl, ranswer, businessProcedure.S));
                }


            }else if(aclm.getSender().equals(a.setup.getAid(AgentType.dl)) && aclm.getPerformative() == ACLMessage.QUERY_REF){ // func nc printSM type market
                // getstuff
                REQ r =null;
                try {
                    r = (REQ) aclm.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }


                double[] PSUBS = new double[r.items.size()];
                int o=0;
                for(String product: r.items) {
                    // if(r.pricelist.get(product) > Pch){continue;}
                    double standardcost = r.maps.get(product) / a.setup.margin; // actual standard cost fixedcost cons, cv without fraud

                    double mean = a.setup.marginCon * standardcost;
                    double stddev = (-1 / (1.29)) * (1 - a.setup.marginCon) * standardcost;
                    //double Psubstitute = Math.min(a.random.nextGaussian()*stddev+mean, Pch);
                    //Psubstitute = Math.max(Psubstitute, Pcl);
                    double Psubstitute = a.random.nextGaussian() * stddev + mean;
                    PSUBS[o]=Psubstitute;
                    o++;
                }

                // get an object of sm -> product -> how much (price is set by dl, use fraud prices)
                // stop at max inventory offer
                // foreach answer make answer and schedule bills and payments

                List<HashMap<String, Integer>> qs_pre = a.setup.marketfunction.getSingleSMs(r.items, r.pricelist, PSUBS);
                List<HashMap<String, Integer>> qs_final = new ArrayList<>();
                HashMap<String, Integer> total = new HashMap<>();

                for(HashMap<String, Integer> h: qs_pre){
                    HashMap<String, Integer> tmp = new HashMap<>();
                   // boolean test=false;
                    for(String s : h.keySet()){
                        /*if(r.itemAmount.get(s) > 0){
                            test = true;
                        }*/
                        total.put(s, total.getOrDefault(s, 0)+h.get(s)); // sum
                        if( (r.itemAmount.get(s)-h.get(s)) >= 0){
                            r.itemAmount.put(s, r.itemAmount.get(s)-h.get(s));
                            tmp.put(s, h.get(s));
                        }else if( (r.itemAmount.get(s) > 1) && (h.get(s) >1) ){
                            tmp.put(s, r.itemAmount.get(s).intValue());
                            r.itemAmount.put(s, 0.0);
                        }

                    }
                    qs_final.add(tmp);
                }

                /* REQ ranswer = new REQ();
                ranswer.Fraudulent = r.Fraudulent;
                ranswer.Fraudtypes = r.Fraudtypes;
                boolean useFraudPrices = r.Fraudulent && r.Fraudtypes.containsKey("InvoiceKickback2") && r.Fraudtypes.get("InvoiceKickback2");
                */


                // add to history
                for(String s : total.keySet()){
                    if(total.get(s) >0) {
                        a.historyProduct.addData(s, r.pricelist.get(s), total.get(s));
                    }
                }

                // make answers
                List<REQ> finalList = new ArrayList<>();
                boolean useFraudPrices = r.Fraudulent && r.Fraudtypes.containsKey("InvoiceKickback2") && r.Fraudtypes.get("InvoiceKickback2");
                for(HashMap<String, Integer> h : qs_final){
                    REQ ranswer = new REQ();
                    ranswer.Fraudulent = r.Fraudulent;
                    ranswer.Fraudtypes = r.Fraudtypes;
                    boolean jumper = true;
                    for(String s : h.keySet()){
                        if(h.get(s) >0){
                            jumper=false;
                            ranswer.items.add(s);
                            ranswer.itemAmount.put(s, (double) h.get(s));
                            if(useFraudPrices){
                                ranswer.pricelist.put(s, r.pricelist.get(s)*(1-a.setup.fraudvalues.get("InvoiceKickback2")));
                            }else{
                                ranswer.pricelist.put(s, r.pricelist.get(s));
                            }
                        }
                    }
                    if(jumper){continue;}
                    finalList.add(ranswer);
                }

                    for(REQ ranswer : finalList){
                        ACLMessage ac = new ACLMessage(ACLMessage.INFORM_REF);
                        ac.addReceiver(a.setup.getAid(AgentType.dl));
                        try {
                            ac.setContentObject(ranswer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        a.send(ac);
                        // only if invoicekickback2 then a bill is fraudulent too
                        if(ranswer.Fraudulent && ranswer.Fraudtypes.containsKey("InvoiceKickback2") && ranswer.Fraudtypes.get("InvoiceKickback2")){
                            ranswer.Fraudtypes = new HashMap<>();
                            ranswer.Fraudtypes.put("InvoiceKickback2", true);
                        }else{
                            ranswer.Fraudulent=false;
                        }
                        ranswer.Fraudtypes.put("Bill2", true);
                        a.addBehaviour(new Bill_Behaviour(a, TimeFrame.DAY.toTime() * a.setup.paymenttime, AgentType.dl, ranswer, businessProcedure.S));
                    }


            }
        }
        block(5);
    }

}
