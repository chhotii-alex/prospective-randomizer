package org.sleepandcognition.prosrand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Hashtable;

/*
 * A null implementation which defines the interface.
 * Not an abstract class-- we have minimal do-nothing implementations of the required methods, to be used for simuluations
 * (when we are not actually interested in saving our results.)
 */
public class SubjectDatabase {
    public ArrayList<MultiDimSubject> ReadSubjectsIntoGroups(
            VariableSet variables, Map<String, InterventionGroup> groups) throws IOException {
        ArrayList<MultiDimSubject> subjects = new ArrayList<MultiDimSubject>();
        return subjects;
    }

    public void WriteOutSubjects(Map<String, MultiDimSubject> subjectsByID, VariableSet variables)
            throws IOException {}
}
