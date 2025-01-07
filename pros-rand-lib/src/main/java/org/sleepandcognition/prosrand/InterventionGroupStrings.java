package org.sleepandcognition.prosrand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InterventionGroupStrings {
    String name;
    private List<Map<String, String>> subjectFeatureStrings;

    public InterventionGroupStrings(String name) {
        this.name = name;
        subjectFeatureStrings = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Map<String, String>> getSubjects() {
        return subjectFeatureStrings;
    }

    void addSubject(Map<String, String> s) {
        subjectFeatureStrings.add(s);
    }
}
