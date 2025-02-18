package org.sleepandcognition.prosrand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VariableSpec {
    String name;
    String type;
    List<String> levels; // only relevant for categorical 

    static List<VariableSpec> getSpecsFromXML(String fileNameOrPath) throws SAXException, IOException, ParserConfigurationException {
        List<VariableSpec> specs = new ArrayList<>();
        File xmlFileName = new File(fileNameOrPath);
        Document document =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFileName);
        NodeList documentNodes = document.getChildNodes();
        NodeList children = documentNodes.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            org.w3c.dom.Node node = children.item(i);
            if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                org.w3c.dom.Node attrNode;
                attrNode = attrs.getNamedItem("name");
                String name = attrNode.getNodeValue();
                attrNode = attrs.getNamedItem("type");
                String type = attrNode.getNodeValue();
                ArrayList<String> options = null;
                if (type.equalsIgnoreCase("categorical")) {
                    options = new ArrayList<>();
                    attrs = node.getAttributes();
        
                    NodeList childOptions = node.getChildNodes();
                    for (int j = 0; j < childOptions.getLength(); ++j) {
                        org.w3c.dom.Node child = childOptions.item(j);
                        if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                            attrs = child.getAttributes();
                            attrNode = attrs.getNamedItem("name");
                            String optionName = attrNode.getNodeValue();
                            options.add(optionName);
                        }
                    }
                }
                VariableSpec spec = new VariableSpec();
                spec.setName(name);
                spec.setType(type);
                spec.setLevels(options);
                specs.add(spec);
            }
        }
        return specs;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<String> getLevels() {
        return levels;
    }

    // TODO do these need to be public?
    public void setName(String n) {
        name = n;
    }

    public void setType(String n) {
        if (n.equals("continuous")) {
            type = n;
        }
        else if (n.equals("categorical")) {
            type = n;
        }
        else {
            throw new IllegalArgumentException(n);
        }
    }

    public void setLevels(List<String> options) {
        levels = options;
    }
}
