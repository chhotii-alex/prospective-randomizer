package org.sleepandcognition;

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

	public Randomizer(String groupListFile, VariableSet variables, 
			SubjectDatabase db, StillGoingFlag listening, boolean allowRevision) throws SAXException, IOException, ParserConfigurationException {
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
	
	protected synchronized boolean checkID(String string) {
		return subjectsByID.containsKey(string);
	}

	protected synchronized boolean addSubject(MultiDimSubject subject) {
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
		}
		else {
			return null;
		}
	}

	protected abstract void assignAnySubjectAGroup();

	protected synchronized void assignSubjectToGroup(InterventionGroup aGroup,
			MultiDimSubject multiDimSubject) {
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
						double diff = meanVector1.get(dimKey).doubleValue()-meanVector2.get(dimKey).doubleValue();
						sum += diff*diff;
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

	public String parseCommand(String inputLine) throws IOException {
		if (inputLine.equalsIgnoreCase("HELLO RAND!")) {
			return String.format("HI CLIENT! v%d", RandomizerCommVersion());
		}
		else if (inputLine.startsWith("QUIT")) {
			controllersOffSwitch.clearFlag();
			return "OK";
		}
		else if (inputLine.startsWith("#")) {
			return inputLine;
		}
		else {
			String[] wordsOnLine = inputLine.split(" ");
			if (wordsOnLine[0].equalsIgnoreCase("EXISTS")) {
				if (checkID(wordsOnLine[1])) {
					return "YES";
				}
				else {
					return "NO";
				}
			}  // END handling EXISTS
			else if (wordsOnLine[0].equalsIgnoreCase("COMMITTED")) {
				String sID = wordsOnLine[1];
				if (checkID(sID) && subjectsByID.get(sID).isCommitted) {
					return "YES";
				}
				else {
					return "NO";
				}
			}
			else if (wordsOnLine[0].equalsIgnoreCase("PUT") || wordsOnLine[0].equalsIgnoreCase("PLACE")) {
				String subjectID = wordsOnLine[1];
				if (allowRevision) {
					if (checkID(subjectID) && !(subjectsByID.get(subjectID).isCommitted)) {
						removeSubject(subjectID);
					}
				}
				MultiDimSubject subject = new MultiDimSubject(subjectID);
				for (int i = 2; i < wordsOnLine.length; ++i) {
					String nameValuePair = wordsOnLine[i];
					String[] tokens = nameValuePair.split("=");
					if (tokens.length != 2) {
						if (tokens.length == 1 && !variables.isMultiDimensional()) {
							subject.addCharacteristics(variables.valuesFromKeyValuePair(null, tokens[0]));
						}
						else {
							System.out.println("Corrupt PUT line? " + inputLine);
							return "?";
						}
					}
					else {
						subject.addCharacteristics(variables.valuesFromKeyValuePair(tokens[0], tokens[1]));
					}
				}
				if (!variables.hasAllVariablesSet(subject.baselineCharacteristics)) {
					System.out.println("Warning! Data missing from new subject: " + inputLine);
				}
				if (addNewSubject(subject)) {
					if (wordsOnLine[0].equalsIgnoreCase("PUT")) {
						return "OK";
					}
					else {
						String groupID = getGroup(subjectID);
						return groupID;
					}
				}
				else {
					return "DUP!!!!";
				}
			}  // END handling "PUT" 
			else if (wordsOnLine[0].equalsIgnoreCase("GET")) {
				String subjectID = wordsOnLine[1];
				String groupID = getGroup(subjectID);
				if (groupID == null) {
					System.out.println("ERROR: client asked for a subject we know nothing about.");
					return "?";
				}
				else {
					return groupID;
				}
			} // END handling "GET" 
			else if (wordsOnLine[0].equalsIgnoreCase("COMMIT")) {
				String subjectID = wordsOnLine[1];
				if (commitSubject(subjectID)) {
					return "OK";
				}
				else {
					return "?";
				}
			}
			else if (wordsOnLine[0].equalsIgnoreCase("ASSIGN")) {
				assignAllSubjects();
				return "OK";
			}
			else {
				return "?";
			}
		}  // END handling any command that has arguments
	}

	private boolean commitSubject(String subjectID) throws IOException {
		if (subjectsByID.containsKey(subjectID)) {
			subjectsByID.get(subjectID).isCommitted = true;
			database.WriteOutSubjects(subjectsByID, variables);
			return true;
		}
		else {
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
