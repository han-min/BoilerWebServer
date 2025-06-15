package com.mint.boilerws.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtil {

    public static Node getRoot(final String xmlString) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xmlString)));
        return doc.getFirstChild();
    }
    
    public static Node getChildNode(final Node parentNode, final String childNodeName) {
        final NodeList nl = parentNode.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType()!= Node.TEXT_NODE 
                    && n.getNodeName().equals(childNodeName)) {
                return n;
            }
        }
        return null;
    }
}
