package Behaviour;

import Agents.AgentType;
import Agents.Markets;
import HelperClasses.ControlUI;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


public class Markets_Daily extends WakerBehaviour {

    private Markets a;
    private int counter =0;
    private final File dir = new File(getClass().getClassLoader().getResource("DailyPricesVolumes").getPath());
    public Markets_Daily(Markets market, long time, int initday){
        super(market,time);
        this.a=market;
        counter = initday;
    }

    @Override
    public void onWake(){
        System.out.println("markets daily called with day : "+counter);
        ControlUI.Instance.UpdateDay(counter);
        if (a.setup.killday <= counter){
            ACLMessage ac = new ACLMessage(ACLMessage.CANCEL);
            ac.addReceiver(a.setup.getAid(AgentType.erp_l));
            a.send(ac);
        }

        //change prices every day
        for (String res : a.setup.ressources.keySet()) {
            this.a.historyEduct.addData(res, a.prices.get(res));
            this.a.prices.put(res, Double.parseDouble(getData(res, 1)));
            }
        if(a.setup.productDataType.equals("data")) {
            for (String pro : a.setup.products.keySet()) {
                this.a.historyProduct.addData(pro, a.prices.get(pro), this.a.volumes.get(pro));

                this.a.prices.put(pro, Double.parseDouble(getData(pro, 1)));
                this.a.volumes.put(pro, Double.parseDouble(getData(pro, 2)));
            }
        }else if(a.setup.productDataType.equals("func")){
            // closed market condition

        }
        counter++;
        reset();
    }

    private String getData(String res, int index){
        String countString = (String.valueOf(counter));
        File[] entries = dir.listFiles();
        if (entries==null) return String.valueOf(0);
        for(File entry : entries){
            if(entry.getName().contains(res)){
                try (BufferedReader br = new BufferedReader(new FileReader(entry))){
                    String line;
                    boolean constcheck=false;
                    while((line=br.readLine()) != null ) { //header
                        if (line.contains("@DATA")){
                            break;
                        }else if(line.contains("@CONSTANT")){
                            constcheck=true;
                            break;
                        }
                    }
                    if(constcheck){
                        return br.readLine(); // const value
                    }

                    Map<String, String> rerun = new HashMap<>(240);
                    while((line = br.readLine()) != null){ // body
                        String[] vals = line.split(",");
                        if(vals[0].equals(countString)){
                            return vals[index];
                        }else{
                            rerun.put(vals[0], vals[index]);
                        }
                    }
                    // nothing found and no more data
                    countString = (String.valueOf(counter % 240));
                    if(rerun.containsKey(countString)){
                        return rerun.get(countString);
                    }else{
                        return "0.0";
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return String.valueOf(0.0);
    }





}
