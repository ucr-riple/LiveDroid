package edu.ucr.cs.ufarooq.model;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import soot.Local;
import soot.SootField;
import soot.Unit;

import java.util.HashMap;
import java.util.Objects;

public class ReturnAnalysisObject extends AnalysisObject {

    private Local local;
    private Unit callSite;

    public ReturnAnalysisObject() {
        this.sootField = null;
        this.fieldsMap = new HashMap<>();
        this.targetClassField = null;
        this.local = null;
        this.callSite = null;
    }

    public ReturnAnalysisObject(Local local, Unit unit) {// initially, field will not be added
        this.local = local;
        this.callSite = unit;
        this.sootField = null;
        this.fieldsMap = new HashMap<>();
        this.targetClassField = null;
    }

    public ReturnAnalysisObject(SootField sootField, HashMap<SootField, SootField> fieldsMap) {
        this.sootField = sootField;
        this.fieldsMap = fieldsMap;
    }

    public ReturnAnalysisObject(ReturnAnalysisObject classField) {
        this.local = classField.local;
        this.callSite = classField.callSite;
        this.sootField = classField.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) classField.fieldsMap.clone();
        this.targetClassField = classField.targetClassField;
    }

    public ReturnAnalysisObject(ReturnPropAnalysisObject returnPropAnalysisObject){
        this.local = returnPropAnalysisObject.getReturnlocal();
        this.callSite = returnPropAnalysisObject.getReturnCallSite();
        this.sootField = returnPropAnalysisObject.sootField;
        this.fieldsMap = (HashMap<SootField, SootField>) returnPropAnalysisObject.fieldsMap.clone();
        this.targetClassField = returnPropAnalysisObject.targetClassField;
    }

    public Unit getCallSite() {
        return callSite;
    }

    public void setCallSite(Unit callSite) {
        this.callSite = callSite;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public void convertCallSiteToFieled(ClassField classField) {
        if (this.sootField == null) {
            this.sootField = classField.sootField;
            this.fieldsMap = (HashMap<SootField, SootField>) classField.fieldsMap.clone();
            this.targetClassField = classField.targetClassField;
        } else {
            SootField tempField = this.sootField; // can be null
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
    }

    public void insert(SootField field, SootField newField) {
        if (this.sootField == null && this.targetClassField == null) {
            this.sootField = field;
            this.targetClassField = field;
            super.insert(field, newField);
        } else {
            super.insert(field, newField);
        }
    }

    public void insert(SootField field) {
        if (this.sootField == null && this.targetClassField == null) {
            this.sootField = field;
            this.targetClassField = field;
        } else {
            super.insert(field);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnAnalysisObject that = (ReturnAnalysisObject) o;
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
//        if (this.sootField == null)
//            return "<<zero>>";
        return "ReturnObject:" + callSite.toString() + ":" + local.getName() + "{" +
                //"sootField=" + sootField.getName() +
                //", fieldsMap=" +
                getMapToPrint() +
                // +
                // ", targetClassField=" + targetClassField.getName() +
                '}';
    }

    private String getMapToPrint() {
        if (this.sootField == null)
            return "<<zero>>";
        else
            return printMap();
    }
}
