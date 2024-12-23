package org.sleepandcognition.prosrandboot;

import java.io.IOException;
import java.util.Hashtable;
import javax.xml.parsers.ParserConfigurationException;
import org.sleepandcognition.prosrand.BalancingRandomizer;
import org.sleepandcognition.prosrand.InterventionGroup;
import org.sleepandcognition.prosrand.MultiDimSubject;
import org.sleepandcognition.prosrand.ProtocolSpec;
import org.sleepandcognition.prosrand.Randomizer;
import org.sleepandcognition.prosrand.SubjectFileDatabase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

@RestController
public class RandomizerController {
    Hashtable<String, Randomizer> randomizers;

    public RandomizerController() throws SAXException, ParserConfigurationException, IOException {
        randomizers = new Hashtable<String, Randomizer>();
    }

    @GetMapping("/")
    public String index() {
        return "This is the group randomization server.";
    }

    @GetMapping("/version")
    public Integer version() {
        return Randomizer.RandomizerCommVersion();
    }

    @PostMapping("/{protocolName}/start")
    synchronized void startProtocol(@PathVariable String protocolName, @RequestBody ProtocolSpec spec)
            throws Exception {
        if (randomizers.containsKey(protocolName)) {
            // already started; ignore
            return;
        }
        String subjectFile = String.format("subjects_%s.txt", protocolName);
        SubjectFileDatabase database = new SubjectFileDatabase(subjectFile);
        Randomizer r = new BalancingRandomizer(spec, database);
        r.setVerbosity(100); // TODO: adjust
        randomizers.put(protocolName, r);
    }

    @PostMapping("/{protocolName}/subject/{id}")
    void putSubject(
            @PathVariable String protocolName, @PathVariable String id, @RequestBody Hashtable<String, String> features)
            throws Exception {
        randomizers.get(protocolName).putOrPlaceSubject(id, features, true);
    }

    @GetMapping("/{protocolName}/subject/{id}/group")
    String getGroup(@PathVariable String protocolName, @PathVariable String id) throws Exception {
        return randomizers.get(protocolName).getGroup(id);
    }

    @PostMapping("/{protocolName}/subject/{id}/group")
    String placeSubject(
            @PathVariable String protocolName, @PathVariable String id, @RequestBody Hashtable<String, String> features)
            throws Exception {
        String group = randomizers.get(protocolName).putOrPlaceSubject(id, features, false);
        return group;
    }

    @PostMapping("/{protocolName}/subject/{id}/commit")
    boolean commitSubject(@PathVariable String protocolName, @PathVariable String id) throws Exception {
        return randomizers.get(protocolName).commitSubject(id);
    }

    @GetMapping("/{protocolName}/subject/{id}/committed")
    boolean isSubjectCommitted(@PathVariable String protocolName, @PathVariable String id) {
        return randomizers.get(protocolName).isCommitted(id);
    }

    @GetMapping("/{protocolName}/groups")
    Hashtable<String, InterventionGroup> getAllGroups(@PathVariable String protocolName) {
        return randomizers.get(protocolName).getGroups();
    }

    @GetMapping("/{protocolName}/variables")
    Hashtable<String, String> getVariables(@PathVariable String protocolName) {
        return randomizers.get(protocolName).getVariables().getVariables();
    }

    @GetMapping("/{protocolName}/subjects")
    Hashtable<String, MultiDimSubject> getSubjects(@PathVariable String protocolName) {
        return randomizers.get(protocolName).getSubjects();
    }

    @PostMapping("/{protocolName}/assignall")
    void assignAll(@PathVariable String protocolName) throws Exception {
        randomizers.get(protocolName).assignAllSubjects();
    }
}
