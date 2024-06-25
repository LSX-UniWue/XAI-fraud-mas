package HelperClasses;

import java.io.Serializable;
import java.util.*;

public class MarketHistory implements Serializable {
    private List<List<Double>> history = new ArrayList<>();
    private boolean volume = false;
    private Map<String, Integer> lookup = new HashMap<>();
    private int lastfree=0;

    public MarketHistory(int NUmberofProducts, boolean volume){
        this.volume=volume;
        int mult = volume ? 2: 1;
        for (int i=0; i<NUmberofProducts*mult; i++){
            List<Double> tmp = new LinkedList<>();
            history.add(tmp);
        }
    }
    public MarketHistory copy(){
        MarketHistory mh = new MarketHistory(history.size(), volume);
        Map<String, Integer> lu = new HashMap<>();
        for(String s : lookup.keySet()){
            lu.put(new String(s), lookup.get(s));
        }
        for(int i=0; i<history.size(); i++){
            mh.history.get(i).addAll(this.history.get(i));
        }
        mh.lastfree = this.lastfree;
        mh.lookup = lu;
        return mh;
    }

    public boolean isPresent(String s){
        return lookup.containsKey(s);
    }



    public Double[] getPrices(String pro, int size){
        if (size ==0 || !lookup.containsKey(pro)) {
            return new Double[0];
        }
        List<Double> tmp = history.get(lookup.get(pro));
        size = Math.min(size, tmp.size()-1);
        Double[] d = new Double[size];
        for(int i=0; i<size; i++){
            d[i] = tmp.get(tmp.size() - i -1);
        }
        return d;
    }
    public Double[] getVolume(String pro, int size){
        if (size ==0 || !volume || !lookup.containsKey(pro)){
            return new Double[0];
        }
        List<Double> tmp = history.get(lookup.get(pro)+1);
        size = Math.min(size, tmp.size()-1);
        Double[] d = new Double[size];
        for(int i=0; i<size; i++){
            d[i] = tmp.get(tmp.size() - i -1);
        }
        return d;
    }


    public Double[] getPrices(String pro){
        if(!lookup.containsKey(pro)){return new Double[0];}
        List<Double> tmp = history.get(lookup.get(pro));
        int size = tmp.size()-1;
        Double[] d = new Double[size];
        for(int i=0; i<size; i++){
            d[i] = tmp.get(tmp.size() - i -1);
        }
        return d;
    }
    public Double[] getVolume(String pro){
        if(!lookup.containsKey(pro)){return new Double[0];}
        List<Double> tmp = history.get(lookup.get(pro)+1);
        int size = tmp.size()-1;
        Double[] d = new Double[size];
        for(int i=0; i<size; i++){
            d[i] = tmp.get(tmp.size() - i -1);
        }
        return d;
    }




    public void addData(String pro, double price, double volume) {
        if (!this.volume) {
            addData(pro, price);
            return;
        }
        if(lookup.containsKey(pro)){
            history.get(lookup.get(pro)).add(price);
            history.get(lookup.get(pro)+1).add(volume);
        }else{
            history.get(lastfree).add(price);
            history.get(lastfree+1).add(volume);
            lookup.put(pro, lastfree);
            lastfree= lastfree+2;
        }
    }

    public void addData(String pro, double price){
        if(lookup.containsKey(pro)){
            history.get(lookup.get(pro)).add(price);
        }else{
            history.get(lastfree).add(price);
            lookup.put(pro, lastfree);
            lastfree++;
        }
    }



}
