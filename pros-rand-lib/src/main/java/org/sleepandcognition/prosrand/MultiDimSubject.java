package org.sleepandcognition.prosrand;

import java.util.Hashtable;

public class MultiDimSubject {
    String identifier;
    Hashtable<String, Double> baselineCharacteristics;
    InterventionGroup myGroup;
    boolean isCommitted;

    public MultiDimSubject(String id) {
        identifier = id;
        baselineCharacteristics = new Hashtable<String, Double>();
        myGroup = null;
    }

    public void addCharacteristics(Hashtable<String, Double> values) {
        baselineCharacteristics.putAll(values);
    }

    public void setGroup(InterventionGroup interventionGroup) {
        myGroup = interventionGroup;
    }

    public String getId() {
        return identifier;
    }

    public Hashtable<String, Double> getFeatures() {
        return baselineCharacteristics;
    }

    public String getGroupName() {
        if (myGroup == null) {
            return null;
        }
        return myGroup.name;
    }

}
