package edu.ucr.cs.ufarooq.android.layout.controls;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ViewControlsAPIProviders {
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

    private Map<SootClass, ViewControl> apiEditablemap;
    private Map<SootClass, ViewControl> userEditablemap;
    private static ViewControlsAPIProviders instance;

    private ViewControlsAPIProviders() throws ParserConfigurationException, SAXException, IOException {
        this.apiEditablemap = new HashMap<>();
        this.userEditablemap = new HashMap<>();
        parse();
    }

    public boolean isUserEditableView(SootClass view) {
        if (this.userEditablemap.containsKey(view)) {
            return true;
        }
        return false;
    }

    public Set<ImmutablePair<SetterMethod, GetterMethod>> getAllUserEditableProperties(SootClass view) {
        if (isUserEditableView(view)) {
            ViewControl viewControl = this.userEditablemap.get(view);
            Set<SetterMethod> preferredSetters = viewControl.getPreferredSetterMethods();
            Set<ImmutablePair<SetterMethod, GetterMethod>> results = new HashSet<>();
            for (SetterMethod setterMethod : preferredSetters) {
                GetterMethod getterMethod = viewControl.findGetterForAPI(setterMethod.getSootMethod());
                ImmutablePair<SetterMethod, GetterMethod> pair = new ImmutablePair<>(setterMethod, getterMethod);
                results.add(pair);
            }
            return results;
        }
        return new HashSet<>();
    }

    public boolean isAPIEditableView(SootClass view) {
        if (this.apiEditablemap.containsKey(view)) {
            return true;
        }
        return false;
    }

    public ImmutablePair<SetterMethod, GetterMethod> getAPIEditableProperty(SootClass view, SootMethod sootMethod, Unit callSite) {
        if (isAPIEditableView(view)) {
            ViewControl viewControl = this.apiEditablemap.get(view);
            ImmutablePair<SetterMethod, GetterMethod> pair = viewControl.findPairForAPI(sootMethod, callSite);
            if (pair != null)
                return pair;
        }
        return null;
    }


    public Set<SootMethod> getAllAPIEditables() {
        Set<SootMethod> set = new HashSet<>();
        this.apiEditablemap.forEach((k, v) -> {
            set.addAll(v.getAllSetAPIs());
        });
        return set;
    }

    public static ViewControlsAPIProviders v() throws IOException, SAXException, ParserConfigurationException {
        if (instance == null) {
            instance = new ViewControlsAPIProviders();
        }
        return instance;
    }

    private void parse() throws IOException, SAXException, ParserConfigurationException {
        parseXMLFile(new File("AndroidViews.xml"));
    }

    private void parseXMLFile(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        //System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
        Node userEditables = doc.getElementsByTagName(TAG_USER_EDITABLE).item(0);
        Node apiEditables = doc.getElementsByTagName(TAG_API_EDITABLE).item(0);
        parseViews(userEditables, this.userEditablemap);
        parseViews(apiEditables, this.apiEditablemap);

    }

    private void parseViews(Node node, Map<SootClass, ViewControl> map) {
        NodeList viewNodes = node.getChildNodes();
        for (int i = 0; i < viewNodes.getLength(); i++) {
            Node currentView = viewNodes.item(i);
            if (currentView.getNodeType() == Node.ELEMENT_NODE) {
                Element viewElement = (Element) currentView;
                String className = viewElement.getAttribute(PROPERTY_NAME);
                SootClass sootClass = Scene.v().getSootClassUnsafe(className);
                ViewControl viewControl = new ViewControl(sootClass);
                //System.out.println("View Name: " + className);

                NodeList attributes = viewElement.getElementsByTagName(TAG_ATTRIBUTE);
                for (int j = 0; j < attributes.getLength(); j++) {
                    Node attribute = attributes.item(j);
                    if (attribute.getNodeType() == Node.ELEMENT_NODE) {
                        Element attributeElement = (Element) attribute;
                        String attributeName = attributeElement.getAttribute(PROPERTY_NAME);
                        //System.out.println("Attribute: " + attributeName);
                        Node settersNode = attributeElement.getElementsByTagName(TAG_SETTERS).item(0);
                        NodeList setterList = settersNode.getChildNodes();
                        Set<SetterMethod> setters = parseSetters(setterList, sootClass, attributeName);
                        viewControl.addSetter(setters);
                        Node getterNode = attributeElement.getElementsByTagName(TAG_GETTER).item(0);
                        GetterMethod getter = parseGetter(getterNode, sootClass, attributeName);
                        viewControl.addGetter(getter);
                    }
                }
//                if (map.containsKey(viewControl.getViewClass())) {// todo: if we allow to split across multiple entries for class
//                    //add new found Property then,
//                    ViewControl oldView = map.get(sootClass);
//                    oldView.addGetter(viewControl.);
//
//                } else {
                map.put(sootClass, viewControl);
//                }
            }
        }
    }

    private Set<SetterMethod> parseSetters(NodeList setters, SootClass sootClass, String attributeName) {
        Set<SetterMethod> setterMethodList = new HashSet<>();
        for (int i = 0; i < setters.getLength(); i++) {
            Node setter = setters.item(i);
            if (setter.getNodeType() == Node.ELEMENT_NODE) {
                Element setterElement = (Element) setter;
                String methodSignature = setterElement.getAttribute(PROPERTY_SIGNATURE);
                String preferred = setterElement.getAttribute(PROPERTY_PREFERRED);
                boolean isPreferred = Boolean.parseBoolean(preferred);
                System.out.println(methodSignature + ":" + sootClass.getName());
                SootMethod sootMethod = searchForMethodByName(sootClass, methodSignature);//sootClass.getMethod(methodSignature);
                if(sootMethod!=null) {
                    SetterMethod setterMethod = new SetterMethod(sootMethod, isPreferred, attributeName);
                    //System.out.println(methodSignature + "\t" + preferred);
                    setterMethodList.add(setterMethod);
                }
            }
        }
        return setterMethodList;
    }

    private GetterMethod parseGetter(Node getter, SootClass sootClass, String attributeName) {
        if (getter.getNodeType() == Node.ELEMENT_NODE) {
            Element getterElement = (Element) getter;
            String toStringRequired = getterElement.getAttribute(PROPERTY_TO_STRING_REQUIRED);
            boolean isToStringRequired = Boolean.parseBoolean(toStringRequired);
            String methodSignature = getterElement.getAttribute(PROPERTY_SIGNATURE);
            SootMethod sootMethod = searchForMethodByName(sootClass, methodSignature);//sootClass.getMethod(methodSignature);
            GetterMethod getterMethod = new GetterMethod(sootMethod, isToStringRequired, attributeName);
            //System.out.println(methodSignature + "\t" + toStringRequired);
            return getterMethod;
        }
        return null;
    }

    public static SootMethod searchForMethodByName(SootClass theClass,
                                                   String signature) {
        while (theClass != null) {
            if (theClass.declaresMethod(signature)) {
                //System.out.println("found method " + signature + " in " + theClass);
                try {
                    return theClass.getMethod(signature);
                } catch (RuntimeException ex) {
                    // Print out what the possible methods are.
                    StringBuffer results = new StringBuffer("Methods are: ");
                    boolean commaNeeded = false;
                    Iterator methods = theClass.getMethods().iterator();
                    while (methods.hasNext()) {
                        SootMethod method = (SootMethod) methods.next();
                        if (commaNeeded) {
                            // Add a comma after the first method
                            results.append(", ");
                        } else {
                            commaNeeded = true;
                        }
                        results.append(method.toString());
                    }
                    throw new RuntimeException("Failed to search \"" + theClass
                            + "\" for \"" + signature + "\" possible " + "methods: "
                            + results.toString(), ex);
                }
            }

            theClass = theClass.getSuperclass();
            if (theClass != null)
                theClass.setLibraryClass();
            else
                return null;
        }

        throw new RuntimeException("Method " + signature + " not found in class "
                + theClass);
    }
}
