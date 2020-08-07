package edu.ucr.cs.ufarooq.model;

import edu.ucr.cs.ufarooq.accessPath.Field;
import soot.SootField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AnalysisObject implements Cloneable { // to extend classField and ParameterField
    protected SootField sootField;
    protected HashMap<SootField, SootField> fieldsMap;
    protected SootField targetClassField;


    public SootField getSootField() {
        return sootField;
    }

    public void setSootField(SootField sootField) {
        this.sootField = sootField;
    }

    public void insert(SootField field, SootField newField) {
        //System.out.println("Insert::" + this.toString() + ":" + field.toString() + ":" + newField.toString());
        //System.out.println("Insert::" + fieldsMap.containsKey(field) + ":" + fieldsMap.containsValue(newField));
        if (!fieldsMap.containsKey(field) && !fieldsMap.containsValue(newField) && !field.equals(newField)) {//avoid cycles
            this.fieldsMap.put(field, newField);
            this.targetClassField = newField;
            //System.out.println("Insert::" + this.toString());
        }
    }

    public void insert(SootField field) {
        //System.out.println("Insert::" + this.toString() + ":" + field.toString());
        //System.out.println("Insert::" + fieldsMap.containsKey(field) + ":" + fieldsMap.containsValue(field));
        if (!fieldsMap.containsKey(field) && !fieldsMap.containsValue(field) && !field.equals(this.targetClassField)) {//avoid cycles
            this.fieldsMap.put(targetClassField, field);
            this.targetClassField = field;
            //System.out.println("Insert::" + this.toString());
        }
    }

    private void insertForSummary(SootField field, SootField newField) {
        //System.out.println("Insert::" + this.toString() + ":" + field.toString() + ":" + newField.toString());
        //System.out.println("Insert::" + fieldsMap.containsKey(field) + ":" + fieldsMap.containsValue(newField));
        if (!fieldsMap.containsKey(field) && !field.equals(newField)) {//avoid cycles
            this.fieldsMap.put(field, newField);
            this.sootField = field;
            //System.out.println("Insert(new)::" + this.toString());
        }
    }

    public SootField getTargetClassField() {
        return targetClassField;
    }

    private HashMap<SootField, SootField> getFieldsMap() {
        return fieldsMap;
    }

    public static void copyClassFields(AnalysisObject field, AnalysisObject copy) {
        SootField sootField = copy.getSootField();
        System.out.println("Field: " + field.toString() + ":" + sootField.toString());
        if (sootField != null)
            field.insert(field.getSootField(), sootField);
        HashMap<SootField, SootField> map = copy.getFieldsMap();
        while (map.get(sootField) != null) {
            SootField newField = map.get(sootField);
            System.out.println("newField: " + newField.getName());
            field.insert(sootField, newField);
            System.out.println("newField(insert): " + field.toString());
            sootField = newField;
        }
    }

    public static void copyClassFieldsForSummary(AnalysisObject field, AnalysisObject copy) {
        SootField sootField = copy.getTargetClassField();
        System.out.println("Field: " + field.toString() + ":" + sootField.toString());
        if (sootField != null)
            field.insertForSummary(sootField, field.getSootField());
        HashMap<SootField, SootField> map = copy.getFieldsMap();
        while (map.get(sootField) != null) {
            SootField newField = map.get(sootField);
            System.out.println("newField: " + newField.getName());
            field.insertForSummary(newField, sootField);
            System.out.println("newField(insert): " + field.toString());
            sootField = newField;
        }
    }


    @Override
    public String toString() {
        //fieldsMap.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ":" + e.getValue()));
        if (this.sootField == null)
            return "<<zero>>";
        return "ClassField{" +
                //"sootField=" + sootField.getName() +
                //", fieldsMap=" +
                printMap() +
                // +
                // ", targetClassField=" + targetClassField.getName() +
                '}';
    }

    public String printMap() {
        String stringBuilder = "";
        SootField field = this.sootField;
        if (field != null) {
            //System.out.println("Field: " + field.getName());
            stringBuilder = field.getName();
            while (this.fieldsMap.get(field) != null) {
                SootField newField = this.fieldsMap.get(field);
                //System.out.println("newField: " + newField.getName());
                stringBuilder = stringBuilder + ":" + newField.getName();
                field = newField;
            }
        }
        return stringBuilder;
    }

    public List<Field> getAsListField() {
        List<Field> fields = new ArrayList<>();
        SootField field = this.sootField;
        if (field != null) {
            Field f = new Field(field);
            fields.add(f);
            while (this.fieldsMap.get(field) != null) {
                SootField newField = this.fieldsMap.get(field);
                Field newF = new Field(newField);
                fields.add(newF);
                field = newField;
            }
        }
        return fields;
    }
}
