package org.sleepandcognition.pros_rand;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RandomizerController {
    @GetMapping("/")
    public String index() {
        return "here is the main page contents";
    }
}