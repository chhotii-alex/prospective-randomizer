package org.sleepandcognition.prosrand;

import java.io.IOException;
import java.util.Hashtable;

public class CommandInterface {
    Randomizer randomizer;

    /*
     Handles communication with a Randomizer instance for a simple socket connection
     or command-line usage. Implements our simple little protocol language.
    */
    public CommandInterface(Randomizer randomizer) {
        this.randomizer = randomizer;
    }

    public String parseCommand(String inputLine) throws IOException {
        if (inputLine.equalsIgnoreCase("HELLO RAND!")) {
            return String.format("HI CLIENT! v%d", Randomizer.RandomizerCommVersion());
        } else if (inputLine.startsWith("QUIT")) {
            randomizer.quit();
            return "OK";
        } else if (inputLine.startsWith("#")) {
            return inputLine;
        } else {
            String[] wordsOnLine = inputLine.split(" ");
            if (wordsOnLine[0].equalsIgnoreCase("EXISTS")) {
                if (randomizer.checkID(wordsOnLine[1])) {
                    return "YES";
                } else {
                    return "NO";
                }
            } // END handling EXISTS
            else if (wordsOnLine[0].equalsIgnoreCase("COMMITTED")) {
                String sID = wordsOnLine[1];
                if (randomizer.isCommitted(sID)) {
                    return "YES";
                } else {
                    return "NO";
                }
            } else if (wordsOnLine[0].equalsIgnoreCase("PUT") || wordsOnLine[0].equalsIgnoreCase("PLACE")) {
                String subjectID = wordsOnLine[1];
                Hashtable<String, String> values = new Hashtable<String, String>();
                for (int i = 2; i < wordsOnLine.length; ++i) {
                    String nameValuePair = wordsOnLine[i];
                    String[] tokens = nameValuePair.split("=");
                    if (tokens.length == 2) {
                        values.put(tokens[0], tokens[1]);
                    } else if (tokens.length == 1) {
                        values.put(tokens[0], "");
                    } else {
                        System.out.println("Corrupt PUT line? " + inputLine);
                        return "?";
                    }
                }
                if (wordsOnLine[0].equalsIgnoreCase("PUT")) {
                    randomizer.putSubject(subjectID, values);
                } else {
                    randomizer.placeSubject(subjectID, values);
                }
            } else if (wordsOnLine[0].equalsIgnoreCase("GET")) {
                String subjectID = wordsOnLine[1];
                String groupID = randomizer.getGroup(subjectID);
                if (groupID == null) {
                    System.out.println("ERROR: client asked for a subject we know nothing about.");
                    return "?";
                } else {
                    return groupID;
                }
            } // END handling "GET"
            else if (wordsOnLine[0].equalsIgnoreCase("COMMIT")) {
                String subjectID = wordsOnLine[1];
                if (randomizer.commitSubject(subjectID)) {
                    return "OK";
                } else {
                    return "?";
                }
            } else if (wordsOnLine[0].equalsIgnoreCase("ASSIGN")) {
                randomizer.assignAllSubjects();
                return "OK";
            }
        } 
        return "?";
    }
}
