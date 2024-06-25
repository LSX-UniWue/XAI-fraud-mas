package CompanyData;

import java.io.Serializable;

public class BelegnummerCounter implements Serializable {
    private int counterSA=0; // für belegnummer
    private int counterWE=0;
    private int counterWA=0;
    private int counterKR=0;
    private int counterDR=0;
    private int counterKG=0;
    private int counterDG=0;

    public String getBelegnummer(String belegnummerID){
        int counter =0;
        // add 8 nullen und zähle hoch
        switch (belegnummerID){
            case "1":
                counter = counterSA;
                counterSA++;
                break;
            case "51":
                counter = counterKR;
                counterKR++;
            break;
            case "14":
                counter= counterDG;
                counterDG++;
            break;
            case  "50":
                counter = counterWE ;
                counterWE++;
            break;
            case "49":
                counter = counterWA;
                counterWA++;
            break;
            case "9":
                counter = counterDR;
                counterDR++;
            break;
            case "15":
                counter = counterKG;
                counterKG++;
            break;
            default:
                System.out.println("no belegnummerID like : "+belegnummerID);
                System.exit(-1);
        }
        StringBuffer buff = new StringBuffer(belegnummerID); // may need to acces with more behaviours = threads -> buffer not builder
        int counterDigits = String.valueOf(counter).length();
        for (int j = (8-counterDigits); j>0; j--){
            if(belegnummerID.equals(("9")) && j==1){ continue;} // 9 needs one less zero
            buff.append("0");
        }
        buff.append(counter);
        //buff.append(",");
        return buff.toString();
    }






}
