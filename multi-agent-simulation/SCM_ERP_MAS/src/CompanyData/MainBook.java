package CompanyData;

import Agents.AgentType;
import HelperClasses.*;
import PassedObjects.SetUp;
import com.sun.istack.Nullable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Objects.nonNull;

public class MainBook extends ArrayList<Account> implements Serializable {
    private ArrayList<Account> accounts=new ArrayList<>();
    private ArrayList<String> log= new ArrayList<>();
    private HashMap<String, Double> inventory=new HashMap<>();
    private HashMap<String, Double> realInventory = new HashMap<>();
    private HashMap<String, Double> mapsIncludingPros = new HashMap<>();
    private HashMap<String, Double> realMapsincludingPros = new HashMap<>();
    private SetUp setUp;
    private  BelegnummerCounter belegnummerCounter = new BelegnummerCounter();
    private ERPClock clock = null;
    private int shipmentCostIndex=0; //increase everytime you sell -> shipmentcost in costcalc every week
    private Random random = new Random();
    private long baselabor;
    private long baseoverhead;
    private long basesga;




    public MainBook(SetUp setUp){
        this.setUp=setUp;
       for (String s : setUp.ressources.keySet()){
            inventory.put(s, 0.0);
            realInventory.put(s, 0.0);
            ControlUI.Instance.UpdateStorage(s, "0.0");
            mapsIncludingPros.put(s, setUp.ressources.get(s).map);
            realMapsincludingPros.put(s, setUp.ressources.get(s).map);
        }
        for(String s : setUp.products.keySet()){
            inventory.put(s,0.0);
            realInventory.put(s,0.0);
            ControlUI.Instance.UpdateStorage(s, "0.0");
            mapsIncludingPros.put(s, 2.5); // standardprice
            realMapsincludingPros.put(s, 2.5); // standardprice
        }
        String f = "Bestandskonto,Bewertungsklasse,Einzelpostenanzeige moeglich,Erfolgskontentyp,Geschaeftsbereich,Gruppenkennzeichen,KZ EKBE,Kennzeichen: Posten nicht kopierbar ?,Kostenstelle,Kreditkontr_Bereich,Laufende Kontierung,PartnerPrctr,Profitcenter,Rechnungsbezug,Soll/Haben-Kennz_,Sperrgrund Menge,Steuerkennzeichen,Bewertungskreis,Steuerstandort,Umsatzwirksam,Verwaltung offener Posten,Werk,Zahlungsbedingung,Zahlungssperre,Alternative Kontonummer,Basismengeneinheit,BestPreisMngEinheit,Bestandsbuchung,Bestellmengeneinheit,Buchungsschluessel,Buchungszeilen-Id,ErfassungsMngEinh,Hauptbuchkonto,Kontoart,Kostenart,Kreditor,Material,Preissteuerung,Sachkonto,Vorgang,Vorgangsart GL,Wertestring,Betrag Hauswaehr,Betrag,Betrag_5,Gesamtbestand,Gesamtwert,Kreditkontr_betrag,Menge in BPME,Menge in ErfassME,Menge,Skontobasis,Label,Belegnummer,Position,Transaktionsart,Erfassungsuhrzeit\n";
        log.add(f); 
    }

    public void setSeed(long seed){
        this.random.setSeed(seed);
    }

    @Override
    public boolean add(Account a){
        for(Account ac : this.accounts){
            if(ac.getAccountnumber().equals(a.getAccountnumber())){
                return false;
            }
        }
        this.accounts.add(a);
        return true;
    }

    public double putInventory(String s, double d, boolean fraud, double realamount){

        if(d==0){
            return 0.0;
        }else if(d>0){
            if(fraud){
                inventory.put(s, inventory.get(s)+realamount); //Inventory only gets the actual amount
                realInventory.put(s, realInventory.get(s)+realamount);
                return d;
            }else{
                inventory.put(s, inventory.get(s)+ d);
                realInventory.put(s, realInventory.get(s)+d);
                return d;
            }
        }else{ // d<0
            d=d*(-1);
            realamount = realamount * (-1);
            if(fraud){
                if(realamount <= realInventory.get(s)){
                    inventory.put(s, inventory.get(s) -d);
                    realInventory.put(s, realInventory.get(s)-realamount);
                    return d;
                }else{ // a > realinventory
                    if(d <= realInventory.get(s)){
                        inventory.put(s, inventory.get(s)-d);
                        realInventory.put(s, realInventory.get(s)-d);
                        return d;
                    }else{
                        correctInventory();
                        double ret = realInventory.get(s);
                        realInventory.put(s, 0.0);
                        inventory.put(s, 0.0);
                        return ret;
                    }
                }
            }else{
                if(d <= realInventory.get(s)){
                    inventory.put(s, inventory.get(s) -d);
                    realInventory.put(s, realInventory.get(s) -d);
                    return d;
                }else{ // invencory difference !
                    correctInventory();
                    double ret = realInventory.get(s);
                    realInventory.put(s, 0.0);
                    inventory.put(s, 0.0);
                    return ret;
                }
            }
        }
    }

    private void correctInventory(){
        Map<String, Double> changedInventory = new HashMap<>();
        for(String str: inventory.keySet()){
            if(inventory.get(str).doubleValue() != realInventory.get(str).doubleValue()){
                double diff = inventory.get(str) - realInventory.get(str);
                inventory.put(str, realInventory.get(str));
                changedInventory.put(str, diff);
            }
        }
        if(changedInventory.keySet().size() !=0){
            bookShrinkage(changedInventory);
        }

    }

    private void bookShrinkage(Map<String, Double> lookup){


        String blgnmr= belegnummerCounter.getBelegnummer("49");
        String time = clock.getTimeStamp();

        int recepCounter=1;
        double totalvaluelost = 0;
        for(String name: lookup.keySet()) {
            boolean isEduct = false;
            for (String s : setUp.ressources.keySet()) {
                if (s.equals(name)) {
                    isEduct = true;
                    break;
                }
            }
            if (isEduct) {
                Ressource r = setUp.ressources.get(name);
                String unitsize = r.unitSize;
                String pricecontrole = r.priceControle;
                double amountlost = lookup.get(name);
                double valuelost = amountlost * mapsIncludingPros.get(name);
                totalvaluelost += valuelost;

                get("300000").book(AccountBooking.HABEN, valuelost);

                CSVData data = new CSVData(true,null,false,false,null,"0",false,true,null,null,null,"AA","AA",null,"H",null,null,"AA",null,false,false,"AA",null,null,null,unitsize,null,null,null,"99","M",unitsize,"300000","M",null,null,name,pricecontrole,null,"BSX","RMRU","WA01",Double.toString(round2(valuelost)),null,"0.0",null,null,"0.0","0",Integer.toString((int) amountlost),Integer.toString((int) amountlost),"0.0","Shrinkage",blgnmr,Integer.toString(recepCounter),"Materialabgang",time);

               log.add(CSVStringBuilder(data));

            } else {
                Product r = setUp.products.get(name);
                String unitsize = r.unitSize;
                String pricecontrole = r.priceControle;
                double amountlost = lookup.get(name);
                double valuelost = amountlost * mapsIncludingPros.get(name);
                totalvaluelost += valuelost;
                get("792000").book(AccountBooking.HABEN, valuelost);

                CSVData data = new CSVData(true,null,false,false,null,"0",false,true,null,null,null,null,"AA",null,"H",null,null,"AA",null,false,false,"AA",null,null,"792000.0",unitsize,null,null,null,"99","M",unitsize,"792000","M",null,null,name,"S",null,"BSX","RMWL","WA01",Double.toString(round2(valuelost)),null,Double.toString(-round2(valuelost)),null,null,"0.0","0",Integer.toString((int) amountlost)," (int) amountlost","0.0","Shrinkage",blgnmr,Integer.toString(recepCounter),"Materialabgang",time);

                log.add(CSVStringBuilder(data));
            }
            recepCounter++;
        }
        get("479000").book(AccountBooking.SOLL, totalvaluelost);

        CSVData data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,"479000.0",null,null,null,null,"40",null,null,"479000","S","479000.0",null,null,null,null,null,"RFBU",null,Double.toString(round2(totalvaluelost)),null,"0.0",null,null,"0.0","0","0","0","0.0","Shrinkage",blgnmr,Integer.toString(recepCounter),"Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));
    }





    public double getInventory(String s){
        if (inventory.containsKey(s)){
            return inventory.get(s);
        }else{
            return 0;
        }
    }

    public double getMapsIncludingPros(String s){
        if (this.mapsIncludingPros.containsKey(s)) {
            return this.mapsIncludingPros.get(s);
        }else{
            return 0.0;
        }
    }
    public double getRealMapsIncludingPros(String s){
        if(this.realMapsincludingPros.containsKey(s)){
            return this.realMapsincludingPros.get(s);
        }else{
            return 0.0;
        }

    }

    public Account get(String accountnumber){
        for(Account a : this.accounts){
            if(a.getAccountnumber().equals(accountnumber)){
                return a;
            }
        }
        return null;
    }

    @Override
    public Account get(int i){
        return this.accounts.get(i);
    }

    public void printAll() throws IOException {

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("mas_scm_data.csv", false));
        for (int i=0; i<log.size()-1; i++){
            bufferedWriter.write(log.get(i));
        }
        String s = log.get(log.size()-1).substring(0, log.get(log.size()-1).lastIndexOf('\n')); // remove last linebreak
        bufferedWriter.write(s);
        bufferedWriter.close();
        // cash , land, buildings, machines, receivables, payables, stock, loan, inventory ,
        System.out.println("cash: "+cash());
        System.out.println("machines: "+machines());
        System.out.println("land: "+get("1000").getSaldo());
        System.out.println("buildings: "+get("2000").getSaldo());
        System.out.println("stocks: "+get("70000").getSaldo());
        System.out.println("Loan: "+get("113101").getSaldo());
        System.out.println("receivables: "+get("140000").getSaldo()); // asset
        System.out.println("payables: "+get("160000").getSaldo()); // liabilites
        System.out.println("discount rate: "+getDiscountRate());

        System.out.println("inventory : ");
        for(String sas: realInventory.keySet()){
            System.out.println(sas+" : "+realInventory.get(sas));
        }

        BufferedWriter fraudCSVWriter = new BufferedWriter(new FileWriter("mas_scm_data_fraudOnly.csv", false));
        //Split Log into 1 CSV Line per List Entry

        // filter Fraud Occurrences
        ArrayList<String> fraudList = new ArrayList<String>(log.stream().filter(t -> !t.contains("NonFraud") && !t.contains("Shrinkage")).toList());
        // remove header
//        fraudList.remove(0);
        // Apply Key
        int key_size = 52; //hacky solution currently, will look into better ways

        for (String element: fraudList) {
            BitSet bools = new BitSet(key_size);
            if(element.contains("InvoiceKickback1")){  // purchase kickback (raw materials, == IK2)
                char[] key = "0000000000000000000000000000000000000000001100001110".toCharArray();
                BitSet keyBits = new BitSet(key_size);
                for (int i = 0; i < key.length; i++) {
                    if(key[i] == '1'){
                        keyBits.set(i);
                    }
                }
                bools.or(keyBits);
            }
            if(element.contains("InvoiceKickback2")){  // selling kickback (end products)
                char[] key = "0000000000000000000000000000000000000000001000000010".toCharArray();
                BitSet keyBits = new BitSet(key_size);
                for (int i = 0; i < key.length; i++) {
                    if(key[i] == '1'){
                        keyBits.set(i);
                    }
                }
                bools.or(keyBits);
            }
            if(element.contains("Theft1")){  // theft of raw materials (== L3)
                char[] key = "0000000000000000000000000000000000000000001100001110".toCharArray();
                BitSet keyBits = new BitSet(key_size);
                for (int i = 0; i < key.length; i++) {
                    if(key[i] == '1'){
                        keyBits.set(i);
                    }
                }
                bools.or(keyBits);
            }
            if(element.contains("CorporateInjury")){
                char[] key = "0000000000000000000000000000000000000000000001000000".toCharArray();
                BitSet keyBits = new BitSet(key_size);
                for (int i = 0; i < key.length; i++) {
                    if(key[i] == '1'){
                        keyBits.set(i);
                    }
                }
                bools.or(keyBits);
            }
            String finalKey = "";
            for(int i = 0; i < key_size; i++){
                if(bools.get(i)){
                    finalKey += "X,";
                }
                else{
                    finalKey += ",";
                }
            }
            List<String> elementSubstrings = Arrays.stream(element.split(",")).toList();
            finalKey += elementSubstrings.get(elementSubstrings.size()-5) + ",";
            finalKey += elementSubstrings.get(elementSubstrings.size()-4) + ",";
            finalKey += elementSubstrings.get(elementSubstrings.size()-3) + ",";
            finalKey += elementSubstrings.get(elementSubstrings.size()-2) + ",";
            finalKey += elementSubstrings.get(elementSubstrings.size()-1);

            if(fraudList.indexOf(element) != 0){
                fraudList.set(fraudList.indexOf(element), finalKey);
            }
        }

        for(String element : fraudList){
            fraudCSVWriter.write(element);
        }
        fraudCSVWriter.close();
    }

    public void init(){
        long stocks = setUp.initstocks;
        long land =setUp.initland;
        long buildings=setUp.initbuildings;
        long machines =setUp.initmachines;
        long loans=setUp.initloans;
        this.baselabor = setUp.baselabor;
        this.baseoverhead = setUp.baseoverhead;
        this.basesga = setUp.basesga;
        init(stocks, land, buildings, machines, loans);
    }
    public void init(long stocks, long land, long building, long machines, long loans){ // init inventory in xmlparser
        // book initial stuff here
        // 40 113300 an 50 70000 , 20000000 // stocks
        // 40 1000 an 50 113300 , 500000 // land
        // 40 2000 an 50 113300 ,1500000 // building
        // 40 11000 an 50 113300 ,24000000 // machines
        // 40 113300 an 50 113101 , 8000000 //loans
        this.clock= new ERPClock(); // sets base timestamp

        String blgnmr= belegnummerCounter.getBelegnummer("1");
        String time = clock.getTimeStamp();
        get("113300").book(AccountBooking.SOLL, stocks);

        CSVData data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"S",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"40",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Long.toString(stocks),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        get("70000").book(AccountBooking.HABEN, stocks);

        data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"70000.0",null,null,null,null,"50",null,null,"70000","S",null,null,null,null,null,null,"RFBU",null,Long.toString(stocks),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        blgnmr = belegnummerCounter.getBelegnummer("1");
        time = clock.getTimeStamp();
        get("1000").book(AccountBooking.SOLL, land);

        data = new CSVData(true,null,false,false,null,"0",false,false,null,null,null,null,null,null,"S",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"40",null,null,"1000","S",null,null,null,null,null,null,"RFBU",null,Long.toString(land),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        get("113300").book(AccountBooking.HABEN, land);

        data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Long.toString(land),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        blgnmr = belegnummerCounter.getBelegnummer("1");
        time = clock.getTimeStamp();
        get("2000").book(AccountBooking.SOLL, building);

        data = new CSVData(true,null,false,false,null,"0",false,false,null,null,null,null,null,null,"S",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"40",null,null,"2000","S",null,null,null,null,null,null,"RFBU",null,Long.toString(building),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        get("113300").book(AccountBooking.HABEN, building);

        data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Long.toString(building),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        blgnmr = belegnummerCounter.getBelegnummer("1");
        time = clock.getTimeStamp();
        get("11000").book(AccountBooking.SOLL, machines);

        data = new CSVData(true,null,false,false,null,"0",false,false,null,null,null,null,null,null,"S",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"40",null,null,"11000","S",null,null,null,null,null,null,"RFBU",null,Long.toString(machines),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        get("113300").book(AccountBooking.HABEN, machines);

        data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Long.toString(machines),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        blgnmr = belegnummerCounter.getBelegnummer("1");
        time = clock.getTimeStamp();
        get("113300").book(AccountBooking.SOLL, loans);

        data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"S",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"40",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Long.toString(loans),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

        get("113101").book(AccountBooking.HABEN, loans);

        data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,true,null,null,null,"113101.0",null,null,null,null,"50",null,null,"113101","S",null,null,null,null,null,null,"RFBU",null,Long.toString(loans),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

        log.add(CSVStringBuilder(data));

    }

    public double cash(){
        return get("113300").getSaldo();
    }
    public double machines(){return get("11000").getSaldo(); }
    public double depreciationMachines(){
        return round2(get("11000").getSaldo()/setUp.afa.machinelife);
    }
    public double depreciationBuilding(){
        return round2(get("2000").getSaldo()/setUp.afa.buildinglife);
    }
    private double round2(double d){
        return Math.round(d * 100.0) / 100.0;
    }
    public double getFixCost(){
        double a = 0.0;
        a += depreciationBuilding();
        a += depreciationMachines();
        a += getDailyInterest()*5;
        //a += 5000; // average shipping cost
        a += basesga+baselabor+baseoverhead; //
        return a;
    }


    public BookingResult book(Booking b, AgentType agent ){ // swtich case of all businessprocedures -> automated booking in erpl
        BookingResult BR = new BookingResult();
        String a;
        if(agent == AgentType.dl){
            a="DL";
        }else if(agent == AgentType.erp_l){
            a="ERPL";
            System.out.println("Agent: "+ a);
            System.out.println("cash: "+(int)(cash()));
            System.out.println("machines" + machines()+"\nstorage: ");
            for(String item : setUp.products.keySet()){
                System.out.println(item +"->"+getInventory(item));
                ControlUI.Instance.UpdateStorage(item, String.format("%.1f",getInventory(item)));
            }
            for(String item : setUp.ressources.keySet()){
                ControlUI.Instance.UpdateStorage(item, String.format("%.1f",getInventory(item)));
            }
        }else if(agent == AgentType.mk){
            a="MK";
            System.out.println("Agent: "+ a);
            System.out.println("cash: "+(int)(cash()));
            System.out.println("machines" + machines()+"\nstorage: ");
            for(String item : setUp.products.keySet()){
                System.out.println(item +"->"+getInventory(item));
                ControlUI.Instance.UpdateStorage(item, String.format("%.1f",getInventory(item)));
            }
            for(String item : setUp.ressources.keySet()){
                ControlUI.Instance.UpdateStorage(item, String.format("%.1f",getInventory(item)));
            }
        }else{
            a="SC";
            System.out.println("Agent: "+ a);
            System.out.println("cash: "+(int)(cash()));
            System.out.println("machines" + machines()+"\nstorage: ");
            for(String item : setUp.products.keySet()){
                System.out.println(item +"->"+getInventory(item));
                ControlUI.Instance.UpdateStorage(item, String.format("%.1f",getInventory(item)));
            }
            for(String item : setUp.ressources.keySet()){
                ControlUI.Instance.UpdateStorage(item, String.format("%.1f",getInventory(item)));
            }
        }



        //StringBuffer buffer = new StringBuffer();
        if(agent== null){
        }else if(agent== AgentType.dl){
            //WA -> verkauf , S -> zahlung angekommen
            if(b.getGV()==businessProcedure.WA){
                // foreach res : 89 300000 an 96 191100

                shipmentCostIndex++ ;// book shipment cost later

                String belgnmr = belegnummerCounter.getBelegnummer("49");
                String timestamp = clock.getTimeStamp();

                int i=0;
                for(String ressname : b.getREQ().items){
                    i++;
                    //String pricecontrole = setUp.products.get(ressname).priceControle;
                    String unitsize = setUp.products.get(ressname).unitSize;
                    double amount = b.getREQ().itemAmount.get(ressname);
                  // double price = b.getREQ().pricelist.get(ressname); Standardprice
                    double price = mapsIncludingPros.get(ressname);

                    double returnedamount = putInventory(ressname, (-1)*b.getREQ().itemAmount.get(ressname), false, 0);
                    if (returnedamount != b.getREQ().itemAmount.get(ressname)){
                        BR.success=false;
                        BR.faults.put(ressname, returnedamount);
                    }





                    get("792000").book(AccountBooking.HABEN, amount*price);

                    CSVData data = new CSVData(true,null,false,false,null,"0",false,true,null,null,null,null,"AA",null,"H",null,null,"AA",null,false,false,"AA",null,null,"792000.0",unitsize,null,null,null,"99","M",unitsize,"792000","M",null,null,ressname,"S",null,"BSX","RMWL","WA01",Double.toString(round2(amount*price)),null,Double.toString(-round2(amount*price)),null,null,"0.0","0",Double.toString(amount),Double.toString(amount),"0.0","NonFraud",belgnmr,Integer.toString(i),"Materialabgang",timestamp);

                    log.add(CSVStringBuilder(data));

                    i++;
                    get("893010").book(AccountBooking.SOLL, amount*price);

                    data = new CSVData(false,null,true,true,null,"0",false,false,null,null,null,null,"AA",null,"S",null,null,"AA",null,false,false,"AA",null,null,null,unitsize,null,null,null,"81","S",unitsize,"893010","S",null,null,ressname,"S",null,"GBB","RMWL","WA01",Double.toString(round2(amount*price)),null,"0.0",null,null,"0.0","0",Double.toString(amount),Double.toString(amount),"0.0","NonFraud",belgnmr,Integer.toString(i),"Sachkontenbuchung",timestamp);

                    log.add(CSVStringBuilder(data));

                }

                // rechnung

                 belgnmr = belegnummerCounter.getBelegnummer("9");
                 timestamp = clock.getTimeStamp();
                 i=1;
                 double total=0;
                 for(String ressname : b.getREQ().items){
                     i++;
                     //String vendor = setUp.ressources.get(ressname).Vendor;
                     //String pricecontrole = setUp.ressources.get(ressname).priceControle;
                     String unitsize = setUp.products.get(ressname).unitSize;
                     double amount = b.getREQ().itemAmount.get(ressname);
                     double price = b.getREQ().pricelist.get(ressname);
                     boolean fraud = b.getREQ().Fraudulent;
                     total += amount*price;

                     StringBuilder fraudStr = new StringBuilder();
                     if(fraud){
                         for(String s: b.getREQ().Fraudtypes.keySet()){
                             fraudStr.append(s).append("-");
                         }
                         fraudStr.append("Fraud");
                     }

                     get("800000").book(AccountBooking.HABEN, amount*price);

                     CSVData data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"H",null,null,null,null,false,false,"AA",null,null,"800000.0",unitsize,null,null,null,"50",null,null,"800000","S","800000.0",null,ressname,null,null,null,"SD00",null,Double.toString(round2(amount*price)),null,"0.0",null,null,"0.0","0","0",Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",belgnmr,Integer.toString(i),"Sachkontenbuchung",timestamp);

                     log.add(CSVStringBuilder(data));

                 }
                 get("140000").book(AccountBooking.SOLL, total);

                CSVData data = new CSVData(true,null,true,false,null,"0",false,false,null,"AA",null,null,null,null,"S",null,null,null,null,true,true,null,"1.0",null,"140000.0",null,null,null,null,"1",null,null,"140000","D",null,null,null,null,"140000.0",null,"SD00",null,Double.toString(round2(total)),null,"0.0",null,null,Double.toString(round2(total)),"0","0","0",Double.toString(round2(total)),"NonFraud",belgnmr,"1","DebitorischeRechnung",timestamp);

                log.add(CSVStringBuilder(data));


                // rebooking if false storage :
                if(!BR.success){
                    i=1;
                    belgnmr = belegnummerCounter.getBelegnummer("50");
                    for(String ressname: BR.faults.keySet()){
                        String unitsize= setUp.products.get(ressname).unitSize;
                        String pricecontrole = setUp.products.get(ressname).priceControle;
                        double amount = b.getREQ().itemAmount.get(ressname) - BR.faults.get(ressname);
                        double price = b.getREQ().pricelist.get(ressname);
                        boolean fraud = b.getREQ().Fraudulent;

                        StringBuilder fraudStr = new StringBuilder();
                        if(fraud){
                            for(String s: b.getREQ().Fraudtypes.keySet()){
                                fraudStr.append(s).append("-");
                            }
                            fraudStr.append("Fraud");
                        }

                        get("792000").book(AccountBooking.SOLL, amount*price);

                        data = new CSVData(true,null,false,false,null,"0",false,true,null,null,null,null,"AA",null,"S",null,null,"AA",null,false,false,"AA",null,null,"792000.0",unitsize,null,null,null,"89","M",unitsize,"792000","M",null,null,ressname,"S",null,"BSX","RMWL","WA01",Double.toString(round2(amount*price)),null,Double.toString(-round2(amount*price)),null,null,"0.0","0",Integer.toString((int)amount),Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",belgnmr,Integer.toString(i),"Materialzugang",timestamp);

                        log.add(CSVStringBuilder(data));

                        i++;
                        get("893010").book(AccountBooking.HABEN, amount*price);

                        data = new CSVData(false,null,true,true,null,"0",false,false,null,null,null,null,"AA",null,"H",null,null,"AA",null,false,false,"AA",null,null,null,unitsize,null,null,null,"91","S",unitsize,"893010","S",null,null,ressname,"S",null,"GBB","RMWL","WA01",Double.toString(round2(amount*price)),null,"0.0",null,null,"0.0","0",Integer.toString((int)amount),Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",belgnmr,Integer.toString(i),"Sachkontenbuchung",timestamp);

                        log.add(CSVStringBuilder(data));

                    }
                    i=1;
                    total =0;
                    belgnmr = belegnummerCounter.getBelegnummer("1");
                    for(String ressname: BR.faults.keySet()) {
                        i++;
                        String unitsize = setUp.products.get(ressname).unitSize;
                        String pricecontrole = setUp.products.get(ressname).priceControle;
                        double amount = b.getREQ().itemAmount.get(ressname) - BR.faults.get(ressname);
                        double price = b.getREQ().pricelist.get(ressname);
                        boolean fraud = b.getREQ().Fraudulent;

                        StringBuilder fraudStr = new StringBuilder();
                        if(fraud){
                            for(String s: b.getREQ().Fraudtypes.keySet()){
                                fraudStr.append(s).append("-");
                            }
                            fraudStr.append("Fraud");
                        }

                        get("800000").book(AccountBooking.SOLL, amount*price);

                        data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,"AA",null,null,"800000.0",unitsize,null,null,null,"50",null,null,"800000","S","800000.0",null,ressname,null,null,null,"SD00",null,Double.toString(round2(amount*price)),null,"0.0",null,null,"0.0","0","0",Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",belgnmr,Integer.toString(i),"Sachkontenbuchung",timestamp);

                        log.add(CSVStringBuilder(data));

                        total += amount*price;
                    }


                    get("113300").book(AccountBooking.HABEN, total);

                    data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,"AA",null,"H",null,null,null,null,false,false,"AA",null,null,"113300.0",null,null,null,null,"40",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Double.toString(round2(total)),null,"0.0",null,null,"0.0","0","0","0","0.0","ShrinkageCorrection",belgnmr,"1","Sachkontenbuchung",timestamp);

                    log.add(CSVStringBuilder(data));



                }
            }else{
                // DR an cash || DG
                double remain = 1;
                String belgnmr = belegnummerCounter.getBelegnummer("14");
                String timestamp = clock.getTimeStamp();
                double total =0;
                for(String ressname : b.getREQ().items){
                    total += b.getREQ().itemAmount.get(ressname)* b.getREQ().pricelist.get(ressname);
                }

                get("113300").book(AccountBooking.SOLL, total*remain);

                CSVData data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,"AA",null,"S",null,null,null,null,false,false,"AA",null,null,"113300.0",null,null,null,null,"40",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Double.toString(round2(total*remain)),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",belgnmr,"1","Sachkontenbuchung",timestamp);

                log.add(CSVStringBuilder(data));

                get("140000").book(AccountBooking.HABEN, total);

                data = new CSVData(true,null,true,false,null,"0",false,false,null,"AA",null,null,null,"V","H",null,null,null,null,true,true,null,"1.0",null,"140000.0",null,null,null,null,"11",null,null,"140000","D",null,null,null,null,"140000.0",null,"RFBU",null,Double.toString(round2(total)),null,"0.0",null,null,Double.toString(round2(total)),"0","0","0","0.0","NonFraud",belgnmr,"2","DebitorischeGutschrift",timestamp);

                log.add(CSVStringBuilder(data));
            }
        }else if(agent== AgentType.sc){
            //we & change map (getsetup ressoirces map -> multiply with inventory + new stuff / new inventory )
            // gv = S -> rechnung angekommen
            if(b.getGV() == businessProcedure.WE){

                String belgnmr = belegnummerCounter.getBelegnummer("50");
                String timestamp = clock.getTimeStamp();

                int i=0;
                for(String ressname : b.getREQ().items){
                    i++;
                    String vendor = setUp.ressources.get(ressname).Vendor;
                    String pricecontrole = setUp.ressources.get(ressname).priceControle;
                    String unitsize = setUp.ressources.get(ressname).unitSize;
                    double amount = b.getREQ().itemAmount.get(ressname);
                    double price = b.getREQ().pricelist.get(ressname);
                    boolean fraud = b.getREQ().Fraudulent;

                    StringBuilder fraudStr = new StringBuilder();
                    if(fraud){
                        for(String s: b.getREQ().Fraudtypes.keySet()){
                            fraudStr.append(s).append("-");
                        }
                        fraudStr.append("Fraud");
                    }


                    mapsIncludingPros.put(ressname, (mapsIncludingPros.get(ressname)*inventory.get(ressname)+amount*price) / (inventory.get(ressname)+amount) );

                    if(!b.getREQ().Fraudulent){ // if fraud prices arent real -> do not include
                        realMapsincludingPros.put(ressname, (realMapsincludingPros.get(ressname)*inventory.get(ressname)+amount*price) / (inventory.get(ressname)+amount) );
                    }

                    boolean fraTheft1 = b.getREQ().Fraudtypes.containsKey("Theft1");
                    double real = fraTheft1? b.getREQ().realAmounts.get(ressname):0;
                    putInventory(ressname, amount, fraTheft1, real);

                    get("300000").book(AccountBooking.SOLL, amount*price);

                    CSVData data = new CSVData(true,"3000.0",false,false,null,"0",true,true,null,null,"0.0",null,"AA",null,"S",null,"XI","AA","DE0000000",false,false,"AA",null,null,null,unitsize,unitsize,"0.0",unitsize,"89","M",unitsize,"300000","M",null,vendor,ressname,pricecontrole,null,"BSX","RMWE","WE01",Double.toString(fraTheft1?round2(real*price):round2(amount*price)),Double.toString(round2(amount*price)),"0",Integer.toString((int)(double)inventory.get(ressname)),Double.toString(round2(inventory.get(ressname)*mapsIncludingPros.get(ressname))),"0",Integer.toString(fraTheft1?(int)real:(int)amount),Integer.toString(fraTheft1?(int)real:(int)amount),Integer.toString(fraTheft1?(int)real:(int)amount),"0",fraud? fraudStr.toString() : "NonFraud",belgnmr,Integer.toString(i),"Materialzugang",timestamp);

                    log.add(CSVStringBuilder(data));

                    i++;

                    get("191100").book(AccountBooking.HABEN, amount*price);

                    data = new CSVData(true,"3000.0",true,false,null,"0",true,true,null,null,"0.0",null,"AA",null,"H",null,"XI","AA","DE0000000",false,true,"AA",null,null,"191100.0",unitsize,unitsize,"0.0",unitsize,"96","W",unitsize,"191100","S",null,vendor,ressname,pricecontrole,null,"WRX","RMWE","WE01",Double.toString(fraTheft1?round2(real*price):round2(amount*price)),Double.toString(round2(amount*price)),"0",Integer.toString((int)(double)inventory.get(ressname)),Double.toString(round2(inventory.get(ressname)*mapsIncludingPros.get(ressname))),"0",Integer.toString(fraTheft1?(int)real:(int)amount),Integer.toString(fraTheft1?(int)real:(int)amount),Integer.toString(fraTheft1?(int)real:(int)amount),"0",fraud? fraudStr.toString() : "NonFraud",belgnmr,Integer.toString(i),"Sachkontenbuchung",timestamp);

                    log.add(CSVStringBuilder(data));
                }

            }else{ // rechnung angekommen

                String timestamp = clock.getTimeStamp();
                String belegnmr = belegnummerCounter.getBelegnummer("51");


                String vendor = setUp.ressources.get(b.getREQ().items.get(0)).Vendor;
                int i=1; // 1= bill , 2-x are we/re's
                int total =0;
                for(String ressname : b.getREQ().items){
                    i++;
                    String pricecontrole = setUp.ressources.get(ressname).priceControle;
                    String unitsize = setUp.ressources.get(ressname).unitSize;
                    double amount = b.getREQ().itemAmount.get(ressname);
                    double price = b.getREQ().pricelist.get(ressname);
                    boolean fraud = b.getREQ().Fraudulent;

                    StringBuilder fraudStr = new StringBuilder();
                    if(fraud){
                        for(String s: b.getREQ().Fraudtypes.keySet()){
                            fraudStr.append(s).append("-");
                        }
                        fraudStr.append("Fraud");
                    }

                    total += amount*price;

                    get("191100").book(AccountBooking.SOLL, amount*price);

                    CSVData data = new CSVData(true,"3000.0",true,false,null,"1",true,true,null,null,"0.0",null,"AA",null,"S",null,"XI","AA","DE0000000",false,true,"AA",null,null,"191100.0",unitsize,unitsize,"0.0",unitsize,"86","W",unitsize,"191100","S",null,null,ressname,pricecontrole,null,"WRX","RMRP","RE01",Double.toString(round2(amount*price)),Double.toString(round2(amount*price)),"0",Integer.toString((int)(double)inventory.get(ressname)),Double.toString(round2(inventory.get(ressname)*mapsIncludingPros.get(ressname))),"0",Integer.toString((int)amount),Integer.toString((int)amount),Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",belegnmr,Integer.toString(i),"Sachkontenbuchung",timestamp);

                    log.add(CSVStringBuilder(data));

                }

                // bill

                get("160000").book(AccountBooking.HABEN, total);

                CSVData data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,"XI",null,null,true,true,null,"1.0","R","160000.0",null,null,null,null,"31",null,null,"160000","K",null,vendor,null,null,"160000.0","KBS","RMRP",null,Double.toString(round2(total)),null,"0.0",null,null,"0.0","0","0","0",Double.toString(round2(total)),"NonFraud",belegnmr,"1","KreditorischeRechnung",timestamp);

                log.add(CSVStringBuilder(data));
            }
        }else if(agent== AgentType.mk){
            // wep or wap  invetoryRESS -> PRO : WAP , PRO->InventoryPRO : WEP
            if(b.getGV()== businessProcedure.WAP){ // raw materials -> verrechnungskonto

                int i=0;
                String blg = belegnummerCounter.getBelegnummer("49");
                String timestamp= clock.getTimeStamp();


                double min=b.getREQ().itemAmount.get(b.getREQ().items.get(0));
                SimpleSTKL graph = setUp.graphs.getStkl(b.getREQ().items.get(0));
                for(int t =0; t< graph.ress.length; t++ ){
                    if(realInventory.get(graph.ress[t].identifier) < ((REQ_MK) b.getREQ()).eductamount.get(graph.ress[t].identifier) );
                    BR.success=false;
                    min = Math.min(min, realInventory.get(graph.ress[t].identifier) / graph.amount[t]);
                }
                for(int t =0; t< graph.ress.length; t++ ){
                    ((REQ_MK) b.getREQ()).eductamount.put(graph.ress[t].identifier, graph.amount[t]*min);
                }
                b.getREQ().itemAmount.put(b.getREQ().items.get(0), min);
                BR.b= b;
                if(min <= 0) {return BR; }


                for(String ressname : ((REQ_MK)(b.getREQ())).educts ){
                    i++;
                    double amount = ( (REQ_MK) (b.getREQ()) ).eductamount.get(ressname);
                    double price = mapsIncludingPros.get(ressname);
                    String pricecontrole = setUp.ressources.get(ressname).priceControle;
                    boolean fraud = b.getREQ().Fraudulent;
                    String unitsize = setUp.ressources.get(ressname).unitSize;

                    putInventory(ressname, ((REQ_MK)(b.getREQ())).eductamount.get(ressname)*(-1), false, 0 );

                    StringBuilder fraudStr = new StringBuilder();
                    if(fraud){
                        for(String s: b.getREQ().Fraudtypes.keySet()){
                            fraudStr.append(s).append("-");
                        }
                        fraudStr.append("Fraud");
                    }

                    get("300000").book(AccountBooking.HABEN, amount*price);

                    CSVData data = new CSVData(true,null,false,false,null,"0",false,true,null,null,null,"AA","AA",null,"H",null,null,"AA",null,false,false,"AA",null,null,null,unitsize,null,null,null,"99","M",unitsize,"300000","M",null,null,ressname,pricecontrole,null,"BSX","RMRU","WA01",Double.toString(round2(amount*price)),null,"0.0",null,null,"0.0","0",Integer.toString((int)amount),Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",blg,Integer.toString(i),"Materialabgang",timestamp);

                    log.add(CSVStringBuilder(data));

                    i++;
                    get("400000").book(AccountBooking.SOLL, amount*price);

                    data = new CSVData(false,null,true,true,null,"0",false,false,null,null,null,"AA","AA",null,"S",null,null,"AA",null,false,false,"AA",null,null,"400000.0",unitsize,null,null,null,"81","S",unitsize,"400000","S","400000.0",null,ressname,pricecontrole,null,"GBB","RMRU","WA01",Double.toString(round2(amount*price)),null,"0.0",null,null,"0.0","0",Integer.toString((int)amount),Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",blg,Integer.toString(i),"Sachkontenbuchung",timestamp);

                    log.add(CSVStringBuilder(data));

                }

            }else{ // Fxx -> invent.


                int i=0;
                String blg = belegnummerCounter.getBelegnummer("49");
                String timestamp= clock.getTimeStamp();

                for(String ressname : b.getREQ().items){
                    i++;
                    double amount = b.getREQ().itemAmount.get(ressname);
                    double price = mapsIncludingPros.get(ressname); // standardprice
                    String pricecontrole = setUp.products.get(ressname).priceControle;
                    boolean fraud = b.getREQ().Fraudulent;
                    String unitsize = setUp.products.get(ressname).unitSize;
                    putInventory(ressname, b.getREQ().itemAmount.get(ressname), false, 0);

                    LocalDateTime nowtime = LocalDateTime.now();
                    System.out.println("MK booked WEP at: "+nowtime.toString());
                    ControlUI.Instance.AddInfoLogEntry("MK booked WEP at: "+nowtime.toString());

                    StringBuilder fraudStr = new StringBuilder();
                    if(fraud){
                        for(String s: b.getREQ().Fraudtypes.keySet()){
                            fraudStr.append(s).append("-");
                        }
                        fraudStr.append("Fraud");
                    }

                    get("792000").book(AccountBooking.SOLL, amount*price);

                    CSVData data = new CSVData(true,null,false,false,null,"0",false,true,null,null,null,"AA","AA",null,"S",null,null,"AA",null,false,false,"AA",null,null,"792000.0",unitsize,null,null,null,"89","M",unitsize,"792000","M",null,null,ressname,"S",null,"BSX","RMRU","WF01",Double.toString(round2(amount*price)),null,Double.toString(round2(amount*price)),null,null,"0.0","0",Integer.toString((int)amount),Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",blg,Integer.toString(i),"Materialzugang",timestamp);

                    log.add(CSVStringBuilder(data));

                    i++;
                    get("895000").book(AccountBooking.HABEN, amount*price);

                    data = new CSVData(false,null,true,true,null,"0",false,false,null,null,null,"AA","AA",null,"H",null,null,"AA",null,false,false,"AA",null,null,"895000.0",unitsize,null,null,null,"91","S",unitsize,"895000","S","895000.0",null,ressname,"S",null,"GBB","RMRU","WF01",Double.toString(round2(price*amount)),null,"0.0",null,null,"0.0","0",Integer.toString((int)amount),Integer.toString((int)amount),"0.0",fraud? fraudStr.toString() : "NonFraud",blg,Integer.toString(i),"Sachkontenbuchung",timestamp);

                    log.add(CSVStringBuilder(data));
                }

            }

        }else if(agent== AgentType.erp_l){
            // payment of bills if req instanceof REQ or of weekly fixcost if req instanceof REQ_FIX
            if(b.getREQ() instanceof REQ_FIX) {
                // interest on loan (476900)
                // depreciation on buildings and machines (2010, 11010)
                // labor cost (500000)
                // factory overhead (510000)
                // general expenses (520000)

                String blgnmr = null;
                String time = null;

                /**   shipping cost **/
                if(shipmentCostIndex >0){
                    shipmentCostIndex=0;
                blgnmr = belegnummerCounter.getBelegnummer("1");
                time = clock.getTimeStamp();

                get("113300").book(AccountBooking.HABEN, setUp.shipmentCost*shipmentCostIndex);

                CSVData data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Double.toString(setUp.shipmentCost*shipmentCostIndex),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                get("472000").book(AccountBooking.SOLL, setUp.shipmentCost*shipmentCostIndex);

                data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"40",null,null,"472000","S","472000.0",null,null,null,null,null,"RFBU",null,Double.toString(setUp.shipmentCost*shipmentCostIndex),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));
            }

                blgnmr = belegnummerCounter.getBelegnummer("1");
                time = clock.getTimeStamp();

                get("2010").book(AccountBooking.HABEN, depreciationBuilding());

                CSVData data = new CSVData(true,null,false,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"50",null,null,"2010","S",null,null,null,null,null,null,"RFBU",null,Double.toString(depreciationBuilding()),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                get("211120").book(AccountBooking.SOLL, depreciationBuilding());

                data = new CSVData(false,null,false,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"40",null,null,"211120","S","211120.0",null,null,null,null,null,"RFBU",null,Double.toString(depreciationBuilding()),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                blgnmr = belegnummerCounter.getBelegnummer("1");
                time = clock.getTimeStamp();

                get("11010").book(AccountBooking.HABEN, depreciationMachines());

                data = new CSVData(true,null,false,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"50",null,null,"11010","S",null,null,null,null,null,null,"RFBU",null,Double.toString(depreciationMachines()),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                get("211130").book(AccountBooking.SOLL, depreciationMachines());

                data = new CSVData(false,null,false,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,null,null,null,null,null,"40",null,null,"211130","S","211130.0",null,null,null,null,null,"RFBU",null,Double.toString(depreciationMachines()),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                blgnmr = belegnummerCounter.getBelegnummer("1");
                time = clock.getTimeStamp();
                double rand = random.nextDouble();
                double ff = 1;
                if(rand >= 1- setUp.fraudLikelihood.get("cj")*setUp.fraudLikelihood.get("AllInAll")){
                    ff = ff + setUp.fraudvalues.get("cj");
                }

                get("113300").book(AccountBooking.HABEN, this.baselabor*ff);

                data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Double.toString(round2(this.baselabor*ff)),null,"0.0",null,null,"0.0","0","0","0","0.0",(ff!=1)?"CorporateInjury-Fraud":"NonFraud",blgnmr,"1","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                get("500000").book(AccountBooking.SOLL, this.baselabor*ff);

                data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,"500000.0",null,null,null,null,"40",null,null,"500000","S","500000.0",null,null,null,null,null,"RFBU",null,Double.toString(round2(this.baselabor*ff)),null,"0.0",null,null,"0.0","0","0","0","0.0",(ff!=1)?"CorporateInjury-Fraud":"NonFraud",blgnmr,"2","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                blgnmr = belegnummerCounter.getBelegnummer("1");
                time = clock.getTimeStamp();

                get("113300").book(AccountBooking.HABEN, this.baseoverhead);

                data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Long.toString(this.baseoverhead),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                get("510000").book(AccountBooking.SOLL, this.baseoverhead);

                data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,"510000.0",null,null,null,null,"40",null,null,"510000","S","510000.0",null,null,null,null,null,"RFBU",null,Long.toString(this.baseoverhead),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                blgnmr = belegnummerCounter.getBelegnummer("1");
                time = clock.getTimeStamp();

                get("113300").book(AccountBooking.HABEN, basesga);

                data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Long.toString(basesga),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                get("520000").book(AccountBooking.SOLL, basesga);

                data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,"520000.0",null,null,null,null,"40",null,null,"520000","S","520000.0",null,null,null,null,null,"RFBU",null,Long.toString(basesga),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

                log.add(CSVStringBuilder(data));

                double interest = round2(((REQ_FIX) b.getREQ()).interest);
                System.out.println("interest paymentent of : "+interest);
                if(interest>0) {
                    blgnmr = belegnummerCounter.getBelegnummer("1");
                    time = clock.getTimeStamp();

                    get("113300").book(AccountBooking.HABEN, interest);

                    data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,null,null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Double.toString(interest),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"1","Sachkontenbuchung",time);

                    log.add(CSVStringBuilder(data));

                    get("476900").book(AccountBooking.SOLL, interest);

                    data = new CSVData(false,null,true,true,"1.0","0",false,false,"AA",null,null,null,"AA",null,"S",null,null,null,null,false,false,null,null,null,"476900.0",null,null,null,null,"40",null,null,"476900","S","476900.0",null,null,null,null,null,"RFBU",null,Double.toString(interest),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blgnmr,"2","Sachkontenbuchung",time);

                    log.add(CSVStringBuilder(data));

                }

          /**   warehouse cost
                 blgnmr = belegnummerCounter.getBelegnummer("1");
                time = clock.getTimeStamp();
                get("113300").book(AccountBooking.HABEN, 20000000);
                buff.append("");
                buff.append(blgnmr);
                buff.append("1,Sachkontenbuchung,");
                buff.append(time);
                get("478100").book(AccountBooking.SOLL, 20000000);
                buff.append("");
                buff.append(blgnmr);
                buff.append(",Sachkontenbuchung,");
                buff.append(time); **/

            }else{
                // S cash an KR/Verb.a.L.L. KG system
                // 113300 , 160000
                REQ r = b.getREQ();
                String vendorname = setUp.ressources.get(r.items.get(0)).Vendor;
                String pricecontrole = setUp.ressources.get(r.items.get(0)).priceControle;
                double sum=0;
                for(String re : r.items){
                    sum += r.itemAmount.get(re)*r.pricelist.get(re);
                }
                String blegnmr = belegnummerCounter.getBelegnummer("15");
                String timestamp = clock.getTimeStamp();

                get("113300").book(AccountBooking.HABEN, sum);

                CSVData data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,null,"H",null,null,null,null,false,false,"AA",null,null,"113300.0",null,null,null,null,"50",null,null,"113300","S",null,null,null,null,null,null,"RFBU",null,Double.toString(round2(sum)),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blegnmr,"1","Sachkontenbuchung",timestamp);

                log.add(CSVStringBuilder(data));

                get("160000").book(AccountBooking.SOLL, sum);

                data = new CSVData(true,null,true,false,null,"0",false,false,null,null,null,null,null,"V","S",null,null,null,null,true,true,null,"1.0",null,"160000.0",null,null,null,null,"21",null,null,"160000","K",null,vendorname,null,null,"160000.0",null,"RFBU",null,Double.toString(round2(sum)),null,"0.0",null,null,"0.0","0","0","0","0.0","NonFraud",blegnmr,"2","KreditorischeGutschrift",timestamp);

                log.add(CSVStringBuilder(data));
            }
        }
        //log.add(buffer.toString());
        return BR ;
    }
    private void runShrinkage(){
        double rand = random.nextDouble();
        if(rand >= 1- setUp.shrinkage.getLikelihood()){
            for(String s : realInventory.keySet()){
                if(realInventory.get(s) > 0){
                    int amountlost =(int) (realInventory.get(s)*setUp.shrinkage.getAmount());
                    if(amountlost>0){
                        realInventory.put(s, realInventory.get(s) - amountlost);
                    }

                }
            }
        }
    }



    public double getDailyInterest()  {
        runShrinkage();
        double assets=0;
        double liabilities=0;

        assets += get("113300").getSaldo();
        assets += get("140000").getSaldo();
        if(assets<=0){
            try {
                printAll();
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("Company went bankrupt!");
            System.exit(0);
        }

        liabilities += get("160000").getSaldo();
        double loan = get("113101").getSaldo();
        liabilities += loan;


        double netdebt= liabilities-assets;
        double interestbaserate =0.05;
        if(netdebt<1000000){
            interestbaserate += 0.01;
        }else if(netdebt<2000000){
            interestbaserate += 0.015;
        }else if(netdebt<3000000){
            interestbaserate += 0.02;
        }else if(netdebt<4000000){
            interestbaserate += 0.0225;
        }else if(netdebt<5000000){
            interestbaserate += 0.0275;
        }else if(netdebt<6000000){
            interestbaserate += 0.03;
        }else if(netdebt<7000000){
            interestbaserate += 0.0325;
        }else if(netdebt<8000000){
            interestbaserate += 0.0375;
        }else if(netdebt<9000000){
            interestbaserate += 0.04;
        }else if(netdebt<10000000){
            interestbaserate += 0.0425;
        }else if(netdebt<11000000){
            interestbaserate += 0.0475;
        }else if(netdebt<12000000){
            interestbaserate += 0.05;
        }else if(netdebt<13000000){
            interestbaserate += 0.0525;
        }else if(netdebt<14000000){
            interestbaserate += 0.0575;
        }else if(netdebt<15000000){
            interestbaserate += 0.06;
        }else if(netdebt<16000000){
            interestbaserate += 0.0625;
        }else if(netdebt<17000000){
            interestbaserate += 0.0675;
        }else if(netdebt<18000000){
            interestbaserate += 0.07;
        }else if(netdebt<19000000){
            interestbaserate += 0.0725;
        }else if(netdebt<20000000){
            interestbaserate += 0.08;
        }else{
            interestbaserate += 0.09;
        }

        double interest_weekly = Math.pow((1+interestbaserate), (1.0/48))-1;
        double interest_daily = interest_weekly/5;

        if(loan>0){
            return round2(loan*interest_daily);
        }else{
            return 0;
        }
    }

    private double getDiscountRate(){
        double assets=0;
        double liabilities=0;

        assets += get("113300").getSaldo();
        assets += get("140000").getSaldo();
        liabilities += get("160000").getSaldo();
        double loan = get("113101").getSaldo();
        liabilities += loan;


        double netdebt= liabilities-assets;
        double interestbaserate =0.05;
        if(netdebt<1000000){
            interestbaserate += 0.03;
        }else if(netdebt<2000000){
            interestbaserate += 0.0375;
        }else if(netdebt<3000000){
            interestbaserate += 0.04;
        }else if(netdebt<4000000){
            interestbaserate += 0.0425;
        }else if(netdebt<5000000){
            interestbaserate += 0.0475;
        }else if(netdebt<6000000){
            interestbaserate += 0.05;
        }else if(netdebt<7000000){
            interestbaserate += 0.0525;
        }else if(netdebt<8000000){
            interestbaserate += 0.0575;
        }else if(netdebt<9000000){
            interestbaserate += 0.06;
        }else if(netdebt<10000000){
            interestbaserate += 0.0625;
        }else if(netdebt<11000000){
            interestbaserate += 0.0675;
        }else if(netdebt<12000000){
            interestbaserate += 0.07;
        }else if(netdebt<13000000){
            interestbaserate += 0.0725;
        }else if(netdebt<14000000){
            interestbaserate += 0.0775;
        }else if(netdebt<15000000){
            interestbaserate += 0.08;
        }else if(netdebt<16000000){
            interestbaserate += 0.0825;
        }else if(netdebt<17000000){
            interestbaserate += 0.09;
        }else if(netdebt<18000000){
            interestbaserate += 0.10;
        }else if(netdebt<19000000){
            interestbaserate += 0.11;
        }else if(netdebt<20000000){
            interestbaserate += 0.12;
        }else{
            interestbaserate += 0.15;
        }
        return interestbaserate;
    }



    private String CSVStringBuilder(CSVData Data){

        StringBuffer OutputStringBuffer = new StringBuffer();
        // Bool: OutputStringBuffer.append(Data.value? "X," : ",");
        // NullableString: OutputStringBuffer.append(nonNull(Data.value)? MessageFormat.format("{0},", Data.value) : ",");
        // NonNullableString: OutputStringBuffer.append(MessageFormat.format("{0},", Data.value));
        OutputStringBuffer.append(Data.Bestandskonto? "X," : ",");
        OutputStringBuffer.append(nonNull(Data.Bewertungsklasse)? MessageFormat.format("{0},", Data.Bewertungsklasse) : ",");
        OutputStringBuffer.append(Data.EinzelpostenanzeigeMoeglich? "X," : ",");
        OutputStringBuffer.append(Data.Erfolgskontentyp? "X," : ",");
        OutputStringBuffer.append(nonNull(Data.Geschaeftsbereich)? MessageFormat.format("{0},", Data.Geschaeftsbereich) : ",");
        OutputStringBuffer.append(nonNull(Data.Gruppenkennzeichen)? MessageFormat.format("{0},", Data.Gruppenkennzeichen) : ",");
        OutputStringBuffer.append(Data.KZEKBE? "X," : ",");
        OutputStringBuffer.append(Data.KennzeichenPostenNichtKopierbar? "X," : ",");
        OutputStringBuffer.append(nonNull(Data.Kostenstelle)? MessageFormat.format("{0},", Data.Kostenstelle) : ",");
        OutputStringBuffer.append(nonNull(Data.KreditkontrBereich)? MessageFormat.format("{0},", Data.KreditkontrBereich) : ",");
        OutputStringBuffer.append(nonNull(Data.LaufenendeKontierung)? MessageFormat.format("{0},", Data.LaufenendeKontierung) : ",");
        OutputStringBuffer.append(nonNull(Data.PartnerPrctr)? MessageFormat.format("{0},", Data.PartnerPrctr) : ",");
        OutputStringBuffer.append(nonNull(Data.Profitcenter)? MessageFormat.format("{0},", Data.Profitcenter) : ",");
        OutputStringBuffer.append(nonNull(Data.Rechnungsbezug)? MessageFormat.format("{0},", Data.Rechnungsbezug) : ",");
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.SollHabenKennz));
        OutputStringBuffer.append(nonNull(Data.SperrgrundMenge)? MessageFormat.format("{0},", Data.SperrgrundMenge) : ",");
        OutputStringBuffer.append(nonNull(Data.Steuerkennzeichen)? MessageFormat.format("{0},", Data.Steuerkennzeichen) : ",");
        OutputStringBuffer.append(nonNull(Data.Bewertungskreis)? MessageFormat.format("{0},", Data.Bewertungskreis) : ",");
        OutputStringBuffer.append(nonNull(Data.Steuerstandort)? MessageFormat.format("{0},", Data.Steuerstandort) : ",");
        OutputStringBuffer.append(Data.Umsatzwirksam? "X," : ",");
        OutputStringBuffer.append(Data.VerwaltungOffenerPosten? "X," : ",");
        OutputStringBuffer.append(nonNull(Data.Werk)? MessageFormat.format("{0},", Data.Werk) : ",");
        OutputStringBuffer.append(nonNull(Data.Zahlungsbedingung)? MessageFormat.format("{0},", Data.Zahlungsbedingung) : ",");
        OutputStringBuffer.append(nonNull(Data.Zahlungssperre)? MessageFormat.format("{0},", Data.Zahlungssperre) : ",");
        OutputStringBuffer.append(nonNull(Data.AlternativeKontonummer)? MessageFormat.format("{0},", Data.AlternativeKontonummer) : ",");
        OutputStringBuffer.append(nonNull(Data.Basismengeneinheit)? MessageFormat.format("{0},", Data.Basismengeneinheit) : ",");
        OutputStringBuffer.append(nonNull(Data.BestPreisMngEinheit)? MessageFormat.format("{0},", Data.BestPreisMngEinheit) : ",");
        OutputStringBuffer.append(nonNull(Data.Bestandsbuchung)? MessageFormat.format("{0},", Data.Bestandsbuchung) : ",");
        OutputStringBuffer.append(nonNull(Data.Bestellmengeneinheit)? MessageFormat.format("{0},", Data.Bestellmengeneinheit) : ",");
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.Buchungsschlüssel));
        OutputStringBuffer.append(nonNull(Data.BuchungszeilenId)? MessageFormat.format("{0},", Data.BuchungszeilenId) : ",");
        OutputStringBuffer.append(nonNull(Data.ErfassungsMngEinheit)? MessageFormat.format("{0},", Data.ErfassungsMngEinheit) : ",");
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.Hauptbuchkonto));
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.Kontoart));
        OutputStringBuffer.append(nonNull(Data.Kostenart)? MessageFormat.format("{0},", Data.Kostenart) : ",");
        OutputStringBuffer.append(nonNull(Data.Kreditor)? MessageFormat.format("{0},", Data.Kreditor) : ",");
        OutputStringBuffer.append(nonNull(Data.Material)? MessageFormat.format("{0},", Data.Material) : ",");
        OutputStringBuffer.append(nonNull(Data.Preissteuerung)? MessageFormat.format("{0},", Data.Preissteuerung) : ",");
        OutputStringBuffer.append(nonNull(Data.Sachkonto)? MessageFormat.format("{0},", Data.Sachkonto) : ",");
        OutputStringBuffer.append(nonNull(Data.Vorgang)? MessageFormat.format("{0},", Data.Vorgang) : ",");
        OutputStringBuffer.append(nonNull(Data.VorgangsartGL)? MessageFormat.format("{0},", Data.VorgangsartGL) : ",");
        OutputStringBuffer.append(nonNull(Data.Wertestring)? MessageFormat.format("{0},", Data.Wertestring) : ",");
        OutputStringBuffer.append(nonNull(Data.BetragHauswaehr)? MessageFormat.format("{0},", Data.BetragHauswaehr) : ",");
        OutputStringBuffer.append(nonNull(Data.Betrag)? MessageFormat.format("{0},", Data.Betrag) : ",");
        OutputStringBuffer.append(nonNull(Data.Betrag_5)? MessageFormat.format("{0},", Data.Betrag_5) : ",");
        OutputStringBuffer.append(nonNull(Data.Gesamtbestand)? MessageFormat.format("{0},", Data.Gesamtbestand) : ",");
        OutputStringBuffer.append(nonNull(Data.Gesamtwert)? MessageFormat.format("{0},", Data.Gesamtwert) : ",");
        OutputStringBuffer.append(nonNull(Data.Kreditkontrbetrag)? MessageFormat.format("{0},", Data.Kreditkontrbetrag) : ",");
        OutputStringBuffer.append(nonNull(Data.MengeinBPME)? MessageFormat.format("{0},", Data.MengeinBPME) : ",");
        OutputStringBuffer.append(nonNull(Data.MengeinErfassME)? MessageFormat.format("{0},", Data.MengeinErfassME) : ",");
        OutputStringBuffer.append(nonNull(Data.Menge)? MessageFormat.format("{0},", Data.Menge) : ",");
        OutputStringBuffer.append(nonNull(Data.Skontobasis)? MessageFormat.format("{0},", Data.Skontobasis) : ",");
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.Label));
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.Belegnummer));
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.Position));
        OutputStringBuffer.append(MessageFormat.format("{0},", Data.Transaktionsart));
        OutputStringBuffer.append(Data.Erfassungsuhrzeit);

        String OutputString = OutputStringBuffer.toString();

        return OutputString;
    }

    private record CSVData(boolean Bestandskonto,
                           @Nullable String Bewertungsklasse,
                           boolean EinzelpostenanzeigeMoeglich,
                           boolean Erfolgskontentyp,
                           @Nullable String Geschaeftsbereich,
                           @Nullable String Gruppenkennzeichen,
                           boolean KZEKBE,
                           boolean KennzeichenPostenNichtKopierbar,
                           @Nullable String Kostenstelle,
                           @Nullable String KreditkontrBereich,
                           @Nullable String LaufenendeKontierung,
                           @Nullable String PartnerPrctr,
                           @Nullable String Profitcenter,
                           @Nullable String Rechnungsbezug,
                           String SollHabenKennz,
                           @Nullable String SperrgrundMenge,
                           @Nullable String Steuerkennzeichen,
                           @Nullable String Bewertungskreis,
                           @Nullable String Steuerstandort,
                           boolean Umsatzwirksam,
                           boolean VerwaltungOffenerPosten,
                           @Nullable String Werk,
                           @Nullable String Zahlungsbedingung,
                           @Nullable String Zahlungssperre,
                           @Nullable String AlternativeKontonummer,
                           @Nullable String Basismengeneinheit,
                           @Nullable String BestPreisMngEinheit,
                           @Nullable String Bestandsbuchung,
                           @Nullable String Bestellmengeneinheit,
                           String Buchungsschlüssel,
                           @Nullable String BuchungszeilenId,
                           @Nullable String ErfassungsMngEinheit,
                           String Hauptbuchkonto,
                           String Kontoart,
                           @Nullable String Kostenart,
                           @Nullable String Kreditor,
                           @Nullable String Material,
                           @Nullable String Preissteuerung,
                           @Nullable String Sachkonto,
                           @Nullable String Vorgang,
                           @Nullable String VorgangsartGL,
                           @Nullable String Wertestring,
                           @Nullable String BetragHauswaehr,
                           @Nullable String Betrag,
                           @Nullable String Betrag_5,
                           @Nullable String Gesamtbestand,
                           @Nullable String Gesamtwert,
                           @Nullable String Kreditkontrbetrag,
                           @Nullable String MengeinBPME,
                           @Nullable String MengeinErfassME,
                           @Nullable String Menge,
                           @Nullable String Skontobasis,
                           String Label,
                           String Belegnummer,
                           String Position,
                           String Transaktionsart,
                           String Erfassungsuhrzeit ){}

}
