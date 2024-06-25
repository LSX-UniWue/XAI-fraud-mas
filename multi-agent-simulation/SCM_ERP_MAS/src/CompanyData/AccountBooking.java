package CompanyData;

public enum AccountBooking {
    SOLL, HABEN;

    public String toString(){
        if(this == AccountBooking.HABEN){
            return "H";
        }else{
            return "S";
        }
    }

}
