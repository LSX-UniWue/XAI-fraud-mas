package CompanyData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Marketfunction implements Serializable {
    private List<MarketWrapper> markets = new ArrayList<>();
    private Map<Integer, MarketWrapper> lookup=new HashMap<>();
    private String type;
    private double alpha;
    private double beta;


    public void add(MarketWrapper market){
        markets.add(market);
    }

    public int getSubstituteNumber(){
        int x= 0;
        for(MarketWrapper mw : markets){
            x = Math.max(x, mw.getSubstituteNumber()) ;
        }
        return x;
    }
    public void setType(String type){
        this.type=type;
    }
    public String getType(){
        return this.type;
    }
    /*public void setAlpha(double alpha){
        this.alpha=alpha;
    }
    public void setBeta(double beta){
        this.beta=beta;
    }*/

    public double getAverageRange(){
        int counter=0;
        double av = 0;
        for(MarketWrapper mw : markets){
            av += mw.getAverageRange();
            counter++;
        }
        return av/counter;
    }

    public HashMap<String, Integer> getQ(List<String> product, HashMap<String, Double> price, double[] substitutePrices){
        HashMap<String, Integer> res = new HashMap<>();
        for(String p : product){
            res.put(p,0);
        }
        if(this.type.equals("c")) {
            int o=0;
            for(String r: product) {
                double q = 0.0;
                for (MarketWrapper mw : markets) {
                    q = q + mw.getQ_const(r, price.get(r), new double[] {substitutePrices[o]}); // j good indepent of i good except external subst.
                }
                res.put(r, (int) q);
                o++;
            }
        }else{
            for(MarketWrapper mw: markets){
                HashMap<String, Integer> tmp = mw.getQ_Notconst(product, price, substitutePrices);
                for(String p: product){
                 res.put(p, tmp.get(p)+res.get(p));
                }
            }

        }
        return res;
    }

    public List<HashMap<String, Integer>> getSingleSMs(List<String> product, HashMap<String, Double> price, double[] substitutePrices){
        List<HashMap<String, Integer>> resList = new ArrayList<>();

        for(MarketWrapper mw: markets){
            resList.addAll(mw.getSingleSM( product,  price, substitutePrices));
        }
        return resList;
    }


}
