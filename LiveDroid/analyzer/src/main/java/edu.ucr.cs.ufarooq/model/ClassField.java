package edu.ucr.cs.ufarooq.model;

import edu.ucr.cs.ufarooq.util.Utils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import soot.SootField;

import java.util.HashMap;
import java.util.Objects;

public class ClassField extends AnalysisObject {

    public ClassField() {
        this.sootField = null;
        this.fieldsMap = new HashMap<>();
        this.targetClassField = null;
    }

    public ClassField(SootField sootField) {
        this.sootField = sootField;
        this.fieldsMap = new HashMap<>();
        this.targetClassField = sootField;
    }

    public ClassField(SootField sootField, HashMap<SootField, SootField> fieldsMap) {
        this.sootField = sootField;
        this.fieldsMap = fieldsMap;
    }

    public ClassField(ClassField classField) {
        this.sootField = classField.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) classField.fieldsMap.clone();
        this.targetClassField = classField.targetClassField;
    }

    public ClassField(ParameterAnalysisObject parameterAnalysisObject) {
        this.sootField = parameterAnalysisObject.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) parameterAnalysisObject.fieldsMap.clone();
        this.targetClassField = parameterAnalysisObject.targetClassField;
    }

    public ClassField(ReturnAnalysisObject returnAnalysisObject) {
        this.sootField = returnAnalysisObject.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) returnAnalysisObject.fieldsMap.clone();
        this.targetClassField = returnAnalysisObject.targetClassField;
    }

    public ClassField(ReturnPropAnalysisObject returnPropAnalysisObject) {
        this.sootField = returnPropAnalysisObject.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) returnPropAnalysisObject.fieldsMap.clone();
        this.targetClassField = returnPropAnalysisObject.targetClassField;
    }

    public void appendField(ClassField classField) {
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

    public void appendAtStartField(ClassField classField) {
        System.out.println("appendAtStartField:" + classField + ":" + toString());
        insert(classField.targetClassField, this.sootField); // connect target of base to root of current
        this.sootField = classField.sootField; //base field changes and target stays same
        // put map as
        classField.fieldsMap.forEach((k, v) -> insert(k, v));
    }

    public boolean removePrefix(ClassField classField) {
        if (Utils.isPrefixObject(classField, this)) {
            SootField tmpField = classField.sootField;
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

    public boolean comparePrefix(ClassField classField) {
        if (classField.sootField.equals(this.sootField)) {
            SootField tmpField = classField.sootField;

            while (classField.fieldsMap.get(tmpField) != null && this.fieldsMap.get(tmpField) != null) {
                SootField prefixField = classField.fieldsMap.get(tmpField);
                SootField thisField = this.fieldsMap.get(tmpField);
                if (prefixField.equals(thisField)) {
                    tmpField = prefixField;
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassField that = (ClassField) o;
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
}
