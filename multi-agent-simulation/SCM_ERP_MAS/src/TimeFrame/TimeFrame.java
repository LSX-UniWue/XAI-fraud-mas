package TimeFrame;

public enum TimeFrame {
    HALFDAY, DAY, WEEK, QUARTER;

    private static final int day = 6000; // 120000ms = 2 min = gametime for a day
    private static final int multiplier = 20; // day_runtime*multiplier = day_"gameTime"

    public long toTime(){
        return switch (this){
            case DAY -> day;
            case WEEK -> 5*day;
            case HALFDAY -> (long)(0.5*day);
            case QUARTER -> 12*5*day;
        };

    }

    static public long intToTime(int days){
        return days*day;
    }

    static  public double doubleToTime(double days){
        return days*day;
    }

    static public int getMultiplier(){return multiplier;}



}
