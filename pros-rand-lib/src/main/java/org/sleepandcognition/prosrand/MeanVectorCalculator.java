package org.sleepandcognition.prosrand;

import java.util.Enumeration;
import java.util.Hashtable;

public class MeanVectorCalculator {
    Hashtable<String, MeanCalculator> calculators;
    
    public MeanVectorCalculator() {
        calculators = new Hashtable<String, MeanCalculator>();
    }
    
    public Enumeration<String> keyIterator() {
        return calculators.keys();
    }

    public void encounter(String key, double doubleValue) {
        if (!calculators.containsKey(key)) {
            calculators.put(key, new MeanCalculator());
        }
        calculators.get(key).addValue(doubleValue);
    }
    
    public double mean(String key) {
        return calculators.get(key).mean();
    }
    
    public double stddev(String key) {
        return calculators.get(key).stddev();
    }

    /* Knuth on-line algorithm for standard deviation */
    private class MeanCalculator {
        private int n;
        private double mean;
        private double M2;
        
        public MeanCalculator() {
            n = 0;
            mean = 0.0;
            M2 = 0.0;
        }
        
        public void addValue(double x) {
            n = n + 1;
            double delta = x - mean;
            mean = mean + delta/n;
            M2 = M2 + delta*(x - mean);            
        }
        
        public double mean() {
            return mean;
        }
        
        public double stddev() {
            if (n > 1) {
                return Math.sqrt(M2/(n));
            }
            else {
                return 0.0;
            }
        }
    }
}
