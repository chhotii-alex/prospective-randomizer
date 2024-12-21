package org.sleepandcognition.prosrandboot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.xml.sax.SAXException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.sleepandcognition.prosrand.Randomizer;
import org.sleepandcognition.prosrand.BalancingRandomizer;
import org.sleepandcognition.prosrand.StillGoingFlag;
import org.sleepandcognition.prosrand.SubjectFileDatabase;
import org.sleepandcognition.prosrand.VariableSet;

@RestController
public class RandomizerController {
    Randomizer randomizer;

    public RandomizerController() throws SAXException, ParserConfigurationException, IOException {
        String groupListFile = "groups.txt";
        String subjectFile = "subjects.txt";
        String variablesSpec = "variables.xml";
        StillGoingFlag listening = new StillGoingFlag();
        SubjectFileDatabase database = new SubjectFileDatabase(subjectFile);
        VariableSet variables = new VariableSet(variablesSpec);
        randomizer = new BalancingRandomizer(groupListFile, variables, database, listening, true);
    }

    @GetMapping("/")
    public String index() {
        StillGoingFlag gah = new StillGoingFlag();
        return "here is the main page contents";
    }
}
