package org.sleepandcognition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class AlternatingRandomizer extends Randomizer {
	protected ArrayList<String> groupNamesInOrder; 

	public AlternatingRandomizer(String groupListFile, VariableSet variables,
			SubjectDatabase db, StillGoingFlag listening, boolean allowRevision)
			throws SAXException, IOException, ParserConfigurationException {
		super(groupListFile, variables, db, listening, allowRevision);
		groupNamesInOrder = new ArrayList<String>();
		groupNamesInOrder.addAll(groups.keySet());
		Collections.sort(groupNamesInOrder);
	}

	protected synchronized void assignAnySubjectAGroup() {
		int minimumScaledGroupSize = Integer.MAX_VALUE;
		for (Iterator<String> it = groupNamesInOrder.iterator(); it.hasNext();) {
			InterventionGroup aGroup = groups.get(it.next());
			if (aGroup.isEmpty()) {
				assignSubjectToGroup(aGroup, unassignedSubjects.get(0));
//				if (!it.hasNext()) {
//					Collections.reverse(groupNamesInOrder);
//				}
				return;
			}
			if (aGroup.scaledGroupSize() < minimumScaledGroupSize) {
				minimumScaledGroupSize = aGroup.scaledGroupSize();
			}
		} // END for each group (first pass)

		for (Iterator<String> it = groupNamesInOrder.iterator(); it.hasNext();) {
			InterventionGroup aGroup = groups.get(it.next());
			if (aGroup.scaledGroupSize() == minimumScaledGroupSize) {  // This group is in least-filled tier; consider adding to it
				assignSubjectToGroup(aGroup, unassignedSubjects.get(0));
//				if (!it.hasNext()) {
//					Collections.reverse(groupNamesInOrder);
//				}
				return;
			}
		} // END for each group (second pass)
	}

}
