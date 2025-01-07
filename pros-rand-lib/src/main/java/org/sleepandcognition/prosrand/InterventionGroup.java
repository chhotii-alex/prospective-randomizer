package org.sleepandcognition.prosrand;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/* See https://clinicaltrials.gov/ct2/about-studies/glossary for terminology
 *
 * An instance of InterventionGroup represents one of the groups to which subjects are to be assigned.
 * Subjects must be assigned to a group at a point in time in the protocol after their baseline characteristics
 * have been determined, but before any treatment differs according to group.
 *
 * The BalancingRandomizer will strive to keep the size of each group equal.
 * */

public class InterventionGroup {
    protected String name;
    protected ArrayList<MultiDimSubject> subjects;
    protected MeanVectorCalculator means;

    static Hashtable<String, InterventionGroup> ReadGroups(String fileNameOrPath) {
        Hashtable<String, InterventionGroup> groups = new Hashtable<String, InterventionGroup>();
        File file = new File(fileNameOrPath);
        FileInputStream fs = null;
        DataInputStream ds = null;
        InputStreamReader is = null;
        BufferedReader br = null;
        String oneLine;
        String[] wordsOnLine;
        try {
            fs = new FileInputStream(file);
            ds = new DataInputStream(fs);
            is = new InputStreamReader(ds);
            br = new BufferedReader(is);
            oneLine = null;
            while ((oneLine = br.readLine()) != null) {
                oneLine = oneLine.trim();
                wordsOnLine = oneLine.split("\t");
                if (wordsOnLine.length > 0 && wordsOnLine[0].length() > 0) {
                    String name = wordsOnLine[0];
                    InterventionGroup group = new InterventionGroup(name);
                    groups.put(name, group);
                }
            }
            fs.close();
        } catch (IOException io) {
            io.printStackTrace();
            System.out.println("Error! Could not read in groups.");
            System.exit(1);
        }
        return groups;
    }

    public InterventionGroup(String n) {
        name = n;
        subjects = new ArrayList<MultiDimSubject>();
    }

    public String getName() {
        return name;
    }

    public int currentGroupSize() {
        return subjects.size();
    }

    public boolean isEmpty() {
        return (subjects.size() < 1);
    }

    public ArrayList<MultiDimSubject> getSubjects() {
        return subjects;
    }

    public void addSubject(MultiDimSubject subject) {
        subjects.add(subject);
        subject.setGroup(this);
        if (means != null) {
            means.encounter(subject);
        }
    }

    protected MeanVectorCalculator getMeans() {
        if (means == null) {
            means = new MeanVectorCalculator();
            for (Iterator<MultiDimSubject> it = subjects.iterator(); it.hasNext(); ) {
                MultiDimSubject s = it.next();
                means.encounter(s);
            }
        }
        return means;
    }

    public Hashtable<String, Double> getMeanVector() {
        Hashtable<String, Double> meanVector = new Hashtable<String, Double>();
        if (subjects.size() > 0) {
            for (Enumeration<String> vit = getMeans().keyIterator(); vit.hasMoreElements(); ) {
                String key = vit.nextElement();
                meanVector.put(key, Double.valueOf(getMeans().mean(key)));
            }
            return meanVector;
        }
        return null;
    }

    public void printSubjectReport() {
        for (Iterator<MultiDimSubject> it = subjects.iterator(); it.hasNext(); ) {
            MultiDimSubject s = it.next();
            System.out.print(s.identifier);
            System.out.print("\t");
            System.out.println(s.baselineCharacteristics);
        }
    }

    public String sizeString() {
        return String.format("%s: %d    ", name, subjects.size());
    }

    public void remove(MultiDimSubject subj) {
        subjects.remove(subj);
        subj.myGroup = null;
        // invalidate calculations so far
        means = null;
    }

    public InterventionGroupStrings makeStringVersion(VariableSet variables) {
        InterventionGroupStrings g = new InterventionGroupStrings(name);
        for (Iterator<MultiDimSubject> it = subjects.iterator(); it.hasNext(); ) {
            MultiDimSubject s = it.next();
            g.addSubject(variables.stringsFromValues(s.baselineCharacteristics));
        }
        return g;
    }
}
