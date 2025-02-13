package org.sleepandcognition.prosrand;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;

public class SubjectFileDatabase extends SubjectDatabase {
    File subjectDatabaseFile;

    public SubjectFileDatabase(String subjectFile) {
        subjectDatabaseFile = new File(subjectFile);
    }

    public ArrayList<MultiDimSubject> ReadSubjectsIntoGroups(
            VariableSet variables, Map<String, InterventionGroup> groups) throws IOException {
        ArrayList<MultiDimSubject> subjects = new ArrayList<MultiDimSubject>();
        FileInputStream fs = null;
        DataInputStream ds = null;
        InputStreamReader is = null;
        BufferedReader br = null;
        String oneLine;
        String[] wordsOnLine;
        if (subjectDatabaseFile.exists()) {
            try {
                fs = new FileInputStream(subjectDatabaseFile);
                ds = new DataInputStream(fs);
                is = new InputStreamReader(ds);
                br = new BufferedReader(is);
                oneLine = null;
                while ((oneLine = br.readLine()) != null) {
                    oneLine = oneLine.trim();
                    wordsOnLine = oneLine.split("\t");
                    if (wordsOnLine.length > 2) {
                        String group = wordsOnLine[0];
                        String id = wordsOnLine[1];
                        MultiDimSubject subject = new MultiDimSubject(id);
                        for (int i = 2; i < wordsOnLine.length; ++i) {
                            String field = wordsOnLine[i];
                            if (!field.isEmpty()) {
                                if (field.equalsIgnoreCase("committed")) {
                                    subject.isCommitted = true;
                                } else {
                                    String[] tokens = field.split("=");
                                    if (tokens.length != 2) {
                                        if (tokens.length == 1 && !variables.isMultiDimensional()) {
                                            subject.addCharacteristics(
                                                    variables.valuesFromKeyValuePair(null, tokens[0]));
                                        } else {
                                            System.out.println("Corrupt subject line? " + oneLine);
                                            throw new IOException("Unexpected data in subject file");
                                        }
                                    } else {
                                        Hashtable<String, Double> characteristics =
                                                variables.valuesFromKeyValuePair(tokens[0], tokens[1]);
                                        subject.addCharacteristics(characteristics);
                                    }
                                } // END it's not the "committed" token
                            } // END non-empty field
                        } // END for each field of info on this subject
                        if (!variables.hasAllVariablesSet(subject.baselineCharacteristics)) {
                            System.out.println("Warning! Data missing from this subject record: " + oneLine);
                        }
                        if (!group.equals("-")) {
                            groups.get(group).addSubject(subject);
                        }
                        subjects.add(subject);
                    }
                }
            } catch (InvalidDataException ex) {
                ex.printStackTrace();
                System.out.println(
                        "Uh-oh... File listing previous subjects appears to exist, but contains invalid data");
                throw new IOException("Invalid data in subject file");
            } finally {
                try {
                    br.close();
                    br = null;
                    is.close();
                    is = null;
                    ds.close();
                    ds = null;
                    fs.close();
                    fs = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } // END if file exists
        return subjects;
    }

    public void WriteOutSubjects(Map<String, MultiDimSubject> subjectsByID, VariableSet variables)
            throws IOException {
        File directory = subjectDatabaseFile.getParentFile();
        File tempFile = File.createTempFile("subj", ".txt", directory);
        FileWriter fw = new FileWriter(tempFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);

        for (Iterator<MultiDimSubject> it = subjectsByID.values().iterator(); it.hasNext(); ) {
            MultiDimSubject aSubject = it.next();
            if (aSubject.myGroup == null) {
                out.print(String.format("-\t%s\t", aSubject.identifier));
            } else {
                out.print(String.format("%s\t%s\t", aSubject.myGroup.name, aSubject.identifier));
            }
            out.print(variables.keyValueEncodingFromValues(aSubject.baselineCharacteristics));
            if (aSubject.isCommitted) {
                out.print("committed");
            }
            out.print("\n");
        }
        out.flush();
        out.close();
        bw.close();
        fw.close();
        if (subjectDatabaseFile.exists()) {
            boolean deleteSuccess = subjectDatabaseFile.delete();
            if (!deleteSuccess) {
                throw new IOException("Could not replace subjects.txt!");
            }
        }
        tempFile.renameTo(subjectDatabaseFile);
    }
}
