package HelperClasses;

import jade.core.AID;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Schedule{
    public ArrayList<ArrayList<REQ>> sclist= new ArrayList<>(); // sc[] to mk[] is one to one index
    private HashMap<String, ScheduleEnum> sclistLookup = new HashMap<>();
    public ArrayList<ArrayList<REQ>> mklist = new ArrayList<>();
    private boolean nonActiveMembers=false;
    private int nonActiveCounter=0;


    public void init(ArrayList<REQ> sclist, ArrayList<REQ> mklist){
        this.sclist.add(sclist);
        this.mklist.add(mklist);
        sclistLookup.put(sclist.get(0).uniqueID, ScheduleEnum.NONACTIVE);
    }

    public void add(Schedule s){
        sclist.addAll(s.sclist);
        mklist.addAll(s.mklist);
        for(String t : s.sclistLookup.keySet()){
            sclistLookup.put(t, s.sclistLookup.get(t));
        }
    }

    public void markAsDone(String uuid){ // remove if all done, all done condition:  all were active
        boolean isSC = false;
        outer:
        for(List<REQ> sc : sclist){
            int count=0;
            for(REQ r : sc){
                if(r.uniqueID.equals(uuid)){
                    isSC = true;
                    r.active=ScheduleEnum.DONE;
                    if(count+1 == sc.size()){ // last element of list
                        sclistLookup.put(sc.get(0).uniqueID, ScheduleEnum.DONE);
                    }
                    break outer;
                }
                count++;
            }
        }

        if(!isSC){
            int k=0;
            outer2:
            for(List<REQ> mk: mklist){
                int c =0;
                for(REQ r: mk){
                    if(r.uniqueID.equals(uuid)){
                        r.active=ScheduleEnum.DONE;
                        if(c+1 == mk.size()){
                            mklist.remove(k);
                            sclist.remove(k);
                        }
                        break outer2;
                    }
                    c++;
                }
                k++;
            }
        }

    }

    public List<REQ> getNext(){
        boolean globalProductionstop = false;
        List<REQ> ret = new ArrayList<>();
        int i=0;
        for(ArrayList<REQ> sc : sclist){
            for(REQ r : sc){
                if(r.active == ScheduleEnum.NONACTIVE){
                    r.active= ScheduleEnum.ACTIVE;
                    ret.add(r);
                }
            }
            if(sc.size() !=0 && (sclistLookup.get(sc.get(0).uniqueID) == ScheduleEnum.DONE)){
                ScheduleEnum last = ScheduleEnum.DONE;
                for(REQ z : mklist.get(i)){
                    if(globalProductionstop){break;}
                    if(z.active == ScheduleEnum.NONACTIVE && last == ScheduleEnum.DONE){
                        globalProductionstop=true;
                        ret.add(z);
                        z.active = ScheduleEnum.ACTIVE;
                        break;
                    }
                    last = z.active;
                }
            }
            i++;
        }
        return ret;
    }





}
