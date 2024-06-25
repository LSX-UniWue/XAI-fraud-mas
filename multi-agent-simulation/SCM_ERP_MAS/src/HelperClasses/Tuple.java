package HelperClasses;

import java.io.Serializable;

public class Tuple<X,Y> implements Serializable {
    public X x;
    public Y y;
    public Tuple(X x, Y y){
        this.x=x;
        this.y=y;
    }

}
