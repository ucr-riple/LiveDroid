package edu.ucr.cs.ufarooq.android.results;

import edu.ucr.cs.ufarooq.accessPath.Field;
import edu.ucr.cs.ufarooq.android.connection.AbsConnection;
import edu.ucr.cs.ufarooq.android.connection.ServiceConnection;
import edu.ucr.cs.ufarooq.android.layout.ViewResult;
import edu.ucr.cs.ufarooq.android.layout.controls.GetterMethod;
import edu.ucr.cs.ufarooq.android.layout.controls.SetterMethod;
import edu.ucr.cs.ufarooq.android.model.FieldOnlyAccessPath;
import edu.ucr.cs.ufarooq.android.problems.Utils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import soot.SootField;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class XMLFileHelper {

    public static Element getAppElement(Document doc, String name, String version, long timeSpent) {
        Element item = doc.createElement("application");
        Attr attrType = doc.createAttribute("packageName");
        attrType.setValue(name);
        item.setAttributeNode(attrType);

        Attr attrVersion = doc.createAttribute("version");
        attrVersion.setValue(version);
        item.setAttributeNode(attrVersion);

        Attr attrTime = doc.createAttribute("runtime");
        attrTime.setValue(timeSpent + "");
        item.setAttributeNode(attrTime);
        return item;
    }

    public static Element getItemWithName(Document doc, String title, String value) {
        Element item = doc.createElement(title);
        Attr attrType = doc.createAttribute("name");
        attrType.setValue(value);
        item.setAttributeNode(attrType);
        return item;
    }

    public static Element getItemWithValue(Document doc, String title, int value) {
        Element item = doc.createElement(title);
        Attr attrType = doc.createAttribute("value");
        attrType.setValue(value + "");
        item.setAttributeNode(attrType);
        return item;
    }

    public static Element getMayPointsToXML(Document doc, Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> map) {
        Element item = doc.createElement("PointsTo");
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> pair = (Map.Entry) it.next();

            FieldOnlyAccessPath key = pair.getKey();
            Set<FieldOnlyAccessPath> value = pair.getValue();
            String ptFieldTg = "PTField";
            Element keyXML = getAccessPathXML(doc, key, ptFieldTg);
            for (FieldOnlyAccessPath accessPath : value) {
                String fieldTg = "Field";
                Element accessPathXML = getAccessPathXML(doc, accessPath, fieldTg);
                keyXML.appendChild(accessPathXML);
            }

            item.appendChild(keyXML);
        }
        return item;
    }

    public static Element getCompleteSetXMLValue(Document doc, String title, Set<FieldOnlyAccessPath> set) {
        Element item = doc.createElement(title);

        for (FieldOnlyAccessPath accessPathAnalysisObject : set) {
            String fieldTg = "Field";
            Element apElement = getAccessPathXML(doc, accessPathAnalysisObject, fieldTg);
            item.appendChild(apElement);
        }
        return item;
    }

    public static Element getAccessPathXML(Document doc, FieldOnlyAccessPath accessPath, String fieldTag) {
        Element apElement = doc.createElement("AccessPath");

        Attr attrLength = doc.createAttribute("length");
        attrLength.setValue(accessPath.getFields().size() + "");
        apElement.setAttributeNode(attrLength);
        //encode AccessPath in XML
        int index = 0;
        for (Field field : accessPath.getFields()) {
            SootField sootField = field.getSootField();

            Element fieldElement = doc.createElement(fieldTag);
            Attr attrType = doc.createAttribute("type");
            attrType.setValue(sootField.getType().toQuotedString());
            fieldElement.setAttributeNode(attrType);

            Attr attrPrimitive = doc.createAttribute("primitive");
            attrPrimitive.setValue(Utils.isPrimitiveType(sootField.getType()) + "");
            fieldElement.setAttributeNode(attrPrimitive);

            Attr attrIndex = doc.createAttribute("index");
            attrIndex.setValue(index + "");
            fieldElement.setAttributeNode(attrIndex);

            Attr attrName = doc.createAttribute("name");
            attrName.setValue(sootField.getName());
            fieldElement.setAttributeNode(attrName);

            Attr attrClass = doc.createAttribute("class");
            attrClass.setValue(sootField.getDeclaringClass().getName());
            fieldElement.setAttributeNode(attrClass);

            apElement.appendChild(fieldElement);
            index++;
        }
        return apElement;
    }

    public static Element getViewsElement(Document doc, Set<ViewResult> uiResults) {
        Element views = doc.createElement("Views");
        for (ViewResult viewResult : uiResults) {
            Element view = doc.createElement("View");
            Attr attrType = doc.createAttribute("type");
            attrType.setValue(viewResult.getControl().getViewClass().toString());
            view.setAttributeNode(attrType);

            Attr attrID = doc.createAttribute("id");
            attrID.setValue(viewResult.getControl().getID() + "");
            view.setAttributeNode(attrID);

            Attr attrName = doc.createAttribute("name");
            attrName.setValue(viewResult.getIdVariableName());
            view.setAttributeNode(attrName);

            for (ImmutablePair<SetterMethod, GetterMethod> property : viewResult.getProperties()) {
                SetterMethod setterMethod = property.left;
                GetterMethod getterMethod = property.right;

                Element propertyElement = doc.createElement("Property");

                Attr attrPropertyName = doc.createAttribute("name");
                attrPropertyName.setValue(setterMethod.getProperty() + "");
                propertyElement.setAttributeNode(attrPropertyName);

                Attr attrSetter = doc.createAttribute("setter");
                attrSetter.setValue(setterMethod.getSootMethod().getSubSignature() + "");
                propertyElement.setAttributeNode(attrSetter);

                Attr attrGetter = doc.createAttribute("getter");
                attrGetter.setValue(getterMethod.getSootMethod().getSubSignature() + "");
                propertyElement.setAttributeNode(attrGetter);

                Attr attrToString = doc.createAttribute("toString");
                attrToString.setValue(getterMethod.isToStringRequired() + "");
                propertyElement.setAttributeNode(attrToString);

                view.appendChild(propertyElement);
            }
            views.appendChild(view);
        }
        return views;
    }

    public static Element getConnectionElement(Document doc, Set<AbsConnection> connections) {
        Element connectionsElement = doc.createElement("Connections");
        for (AbsConnection connection : connections) {
            Element connectionElement = doc.createElement("Connection");
            Attr attrType = doc.createAttribute("type");
            attrType.setValue(connection.getType());
            connectionElement.setAttributeNode(attrType);

            Attr attrFieldType = doc.createAttribute("fieldType");
            attrFieldType.setValue(connection.getConnectionType().toQuotedString());
            connectionElement.setAttributeNode(attrFieldType);

            Attr attrOpenCallbackSignature = doc.createAttribute("openCallbackSignature");
            attrOpenCallbackSignature.setValue(connection.getOpeningCallback().getSubSignature());
            connectionElement.setAttributeNode(attrOpenCallbackSignature);

            Attr attrOpenCallback = doc.createAttribute("openCallback");
            attrOpenCallback.setValue(connection.getOpeningCallback().getName());
            connectionElement.setAttributeNode(attrOpenCallback);

            Attr attrCloseCallbackSignature = doc.createAttribute("closeCallbackSignature");
            attrCloseCallbackSignature.setValue(connection.getClosingCallback().getSubSignature());
            connectionElement.setAttributeNode(attrCloseCallbackSignature);

            Attr attrCloseCallback = doc.createAttribute("closeCallback");
            attrCloseCallback.setValue(connection.getClosingCallback().getName());
            connectionElement.setAttributeNode(attrCloseCallback);

            connectionsElement.appendChild(connectionElement);
        }
        return connectionsElement;
    }
}
