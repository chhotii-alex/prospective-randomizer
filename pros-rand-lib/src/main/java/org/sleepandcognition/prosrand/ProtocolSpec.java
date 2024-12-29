package org.sleepandcognition.prosrand;

import java.util.List;

public class ProtocolSpec {
    List<String> groupNames;
    List<String> variableSpec;
    boolean allowRevision;

    public void setGroupNames(List<String> g) {
        groupNames = g;
    }

    public List<String> getGroupNames() {
        return groupNames;
    }

    public void setVariableSpec(List<String> v) {
        variableSpec = v;
    }

    public List<String> getVariableSpec() {
        return variableSpec;
    }
    
    public void setAllowRevision(boolean flag) {
        allowRevision = flag;
    }

    public boolean getAllowRevision() {
        return allowRevision;
    } 
}
