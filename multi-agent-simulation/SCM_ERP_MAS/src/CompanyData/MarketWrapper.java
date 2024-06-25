package CompanyData;

import HelperClasses.CMapping;
import HelperClasses.Tuple;

import java.io.Serializable;
import java.util.*;

public class MarketWrapper implements Serializable {
    String marketname;
    int primaryProductNumber;
    int substituteNumber;
    double PCL;
    double PCH;
    private double[][] elasticity ;
    private double marketcap;
    private Map<String, Tuple<Integer, Integer>> lookup= new HashMap<>();
    private Map<String, Boolean> isPrimary = new HashMap<>();
    private double[] alphas;
    private HashMap<String, Double>[] betas;
    private double[] sigmas;
    private double[] sigmas2;
    private double[] subreduction;
    private boolean[] issets;
    private int[] setnumbers;
    private int nextrow=0;
    private int nextcol=0;
    private Random rand = new Random();
    private CMapping capacityMapping;
    private double TFactor;

    public MarketWrapper(int substitutenumber, int primaryProductnumber) {
        this.elasticity= new double[substitutenumber+primaryProductnumber+1][substitutenumber+primaryProductnumber];
        this.primaryProductNumber=primaryProductnumber;
        this.substituteNumber=substitutenumber;
    }

    public void setSeed(long seed){
        this.rand.setSeed(seed);
    }

    public void setMarketcap(double marketcap) {
        this.marketcap = marketcap;
    }

    public double getMarketcap() {
        return marketcap;
    }

    public void setCustomerNumber(int number){
        this.alphas= new double[number];
        this.betas= new HashMap[number];
        this.sigmas= new double[number];
        this.sigmas2 = new double[number];
        this.subreduction = new double[number];
        this.issets= new boolean[number];
        this.setnumbers = new int[number];
        for(int i =0; i< this.alphas.length; i++){
            this.betas[i] = new HashMap<>();
        }
    }

    public void setMarketname(String marketname){
        this.marketname = marketname;
    }
    public String getMarketname(){
        return this.marketname;
    }
    public void setPCL(double PCL){
        this.PCL=PCL;
    }
    public void setPCH(double PCH){
        this.PCH=PCH;
    }

    public void addElasticity(String pricechangedon, String primaryProduct, double elasticity) {
        int column, row;
        if (lookup.containsKey(primaryProduct)){
            column = lookup.get(primaryProduct).y;
        }else{
            column = nextcol;
            lookup.put(primaryProduct, new Tuple<>(nextrow, nextcol));
            nextcol +=1;
            nextrow +=1;
        }
        if(lookup.containsKey(pricechangedon)){
            row= lookup.get(pricechangedon).x;
        }else{
            row=nextrow;
            lookup.put(pricechangedon, new Tuple<>(nextrow, nextcol));
            nextcol +=1;
            nextrow +=1;
        }
        if(pricechangedon.equals(primaryProduct)){isPrimary.put(primaryProduct, true); }
        this.elasticity[row][column] = elasticity;

    }

    public void addFavor(String name, double value){
        int col;
        if(lookup.containsKey(name)){
            col= lookup.get(name).y;
        }else{
            col= nextcol;
            lookup.put(name, new Tuple<>(nextrow, nextcol));
            nextrow +=1;
            nextcol +=1;
        }
        this.elasticity[this.elasticity.length-1][col] = value;
    }

    public void addAlpha(int index, double value){
        this.alphas[index] = value;
    }
    public void addBeta(int index, String id, double value){
        if(index>=0 && index< this.betas.length && !this.betas[index].containsKey(id)){
            this.betas[index].put(id, value);
        }
    }
    public void setCapacityMapping(CMapping cm ){
        this.capacityMapping = cm;
    }
    public void setTFactor(double tf){
        this.TFactor = tf;
    }

    public void addSigma(int ind, double value){
        this.sigmas[ind]= value;
    }
    public void addSigma2(int ind, double value){this.sigmas2[ind] = value; }
    public void addSubReduction(int ind, double value){this.subreduction[ind] = value; }
    public void setSetNumber(int index, int value){
        this.setnumbers[index] = value;
    }
    public void setSets(int index, boolean value){
        this.issets[index]=value;
    }

    public int getSubstituteNumber(){return this.substituteNumber; }

    public HashMap<String, Integer> getQ_Notconst(List<String> productList, HashMap<String, Double> prices, double[] substitutePrices){
        int costumernumber = this.alphas.length;
        double singleCap = this.marketcap*2;
        HashMap<String, Integer> retmap = new HashMap<>();
        for (String p: productList){retmap.put(p,0); }

        for(int i=0; i< costumernumber; i++){ // over all consumers in market

            double div = 1; // nen / div
            for(int j =0; j< productList.size(); j++){
                String s = productList.get(j);
                double betaNow = this.betas[i].get(s);
                div += Math.exp(prices.get(s)*this.alphas[i]+this.betas[i].get(s));
                double betasubs = rand.nextGaussian()*this.sigmas2[i]+betaNow-this.subreduction[i];
                int set;
                if(this.issets[i]){
                    set = this.setnumbers[i];
                }else{
                    set = productList.size();
                }
                    div += (((double) set) / productList.size()) * Math.exp(substitutePrices[j] * this.alphas[i] + betasubs);
            }
            for(String p: productList){
                double nen = Math.exp(prices.get(p)*this.alphas[i]+this.betas[i].get(p));
                double shares = nen/div;
                double r = rand.nextGaussian()*this.sigmas[i];
                int res =0;
                if(shares > 0.0005){ // no fluctuation if product is not wanted
                    if(capacityMapping == CMapping.FIX) {
                        res = (int) ((shares + r) * singleCap * this.TFactor * (1/(double)costumernumber) * (1 / getAverageRange()));
                    }else{
                        res = (int) ((shares + r) * singleCap * this.TFactor * (1/(double)costumernumber) * (1 / prices.get(p)));
                    }
                }
                res = res <0 ? 0:  res;
                retmap.put(p, retmap.get(p)+res);
            }

        }
        return retmap;
    }

    public List<HashMap<String, Integer>> getSingleSM(List<String> productList, HashMap<String, Double> prices, double[] substitutePrices){
        int costumernumber = this.alphas.length;
        List<HashMap<String, Integer>> returnList = new ArrayList<>(costumernumber);
        double singleCap = this.marketcap*2;
        for (int i=0;  i<costumernumber; i++){
            HashMap<String, Integer> retmap = new HashMap<>();

            double div = 1; // nen / div
            for(int j =0; j< productList.size(); j++){
                String s = productList.get(j);
                double betaNow = this.betas[i].get(s);
                div += Math.exp(prices.get(s)*this.alphas[i]+this.betas[i].get(s));
                double betasubs = rand.nextGaussian()*this.sigmas2[i]+betaNow-this.subreduction[i];
                int set;
                if(this.issets[i]){
                    set = this.setnumbers[i];
                }else{
                    set = productList.size();
                }
                    div += (((double) set) / productList.size()) * Math.exp(substitutePrices[j] * this.alphas[i] + betasubs);

            }
            for(String p: productList){
                double nen = Math.exp(prices.get(p)*this.alphas[i]+this.betas[i].get(p));
                double shares = nen/div;
                double r = rand.nextGaussian()*this.sigmas[i];
                int res =0;
                if(shares > 0.0005){ // no fluctuation if product is not wanted
                    if(this.capacityMapping == CMapping.FIX) {
                        res = (int) ((shares + r) * singleCap * this.TFactor * (1/(double)costumernumber) * (1 / getAverageRange()));
                    }else{
                        res = (int) ((shares + r) * singleCap * this.TFactor * (1/(double)costumernumber) * (1 / prices.get(p)));
                    }
                }
                res = res <0 ? 0:  res;
                retmap.put(p, res);
            }
            returnList.add(retmap);

            }

        return returnList;
    }




    public double getQ_const(String product, double price, double[] substitutePrices) {
        if(!lookup.containsKey(product)){return 0.0; }
        if(price> this.PCH){return 0.0;}
        if(substitutePrices.length==0){return 0.0; }
        if(substitutePrices.length < this.substituteNumber){
            double tmp = substitutePrices[0];
            substitutePrices= new double[substituteNumber];
            for(int k=0; k<substituteNumber; k++){
                substitutePrices[k]=tmp;
            }
        }

        int col = lookup.get(product).y;
        double factor =1;
        int counter = 0;
        double elastsum=0;
        for(String name: lookup.keySet()){
            if(isPrimary.containsKey(name) && !name.equals(product)){continue;}
            int row = lookup.get(name).x;
            double elast = this.elasticity[row][col];
            elastsum += elast;
            double p=0;
            if(name.equals(product)){
                p=price;
            }else{
                p=Math.min(substitutePrices[counter], PCH);
                p=Math.max(p, PCL);
                counter++;
            }
            factor = factor * Math.pow(p, elast);
        }
        factor = factor * ((this.marketcap/Math.pow(this.PCL, elastsum+1))*this.elasticity[this.elasticity.length-1][col]);
        if(price >= PCL){
            factor= Math.min(factor, marketcap/price);

        }else{
            factor = Math.min(factor, marketcap/PCL);

        }

        return factor;
    }

    public double getAverageRange(){
        return (PCL+PCH)/2;
    }







}
