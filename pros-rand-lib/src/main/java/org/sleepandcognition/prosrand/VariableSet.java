package org.sleepandcognition.prosrand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/* Subjects have one or more variables which will be used as baseline characteristics
 * that we are concerned about balancing among groups.
 * Each characteristic, each variable that we are concerned about, has a name (a string with no white space).
 * The value of each variable must be converted into a number.
 * Variables come in several types:
 * * Continuous numeric variables (such as weight)
 * * Discrete numeric variables (such as number of years of education completed)
 * * Ordinal variables. These are coded as integers. However, this may be problematic.
 * * Catagorical variables. These each have a list of possible (string) values. For example, "sex" may have the
 * possible values 'male', 'female', and 'unknown'. These are handled by converting into an array of variables, one for
 * each category, i.e. 'is_male_sex', 'is_female_sex', and 'is_unknown_sex', each of which will have the value 0 or 1.
 */

public class VariableSet {
    Hashtable<String, VariableSetterGetter> variables;
    boolean multiDimensional;

    public VariableSet(String fileNameOrPath) throws SAXException, IOException, ParserConfigurationException {
        multiDimensional = false;
        variables = new Hashtable<String, VariableSetterGetter>();
        File xmlFileName = new File(fileNameOrPath);
        Document document =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFileName);
        NodeList documentNodes = document.getChildNodes();
        NodeList children = documentNodes.item(0).getChildNodes();
        boolean first = true;
        for (int i = 0; i < children.getLength(); ++i) {
            org.w3c.dom.Node node = children.item(i);
            if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                org.w3c.dom.Node attrNode;
                attrNode = attrs.getNamedItem("name");
                String name = attrNode.getNodeValue();
                attrNode = attrs.getNamedItem("type");
                String type = attrNode.getNodeValue();
                VariableSetterGetter getter = null;
                if (type.equals("continuous")) {
                    getter = new ContinuousVariableSetterGetter(name);
                } else if (type.equals("catagorical")) {
                    getter = new CatagoricalVariableSetterGetter(name);
                }
                getter.readOptionsFromXML(node);
                variables.put(name, getter);
                if (!first) {
                    multiDimensional = true;
                }
                first = false;
            }
        }
    }

    public VariableSet(List<String> variableSpec) {
        variables = new Hashtable<String, VariableSetterGetter>();
        for (Iterator<String> it = variableSpec.iterator(); it.hasNext(); ) {
            String name = it.next();
            VariableSetterGetter getter = new ContinuousVariableSetterGetter(name);
            variables.put(name, getter);
        }
        if (variableSpec.size() > 1) {
            multiDimensional = true;
        }
    }

    public Hashtable<String, Double> valuesFromKeyValuePair(String key, String value) {
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
        return getter.valuesFromKeyValuePair(value);
    }

    public Hashtable<String, String> getVariables() {
        Hashtable<String, String> vars = new Hashtable<String, String>();
        for (Enumeration<String> e = variables.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            VariableSetterGetter getter = variables.get(key);
            vars.put(key, getter.getTypeName());
        }
        return vars;
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

    public String randomValuesString(Random r) {
        String result = "";
        for (Enumeration<String> e = variables.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            VariableSetterGetter getter = variables.get(key);
            result = result + getter.randomValueString(r) + " ";
        }
        return result;
    }

    boolean isMultiDimensional() {
        return multiDimensional;
    }

    private abstract class VariableSetterGetter {
        String key;

        public VariableSetterGetter(String name) {
            key = name;
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

        public void readOptionsFromXML(Node node) {}

        public String keyValuePairFromValues(Hashtable<String, Double> values) {
            Double value = values.get(key);
            if (value == null) {
                return "";
            }
            return String.format("%s=%f", key, value.doubleValue());
        }

        /* This is purely for simulation, to test/experiment, rather than real-world use of the algorithm. */
        public String randomValueString(Random random) {
            double val = random.nextGaussian();
            return String.format("%s=%f", key, val);
        }
    }

    private class ContinuousVariableSetterGetter extends VariableSetterGetter {
        double simulationMean;
        double simulationStdDev;

        public ContinuousVariableSetterGetter(String name) {
            super(name);
            simulationMean = 0.0;
            simulationStdDev = 1.0;
        }

        public String getTypeName() {
            return "continuous";
        }

        public void readOptionsFromXML(Node node) {
            org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
            org.w3c.dom.Node attrNode;
            attrNode = attrs.getNamedItem("mean");
            if (attrNode != null) {
                simulationMean = Double.parseDouble(attrNode.getNodeValue());
            }
            attrNode = attrs.getNamedItem("stdev");
            if (attrNode != null) {
                simulationStdDev = Double.parseDouble(attrNode.getNodeValue());
            }
        }

        /* This is purely for simulation, to test/experiment, rather than real-world use of the algorithm. */
        public String randomValueString(Random random) {
            double val = simulationMean + random.nextGaussian() * simulationStdDev;
            return String.format("%s=%f", key, val);
        }
    }

    private class CatagoricalVariableSetterGetter extends VariableSetterGetter {
        ArrayList<CatagoricalVariableOption> options;

        public CatagoricalVariableSetterGetter(String name) {
            super(name);
            options = new ArrayList<CatagoricalVariableOption>();
        }

        public String getTypeName() {
            return "categorical";
        }

        public void readOptionsFromXML(Node node) {
            org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
            org.w3c.dom.Node attrNode;

            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                org.w3c.dom.Node child = children.item(i);
                if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    attrs = child.getAttributes();
                    attrNode = attrs.getNamedItem("name");
                    String name = attrNode.getNodeValue();
                    int weight = 1;
                    attrNode = attrs.getNamedItem("num");
                    if (attrNode != null) {
                        weight = Integer.parseInt(attrNode.getNodeValue());
                    }
                    options.add(new CatagoricalVariableOption(name, weight));
                }
            }
        } // END readOptionsFromXML

        Hashtable<String, Double> valuesFromKeyValuePair(String value) {
            Hashtable<String, Double> val = new Hashtable<String, Double>();
            for (Iterator<CatagoricalVariableOption> it = options.iterator(); it.hasNext(); ) {
                CatagoricalVariableOption option = it.next();
                Double optionValue = new Double(0);
                if (value.equals(option.toString())) {
                    optionValue = new Double(1);
                }
                String optionKey = String.format("%s_is%s", key, option);
                val.put(optionKey, optionValue);
            }
            return val;
        }

        public boolean hasValues(Hashtable<String, Double> values) {
            for (Iterator<CatagoricalVariableOption> it = options.iterator(); it.hasNext(); ) {
                CatagoricalVariableOption option = it.next();
                String optionKey = String.format("%s_is%s", key, option);
                if (!values.containsKey(optionKey)) {
                    return false;
                }
            }
            return true;
        }

        public String keyValuePairFromValues(Hashtable<String, Double> values) {
            for (Iterator<CatagoricalVariableOption> it = options.iterator(); it.hasNext(); ) {
                CatagoricalVariableOption option = it.next();
                String optionKey = String.format("%s_is%s", key, option);
                if (values.get(optionKey).doubleValue() > 0) {
                    return String.format("%s=%s", key, option);
                }
            }
            System.out.println("Inconceivable!");
            System.out.println("No option set for key " + key);
            return "";
        }

        public String randomValueString(Random random) {
            int demonimator = 0;
            for (Iterator<CatagoricalVariableOption> it = options.iterator(); it.hasNext(); ) {
                CatagoricalVariableOption option = it.next();
                demonimator += option.probabilityWeight;
            }
            int r = random.nextInt(demonimator);
            CatagoricalVariableOption chosenOption = null;
            for (Iterator<CatagoricalVariableOption> it = options.iterator(); it.hasNext() && chosenOption == null; ) {
                CatagoricalVariableOption option = it.next();
                for (int j = 0; j < option.probabilityWeight && chosenOption == null; ++j) {
                    if (r == 0) {
                        chosenOption = option;
                        break;
                    }
                    --r;
                }
            }
            return String.format("%s=%s", key, chosenOption);
        }

        private class CatagoricalVariableOption {
            String name;
            int probabilityWeight;

            public CatagoricalVariableOption(String name, int probabilityWeight) {
                this.name = name;
                this.probabilityWeight = probabilityWeight;
            }

            public String toString() {
                return name;
            }
        }
    } // END class CatagoricalVariableSetterGetter
}
