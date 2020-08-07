package edu.ucr.cs.ufarooq.android.util.objectHeirarchy;

import soot.Scene;
import soot.SootClass;

import java.util.ArrayList;

public class Application {
    String packageName;
    ArrayList<SootClass> activities;
    ArrayList<String> activitiesClasses;

    public Application() {
    }

    public Application(String packageName, ArrayList<String> activities) {
        this.packageName = packageName;
        this.activitiesClasses = activities;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void process() {
        activities = new ArrayList<>();
        for (String activity : this.activitiesClasses) {
            //if(Scene.v().containsClass(activity)){
            this.activities.add(Scene.v().getSootClassUnsafe(activity));
            //}
        }
    }

    public ArrayList<SootClass> getActivities() {
        return activities;
    }

}
