package org.sleepandcognition.prosrandboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProsRandApplication {
    /*
     * Some development notes:
     * Maintain consistent source code formatting using the maven "spotless" plug-in.
     * This is configured for each of the modules (but not for the aggregated project,
     * as that has no code). To invoke, cd into the directory of whatever module has
     * been edited, then invoke mvn, i.e.:
     * cd pros-rand-boot
     * mvn spotless:apply
     */

    public static void main(String[] args) {
        SpringApplication.run(ProsRandApplication.class, args);
    }
}
