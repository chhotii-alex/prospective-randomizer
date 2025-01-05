package org.sleepandcognition.prosrand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/* Subjects have one or more variables which will be used as baseline characteristics
 * that we are concerned about balancing among groups.
 * Each characteristic, each variable that we are concerned about, has a name (a string with no white space).
 * Variables are either numeric or categorical ("factors" with levels, in R terminology).
 * For the purpose of calculating feature vectors, all features must be numeric; for categorical variables, this
 * is handled with one-hot encoding.
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
                if (type.equalsIgnoreCase("continuous")) {
                    getter = new ContinuousVariableSetterGetter(name);
                } else if (type.equalsIgnoreCase("categorical")) {
                    getter = new CategoricalVariableSetterGetter(name);
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

    public VariableSet(HashMap<String, List<String>> variableSpec) {
        variables = new Hashtable<String, VariableSetterGetter>();
        for (Iterator<String> it = variableSpec.keySet().iterator(); it.hasNext(); ) {
            String name = it.next();
            VariableSetterGetter getter;
            List<String> choices = variableSpec.get(name);
            if (choices == null) {
                getter = new ContinuousVariableSetterGetter(name);
            } else {
                getter = new CategoricalVariableSetterGetter(name);
                for (Iterator<String> opts = choices.listIterator(); opts.hasNext(); ) {
                    String optionName = opts.next();
                    ((CategoricalVariableSetterGetter) getter).addOption(optionName);
                }
            }
            variables.put(name, getter);
        }
        if (variableSpec.size() > 1) {
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

        void addOption(String name) {
            options.add(name);
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
                    addOption(name);
                }
            }
        } // END readOptionsFromXML

        Hashtable<String, Double> valuesFromKeyValuePair(String value) {
            Hashtable<String, Double> val = new Hashtable<String, Double>();
            for (Iterator<String> it = options.iterator(); it.hasNext(); ) {
                String option = it.next();
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
            for (Iterator<String> it = options.iterator(); it.hasNext(); ) {
                String option = it.next();
                String optionKey = String.format("%s_is%s", key, option);
                if (!values.containsKey(optionKey)) {
                    return false;
                }
            }
            return true;
        }

        public String keyValuePairFromValues(Hashtable<String, Double> values) {
            for (Iterator<String> it = options.iterator(); it.hasNext(); ) {
                String option = it.next();
                String optionKey = String.format("%s_is%s", key, option);
                if (values.get(optionKey).doubleValue() > 0) {
                    return String.format("%s=%s", key, option);
                }
            }
            System.out.println("Inconceivable!");
            System.out.println("No option set for key " + key);
            return "";
        }
    } // END class CategoricalVariableSetterGetter

    public boolean matchesSpec(HashMap<String, List<String>> variableSpec) {
        for (Iterator<String> each = variableSpec.keySet().iterator(); each.hasNext(); ) {
            String name = each.next();
            if (!variables.containsKey(name)) {
                return false;
            }
            List<String> options = variableSpec.get(name);
            if (options == null) {
                if (!variables.get(name).getTypeName().equals("continuous")) {
                    return false;
                }
            } else {
                if (!variables.get(name).getTypeName().equals("categorical")) {
                    return false;
                }
                for (Iterator<String> it = options.iterator(); it.hasNext(); ) {
                    String anOption = it.next();
                    if (!((CategoricalVariableSetterGetter) (variables.get(name))).options.contains(anOption)) {
                        return false;
                    }
                }
            }
        }
        if (variableSpec.size() != variables.size()) {
            return false;
        }
        return true;
    }
}
