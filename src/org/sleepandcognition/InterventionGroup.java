package org.sleepandcognition;

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
 * By default the BalancingRandomizer will strive to keep the size of each group equal, and the relativeGroupSize
 * of each group is set to 1. However, a group can be set to grow proportionally faster (or slower) than other groups
 * by specifying a relativeGroupSize other than 1. If the relativeGroupSize is x, the BalancingRandomizer will strive to
 * keep the group x times as large as a group with a relativeGroupSize of 1. For example, if there are two groups, A and
 * B, and the relativeGroupSize of A is 2 whilst the relativeGroupSize of B is left at 1, then of each 3 subjects enrolled
 * in the study, 2 will be added to group A and one to group B. 
 * */

public class InterventionGroup {
	protected String name;
	protected double relativeGroupSize;  /* defaults to 1 */
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
				if (wordsOnLine.length > 0 && wordsOnLine[0].length()>0) {
					String name = wordsOnLine[0];
					InterventionGroup group = null;
					if (wordsOnLine.length > 1) {
						double relativeGroupSize = Double.parseDouble(wordsOnLine[1]);
						group = new InterventionGroup(name, relativeGroupSize);
					}
					else {
						group = new InterventionGroup(name);
					}
					groups.put(name, group);
				}
			}
			fs.close();
		}
		catch (IOException io) {
			io.printStackTrace();
			System.out.println("Error! Could not read in groups.");
			System.exit(1);
		}
		return groups;
	}
	
	public InterventionGroup(String n, double s) {
		name = n;
		means = new MeanVectorCalculator();
		relativeGroupSize = s;
		subjects = new ArrayList<MultiDimSubject>();
	}

	public InterventionGroup(String n) {
		name = n;
		means = new MeanVectorCalculator();
		relativeGroupSize = 1.0;
		subjects = new ArrayList<MultiDimSubject>();
	}
	
	public String name() {
		return name;
	}

	public int currentGroupSize() {
		return subjects.size();
	}
	
	/* 
	 * N.B. that relativeGroupSize is usually 1, so this usually returns simply the number of subjects currently in the group.
	 * (However, if we want to enroll a larger (or smaller) proportion of subjects in one group than in others, a value
	 * of relativeGroupSize can be set in the configuration.)
	 * We always add subjects only to the least-filled tier of groups. I.e., (assuming all groups have the same relativeGroupSize for the 
	 * moment), if some groups contain 2 subjects and others
	 * contain 3, we ONLY consider adding subject to a group that has 2 subjects. This keeps group sizes equalized over time:
	 * thus we don't bias one group towards subjects who were enrolled earlier, and another group towards subjects enrolled
	 * later.
	 * If a group has a relativeGroupSize other than 1, its fullness tier is determined by what multiple number of subjects
	 * it should have compared to other groups. For example, if group A has relativeGroupSize of 2, and group B has a relativeGroupSize
	 * of 1, 2 subjects must be added to group A for every 1 subject in group B. These two groups are considered to be on
	 * the same fullness tier-- have the same scaledGroupSize()-- when group B has n subjects and group A has 2*n subjects.
	 * At that point, of the next 3 subjects, we want to assign 1 to group B and 2 to group A. Thus, adding a subject to 
	 * group B will bump it to the next fullness tier-- but it requires adding TWO subjects to group A to bump it to the
	 * next fullness tier. After adding just one to A, we still need another of the current triplet of incoming subjects.
	 * Thus the use of Math.floor(), so that a mere fraction of the current tranche does not take the group out of consideration.
	 * 
	 *  THIS IS SEVERELY BROKEN!!! Just don't set a relativeGroupSize in groups.txt until we fix this. (Ditch this feature entirely?)
	 */
	public int scaledGroupSize() {
		if (relativeGroupSize <= 0.0) {
			return Integer.MAX_VALUE;
		}
		int s = (int)Math.floor(currentGroupSize()/relativeGroupSize);
		return s;
	}

	public boolean isEmpty() {
		return ((relativeGroupSize > 0.0) && (subjects.size() < 1));
	}

	public void addSubject(MultiDimSubject subject) {
		subjects.add(subject);
		subject.setGroup(this);
		for (Enumeration<String> vit = subject.baselineCharacteristics.keys(); vit.hasMoreElements(); ) {
			String key = vit.nextElement();
			Double value = subject.baselineCharacteristics.get(key);
			means.encounter(key, value.doubleValue());
		}
	}

	public Hashtable<String, Double> meanVectorForVariables (VariableSet variables) {
		Hashtable<String, Double> meanVector = new Hashtable<String, Double>();
		if (subjects.size() > 0) {
			for (Enumeration<String> vit = means.keyIterator(); vit.hasMoreElements(); ) {
				String key = vit.nextElement();
				meanVector.put(key, new Double(means.mean(key)));
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


}
