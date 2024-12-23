package org.sleepandcognition.prosrand;

public class RandomizerSimulation {

    /*
    DEAD code; will probably implement a different simulation framework;
    public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
        String variablesSpec = "variables.xml";
        String groupListFile = "groups.txt";
        Random r = new Random();
        int subjectsPerRun = 12;
        int numberOfRuns = 10;
        int argNum = 0;
        while (argNum < args.length) {
            String currArg = args[argNum];
            if (currArg.length() == 2 && currArg.charAt(0) == '-') {
                switch (currArg.charAt(1)) {
                    case 'r':
                        variablesSpec = args[argNum + 1];
                        ++argNum;
                        break;
                    case 'g':
                        groupListFile = args[argNum + 1];
                        ++argNum;
                        break;
                    case 's':
                        r.setSeed(Long.parseLong(args[argNum + 1]));
                        ++argNum;
                        break;
                    case 'j':
                        subjectsPerRun = Integer.parseInt(args[argNum + 1]);
                        ++argNum;
                        break;
                    case 'n':
                        numberOfRuns = Integer.parseInt(args[argNum + 1]);
                        ++argNum;
                        break;
                } // END switch
            } // END if
        } // END while processing command-line arguments

        // Null subject database (we don't save data):
        SubjectDatabase database = new SubjectDatabase();
        VariableSet variables = new VariableSet(variablesSpec);
        for (int i = 0; i < numberOfRuns; ++i) {
            ArrayList<Randomizer> randomizers = new ArrayList<Randomizer>();
            Randomizer randomizer;
            // randomizer = new BalancingRandomizer(groupListFile, variables, database, new StillGoingFlag(), false);
            // randomizers.add(randomizer);
            randomizer = new AlternatingRandomizer(groupListFile, variables, database, new StillGoingFlag(), false);
            randomizers.add(randomizer);

            for (int j = 0; j < subjectsPerRun; ++j) {
                String subjectID = String.format("s%d", j);
                String putCommand = String.format("put %s %s", subjectID, variables.randomValuesString(r));
                for (Iterator<Randomizer> it = randomizers.iterator(); it.hasNext(); ) {
                    it.next().parseCommand(putCommand);
                }
                String getCommand = String.format("get %s", subjectID);
                for (Iterator<Randomizer> it = randomizers.iterator(); it.hasNext(); ) {
                    it.next().parseCommand(getCommand);
                }
            } // END for each simulated PUT command
            // print summary of results:
            for (Iterator<Randomizer> it = randomizers.iterator(); it.hasNext(); ) {
                randomizer = it.next();
                System.out.println(String.format(
                        "%s: %f  ", randomizer.getClass().getName(), randomizer.maxDistanceBetweenGroups()));
                randomizer.printGroups();
            }
            System.out.println();
        } // END for each simulation
    } // END main()
     */
}
