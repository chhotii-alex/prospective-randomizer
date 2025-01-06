package org.sleepandcognition.prosrandboot;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import org.sleepandcognition.prosrand.AlternatingRandomizer;
import org.sleepandcognition.prosrand.BalancingRandomizer;
import org.sleepandcognition.prosrand.InterventionGroup;
import org.sleepandcognition.prosrand.InvalidDataException;
import org.sleepandcognition.prosrand.MultiDimSubject;
import org.sleepandcognition.prosrand.ProtocolSpec;
import org.sleepandcognition.prosrand.Randomizer;
import org.sleepandcognition.prosrand.SubjectDatabase;
import org.sleepandcognition.prosrand.SubjectFileDatabase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

@CrossOrigin
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

    protected Randomizer randomizerOfName(String protocolName) {
        if (randomizers.containsKey(protocolName)) {
            return randomizers.get(protocolName);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{protocolName}/start")
    synchronized void startProtocol(@PathVariable String protocolName, @RequestBody ProtocolSpec spec, 
                 @RequestParam(required = false) boolean temp)
            throws Exception {
        if (randomizers.containsKey(protocolName)) {
            if (!randomizerOfName(protocolName).matchesSpecs(spec)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            // already started; ignore
            return;
        }
        SubjectDatabase database;
        if (temp) {
            database = new SubjectDatabase();
        }
        else {
            String subjectFile = String.format("subjects_%s.txt", protocolName);
            database = new SubjectFileDatabase(subjectFile);
        }
        Randomizer r;
        if (spec.getAlgorithm().equals("Alternating")) {
            r = new AlternatingRandomizer(spec, database);
        } else if (spec.getAlgorithm().equals("Balanced")) {
            r = new BalancingRandomizer(spec, database);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        // Everything possible in log, as that can be useful for troubleshooting
        r.setVerbosity(100);
        randomizers.put(protocolName, r);
    }

    @GetMapping("/{protocolName}/subject/{id}")
    boolean getSubject(@PathVariable String protocolName, @PathVariable String id) {
        boolean doesExist = randomizerOfName(protocolName).checkID(id);
        if (doesExist) {
            return true;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{protocolName}/subject/{id}")
    void putSubject(
            @PathVariable String protocolName, @PathVariable String id, @RequestBody Hashtable<String, String> features)
            throws Exception {
        try {
            randomizerOfName(protocolName).putOrPlaceSubject(id, features, true);
        } catch (InvalidDataException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{protocolName}/subject/{id}/group")
    String getGroup(@PathVariable String protocolName, @PathVariable String id) throws Exception {
        return randomizerOfName(protocolName).getGroup(id);
    }

    @PostMapping("/{protocolName}/subject/{id}/group")
    String placeSubject(
            @PathVariable String protocolName, @PathVariable String id, @RequestBody Hashtable<String, String> features)
            throws Exception {
        try {
            String group = randomizerOfName(protocolName).putOrPlaceSubject(id, features, false);
            return group;
        } catch (InvalidDataException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{protocolName}/subject/{id}/commit")
    boolean commitSubject(@PathVariable String protocolName, @PathVariable String id) throws Exception {
        return randomizerOfName(protocolName).commitSubject(id);
    }

    @GetMapping("/{protocolName}/subject/{id}/committed")
    boolean isSubjectCommitted(@PathVariable String protocolName, @PathVariable String id) {
        return randomizerOfName(protocolName).isCommitted(id);
    }

    @GetMapping("/{protocolName}/groups")
    List<InterventionGroup> getAllGroups(@PathVariable String protocolName) {
        return randomizerOfName(protocolName).getGroups();
    }

    @GetMapping("/{protocolName}/variables")
    Hashtable<String, String> getVariables(@PathVariable String protocolName) {
        return randomizerOfName(protocolName).getVariables().getVariables();
    }

    @GetMapping("/{protocolName}/subjects")
    List<MultiDimSubject> getSubjects(@PathVariable String protocolName) {
        return randomizerOfName(protocolName).getSubjects();
    }

    @PostMapping("/{protocolName}/assignall")
    void assignAll(@PathVariable String protocolName) throws Exception {
        randomizerOfName(protocolName).assignAllSubjects();
    }
}
