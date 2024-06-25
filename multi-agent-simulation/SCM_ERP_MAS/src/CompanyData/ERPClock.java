package CompanyData;

import TimeFrame.TimeFrame;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ERPClock implements Serializable {
    private LocalTime startTime = null;

    public ERPClock(){
        this.startTime = LocalTime.now();
    }

    public  String getTimeStamp(){
        LocalTime lt = LocalTime.now();
        DateTimeFormatter form = DateTimeFormatter.ofPattern("HH:mm:ss");
        long diff = startTime.until(lt, ChronoUnit.MILLIS)* TimeFrame.getMultiplier();
        lt= lt.plus(diff, ChronoUnit.MILLIS);
        return form.format(lt)+"\n";
    }



}
