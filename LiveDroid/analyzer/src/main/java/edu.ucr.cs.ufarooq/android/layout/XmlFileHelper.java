package edu.ucr.cs.ufarooq.android.layout;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class XmlFileHelper {

    private static final String TAG_USER_EDITABLE = "UserEditable";
    private static final String TAG_API_EDITABLE = "APIEditable";
    private static final String TAG_VIEW = "view";
    private static final String TAG_ATTRIBUTE = "attribute";
    private static final String TAG_SETTERS = "setters";
    private static final String TAG_SETTER = "setter";
    private static final String TAG_GETTER = "getter";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_SIGNATURE = "signature";
    private static final String PROPERTY_PREFERRED = "preferred";
    private static final String PROPERTY_TO_STRING_REQUIRED = "toStringRequired";


    public static void main(String argv[]) {

        try {
            v().parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static XmlFileHelper instance;

    private XmlFileHelper() {

    }

    public static XmlFileHelper v() {
        if (instance == null)
            instance = new XmlFileHelper();
        return instance;
    }

    public void parse() throws IOException, SAXException, ParserConfigurationException {
        parseXMLFile(new File("AndroidViews.xml"));
    }

    private void parseXMLFile(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
        Node userEditables = doc.getElementsByTagName(TAG_USER_EDITABLE).item(0);
        Node apiEditables = doc.getElementsByTagName(TAG_API_EDITABLE).item(0);
        parseViews(userEditables);
        parseViews(apiEditables);

    }

    private void parseViews(Node node) {
        NodeList viewNodes = node.getChildNodes();
        for (int i = 0; i < viewNodes.getLength(); i++) {
            Node currentView = viewNodes.item(i);
            if (currentView.getNodeType() == Node.ELEMENT_NODE) {
                Element viewElement = (Element) currentView;
                String className = viewElement.getAttribute(PROPERTY_NAME);
                System.out.println("View Name: " + className);
                NodeList attributes = viewElement.getElementsByTagName(TAG_ATTRIBUTE);
                parseAttributes(attributes);

            }
        }
    }

    private void parseAttributes(NodeList attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (attribute.getNodeType() == Node.ELEMENT_NODE) {
                Element attributeElement = (Element) attribute;
                String attributeName = attributeElement.getAttribute(PROPERTY_NAME);
                System.out.println("Attribute: " + attributeName);
                Node settersNode = attributeElement.getElementsByTagName(TAG_SETTERS).item(0);
                NodeList setterList = settersNode.getChildNodes();
                parseSetters(setterList);
                Node getterNode = attributeElement.getElementsByTagName(TAG_GETTER).item(0);
                parseGetter(getterNode);

            }
        }
    }

    private void parseSetters(NodeList setters) {
        for (int i = 0; i < setters.getLength(); i++) {
            Node setter = setters.item(i);
            if (setter.getNodeType() == Node.ELEMENT_NODE) {
                Element setterElement = (Element) setter;
                String methodSignature = setterElement.getAttribute(PROPERTY_SIGNATURE);
                String preferred = setterElement.getAttribute(PROPERTY_PREFERRED);
                System.out.println(methodSignature + "\t" + preferred);
            }
        }
    }

    private void parseGetter(Node getter) {
        if (getter.getNodeType() == Node.ELEMENT_NODE) {
            Element getterElement = (Element) getter;
            String toStringRequired = getterElement.getAttribute(PROPERTY_TO_STRING_REQUIRED);
            String methodSignature = getterElement.getAttribute(PROPERTY_SIGNATURE);
            System.out.println(methodSignature + "\t" + toStringRequired);
        }
    }
}