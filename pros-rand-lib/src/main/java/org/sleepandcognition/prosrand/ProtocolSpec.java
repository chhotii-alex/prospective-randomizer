package org.sleepandcognition.prosrand;

import java.util.List;

public class ProtocolSpec {
    List<String> groupNames;
    List<VariableSpec> variableSpec;
    boolean allowRevision;
    String algorithm;

    public void setGroupNames(List<String> g) {
        groupNames = g;
    }

    public List<String> getGroupNames() {
        return groupNames;
    }

    public void setVariableSpec(List<VariableSpec> v) {
        variableSpec = v;
    }

    public List<VariableSpec> getVariableSpec() {
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
