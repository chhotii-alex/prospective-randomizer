package org.sleepandcognition.prosrand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public abstract class Randomizer {
    VariableSet variables;
    Hashtable<String, InterventionGroup> groups;
    protected Hashtable<String, MultiDimSubject> subjectsByID;
    protected ArrayList<MultiDimSubject> unassignedSubjects;
    StillGoingFlag controllersOffSwitch;
    SubjectDatabase database;
    int verbosity;
    boolean allowRevision;

    static int RandomizerCommVersion() {
        return 5;
    }

    public Randomizer(
            String groupListFile,
            VariableSet variables,
            SubjectDatabase db,
            StillGoingFlag listening,
            boolean allowRevision)
            throws SAXException, IOException, ParserConfigurationException {
        this.allowRevision = allowRevision;
        database = db;
        this.variables = variables;
        groups = InterventionGroup.ReadGroups(groupListFile);
        ArrayList<MultiDimSubject> subjects = database.ReadSubjectsIntoGroups(variables, groups);
        subjectsByID = new Hashtable<String, MultiDimSubject>();
        unassignedSubjects = new ArrayList<MultiDimSubject>();
        for (Iterator<MultiDimSubject> it = subjects.iterator(); it.hasNext(); ) {
            addSubject(it.next());
        }
        controllersOffSwitch = listening;
    }

    protected synchronized void quit() {
        controllersOffSwitch.clearFlag();
    }

    protected synchronized boolean checkID(String string) {
        return subjectsByID.containsKey(string);
    }

    protected synchronized boolean isCommitted(String sID) {
        return checkID(sID) && subjectsByID.get(sID).isCommitted;
    }

    protected synchronized boolean isRemovable(String subjectID) {
        return checkID(subjectID) && !(subjectsByID.get(subjectID).isCommitted);
    }

    protected synchronized String putOrPlaceSubject(String subjectID, Hashtable<String, String> values, boolean putFlag)
            throws IOException {
        if (allowRevision) {
            if (isRemovable(subjectID)) {
                removeSubject(subjectID);
            }
        }
        MultiDimSubject subject = new MultiDimSubject(subjectID);
        for (Enumeration<String> e = values.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            String value = values.get(key);
            if (value.length() < 1) {
                if (!variables.isMultiDimensional()) {
                    throw new InvalidDataException("no value for key");
                }
                subject.addCharacteristics(variables.valuesFromKeyValuePair(null, key));
            } else {
                subject.addCharacteristics(variables.valuesFromKeyValuePair(key, value));
            }
        }
        if (!variables.hasAllVariablesSet(subject.baselineCharacteristics)) {
            throw new InvalidDataException("Missing data");
        }
        if (addNewSubject(subject)) {
            if (putFlag) {
                return null;
            } else {
                String groupID = getGroup(subjectID);
                return groupID;
            }
        } else {
            throw new InvalidDataException("duplicate subject?");
        }
    }

    protected synchronized void putSubject(String subjectID, Hashtable<String, String> values) throws IOException {
        putOrPlaceSubject(subjectID, values, true);
    }

    protected synchronized void placeSubject(String subjectID, Hashtable<String, String> values) throws IOException {
        putOrPlaceSubject(subjectID, values, false);
    }

    protected synchronized boolean addSubject(MultiDimSubject subject) throws IOException {
        if (subjectsByID.containsKey(subject.identifier)) {
            System.out.println("WARNING, there was an attempt to add duplicate subject ID " + subject.identifier);
            return false;
        }
        subjectsByID.put(subject.identifier, subject);
        if (subject.myGroup == null) {
            unassignedSubjects.add(subject);
        }
        return true;
    }

    protected synchronized boolean addNewSubject(MultiDimSubject subject) throws IOException {
        boolean result = addSubject(subject);
        database.WriteOutSubjects(subjectsByID, variables);
        return result;
    }

    protected synchronized String getGroup(String subjectID) throws IOException {
        if (subjectsByID.containsKey(subjectID)) {
            MultiDimSubject subject = subjectsByID.get(subjectID);
            boolean didAnyAssignments = false;
            while (subject.myGroup == null) {
                assignAnySubjectAGroup();
                didAnyAssignments = true;
            }
            if (didAnyAssignments) {
                database.WriteOutSubjects(subjectsByID, variables);
                if (verbosity >= 0) {
                    System.out.println("Current group means:");
                    for (Enumeration<String> it = groups.keys(); it.hasMoreElements(); ) {
                        String key = it.nextElement();
                        System.out.print(String.format("%s: ", key));
                        InterventionGroup group = groups.get(key);
                        System.out.println(group.meanVectorForVariables(variables));
                    }
                }
            }
            return subject.myGroup.name;
        } else {
            return null;
        }
    }

    protected abstract void assignAnySubjectAGroup();

    protected synchronized void assignSubjectToGroup(InterventionGroup aGroup, MultiDimSubject multiDimSubject) {
        aGroup.addSubject(multiDimSubject);
        unassignedSubjects.remove(multiDimSubject);
        if (verbosity >= 0) {
            System.out.println(String.format("Assigned %s to %s", multiDimSubject.identifier, aGroup.name));
            if (verbosity > 0) {
                printGroups();
            }
        }
    }

    protected synchronized void printGroups() {
        for (Enumeration<String> it = groups.keys(); it.hasMoreElements(); ) {
            String key = it.nextElement();
            System.out.println(String.format("Composition of group %s:", key));
            InterventionGroup group = groups.get(key);
            group.printSubjectReport();
        }
    }

    public synchronized double maxDistanceBetweenGroups() {
        double max = 0.0;
        for (Enumeration<String> it1 = groups.keys(); it1.hasMoreElements(); ) {
            String key1 = it1.nextElement();
            Hashtable<String, Double> meanVector1 = groups.get(key1).meanVectorForVariables(variables);
            for (Enumeration<String> it2 = groups.keys(); it2.hasMoreElements(); ) {
                String key2 = it2.nextElement();
                if (key1 != key2) {
                    Hashtable<String, Double> meanVector2 = groups.get(key2).meanVectorForVariables(variables);
                    double sum = 0.0;
                    for (Enumeration<String> e = meanVector2.keys(); e.hasMoreElements(); ) {
                        String dimKey = e.nextElement();
                        double diff = meanVector1.get(dimKey).doubleValue()
                                - meanVector2.get(dimKey).doubleValue();
                        sum += diff * diff;
                    } // END for each dimension
                    double dist = Math.sqrt(sum);
                    if (dist > max) {
                        max = dist;
                    }
                }
            }
        }
        return max;
    }

    private boolean commitSubject(String subjectID) throws IOException {
        if (subjectsByID.containsKey(subjectID)) {
            subjectsByID.get(subjectID).isCommitted = true;
            database.WriteOutSubjects(subjectsByID, variables);
            return true;
        } else {
            return false;
        }
    }

    private void removeSubject(String subjectID) throws IOException {
        MultiDimSubject subj = subjectsByID.get(subjectID);
        subjectsByID.remove(subjectID);
        if (subj.myGroup != null) {
            subj.myGroup.subjects.remove(subj);
        }
        database.WriteOutSubjects(subjectsByID, variables);
    }

    private synchronized void assignAllSubjects() throws IOException {
        boolean didAnyAssignments = false;
        while (unassignedSubjects.size() > 0) {
            assignAnySubjectAGroup();
            didAnyAssignments = true;
        }
        if (didAnyAssignments) {
            database.WriteOutSubjects(subjectsByID, variables);
        }
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }
}
