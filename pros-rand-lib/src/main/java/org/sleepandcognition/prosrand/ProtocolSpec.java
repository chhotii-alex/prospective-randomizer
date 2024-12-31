package org.sleepandcognition.prosrand;

import java.util.HashMap;
import java.util.List;

public class ProtocolSpec {
    List<String> groupNames;
    HashMap<String, List<String>> variableSpec;
    boolean allowRevision;
    String algorithm;

    public void setGroupNames(List<String> g) {
        groupNames = g;
    }

    public List<String> getGroupNames() {
        return groupNames;
    }

    public void setVariableSpec(HashMap<String, List<String>> v) {
        variableSpec = v;
    }

    public HashMap<String, List<String>> getVariableSpec() {
        return variableSpec;
    }

    public void setAllowRevision(boolean flag) {
        allowRevision = flag;
    }

    public boolean getAllowRevision() {
        return allowRevision;
    }

    public void setAlgorithm(String name) {
        algorithm = name;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
