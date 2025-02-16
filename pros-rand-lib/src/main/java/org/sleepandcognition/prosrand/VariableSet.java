package org.sleepandcognition.prosrand;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* Subjects have one or more variables which will be used as baseline characteristics
 * that we are concerned about balancing among groups.
 * Each characteristic, each variable that we are concerned about, has a name (a string with no white space).
 * Variables are either numeric or categorical ("factors" with levels, in R terminology).
 * For the purpose of calculating feature vectors, all features must be numeric; for categorical variables, this
 * is handled with one-hot encoding.
 */

public class VariableSet {
    List<VariableSpec> specs;
    Hashtable<String, VariableSetterGetter> variables;
    Hashtable<String, String> variableFromDimension;
    boolean multiDimensional;

    public VariableSet(List<VariableSpec> specs) {
        this.specs = specs;
        variables = new Hashtable<String, VariableSetterGetter>();
        variableFromDimension = new Hashtable<String, String>();
        for (Iterator<VariableSpec> it = specs.iterator(); it.hasNext(); ) {
            VariableSpec spec = it.next();
            String name = spec.getName();
            VariableSetterGetter getter;
            if (spec.getType().equals("continuous")) {
                getter = new ContinuousVariableSetterGetter(name);
                variableFromDimension.put(name, name);
            }
            else {
                CategoricalVariableSetterGetter catGetter = new CategoricalVariableSetterGetter(name);
                for (Iterator<String> opts = spec.getLevels().iterator(); opts.hasNext(); ) {
                    String optionName = opts.next();
                    catGetter.addOption(optionName);
                    variableFromDimension.put(catGetter.optionKey(optionName), name);
                }
                getter = catGetter;
            }
            variables.put(name, getter);
        }
        if (specs.size() > 1) {
            multiDimensional = true;
        }
    }

    public Hashtable<String, Double> valuesFromKeyValuePair(String key, String value) throws InvalidDataException {
        VariableSetterGetter getter;
        if (key == null) {
            if (!isMultiDimensional()) {
                getter = variables.values().iterator().next();
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            getter = variables.get(key);
        }
        if (getter == null) {
            throw new InvalidDataException("variable with this key not found");
        }
        return getter.valuesFromKeyValuePair(value);
    }

    public List<VariableSpec> getVariableSpecs() {
        return specs;
    }

    public String keyValueEncodingFromValues(Hashtable<String, Double> values) {
        String result = "";
        for (Enumeration<String> e = variables.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            VariableSetterGetter getter = variables.get(key);
            result = result + getter.keyValuePairFromValues(values) + "\t";
        }
        return result;
    }

    public boolean hasAllVariablesSet(Hashtable<String, Double> values) {
        for (Enumeration<String> e = variables.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            VariableSetterGetter getter = variables.get(key);
            if (!getter.hasValues(values)) {
                System.out.println("Data missing for: " + key);
                return false;
            }
        }
        return true;
    }

    boolean isMultiDimensional() {
        return multiDimensional;
    }

    private abstract class VariableSetterGetter {
        String key;

        public VariableSetterGetter(String name) {
            key = name;
        }

        public double weight() {
            return 1.0;
        }

        public abstract String getTypeName();

        Hashtable<String, Double> valuesFromKeyValuePair(String value) {
            Hashtable<String, Double> val = new Hashtable<String, Double>();
            val.put(key, Double.parseDouble(value));
            return val;
        }

        public boolean hasValues(Hashtable<String, Double> values) {
            return (values.containsKey(key));
        }

        public Double valueFromValues(Hashtable<String, Double> values) {
            Double value = values.get(key);
            if (value == null) {
                // should never happen
                throw new RuntimeException("How could we not have a value?");
            }
            return value;
        }

        public String strValueFromValues(Hashtable<String, Double> values) {
            return String.format("%f", valueFromValues(values).doubleValue());
        }

        public String keyValuePairFromValues(Hashtable<String, Double> values) {
            return String.format("%s=%s", key, strValueFromValues(values));
        }
    }

    private class ContinuousVariableSetterGetter extends VariableSetterGetter {
        public ContinuousVariableSetterGetter(String name) {
            super(name);
        }

        public String getTypeName() {
            return "continuous";
        }
    }

    private class CategoricalVariableSetterGetter extends VariableSetterGetter {
        ArrayList<String> options;

        public CategoricalVariableSetterGetter(String name) {
            super(name);
            options = new ArrayList<String>();
        }

        public String getTypeName() {
            return "categorical";
        }

        public double weight() {
            return 1.0 / options.size();
        }

        void addOption(String name) {
            options.add(name);
        }

        Hashtable<String, Double> valuesFromKeyValuePair(String value) {
            Hashtable<String, Double> val = new Hashtable<String, Double>();
            for (Iterator<String> it = options.iterator(); it.hasNext(); ) {
                String option = it.next();
                Double optionValue = new Double(0);
                if (value.equals(option.toString())) {
                    optionValue = new Double(1);
                }
                val.put(optionKey(option), optionValue);
            }
            return val;
        }

        public boolean hasValues(Hashtable<String, Double> values) {
            for (Iterator<String> it = options.iterator(); it.hasNext(); ) {
                String option = it.next();
                if (!values.containsKey(optionKey(option))) {
                    return false;
                }
            }
            return true;
        }

        public String optionKey(String option) {
            return String.format("%s_is%s", key, option);
        }

        public String strValueFromValues(Hashtable<String, Double> values) {
            for (Iterator<String> it = options.iterator(); it.hasNext(); ) {
                String option = it.next();
                if (values.get(optionKey(option)).doubleValue() > 0) {
                    return option;
                }
            }
            // One should be valued, so we should never get here:
            throw new RuntimeException("How could we not have a value? (categorical)");
        }
    } // END class CategoricalVariableSetterGetter

    public boolean matchesSpec(List<VariableSpec> specs) {
        for (Iterator<VariableSpec> it = specs.iterator(); it.hasNext(); ) {
            VariableSpec each = it.next();
            String name = each.getName();
            if (!variables.containsKey(name)) {
                return false;
            }
            if (each.getType().equals("continuous")) {
                if (!variables.get(name).getTypeName().equals("continuous")) {
                    return false;
                }
            } else {
                if (!variables.get(name).getTypeName().equals("categorical")) {
                    return false;
                }
                List<String> options = each.getLevels();
                for (Iterator<String> opts = options.iterator(); opts.hasNext(); ) {
                    String anOption = opts.next();
                    if (!((CategoricalVariableSetterGetter) (variables.get(name))).options.contains(anOption)) {
                        System.out.println("Option not found");
                        return false;
                    }
                }
            }
        }
        if (specs.size() != variables.size()) {
            System.out.println(specs.size());
            return false;
        }
        return true;
    }

    public Map<String, String> stringsFromValues(Hashtable<String, Double> baselineCharacteristics) {
        HashMap<String, String> map = new HashMap<>();
        for (Enumeration<String> e = variables.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            VariableSetterGetter getter = variables.get(key);
            String val = getter.strValueFromValues(baselineCharacteristics);
            map.put(key, val);
        }
        return map;
    }

    public double weightForKey(String key) {
        return variables.get(variableFromDimension.get(key)).weight();
    }
}
