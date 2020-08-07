package edu.ucr.cs.ufarooq.model;

import edu.ucr.cs.ufarooq.util.Utils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import soot.Local;
import soot.SootField;

import java.util.HashMap;
import java.util.Objects;

public class ParameterAnalysisObject extends AnalysisObject {

    private Local local;
    private int parameterIndex;

    public ParameterAnalysisObject() {
        this.sootField = null;
        this.fieldsMap = new HashMap<>();
        this.targetClassField = null;
        this.local = null;
        parameterIndex = -1;
    }

    public ParameterAnalysisObject(Local local, int parameterIndex, SootField sootField) {// Note: Don't let it call without field access
        this.local = local;
        this.parameterIndex = parameterIndex;
        this.sootField = sootField;
        this.fieldsMap = new HashMap<>();
        this.targetClassField = sootField;
    }

    public ParameterAnalysisObject(SootField sootField, HashMap<SootField, SootField> fieldsMap) {
        this.sootField = sootField;
        this.fieldsMap = fieldsMap;
    }

    public ParameterAnalysisObject(Local local, int parameterIndex, AnalysisObject analysisObject) {

        this.local = local;
        this.parameterIndex = parameterIndex;
        this.sootField = analysisObject.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) analysisObject.fieldsMap.clone();
        this.targetClassField = analysisObject.targetClassField;
    }

    public ParameterAnalysisObject(ParameterAnalysisObject classField) {
        this.local = classField.local;
        this.parameterIndex = classField.parameterIndex;
        this.sootField = classField.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) classField.fieldsMap.clone();
        this.targetClassField = classField.targetClassField;
    }

    public ParameterAnalysisObject(ReturnPropAnalysisObject returnPropAnalysisObject) {
        this.local = returnPropAnalysisObject.getParameterlocal();
        this.parameterIndex = returnPropAnalysisObject.getParameterIndex();
        this.sootField = returnPropAnalysisObject.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) returnPropAnalysisObject.fieldsMap.clone();
        this.targetClassField = returnPropAnalysisObject.targetClassField;
    }


    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public void setParameterIndex(int parameterIndex) {
        this.parameterIndex = parameterIndex;
    }

    public static ParameterAnalysisObject appendParamAtStart(ParameterAnalysisObject parameterAnalysisObject, ClassField classField) {
        ParameterAnalysisObject returnObject = new ParameterAnalysisObject(parameterAnalysisObject);
        System.out.println("appendAtStartField:" + classField + ":" + returnObject.toString());
        if (returnObject.sootField != null) {
            returnObject.insert(returnObject.targetClassField, classField.sootField); // connect target of base to root of current
            // put map as
            returnObject.fieldsMap.forEach((k, v) -> returnObject.insert(k, v));
        } else {
            returnObject.sootField = classField.sootField;
            returnObject.targetClassField = classField.targetClassField;
            returnObject.fieldsMap = (HashMap<SootField, SootField>) classField.fieldsMap.clone();
        }
        return returnObject;
    }

    public void convertFormalParamToActual(ClassField classField) {
        //i0-ParameterObject:0:r1{textField} , r1 points to classField object here
        SootField tempField = this.sootField;
        HashMap<SootField, SootField> tempFieldsMap = (HashMap<SootField, SootField>) this.fieldsMap.clone();
        this.sootField = classField.sootField; //base field changes and target stays same
        this.fieldsMap = (HashMap<SootField, SootField>) classField.fieldsMap.clone();
        // process map to map correctly
        SootField field = tempField;
        SootField tempTarget = classField.targetClassField;
        insert(tempTarget, field); // base field can't be null
        //System.out.println("Field: " + field.getName());
        while (tempFieldsMap.get(field) != null) {
            SootField newField = tempFieldsMap.get(field);
            insert(newField);
            field = newField;
        }
    }

    public boolean removePrefix(ParameterAnalysisObject classField) {
        if (Utils.isPrefixObject(classField, this)) {
            SootField tmpField = classField.sootField;
            if (tmpField == null)
                return true;// can happen when parameter itself doing something
            HashMap<SootField, SootField> tmpMap = (HashMap<SootField, SootField>) classField.fieldsMap.clone();
            System.out.println("removePrefix: " + classField + ":" + this.toString());
            while (tmpMap.get(tmpField) != null && this.fieldsMap.get(tmpField) != null) {
                SootField prefixField = tmpMap.get(tmpField);
                SootField originalField = this.fieldsMap.get(tmpField);
                System.out.println("removePrefix: " + originalField + ":" + prefixField);
                if (prefixField.equals(originalField)) {
                    this.fieldsMap.remove(tmpField);
                    tmpMap.remove(tmpField);
                    classField.sootField = prefixField;
                    this.sootField = originalField;
                    tmpField = prefixField;
                } else {
                    return false;
                }
            }

            if (tmpField.equals(classField.targetClassField)) {// map is empty, only 1 field
                if (this.fieldsMap.get(tmpField) != null) {
                    SootField newBase = this.fieldsMap.get(tmpField);
                    this.fieldsMap.remove(tmpField);
                    this.sootField = newBase;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean comparePrefix(ParameterAnalysisObject parameterAnalysisObject) {// todo: might need to treat some cases
        if (parameterAnalysisObject.local.equivTo(this.local) && parameterAnalysisObject.parameterIndex == this.parameterIndex) {
            SootField tmpField = parameterAnalysisObject.sootField;
            while (parameterAnalysisObject.fieldsMap.get(tmpField) != null && this.fieldsMap.get(tmpField) != null) {
                SootField prefixField = parameterAnalysisObject.fieldsMap.get(tmpField);
                SootField thisField = this.fieldsMap.get(tmpField);
                if (prefixField.equals(thisField)) {
                    tmpField = prefixField;
                } else {
                    return false;
                }
            }
            return true;// return true, as long as its same parameter
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterAnalysisObject that = (ParameterAnalysisObject) o;
        return Objects.equals(sootField, that.sootField) &&
                Objects.equals(fieldsMap, that.fieldsMap) &&
                Objects.equals(targetClassField, that.targetClassField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootField, fieldsMap, targetClassField);
    }


    public Element getXML(Document doc) {

        SootField field = this.sootField;
        //System.out.println("Field: " + field.getName());
        Element mainItem = getXMLElement(doc, field);
        Element item = mainItem;
        while (this.fieldsMap.get(field) != null) {
            SootField newField = this.fieldsMap.get(field);
            //System.out.println("newField: " + newField.getName());
            Element newItem = getXMLElement(doc, newField);
            newItem.appendChild(item);
            field = newField;
            item = newItem;
        }
        return item;
    }


    private Element getXMLElement(Document doc, SootField field) {
        Element item = doc.createElement("item");
        Attr attrType = doc.createAttribute("type");
        attrType.setValue("field");
        item.setAttributeNode(attrType);
        Attr attrValue = doc.createAttribute("value");
        attrValue.setValue(field.getName());
        item.setAttributeNode(attrValue);
        Attr fieldType = doc.createAttribute("fieldType");
        fieldType.setValue(field.getType().toQuotedString());
        item.setAttributeNode(fieldType);
        Attr fieldField = doc.createAttribute("field");
        fieldField.setValue(field.toString());
        item.setAttributeNode(fieldField);
        return item;
    }

    @Override
    public String toString() {
        //fieldsMap.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ":" + e.getValue()));
        if (this.local == null && this.parameterIndex < 0)
            return "ParameterObject: <<zero>>";
        return "ParameterObject:" + parameterIndex + ":" + local.getName() + "{" +
                //"sootField=" + sootField.getName() +
                //", fieldsMap=" +
                printMap() +
                // +
                // ", targetClassField=" + targetClassField.getName() +
                '}';
    }
}
