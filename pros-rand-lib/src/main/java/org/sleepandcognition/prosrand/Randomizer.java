package org.sleepandcognition.prosrand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public abstract class Randomizer {
    VariableSet variables;
    Map<String, InterventionGroup> groups;
    protected ConcurrentHashMap<String, MultiDimSubject> subjectsByID;
    protected ArrayList<MultiDimSubject> unassignedSubjects;
    StillGoingFlag controllersOffSwitch;
    SubjectDatabase database;
    int verbosity;
    boolean allowRevision;
    protected ArrayList<String> groupNamesInOrder;

    public static int RandomizerCommVersion() {
        return 5;
    }

    public Randomizer(
            String groupListFile,
            VariableSet variables,
            SubjectDatabase db,
            StillGoingFlag listening,
            boolean allowRevision)
            throws SAXException, IOException, ParserConfigurationException, InvalidDataException {
        this.allowRevision = allowRevision;
        database = db;
        this.variables = variables;
        groups = InterventionGroup.ReadGroups(groupListFile);
        controllersOffSwitch = listening;
        readSubjects();
        init();
    }

    public Randomizer(ProtocolSpec spec, SubjectDatabase db) throws IOException, InvalidDataException {
        this.allowRevision = spec.allowRevision;
        database = db;
        variables = new VariableSet(spec.variableSpec);
        groups = new ConcurrentHashMap<String, InterventionGroup>();
        for (Iterator<String> it = spec.groupNames.iterator(); it.hasNext(); ) {
            String groupName = it.next();
            groups.put(groupName, new InterventionGroup(groupName));
        }
        readSubjects();
        init();
    }

    abstract public String getAlgorithm();

    private void init() {
        groupNamesInOrder = new ArrayList<>();
        groupNamesInOrder.addAll(groups.keySet());
        Collections.sort(groupNamesInOrder);
    }

    private void readSubjects() throws IOException, InvalidDataException {
        ArrayList<MultiDimSubject> subjects = database.ReadSubjectsIntoGroups(variables, groups);
        subjectsByID = new ConcurrentHashMap<String, MultiDimSubject>();
        unassignedSubjects = new ArrayList<MultiDimSubject>();
        for (Iterator<MultiDimSubject> it = subjects.iterator(); it.hasNext(); ) {
            addSubject(it.next());
        }
    }

    public synchronized void quit() {
        if (controllersOffSwitch != null) {
            controllersOffSwitch.clearFlag();
        }
    }

    public synchronized boolean checkID(String string) {
        return subjectsByID.containsKey(string);
    }

    public synchronized boolean isCommitted(String sID) {
        return checkID(sID) && subjectsByID.get(sID).isCommitted;
    }

    public synchronized boolean isRemovable(String subjectID) {
        return checkID(subjectID) && !(subjectsByID.get(subjectID).isCommitted);
    }

    public synchronized String putOrPlaceSubject(String subjectID, Map<String, String> values, boolean putFlag)
            throws IOException, InvalidDataException {
        if (allowRevision) {
            if (isRemovable(subjectID)) {
                removeSubject(subjectID);
            }
        }
        MultiDimSubject subject = new MultiDimSubject(subjectID);
        for (Iterator<String> e = values.keySet().iterator(); e.hasNext(); ) {
            String key = e.next();
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

    public synchronized void putSubject(String subjectID, Map<String, String> values)
            throws IOException, InvalidDataException {
        putOrPlaceSubject(subjectID, values, true);
    }

    public synchronized void placeSubject(String subjectID, Map<String, String> values)
            throws IOException, InvalidDataException {
        putOrPlaceSubject(subjectID, values, false);
    }

    protected synchronized boolean addSubject(MultiDimSubject subject) throws IOException, InvalidDataException {
        if (subjectsByID.containsKey(subject.identifier)) {
            throw new InvalidDataException("Attempt to add duplicate subject ID ");
        }
        subjectsByID.put(subject.identifier, subject);
        if (subject.myGroup == null) {
            unassignedSubjects.add(subject);
        }
        return true;
    }

    public synchronized boolean addNewSubject(MultiDimSubject subject) throws IOException, InvalidDataException {
        boolean result = addSubject(subject);
        database.WriteOutSubjects(subjectsByID, variables);
        return result;
    }

    public synchronized String getGroup(String subjectID) throws IOException {
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
                    for (Iterator<String> it = groups.keySet().iterator(); it.hasNext(); ) {
                        String key = it.next();
                        System.out.print(String.format("%s: ", key));
                        InterventionGroup group = groups.get(key);
                        System.out.println(group.getMeanVector());
                    }
                }
            }
            return subject.myGroup.name;
        } else {
            return null;
        }
    }

    protected abstract void assignAnySubjectAGroup();

    public synchronized void assignSubjectToGroup(InterventionGroup aGroup, MultiDimSubject multiDimSubject) {
        aGroup.addSubject(multiDimSubject);
        unassignedSubjects.remove(multiDimSubject);
        if (verbosity >= 0) {
            System.out.println(String.format("Assigned %s to %s", multiDimSubject.identifier, aGroup.name));
            if (verbosity > 1) {
                printGroups();
            }
        }
    }

    protected synchronized void printGroups() {
        for (Iterator<String> it = groups.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            if (verbosity > 1) {
                System.out.println(String.format("Composition of group %s:", key));
            }
            InterventionGroup group = groups.get(key);
            group.printSubjectReport();
        }
    }

    public synchronized double maxDistanceBetweenGroups() {
        double max = 0.0;
        for (Iterator<String> it1 = groups.keySet().iterator(); it1.hasNext(); ) {
            String key1 = it1.next();
            Map<String, Double> meanVector1 = groups.get(key1).getMeanVector();
            for (Iterator<String> it2 = groups.keySet().iterator(); it2.hasNext(); ) {
                String key2 = it2.next();
                if (key1 != key2) {
                    Map<String, Double> meanVector2 = groups.get(key2).getMeanVector();
                    double sum = 0.0;
                    for (Iterator<String> e = meanVector2.keySet().iterator(); e.hasNext(); ) {
                        String dimKey = e.next();
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

    public synchronized boolean commitSubject(String subjectID) throws IOException {
        if (subjectsByID.containsKey(subjectID)) {
            subjectsByID.get(subjectID).isCommitted = true;
            database.WriteOutSubjects(subjectsByID, variables);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void removeSubject(String subjectID) throws IOException {
        MultiDimSubject subj = subjectsByID.get(subjectID);
        subjectsByID.remove(subjectID);
        if (subj.myGroup != null) {
            subj.myGroup.remove(subj);
        }
        else {
            unassignedSubjects.remove(subj);
        }
        database.WriteOutSubjects(subjectsByID, variables);
    }

    public synchronized void assignAllSubjects() throws IOException {
        boolean didAnyAssignments = false;
        while (unassignedSubjects.size() > 0) {
            assignAnySubjectAGroup();
            didAnyAssignments = true;
        }
        if (didAnyAssignments) {
            database.WriteOutSubjects(subjectsByID, variables);
        }
    }

    public synchronized void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

    public synchronized List<InterventionGroup> getGroups() {
        return new ArrayList<>(groups.values());
    }

    public synchronized List<VariableSpec> getVariables() {
        return variables.getVariableSpecs();
    }

    public synchronized List<MultiDimSubject> getSubjects() {
        return new ArrayList<>(subjectsByID.values());
    }

    public synchronized boolean matchesSpecs(ProtocolSpec spec) {
        if (!variables.matchesSpec(spec.variableSpec)) {
            return false;
        }
        for (Iterator<String> it = spec.groupNames.iterator(); it.hasNext(); ) {
            String groupName = it.next();
            if (!groups.containsKey(groupName)) {
                return false;
            }
        }
        if (spec.groupNames.size() != groups.size()) {
            return false;
        }
        return true;
    }
}
