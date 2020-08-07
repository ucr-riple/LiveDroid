package edu.ucr.cs.ufarooq.liveDroid.staticResults;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import edu.ucr.cs.ufarooq.liveDroid.Utils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

public class ApplicationAnalysisResult {
    private final String packageName;
    private final String version;
    private Set<ActivityAnalysisResult> activityAnalysisResultSet;
    private final PsiManager psiManager;

    public ApplicationAnalysisResult(String packageName, String version, PsiManager psiManager) {
        this.packageName = packageName;
        this.version = version;
        this.psiManager = psiManager;
        this.activityAnalysisResultSet = new HashSet<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public Set<ActivityAnalysisResult> getActivityAnalysisResultSet() {
        return activityAnalysisResultSet;
    }

    public void parseAndSetActivities(NodeList activitiesElement) {
        //todo: parse and assign activities objects here
        for (int i = 0; i < activitiesElement.getLength(); i++) {
            Node activityNode = activitiesElement.item(i);
            if (activityNode.getNodeType() == Node.ELEMENT_NODE) {

                Element activityElement = (Element) activityNode;

                Node criticalData = activityElement.getElementsByTagName("CriticalData").item(0);
                Set<AccessPath> criticalAccessPaths = parseAccessPaths(criticalData);

                Node mayUse = activityElement.getElementsByTagName("MayUse").item(0);
                Set<AccessPath> mayUseAccessPaths = parseAccessPaths(mayUse);

                Node mayModify = activityElement.getElementsByTagName("MayModify").item(0);
                Set<AccessPath> mayModifyAccessPaths = parseAccessPaths(mayModify);

                //Node mayAllocate = activityElement.getElementsByTagName("MayAllocate").item(0);
                //Set<AccessPath> mayAllocateAccessPaths = parseAccessPaths(mayAllocate);

                Node mayPointsTo = activityElement.getElementsByTagName("PointsTo").item(0);
                Map<AccessPath, Set<AccessPath>> mayPointsToMap = parseMayPointsTo(mayPointsTo);

                Node viewsNode = activityElement.getElementsByTagName("Views").item(0);
                Set<ViewResult> viewControls = parseViewControls(viewsNode);

                String className = activityElement.getAttribute("name");
                PsiClass activityClass = Utils.findClass(this.psiManager, className);

                ActivityAnalysisResult analysisResult = new ActivityAnalysisResult(activityClass, viewControls,criticalAccessPaths, mayUseAccessPaths, mayModifyAccessPaths/*, mayAllocateAccessPaths*/, mayPointsToMap);
                this.activityAnalysisResultSet.add(analysisResult);
            }
        }

    }

    public Map<AccessPath, Set<AccessPath>> parseMayPointsTo(Node pointsTo) {
        NodeList accessPaths = pointsTo.getChildNodes();
        Map<AccessPath, Set<AccessPath>> map = new HashMap<>();
        List<AccessPath> accessPathList = new ArrayList<>(accessPaths.getLength());
        for (int i = 0; i < accessPaths.getLength(); i++) {
            Node apNode = accessPaths.item(i);
            if (apNode.getNodeType() == Node.ELEMENT_NODE) {
                Element apElement = (Element) apNode;
                int apSize = Integer.parseInt(apElement.getAttribute("length"));
                NodeList fieldNodeList = apElement.getElementsByTagName("PTField");
                AccessPath accessPath = parseAccessPath(fieldNodeList);
                Set<AccessPath> pointsToSet = new HashSet<>();
                NodeList pointsToAPs = apElement.getElementsByTagName("AccessPath");
                for (int j = 0; j < pointsToAPs.getLength(); j++) {
                    Node currentAP = pointsToAPs.item(j);
                    if (currentAP.getNodeType() == Node.ELEMENT_NODE) {
                        Element currentAPElement = (Element) currentAP;
                        NodeList currentAPElementFieldList = currentAPElement.getElementsByTagName("Field");
                        AccessPath currentAccessPath = parseAccessPath(currentAPElementFieldList);
                        pointsToSet.add(currentAccessPath);
                    }
                }
                map.put(accessPath, pointsToSet);
            }
        }
        return map;
    }

    public Set<AccessPath> parseAccessPaths(Node node) {
        NodeList accessPaths = node.getChildNodes();
        Set<AccessPath> accessPathList = new HashSet<>();
        for (int i = 0; i < accessPaths.getLength(); i++) {
            Node apNode = accessPaths.item(i);
            if (apNode.getNodeType() == Node.ELEMENT_NODE) {
                Element apElement = (Element) apNode;
                int apSize = Integer.parseInt(apElement.getAttribute("length"));
                NodeList fieldNodeList = apElement.getElementsByTagName("Field");
                AccessPath accessPath = parseAccessPath(fieldNodeList);
                accessPathList.add(accessPath);
            }
        }
        return accessPathList;
    }

    public AccessPath parseAccessPath(NodeList nodeList) {
        List<PsiField> fields = new ArrayList<>(nodeList.getLength());
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
                PsiClass declaringPsiClass = Utils.findClass(this.psiManager, declaringClass);
                if (declaringClass != null && !stopAP) {
                    try {
                        PsiField psiField = declaringPsiClass.findFieldByName(fieldName, true);
                        //todo handle field declared in super class
                        // assert psiField == null;
                        fields.add(index, psiField);
                    }catch (Exception e){
                        stopAP = true;
                    }
                } else {
                    stopAP = true;
                    break;
                }
            }
        }
        return new AccessPath(fields);
    }

    public Set<ViewResult> parseViewControls(Node node) {
        Set<ViewResult> viewResults = new HashSet<>();
        NodeList viewNodes = node.getChildNodes();
        for (int i = 0; i < viewNodes.getLength(); i++) {
            Node viewNode = viewNodes.item(i);
            if (viewNode.getNodeType() == Node.ELEMENT_NODE) {
                Element viewElement = (Element) viewNode;
                int viewId = Integer.parseInt(viewElement.getAttribute("id"));
                String name = viewElement.getAttribute("name");
                String type = viewElement.getAttribute("type");
                ViewResult viewResult = new ViewResult(viewId, name, type);
                NodeList propertyNodeList = viewElement.getElementsByTagName("Property");
                Set<ViewPropertyResult> properties = parseViewProperties(propertyNodeList);
                viewResult.setProperties(properties);
                viewResults.add(viewResult);
            }
        }
        return viewResults;

    }

    public Set<ViewPropertyResult> parseViewProperties(NodeList nodeList) {
        Set<ViewPropertyResult> propertyResults = new HashSet<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node propertyNode = nodeList.item(i);
            if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                Element propertyElement = (Element) propertyNode;
                String name = propertyElement.getAttribute("name");
                String getter = propertyElement.getAttribute("getter");
                String setter = propertyElement.getAttribute("setter");
                boolean toString = Boolean.parseBoolean(propertyElement.getAttribute("toString"));
                ViewPropertyResult viewPropertyResult = new ViewPropertyResult(name, setter, getter, toString);
                propertyResults.add(viewPropertyResult);
            }
        }
        return propertyResults;
    }
}