package edu.ucr.cs.ufarooq.android.patch;

import edu.ucr.cs.ufarooq.accessPath.Field;
import edu.ucr.cs.ufarooq.android.connection.AbsConnection;
import edu.ucr.cs.ufarooq.android.layout.ViewResult;
import edu.ucr.cs.ufarooq.android.layout.controls.GetterMethod;
import edu.ucr.cs.ufarooq.android.layout.controls.SetterMethod;
import edu.ucr.cs.ufarooq.android.model.FieldOnlyAccessPath;
import edu.ucr.cs.ufarooq.android.results.ActivityAnalysisResult;
import edu.ucr.cs.ufarooq.android.results.ApplicationAnalysisResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

public class ResultImporter {
  private String xmlPath;
  private static ResultImporter instance;

  private ResultImporter(String xmlFile) {
    xmlPath = xmlFile;
  }

  public static ResultImporter getInstance(String xmlFile) {
    if (instance == null) {
      instance = new ResultImporter(xmlFile);
    }
    // if old instance exists for different, load new One
    if (!instance.xmlPath.equals(xmlFile)) {
      instance = new ResultImporter(xmlFile);
    }
    return instance;
  }

  public ApplicationAnalysisResult parseXMLResults()
      throws ParserConfigurationException, IOException, SAXException {
    File xmlFile = new File(this.xmlPath);
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(xmlFile);
    doc.getDocumentElement().normalize();

    Element appElement = doc.getDocumentElement();
    String packageName = appElement.getAttribute("packageName");
    String version = appElement.getAttribute("version");
    ApplicationAnalysisResult applicationAnalysisResult =
        new ApplicationAnalysisResult(packageName, packageName, version, 0);

    Node activitiesNode = appElement.getElementsByTagName("Activities").item(0);
    NodeList activityNodeList = activitiesNode.getChildNodes();
    applicationAnalysisResult.setActivities(parseAndSetActivities(activityNodeList));
    return applicationAnalysisResult;
  }

  public static Set<ActivityAnalysisResult> parseAndSetActivities(NodeList activitiesElement) {
    // todo: parse and assign activities objects here
    Set<ActivityAnalysisResult> activityAnalysisResultSet = new HashSet<>();
    for (int i = 0; i < activitiesElement.getLength(); i++) {
      Node activityNode = activitiesElement.item(i);
      if (activityNode.getNodeType() == Node.ELEMENT_NODE) {

        Element activityElement = (Element) activityNode;
        Node mayUse = activityElement.getElementsByTagName("MayUse").item(0);
        Set<FieldOnlyAccessPath> mayUseAccessPaths = parseAccessPaths(mayUse);

        Node mayModify = activityElement.getElementsByTagName("MayModify").item(0);
        Set<FieldOnlyAccessPath> mayModifyAccessPaths = parseAccessPaths(mayModify);

        // Node mayAllocate = activityElement.getElementsByTagName("MayAllocate").item(0);
        // Set<AccessPath> mayAllocateAccessPaths = parseAccessPaths(mayAllocate);

        Node mayPointsTo = activityElement.getElementsByTagName("PointsTo").item(0);
        Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> mayPointsToMap =
            parseMayPointsTo(mayPointsTo);

        Node viewsNode = activityElement.getElementsByTagName("Views").item(0);
        Set<ViewResult> viewControls = parseViewControls(viewsNode);

        String className = activityElement.getAttribute("name");
        SootClass activityClass = Scene.v().getSootClassUnsafe(className);
        Set<AbsConnection> connections = new HashSet<>();

        ActivityAnalysisResult analysisResult =
            new ActivityAnalysisResult(
                viewControls,
                mayUseAccessPaths,
                mayModifyAccessPaths,
                mayPointsToMap,
                connections,
                activityClass);
        activityAnalysisResultSet.add(analysisResult);
      }
    }
    return activityAnalysisResultSet;
  }

  public static Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> parseMayPointsTo(Node pointsTo) {
    NodeList accessPaths = pointsTo.getChildNodes();
    Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> map = new HashMap<>();
    List<FieldOnlyAccessPath> accessPathList = new ArrayList<>(accessPaths.getLength());
    for (int i = 0; i < accessPaths.getLength(); i++) {
      Node apNode = accessPaths.item(i);
      if (apNode.getNodeType() == Node.ELEMENT_NODE) {
        Element apElement = (Element) apNode;
        int apSize = Integer.parseInt(apElement.getAttribute("length"));
        NodeList fieldNodeList = apElement.getElementsByTagName("PTField");
        FieldOnlyAccessPath accessPath = parseAccessPath(fieldNodeList);
        Set<FieldOnlyAccessPath> pointsToSet = new HashSet<>();
        NodeList pointsToAPs = apElement.getElementsByTagName("AccessPath");
        for (int j = 0; j < pointsToAPs.getLength(); j++) {
          Node currentAP = pointsToAPs.item(j);
          if (currentAP.getNodeType() == Node.ELEMENT_NODE) {
            Element currentAPElement = (Element) currentAP;
            NodeList currentAPElementFieldList = currentAPElement.getElementsByTagName("Field");
            FieldOnlyAccessPath currentAccessPath = parseAccessPath(currentAPElementFieldList);
            pointsToSet.add(currentAccessPath);
          }
        }
        map.put(accessPath, pointsToSet);
      }
    }
    return map;
  }

  public static Set<FieldOnlyAccessPath> parseAccessPaths(Node node) {
    NodeList accessPaths = node.getChildNodes();
    Set<FieldOnlyAccessPath> accessPathList = new HashSet<>();
    for (int i = 0; i < accessPaths.getLength(); i++) {
      Node apNode = accessPaths.item(i);
      if (apNode.getNodeType() == Node.ELEMENT_NODE) {
        Element apElement = (Element) apNode;
        int apSize = Integer.parseInt(apElement.getAttribute("length"));
        NodeList fieldNodeList = apElement.getElementsByTagName("Field");
        FieldOnlyAccessPath accessPath = parseAccessPath(fieldNodeList);
        accessPathList.add(accessPath);
      }
    }
    return accessPathList;
  }

  public static FieldOnlyAccessPath parseAccessPath(NodeList nodeList) {
    List<Field> fields = new ArrayList<>(nodeList.getLength());
    boolean stopAP = false;
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node fieldNode = nodeList.item(i);
      if (fieldNode.getNodeType() == Node.ELEMENT_NODE) {
        Element fieldElement = (Element) fieldNode;
        String declaringClass = fieldElement.getAttribute("class");
        String fieldName = fieldElement.getAttribute("name");
        boolean isPrimitive = Boolean.parseBoolean(fieldElement.getAttribute("primitive"));
        String type = fieldElement.getAttribute("type");
        int index = Integer.parseInt(fieldElement.getAttribute("index"));
        SootClass declaringPsiClass = Scene.v().getSootClassUnsafe(declaringClass);
        if (declaringClass != null && !stopAP) {
          try {
            SootField psiField = declaringPsiClass.getFieldByNameUnsafe(fieldName);
            Field field = new Field(psiField);
            // todo handle field declared in super class
            // assert psiField == null;
            fields.add(index, field);
          } catch (Exception e) {
            stopAP = true;
          }
        } else {
          stopAP = true;
          break;
        }
      }
    }
    return new FieldOnlyAccessPath(fields);
  }

  public static Set<ViewResult> parseViewControls(Node node) {
    Set<ViewResult> viewResults = new HashSet<>();
    NodeList viewNodes = node.getChildNodes();
    for (int i = 0; i < viewNodes.getLength(); i++) {
      Node viewNode = viewNodes.item(i);
      if (viewNode.getNodeType() == Node.ELEMENT_NODE) {
        Element viewElement = (Element) viewNode;
        int viewId = Integer.parseInt(viewElement.getAttribute("id"));
        String name = viewElement.getAttribute("name");
        String type = viewElement.getAttribute("type");
        SootClass typeView = Scene.v().getSootClassUnsafe(type);
        NodeList propertyNodeList = viewElement.getElementsByTagName("Property");
        Set<ImmutablePair<SetterMethod, GetterMethod>> properties =
            parseViewProperties(propertyNodeList, typeView);

        ViewResult viewResult = new ViewResult(properties, viewId);
        viewResults.add(viewResult);
      }
    }
    return viewResults;
  }

  public static Set<ImmutablePair<SetterMethod, GetterMethod>> parseViewProperties(
      NodeList nodeList, SootClass typeView) {
    Set<ImmutablePair<SetterMethod, GetterMethod>> pairSet = new HashSet<>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node propertyNode = nodeList.item(i);
      if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
        Element propertyElement = (Element) propertyNode;
        String name = propertyElement.getAttribute("name");
        String getter = propertyElement.getAttribute("getter");
        String setter = propertyElement.getAttribute("setter");

        boolean toString = Boolean.parseBoolean(propertyElement.getAttribute("toString"));
        SootMethod setterSootMethod = typeView.getMethodByNameUnsafe(setter);
        SootMethod getterSootMethod = typeView.getMethodByNameUnsafe(getter);
        SetterMethod setterMethod = new SetterMethod(setterSootMethod, false, name);
        GetterMethod getterMethod = new GetterMethod(getterSootMethod, false, name);
        ImmutablePair<SetterMethod, GetterMethod> pair =
            new ImmutablePair<>(setterMethod, getterMethod);
        pairSet.add(pair);
      }
    }
    return pairSet;
  }
}
