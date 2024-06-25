package HelperClasses;

import CompanyData.*;
import PassedObjects.SetUp;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

public class XMLParser {

    public static SetUp XMLtoSETUP() throws ParserConfigurationException, IOException, SAXException { // change to return SetUp
        InputStream filestream = XMLParser.class.getClassLoader().getResourceAsStream("company.xml");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse(filestream);
        doc.getDocumentElement().normalize();

        SetUp setUp= new SetUp();

        HashMap<String, Random> randommap = new HashMap<>();
        // get seeds <- that is very important
        NodeList seedlist = doc.getElementsByTagName("seeds");
        for(int i=0; i< seedlist.getLength(); i++){
            Node n = seedlist.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) n;
                NodeList seed = el.getElementsByTagName("seed");
                for(int j=0; j< seed.getLength(); j++){
                 Node ns = seed.item(j);
                 if (ns.getNodeType() == Node.ELEMENT_NODE){
                   Element s = (Element) ns;
                   String id = s.getAttribute("id");
                   String val = s.getTextContent();
                   Random r=null;
                   if(val != null && !val.equals("")){
                       long value = Long.parseLong(val);
                       r = new Random(value);
                   }else{
                       r=new Random();
                   }
                   randommap.put(id, r);
                 }
                }
            }
        }
        setUp.randmap = randommap;


        //get ressources
        NodeList nl = doc.getElementsByTagName("ressource");
        for(int i = 0; i< nl.getLength(); i++ ){
            Node n = nl.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                Ressource r = new Ressource();
                Element el = (Element) n;
                r.identifier= el.getAttribute("id");
                r.name = el.getElementsByTagName("name").item(0).getTextContent();
                r.priceControle= el.getElementsByTagName("pricecontrole").item(0).getTextContent();
                r.unitSize = el.getElementsByTagName("unit").item(0).getTextContent();
                r.map =Double.parseDouble(el.getElementsByTagName("mvp").item(0).getTextContent());
                r.Vendor = el.getElementsByTagName("Vendor").item(0).getTextContent();
                r.deliverytime =  Integer.parseInt(el.getElementsByTagName("deliverytime").item(0).getTextContent());
                setUp.ressources.put(r.identifier, r);
            }
        }

        //get vendors
        nl = doc.getElementsByTagName("VendorA");
        for(int i=0; i< nl.getLength(); i++){
            Node n = nl.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                Vendor v = new Vendor();
                Element el = (Element) n;

                v.identifier = el.getElementsByTagName("name").item(0).getTextContent();
                v.paymentTime = Integer.parseInt(el.getElementsByTagName("paymenttime").item(0).getTextContent());
                v.currency = el.getElementsByTagName("currency").item(0).getTextContent();
                v.country = el.getElementsByTagName("country").item(0).getTextContent();
                setUp.vendors.put(v.identifier, v);
            }
        }

        //get products
        nl = doc.getElementsByTagName("product");
        for(int i=0; i< nl.getLength(); i++){
            Node n = nl.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                Product p = new Product();
                Element el = (Element) n;

                p.identifier = el.getAttribute("id");
                p.name = el.getElementsByTagName("name").item(0).getTextContent();
                p.unitSize = el.getElementsByTagName("unit").item(0).getTextContent();
                p.priceControle = el.getElementsByTagName("pricecontrole").item(0).getTextContent();
                setUp.products.put(p.identifier, p);

            }
        }

        // get afa
        Node n  = doc.getElementsByTagName("Afa").item(0);
        if(n.getNodeType() == Node.ELEMENT_NODE){
            AFA afa = new AFA();
            Element el = (Element) n;
            afa.afatype = el.getElementsByTagName("type").item(0).getTextContent();
            afa.buildinglife = Integer.parseInt(el.getElementsByTagName("buildinglife").item(0).getTextContent());
            afa.machinelife = Integer.parseInt(el.getElementsByTagName("machinelife").item(0).getTextContent());
            setUp.afa = afa;
        }

        n = doc.getElementsByTagName("ShippingCost").item(0);
        if (n.getNodeType() == Node.TEXT_NODE){
            Element el = (Element) n;
            setUp.shipmentCost = Double.parseDouble(el.getElementsByTagName("costShipment").item(0).getTextContent());
        }


        // get stuecklisten
        nl = doc.getElementsByTagName("stueckliste");
        GozintoGraphs graphs = new GozintoGraphs();
        for(int i=0; i< nl.getLength(); i++){
            Node nd = nl.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) nd;

                NodeList nl2= el.getElementsByTagName("component");
                SimpleSTKL stkl = new SimpleSTKL(nl2.getLength());
                stkl.target = el.getElementsByTagName("target").item(0).getTextContent();
                stkl.capacity = Integer.parseInt(el.getElementsByTagName("capacity").item(0).getTextContent());
                stkl.setuptime = Double.parseDouble(el.getElementsByTagName("setuptime").item(0).getTextContent());

                for(int j = 0; j< nl2.getLength(); j++){
                    Node nd2= nl2.item(j);
                    if(nd2.getNodeType()== Node.ELEMENT_NODE){
                        Element el2= (Element) nd2;
                        stkl.amount[j]= Double.parseDouble(el2.getElementsByTagName("amount").item(0).getTextContent());
                        stkl.ress[j]= setUp.ressources.get(el2.getElementsByTagName("identifier").item(0).getTextContent());
                    }
                }
                graphs.addStkl(stkl);
            }
        }
        setUp.graphs= graphs;


        // get accounts
        nl = doc.getElementsByTagName("account");
        MainBook mainBook = new MainBook(setUp);
        for(int i=0; i< nl.getLength(); i++){
            Node n2 = nl.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) n2;

                Account a = new Account( el.getAttribute("id"));
                a.setName(el.getElementsByTagName("name").item(0).getTextContent());
                a.setType(el.getElementsByTagName("typeA").item(0).getTextContent());
                mainBook.add(a);
            }
        }
        mainBook.setSeed(randommap.get("Mainbook").nextLong());
        setUp.mainBook= mainBook;

        /*for(String s: setUp.products.keySet()){
            setUp.mainBook.putInventory(s, 0.0);
        }
        for(String s: setUp.ressources.keySet()){
            setUp.mainBook.putInventory(s, 0.0);
        }*/
        

        // get warehouse capacities

        nl= doc.getElementsByTagName("warehousecapacity") ;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                setUp.warehousecap= new int[Integer.parseInt(el.getElementsByTagName("types").item(0).getTextContent() )];
                NodeList n3 = el.getElementsByTagName("capacity");
                for(int j=0; j<n3.getLength(); j++){
                    Node n4 = n3.item(j);
                    if(n4.getNodeType() == Node.ELEMENT_NODE){
                        Element el2 = (Element) n4;
                        setUp.warehousecap[j]= Integer.parseInt(((Element) (el2.getElementsByTagName("limit") ).item(0) ).getTextContent());
                        NodeList n5 = el2.getElementsByTagName("item");
                        for(int k=0; k<n5.getLength(); k++){
                            Node n6 = n5.item(k);
                            if(n6.getNodeType() == Node.ELEMENT_NODE){
                                Element el5 = (Element) n6;
                                String name = el5.getTextContent();
                                if(setUp.ressources.containsKey(name)){
                                    setUp.ressources.get(name).capacityType =j;
                                }else if(setUp.products.containsKey(name)){
                                    setUp.products.get(name).capacityType=j;
                                }
                            }
                        }
                    }

                }
            }
        }

        nl = doc.getElementsByTagName("productDataType");
        setUp.productDataType = nl.item(0).getTextContent();
        nl = doc.getElementsByTagName("storagesecurity");
        setUp.storageSecurity = Double.parseDouble(nl.item(0).getTextContent());
        nl = doc.getElementsByTagName("productionStrategy");
        setUp.productionStrategy = nl.item(0).getTextContent();
        nl = doc.getElementsByTagName("maxInv");
        setUp.maxInv = Integer.parseInt(nl.item(0).getTextContent());
        nl= doc.getElementsByTagName("forecastingmethod");
        setUp.forecastingMethod = nl.item(0).getTextContent();

        nl = doc.getElementsByTagName("fraudlikelihood");
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                NodeList ne = el.getElementsByTagName("fraud");
                for(int j=0; j<ne.getLength(); j++){
                    Node noe = ne.item(j);
                    if(noe.getNodeType() == Node.ELEMENT_NODE){
                        Element el2 = (Element) noe;
                        setUp.fraudLikelihood.put(el2.getAttribute("id"), Double.parseDouble(el2.getTextContent()));
                    }
                }
            }
        }
        nl = doc.getElementsByTagName("fraudvalues");
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                NodeList ne = el.getElementsByTagName("fraud");
                for(int j=0; j<ne.getLength(); j++){
                    Node noe = ne.item(j);
                    if(noe.getNodeType() == Node.ELEMENT_NODE){
                        Element el2 = (Element) noe;
                        setUp.fraudvalues.put(el2.getAttribute("id"), Double.parseDouble(el2.getTextContent()));
                    }
                }
            }
        }

        Shrinkage shrinkage = new Shrinkage();
        nl = doc.getElementsByTagName("shrinkage");
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                NodeList ne = el.getElementsByTagName("sh");
                for(int j =0; j< ne.getLength(); j++){
                    Node noe = ne.item(j);
                    if(noe.getNodeType() == Node.ELEMENT_NODE){
                        Element el2 = (Element) noe;
                        String id = el2.getAttribute("id");
                        if(id.equals("likelihood")){
                            shrinkage.setLikelihood(Double.parseDouble(el2.getTextContent()));
                        }else{
                            shrinkage.setAmount(Double.parseDouble(el2.getTextContent()));
                        }
                    }

                }

            }
        }
        setUp.shrinkage=shrinkage;




        nl = doc.getElementsByTagName("markup");
        setUp.margin = Double.parseDouble(nl.item(0).getTextContent());
        nl = doc.getElementsByTagName("markupCon");
        setUp.marginCon = Double.parseDouble(nl.item(0).getTextContent());


        Marketfunction mf = new Marketfunction();
        nl = doc.getElementsByTagName("marketfunc");
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                mf.setType(el.getElementsByTagName("type").item(0).getTextContent());
                //mf.setAlpha(Double.parseDouble(el.getElementsByTagName("alpha").item(0).getTextContent()));
                //mf.setBeta(Double.parseDouble(el.getElementsByTagName("beta").item(0).getTextContent()));

                NodeList nl2 = el.getElementsByTagName("market");
                for(int j=0; j< nl2.getLength(); j++){
                    Node no2 = nl2.item(j);
                    if(no2.getNodeType() == Node.ELEMENT_NODE){
                        Element el2 = (Element) no2; // markets
                        double PCL = Double.parseDouble(el2.getElementsByTagName("PCL").item(0).getTextContent());
                        double PCH = Double.parseDouble(el2.getElementsByTagName("PCH").item(0).getTextContent());
                        String name = el2.getAttribute("id");
                        int marketcap = Integer.parseInt(el2.getElementsByTagName("marketcap").item(0).getTextContent());
                        int sets=0;
                        boolean issets = Boolean.parseBoolean(((Element)(el2.getElementsByTagName("sets").item(0))).getAttribute("boolean"));
                        if(issets){
                            sets = Integer.parseInt(((Element)(el2.getElementsByTagName("sets").item(0))).getTextContent());
                        }

                        String copy = ((Element)(el2.getElementsByTagName("copySubs").item(0))).getAttribute("boolean");
                        boolean copySubs = copy.equals("true");
                        int copynumber=0;
                        int favSubs=0;
                        double tfactor = 0;
                        if(copySubs){
                            copynumber =  Integer.parseInt(((Element)(el2.getElementsByTagName("copySubs").item(0))).getTextContent());
                            favSubs = Integer.parseInt(((Element)(el2.getElementsByTagName("copySubs").item(0))).getAttribute("favSubs"));
                            tfactor = Double.parseDouble(((Element)(el2.getElementsByTagName("tfactor").item(0))).getTextContent());
                        }

                        String capacitymapping = el2.getElementsByTagName("capacitymapping").item(0).getTextContent();
                        CMapping cm = CMapping.FIX;
                        if(capacitymapping.equals("variable")){
                            cm=CMapping.VARIABEL;
                        }




                        NodeList nl3 = el2.getElementsByTagName("fav");
                        NodeList nl4 = el2.getElementsByTagName("elast");
                        NodeList nl5 = el2.getElementsByTagName("cs");

                        int primaryProductNumber = nl3.getLength();
                        int substitutenumber = (nl4.getLength()- primaryProductNumber)/primaryProductNumber;
                        MarketWrapper marketWrapper= new MarketWrapper(substitutenumber, primaryProductNumber);
                        marketWrapper.setSeed(randommap.get("MarketWrapper").nextLong());
                        marketWrapper.setMarketcap(marketcap);
                        marketWrapper.setCapacityMapping(cm);
                        marketWrapper.setMarketname(name);
                        marketWrapper.setPCH(PCH);
                        marketWrapper.setPCL(PCL);

                        for(int t =0; t< nl4.getLength(); t++){
                            Node no4 = nl4.item(t);
                            if(no4.getNodeType()== Node.ELEMENT_NODE){
                                Element el4 = (Element) no4;
                                String pricechangeon = el4.getAttribute("pricechangeon");
                                String volumechangeon = el4.getAttribute("volumechangeon");
                                double value = Double.parseDouble(el4.getTextContent());
                                marketWrapper.addElasticity(pricechangeon, volumechangeon, value);
                            }
                        }

                        for(int t=0; t<nl3.getLength(); t++){
                            Node no3 = nl3.item(t);
                            if(no3.getNodeType()== Node.ELEMENT_NODE){
                             Element el3 = (Element) no3;
                             String on = el3.getAttribute("on");
                             double value = Double.parseDouble(el3.getTextContent());
                             marketWrapper.addFavor(on, value);
                            }
                        }


                        if (copySubs){
                            marketWrapper.setCustomerNumber(copynumber);
                            marketWrapper.setTFactor(tfactor);
                        }else{
                            marketWrapper.setCustomerNumber(nl5.getLength()); // initializes submarkets
                            marketWrapper.setTFactor(1);
                        }
                        for(int t=0; t< nl5.getLength(); t++){ //cs == for every submarket
                         Node no5 = nl5.item(t);
                         if(no5.getNodeType() == Node.ELEMENT_NODE){
                             Element el5 = (Element) no5;
                             double alpha = Double.parseDouble(el5.getElementsByTagName("alpha").item(0).getTextContent());
                             double sigma1= Double.parseDouble(el5.getElementsByTagName("Cstddef").item(0).getTextContent());
                             double sigma2= Double.parseDouble(el5.getElementsByTagName("Cstddef2").item(0).getTextContent());
                             double subreduction= Double.parseDouble(el5.getElementsByTagName("SubReduction").item(0).getTextContent());

                             if (!copySubs){
                             marketWrapper.addAlpha(t, alpha);
                             marketWrapper.addSigma(t, sigma1);
                             marketWrapper.addSigma2(t, sigma2);
                             marketWrapper.addSubReduction(t, subreduction);
                             marketWrapper.setSets(t, issets);
                             marketWrapper.setSetNumber(t, sets);

                             NodeList nl7 = el5.getElementsByTagName("beta");
                          for(int z=0; z<nl7.getLength(); z++){
                              Node no6 = nl7.item(z);
                              if(no6.getNodeType() ==Node.ELEMENT_NODE){
                                  Element el6 = (Element) no6;
                                  String id = el6.getAttribute("on");
                                  double value = Double.parseDouble(el6.getTextContent());
                                  marketWrapper.addBeta(t, id, value);
                              }
                          }
                             }else{
                                 for(int k=0; k<copynumber; k++){
                                     marketWrapper.addAlpha(k, alpha);
                                     marketWrapper.addSigma(k, sigma1);
                                     marketWrapper.addSigma2(k, sigma2);
                                     marketWrapper.addSubReduction(k, subreduction);
                                     marketWrapper.setSets(k, issets);
                                     marketWrapper.setSetNumber(k, sets);

                                     NodeList nl7 = el5.getElementsByTagName("beta");
                                     for(int z=0; z<nl7.getLength(); z++){
                                         Node no6 = nl7.item(z);
                                         if(no6.getNodeType() ==Node.ELEMENT_NODE){
                                             Element el6 = (Element) no6;
                                             String id = el6.getAttribute("on");
                                             double value=0.0;
                                             if(k >= favSubs){
                                                 value = -100.0;
                                             }else{
                                                 value = Double.parseDouble(el6.getTextContent());
                                             }

                                             marketWrapper.addBeta(k, id, value);
                                         }
                                     }
                                 }
                                 break;
                             }


                         }
                        }

                        mf.add(marketWrapper);
                    }

                }
            }
        }
        setUp.marketfunction=mf;



        nl = doc.getElementsByTagName("initdays");
        int id=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                id = Integer.parseInt(el.getTextContent());
            }
        }
        setUp.initdays = id;


        nl = doc.getElementsByTagName("initstocks");
        long stocks=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                stocks = Long.parseLong(el.getTextContent());
            }
        }
    setUp.initstocks=stocks;

        nl = doc.getElementsByTagName("initland");
        long land=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                land = Long.parseLong(el.getTextContent());
            }
        }
        setUp.initland=land;

        nl = doc.getElementsByTagName("initbuildings");
        long buildings=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                buildings = Long.parseLong(el.getTextContent());
            }
        }
        setUp.initbuildings=buildings;

        nl = doc.getElementsByTagName("initmachines");
        long machines=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                machines = Long.parseLong(el.getTextContent());
            }
        }
        setUp.initmachines=machines;

        nl = doc.getElementsByTagName("initloans");
        long laons=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                laons = Long.parseLong(el.getTextContent());
            }
        }
        setUp.initloans=laons;


        nl = doc.getElementsByTagName("baselabor");
        long baselabor=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                baselabor = Long.parseLong(el.getTextContent());
            }
        }
        setUp.baselabor=baselabor;

        nl = doc.getElementsByTagName("baseoverhead");
        long baseoverhead=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                baseoverhead = Long.parseLong(el.getTextContent());
            }
        }
        setUp.baseoverhead=baseoverhead;

        nl = doc.getElementsByTagName("basesga");
        long basesga=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                basesga = Long.parseLong(el.getTextContent());
            }
        }
        setUp.basesga=basesga;

        nl = doc.getElementsByTagName("dataPriceIncrease");
        double dataPriceIncrease=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) no;
                dataPriceIncrease = Double.parseDouble(el.getTextContent());
            }
        }
        setUp.dataPriceIncrease = dataPriceIncrease;

        nl=doc.getElementsByTagName("minlots");
        double minlots=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                minlots = Double.parseDouble(el.getTextContent());
            }
        }
        setUp.minlots = minlots;
        nl=doc.getElementsByTagName("maxlots");
        double maxlots=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                maxlots = Double.parseDouble(el.getTextContent());
            }
        }
        setUp.maxlots = maxlots;

        nl=doc.getElementsByTagName("killday");
        int killday=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                killday = Integer.parseInt(el.getTextContent());
            }
        }
        setUp.killday = killday;

        nl=doc.getElementsByTagName("maxCash");
        double maxCash=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                maxCash = Double.parseDouble(el.getTextContent());
            }
        }
        setUp.maxCash=maxCash;

        nl=doc.getElementsByTagName("printbySM");
        boolean pbsm=false;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                pbsm = Boolean.parseBoolean(el.getTextContent());
            }
        }
        setUp.printBySM = pbsm;

        nl=doc.getElementsByTagName("cashReserve");
        double cr=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                cr = Double.parseDouble(el.getTextContent());
            }
        }
        setUp.cashReserve = cr;

        nl=doc.getElementsByTagName("paymenttimeAll");
        int pt=0;
        for(int i=0; i<nl.getLength(); i++){
            Node no = nl.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE){
                Element el = (Element) no;
                pt = Integer.parseInt(el.getTextContent());
            }
        }
        setUp.paymenttime = pt;








        return setUp;
    }


}
