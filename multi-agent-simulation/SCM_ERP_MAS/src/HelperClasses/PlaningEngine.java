package HelperClasses;

import Agents.AgentType;
import CompanyData.SimpleSTKL;
import PassedObjects.SetUp;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import weka.classifiers.timeseries.HoltWinters;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class PlaningEngine {
    private static String lastproduced =null;
    private static int continouisCounter =0;
    private static int counter =0;
    private static Map<String, Integer> Bestellmenge = new HashMap<>();
    private static int counterDay = 719;

    public static Schedule plan(SetUp setup, REQ r , double cash, double machines ){
        counterDay++;
       // System.out.println("planning start");
        Schedule s = new Schedule();
        //ArrayList<ArrayList<REQ>> rl= new ArrayList<>();


        // get expected prices and volumes
        // get dbi
        // get side constraints
        // sort dbis
        // produce descending till constraint hit

        counter++;
        for(String bstm : Bestellmenge.keySet()){
            if(Bestellmenge.get(bstm) >= 0){
                Bestellmenge.put(bstm, Bestellmenge.get(bstm)-1);
            }
        }

        if (counter>=4){
            counter=0;
        }
        // get forecasts
        forecastEducts(r, setup);
        forecastProducts(r, setup);

        // get dbi's
        Map<String, Double>   dbi = new HashMap<>();
        Map<String, Double> cv = new HashMap<>();
        outer:
        for (String pro : setup.products.keySet()){
            if (!r.pricelist.containsKey(pro)) {continue outer;}
            double revenueperunit = r.pricelist.get(pro);
            SimpleSTKL stkl = setup.graphs.getStkl(pro);
            double costperunit=0;
            for (int j=0; j< stkl.ress.length; j++){
                if(!r.pricelist.containsKey(stkl.ress[j].identifier)){continue outer;}
                costperunit += r.pricelist.get(stkl.ress[j].identifier)*stkl.amount[j];
            }
            double db = revenueperunit-costperunit; //DB
            if(db <0 ){continue outer; }
            dbi.put(pro, db);
            cv.put(pro, costperunit);
        }

        // three constrained:
        // a) storage cap
        // b) time cap
        // c) capital cap


        List<String> targets ;
        Comparator<? super Map.Entry<String, Double>> comp = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String nameA = ((Map.Entry<String, Double>)o1).getKey();
                double demandVolumeA = r.demandvolume.get(nameA);
                double DBa = ((Map.Entry<String, Double>)o1).getValue();
                String nameB = ((Map.Entry<String, Double>)o2).getKey();
                double demandVolumeB = r.demandvolume.get(nameB);
                double DBb = ((Map.Entry<String, Double>)o2).getValue();
                if(demandVolumeA == 0 && demandVolumeB != 0){
                    return 1;
                }else if(demandVolumeA != 0 && demandVolumeB == 0){
                    return -1;
                }else {
                    if (DBa < DBb) {
                        return 1;
                    } else if (DBa > DBb) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
        };
        Comparator<? super String> comp2 = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String s1 = (String) o1;
                String s2 = (String) o2;
                    if(r.demandvolume.get(s1) < r.demandvolume.get(s2)){
                        return 1;
                    }else if(r.demandvolume.get(s1) > r.demandvolume.get(s2)){
                        return -1;
                    }else{
                        if(dbi.get(s1) < dbi.get(s2)){
                            return 1;
                        }else if(dbi.get(s1) > dbi.get(s2)){
                            return -1;
                        } else{
                            return 0;
                        }
                    }
            }
        };

        //System.out.println(dbi.keySet().size());
        targets= dbi.entrySet().stream().sorted(comp).map(Map.Entry::getKey).collect(Collectors.toList());
        // demand for security
        //Collections.sort(targets, comp2);



        List<String> productionplan = new ArrayList<>();
        HashMap<String, Double> pamount=new HashMap<>();
        HashMap<String, Double> eamount =new HashMap<>();

        double storage=0;
        for (String ss : setup.products.keySet()){
            storage += setup.mainBook.getInventory(ss);
        }
        HashMap<String, Double> ressStorage= new HashMap<>();
        for(String ss: setup.ressources.keySet()){
            ressStorage.put(ss, setup.mainBook.getInventory(ss));
        }

        double time =0;
        double capital = 0;
        for (int i =0; i<targets.size(); i++){
            //System.out.println("planning start");
            double storagereq=0;
            if(setup.productionStrategy.equals("d")) {
                if (Bestellmenge.containsKey(targets.get(i)) && Bestellmenge.get(targets.get(i)) != 0) {
                    storagereq = r.demandvolume.get(targets.get(i)) - (setup.mainBook.getInventory(targets.get(i)));
                } else {
                    storagereq = r.demandvolume.get(targets.get(i)) - (setup.mainBook.getInventory(targets.get(i)) - setup.storageSecurity);
                }
            }else if(setup.productionStrategy.equals("ss")){ // ss
                double inventory =  setup.mainBook.getInventory(targets.get(i));
                double dem = r.demandvolume.get(targets.get(i));
                if((inventory <= setup.storageSecurity) && (dem > 0)){
                 storagereq = (int)(setup.maxInv - inventory);
                }
            }else{ // other stochastical strategies
                System.out.println("no productionStrategy like this!");
                System.exit(1);
            }

            double remain=1-setup.graphs.getStkl(targets.get(i)).setuptime;
            double remainam = 0;
            for(String pro: productionplan){
                remain -= setup.graphs.getStkl(pro).setuptime;
                remainam += pamount.get(pro);
            }
            storagereq = Math.max(0, storagereq);
            remain = (remain * (machines/1000.0) ) - (remainam);
            storagereq = Math.min(remain, storagereq);
            storagereq = (int) Math.max(storagereq, setup.minlots);
            storagereq = (int) Math.min(storagereq, setup.maxlots);

            if (remain <=0){break;}
            if (storagereq <= 0){continue;}
            double timereq = getTimeRequirement(setup, targets.get(i), storagereq, machines);
            double capitalreq = cv.get(targets.get(i)) * storagereq;
            for(int j=0; j< setup.graphs.getStkl(targets.get(i)).ress.length; j++){
                String e = setup.graphs.getStkl(targets.get(i)).ress[j].identifier;
                Double ea = setup.graphs.getStkl(targets.get(i)).amount[j]*storagereq;
                if(eamount.containsKey(e)){
                 eamount.put(e, eamount.get(e)+ea);
                }else{
                    eamount.put(e, ea);
                }
            }

           time += timereq;
           storage += storagereq;
           capital += capitalreq;

           if(time-timereq+setup.graphs.getStkl(targets.get(i)).setuptime >1){break;}

           if(time >= 1 || storage > setup.warehousecap[1] || capital > setup.maxCash*(cash-setup.cashReserve)|| !eductStorageTest(eamount, ressStorage, setup)){
               double w = storagereq;
               double v=0;
               double k=storagereq;
               boolean a2 = false;
               double helper=0;
               while(true){
                   a2 = !(time >= 1 || storage > setup.warehousecap[1] || capital > 0.4*cash|| !eductStorageTest(eamount, ressStorage, setup ));
                   if(!a2){
                       if(w-v <= 1){
                           helper=k;
                           k=0;
                       }else{
                          helper=k;
                          w=k;
                          k=(int)(((w-v)/2)+v);
                       }
                   }else{
                       if(w-v<=1){
                           storagereq=v;
                           break;
                       }else{
                        helper=k;
                        v=k;
                        k=(int)(((w-v)/2)+v);
                       }
                   }

                   time -= timereq;
                   storage -= helper;
                   capital -= capitalreq;


                   timereq = getTimeRequirement(setup, targets.get(i), k, machines);
                   capitalreq = cv.get(targets.get(i)) * k;
                   for(int j=0; j< setup.graphs.getStkl(targets.get(i)).ress.length; j++){
                       String e = setup.graphs.getStkl(targets.get(i)).ress[j].identifier;
                       Double ea = setup.graphs.getStkl(targets.get(i)).amount[j]*(k-helper);
                           eamount.put(e, eamount.get(e)+ea);
                   }

                   if (k <= 0){
                       storagereq=k;
                       break;
                   }

                   time += timereq;
                   storage += k;
                   capital += capitalreq;
               }

               if(storagereq >0){
                   productionplan.add(targets.get(i));
                   pamount.put(targets.get(i), storagereq);
                   // test for storagesecurity buy
                    if(r.demandvolume.get(targets.get(i)) - (setup.mainBook.getInventory(targets.get(i))) <= 0){
                        Bestellmenge.put(targets.get(i), 3);
                    }
               }
            break;

           }else{
               productionplan.add(targets.get(i));
               pamount.put(targets.get(i), storagereq);
               // test for storagesecurity buy
               if(r.demandvolume.get(targets.get(i)) - (setup.mainBook.getInventory(targets.get(i))) <= 0){
                   Bestellmenge.put(targets.get(i), 3);
               }
           }
        }
        //System.out.println("planning ended");


        REQ rSC1 = new REQ();
        REQ rSC2 = new REQ();
        //REQ rDL = new REQ();
        //REQ rERP = new REQ();

        rSC1.target= AgentType.sc;
        rSC2.target=AgentType.sc;
        //rDL.target =AgentType.dl;
        //rERP.target=AgentType.erp_l;

        // to do split educts in vendors !
        ArrayList<String> educSC1 = new ArrayList<>();
        HashMap<String, Double> amountEDUCSC1 = new HashMap<>();
        ArrayList<String> educSC2 = new ArrayList<>();
        HashMap<String, Double> amountEDUCSC2 = new HashMap<>();
        for(String str : eamount.keySet()){
            if (setup.ressources.get(str).Vendor.equals("V01")){
                educSC1.add(str);
                amountEDUCSC1.put(str, eamount.get(str));
            }else{ //V02
                educSC2.add(str);
                amountEDUCSC2.put(str, eamount.get(str));
            }
        }
        rSC1.items= educSC1;
        rSC1.itemAmount = amountEDUCSC1;
        rSC2.items= educSC2;
        rSC2.itemAmount = amountEDUCSC2;



        //rDL.items= new ArrayList<>();
        //rDL.items.add(target);
        //rDL.itemAmount= new HashMap<>();
        //rDL.itemAmount.put(target, (double)targetamount);

        ArrayList<REQ> l1 = new ArrayList<>();
        l1.add(rSC1);
        l1.add(rSC2);
        //rl.add(l1);

        //ArrayList<REQ> l2 = new ArrayList<>();
        ArrayList<REQ> l2 = new ArrayList<>();
        for(String ss : productionplan){

            REQ_MK rMK = new REQ_MK();
            rMK.target = AgentType.mk;
            rMK.items= new ArrayList<>();
            rMK.items.add(ss);
            rMK.itemAmount=new HashMap<>();
            rMK.itemAmount.put(ss, pamount.get(ss));
            rMK.stkl = setup.graphs.getStkl(ss);
            l2.add(rMK);
            //rl.add(tmp);
        }



        //ArrayList<REQ> l3 = new ArrayList<>();
        //l3.add(rDL);
        //ArrayList<REQ> l4 = new ArrayList<>();
        //l4.add(rERP);

        //rl.add(l2);
       // rl.add(l3);
        //rl.add(l4);
        s.init(l1, l2);
        LocalDateTime nowtime=LocalDateTime.now();
       System.out.println("Planning ended at: "+nowtime.toString());
       ControlUI.Instance.AddInfoLogEntry("Planning ended at: "+nowtime.toString());
        return s;
    }


    private static void forecastEducts(REQ r, SetUp setUp){
        if(setUp.forecastingMethod.equals("mvp")) { // educt always open market
            for (String s : setUp.ressources.keySet()) {
                if (r.eductHist.isPresent(s)) {
                    Double[] d = r.eductHist.getPrices(s, 5); // index 0 is newest
                    double priceFin = 0;
                    double run = 0;
                    for (int i = 0; i < d.length; i++) {
                        priceFin += d[i] * 0.2;
                        run += 0.2;
                    }
                    priceFin = priceFin / run;
                    r.pricelist.put(s, priceFin);
                }
            }
        }else{ // holt winters
            for(String s: setUp.ressources.keySet()){
                if(r.eductHist.isPresent(s)){
                    Double[] d = r.eductHist.getPrices(s);
                    double priceFin = 0;
                    HoltWinters HW = new HoltWinters();
                    // set options
                    HW.setSeasonCycleLength(240);
                    HW.setExcludeSeasonalCorrection(false);
                    HW.setExcludeTrendCorrection(false);
                 /*
                 HW.setTrendSmoothingFactor();
                 HW.setSeasonalSmoothingFactor();
                 HW.setValueSmoothingFactor();
                 */

                    int size = d.length;
                    int minreqtrainingpoints = HW.getMinRequiredTrainingPoints();
                    if(HW.getMinRequiredTrainingPoints() > size){
                        System.out.println("min. requirement not meat for holt winters: not enough data points, required: "+minreqtrainingpoints+" ,given: "+size);
                        double run = 0;
                        for (int i = 0; i < 5; i++) { // mvp if holt winters not working
                            priceFin += d[i] * 0.2;
                            run += 0.2;
                        }
                        priceFin = priceFin / run;
                        r.pricelist.put(s, priceFin);
                    }else{
                        Attribute time = new Attribute("time");
                        Attribute price = new Attribute("price");
                        ArrayList<Attribute> attributeArrayList = new ArrayList<>();
                        attributeArrayList.add(time);
                        attributeArrayList.add(price);

                        Instances instances = new Instances("data", attributeArrayList, size);
                        instances.setClass(price);
                        for(int k = (size-1); k>=0; k-- ){
                            Instance in = new DenseInstance(2);
                            in.setValue(0, k+1 );
                            in.setValue(1, d[k]);
                            instances.add(in);
                        }
                        try {
                            HW.buildClassifier(instances);
                            priceFin = HW.forecast(); // next day = delivery day
                        }catch (Exception e){
                            System.out.println("ERROR: HW");
                            e.printStackTrace();
                        }
                        r.pricelist.put(s, priceFin);

                    }
                }
            }


        }



    }
    private static void forecastProducts(REQ r, SetUp setUp){
        if(setUp.forecastingMethod.equals("mvp") || setUp.productDataType.equals("func")) {
            for (String s : setUp.products.keySet()) {
                if (r.productHist.isPresent(s)) {
                    Double[] d = r.productHist.getPrices(s, 5); // index 0 is newest
                    Double[] dv = r.productHist.getVolume(s, 5);
                    int size = Math.min(d.length, dv.length);
                    double priceFin = 0;
                    double volFin = 0;
                    double run=0;
                    for (int i = 0; i < size; i++) {
                        run += 0.2;
                        priceFin += d[i]*0.2;
                        volFin += dv[i]*0.2;
                    }
                    priceFin = (priceFin/ run);
                    volFin = (int)(volFin/ run);
                    r.pricelist.put(s, priceFin);
                    r.demandvolume.put(s, volFin);
                }
            }
        } else {
            for(String s: setUp.products.keySet()){
             if(r.productHist.isPresent(s)){
                 Double[] d = r.productHist.getPrices(s ); // index 0 is newest
                 Double[] dv = r.productHist.getVolume(s );
                 int size = Math.min(d.length, dv.length);
                 double volFin = 0;
                 // volume average not seasonal
                 double run=0;
                 for (int i = 0; i < 5; i++) { // mvp for hw for volumes
                     run += 0.2;
                     volFin += dv[i]*0.2;
                 }
                 volFin = (int)(volFin/ run);
                 r.demandvolume.put(s, volFin);

                 // prices seasonal -> winters method
                 double priceFin = 0;
                 HoltWinters HW = new HoltWinters();
                 // set options
                 HW.setSeasonCycleLength(240);
                 HW.setExcludeSeasonalCorrection(false);
                 HW.setExcludeTrendCorrection(false);
                 /*
                 HW.setTrendSmoothingFactor();
                 HW.setSeasonalSmoothingFactor();
                 HW.setValueSmoothingFactor();
                 */
                 int minreqtrainingpoints = HW.getMinRequiredTrainingPoints();
                 if(minreqtrainingpoints > size){
                     // average
                     System.out.println("min. requirement not meat for holt winters: not enough data points, required: "+minreqtrainingpoints+" ,given: "+size);
                     run=0;
                     for (int i = 0; i < 5; i++) {
                         run += 0.2;
                         priceFin += d[i]*0.2;
                     }
                     priceFin = priceFin/run;
                     r.pricelist.put(s, priceFin);
                 }else{
                    // from d[0] to d[size-1] , 0 newest make instance , push to classifier and forecast one step.
                     Attribute time = new Attribute("time");
                     Attribute price = new Attribute("price");
                     ArrayList<Attribute> attributeArrayList = new ArrayList<>();
                     attributeArrayList.add(time);
                     attributeArrayList.add(price);

                     Instances instances = new Instances("data", attributeArrayList, size);
                     instances.setClass(price);
                     for(int k = (size-1); k>=0; k-- ){
                         Instance in = new DenseInstance(2);
                         in.setValue(0, k+1 );
                         in.setValue(1, d[k]);
                         instances.add(in);
                     }
                        try {
                               HW.buildClassifier(instances);
                               priceFin = HW.forecast(); // next day = delivery day
                                HW.updateForecaster(priceFin);
                                priceFin = HW.forecast(); // day 2 = production day
                                HW.updateForecaster(priceFin);
                                priceFin = HW.forecast(); // sale day
                        }catch (Exception e){
                            System.out.println("ERROR: HW");
                            e.printStackTrace();
                        }
                        if( ((Double)priceFin).isNaN() ){
                            priceFin=0.0; // no data available returns 0 price -> HW weka can not forecast 0 average timeseries
                        }
                        r.pricelist.put(s, priceFin);

                 }


             }
            }
        }

    }

    private static double getTimeRequirement(SetUp setUp, String pro, Double amount, double machines){
        double st = setUp.graphs.getStkl(pro).setuptime;
        double k = 1000.0/machines;   // cap = 1 day -> 1 = 1/cap days
        return st+k*amount;
    }
    private static double getTimeRequirementNoSetuptime(SetUp setUp, String pro, Double amount, double machines){
       // double st = setUp.graphs.getStkl(pro).setuptime;
        double k = 1.0/machines;   // cap = 1 day -> 1 = 1/cap days
        return k*amount;
    }




    private static boolean eductStorageTest(Map<String, Double> produced, Map<String, Double> oldstorage, SetUp setUp){
        double[] soll = new double[setUp.warehousecap.length];

        for(String s :produced.keySet()){
            soll[setUp.ressources.get(s).capacityType] += produced.get(s);
        }
        for(String s : oldstorage.keySet()){
            soll[setUp.ressources.get(s).capacityType] += oldstorage.get(s);
        }
        int[] caps = setUp.warehousecap;
        boolean b = true;
        for(int i =0; i< caps.length; i++){
            if (caps[i] < soll[i]) {
                b = false;
                break;
            }
        }
        return b;
    }


}
