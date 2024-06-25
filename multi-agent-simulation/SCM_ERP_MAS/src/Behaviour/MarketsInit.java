package Behaviour;

import Agents.Markets;
import jade.core.behaviours.WakerBehaviour;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MarketsInit extends WakerBehaviour {

    private Markets a;
    private int counter =0;
    private int initday=0;
    private final File dir = new File(getClass().getClassLoader().getResource("DailyPricesVolumes").getPath());
    public MarketsInit(Markets market, long time, int initday){
        super(market,time);
        this.a=market;
        this.initday=initday;
    }

    @Override
    public void onWake(){
        for(int i = 1; i<initday; i++) {
            //change prices every day
            for (String res : a.setup.ressources.keySet()) {
                this.a.historyEduct.addData(res, a.prices.get(res));
                this.a.prices.put(res, Double.parseDouble(getData(res, 1)));
            }
            if (a.setup.productDataType.equals("data")) {
                for (String pro : a.setup.products.keySet()) {
                    this.a.historyProduct.addData(pro, a.prices.get(pro), this.a.volumes.get(pro));

                    this.a.prices.put(pro, Double.parseDouble(getData(pro, 1)));
                    this.a.volumes.put(pro, Double.parseDouble(getData(pro, 2)));
                }
            } else if (a.setup.productDataType.equals("func")) {
                List<String> templ = new ArrayList<>();
                HashMap<String, Double> tmplHash= new HashMap<>();
                double[] allprices = new double[a.setup.products.keySet().size()];
                double allPrice = a.setup.marketfunction.getAverageRange();
                int o=0;
                for (String pro : a.setup.products.keySet()) {
                    this.a.historyProduct.addData(pro, a.prices.get(pro), this.a.volumes.get(pro));
                    templ.add(pro);
                    tmplHash.put(pro, allPrice);
                    allprices[o] = allPrice;
                    o++;
                }

                    HashMap<String, Integer> res= a.setup.marketfunction.getQ(templ ,tmplHash, allprices );


                    /*double Pcl = 0;
                    double Pch = 10;
                    double nx = -1.0078;
                    double ny = 0.58;
                    double n = nx+ny;
                    int C = 2;
                    int MC1= (int)(18000.0/Pch); // hyper
                    //int MC1max = 5000;
                    int MC2 = (int)(44000/Pch); // chains
                    //int MC2max = 2000;

                    double Psubstitute = (Pch+Pcl)/2;
                    this.a.prices.put(pro, Psubstitute);
                    double correctionfac = (Math.pow(Psubstitute, ny))/(C*Math.pow(Pch, n));

                    double q1 = Math.pow(Psubstitute, nx)*MC1*correctionfac;
                    double q2 = Math.pow(Psubstitute, nx)*MC2*correctionfac;
                    //int q = (int) (Math.min(q1, MC1max)+Math.min(q2, MC2max));
                    int q =  (int)(Math.min(q1,MC1/Psubstitute ) + Math.min(q2, MC2/Psubstitute));
                    */

                for(String s: res.keySet()) {
                    if (res.get(s) <= 0) {
                        this.a.volumes.put(s, 0.0);
                    } else {
                        this.a.volumes.put(s, (double) res.get(s));
                    }
                }




            }
            counter++;
            //reset();
        }
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
                        return br.readLine();
                    }

                    int linecounter =0;
                    while((line = br.readLine()) != null){ // body
                        linecounter++;
                        String[] vals = line.split(",");
                        if(vals[0].equals(countString)){
                            return vals[index];
                        }
                    }
                   /* if(linecounter ==0){
                        int u = 51;
                    }*/
                    countString = (String.valueOf(counter % linecounter));
                    while((line = br.readLine()) != null){ // body
                        String[] vals = line.split(",");
                        if(vals[0].equals(countString)){
                            return vals[index];
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return String.valueOf(0);
    }









}
