package org.sleepandcognition.prosrand;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class BalancingRandomizer extends Randomizer {
    public BalancingRandomizer(
            String groupListFile,
            VariableSet variables,
            SubjectDatabase db,
            StillGoingFlag listening,
            boolean allowRevision)
            throws SAXException, IOException, ParserConfigurationException, InvalidDataException {
        super(groupListFile, variables, db, listening, allowRevision);
    }

    public BalancingRandomizer(ProtocolSpec spec, SubjectDatabase db) throws IOException, InvalidDataException {
        super(spec, db);
    }

    private MeanVectorCalculator means;

    protected synchronized MeanVectorCalculator getMeans() {
        if (means == null) {
            means = new MeanVectorCalculator();
        }
        return means;
    }

    @Override
    protected synchronized boolean addSubject(MultiDimSubject subject) throws IOException, InvalidDataException {
        if (super.addSubject(subject)) {
            /* Keep track of statistics on all subjects encountered: */
            for (Enumeration<String> vit = subject.baselineCharacteristics.keys(); vit.hasMoreElements(); ) {
                String key = vit.nextElement();
                Double value = subject.baselineCharacteristics.get(key);
                getMeans().encounter(key, value);
            }
            return true;
        } else {
            return false;
        }
    }

    /* This method is the core of the balancing randomizer.
     * Find the un-assigned subject and the group that have the most negative dot product
     * (i.e. the greatest mis-match) and assign that subject to that group.
     *
     * First iterate through the groups to find the minimum scaled group size.
     *  For each group, check that it is not empty in this pass. (If we do
     * find an empty group, immediately assign an arbitrary subject to that group and we are done.)
     *
     * Iterate over the groups again, picking out those whose scaled group size is the minimum
     * scaled group size. For each of these groups, calculate the average vector, averaged over the
     * subjects already in that group; and for each unassigned subject, calculate the dot product
     * of the group's vector and the subject's vector. Keep track of which group/subject pairing is the
     * winner for most negative.
     *
     * Be sure to save the subject database after each assignment, so that we do not lose
     * any assignment which may have been announced to the world.
     */
    @Override
    protected synchronized void assignAnySubjectAGroup() {
        int minimumScaledGroupSize = Integer.MAX_VALUE;
        for (Iterator<String> it = groupNamesInOrder.iterator(); it.hasNext(); ) {
            InterventionGroup aGroup = groups.get(it.next());
            if (aGroup.isEmpty()) {
                if (verbosity > 0) {
                    System.out.println("Found empty group, will assign to that one");
                }
                // Find the most normal subject so far
                double shortestVector = Double.MAX_VALUE;
                MultiDimSubject winningSubject = null;
                for (MultiDimSubject subject : unassignedSubjects) {
                    double length = nomalizedLength(subject.baselineCharacteristics);
                    if (length < shortestVector) {
                        shortestVector = length;
                        winningSubject = subject;
                    }
                }
                assignSubjectToGroup(aGroup, winningSubject);
                return;
            }
            if (aGroup.currentGroupSize() < minimumScaledGroupSize) {
                minimumScaledGroupSize = aGroup.currentGroupSize();
            }
        } // END for each group (first pass)

        if (verbosity >= 0) {
            for (Enumeration<String> it = means.keyIterator(); it.hasMoreElements(); ) {
                String key = it.nextElement();
                if (verbosity > 0) {
                    System.out.print(String.format("Mean of %s, all subjects: %f  ", key, means.mean(key)));
                    System.out.println(String.format("Std dev of %s: %f", key, means.stddev(key)));
                }
            }
        }
        if (verbosity >= 0) {
            System.out.println("Considering groups:");
        }
        double mostNegativeDotProduct = Double.MAX_VALUE;
        InterventionGroup winningGroup = null;
        MultiDimSubject winningSubject = null;
        for (Iterator<String> it = groupNamesInOrder.iterator(); it.hasNext(); ) {
            InterventionGroup aGroup = groups.get(it.next());
            if (aGroup.currentGroupSize()
                    == minimumScaledGroupSize) { // This group is in least-filled tier; consider adding to it
                Map<String, Double> vector = aGroup.getMeanVector();
                if (verbosity >= 0) {
                    System.out.println(aGroup.sizeString());
                }
                for (MultiDimSubject subject : unassignedSubjects) {
                    double dotProduct = dotProductForVectors(vector, subject.baselineCharacteristics);
                    if (dotProduct < mostNegativeDotProduct) {
                        mostNegativeDotProduct = dotProduct;
                        winningGroup = aGroup;
                        winningSubject = subject;
                    }
                }
            }
        } // END for each group (second pass)
        assignSubjectToGroup(winningGroup, winningSubject);
    }

    private double nomalizedLength(Map<String, Double> baselineCharacteristics) {
        double accum = dotProductForVectors(baselineCharacteristics, baselineCharacteristics);
        return Math.sqrt(accum);
    }

    private double dotProductForVectors(
            Map<String, Double> vector, Map<String, Double> baselineCharacteristics) {
        double accum = 0.0;

        for (String key : baselineCharacteristics.keySet()) {
            double mean = getMeans().mean(key);
            double stddev = getMeans().stddev(key);
            double v1 = 0.0;
            double v2 = 0.0;
            if (stddev > 0.0) {
                v1 = (baselineCharacteristics.get(key) - mean) / stddev;
                v2 = (vector.get(key) - mean) / stddev;
            }
            double weight = variables.weightForKey(key);
            accum += weight * v1 * v2;
        }
        return accum;
    }

    @Override
    public String getAlgorithm() {
        return "Balanced";
    }
}
