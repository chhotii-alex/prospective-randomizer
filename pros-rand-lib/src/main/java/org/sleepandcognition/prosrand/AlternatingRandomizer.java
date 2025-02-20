package org.sleepandcognition.prosrand;

import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class AlternatingRandomizer extends Randomizer {

    public AlternatingRandomizer(
            String groupListFile,
            VariableSet variables,
            SubjectDatabase db,
            StillGoingFlag listening,
            boolean allowRevision)
            throws SAXException, IOException, ParserConfigurationException, InvalidDataException {
        super(groupListFile, variables, db, listening, allowRevision);
    }

    public AlternatingRandomizer(ProtocolSpec spec, SubjectDatabase db) throws IOException, InvalidDataException {
        super(spec, db);
    }

    @Override
    protected synchronized void assignAnySubjectAGroup() {
        int minimumScaledGroupSize = Integer.MAX_VALUE;
        for (Iterator<String> it = groupNamesInOrder.iterator(); it.hasNext(); ) {
            InterventionGroup aGroup = groups.get(it.next());
            if (aGroup.isEmpty()) {
                assignSubjectToGroup(aGroup, unassignedSubjects.get(0));
                return;
            }
            if (aGroup.currentGroupSize() < minimumScaledGroupSize) {
                minimumScaledGroupSize = aGroup.currentGroupSize();
            }
        } // END for each group (first pass)

        for (Iterator<String> it = groupNamesInOrder.iterator(); it.hasNext(); ) {
            InterventionGroup aGroup = groups.get(it.next());
            if (aGroup.currentGroupSize()
                    == minimumScaledGroupSize) { // This group is in least-filled tier; consider adding to it
                assignSubjectToGroup(aGroup, unassignedSubjects.get(0));
                return;
            }
        } // END for each group (second pass)
    }

    @Override
    public String getAlgorithm() {
        return "Alternating";
    }
}
