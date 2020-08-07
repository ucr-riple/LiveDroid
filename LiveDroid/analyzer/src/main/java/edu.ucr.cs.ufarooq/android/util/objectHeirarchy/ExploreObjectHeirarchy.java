package edu.ucr.cs.ufarooq.android.util.objectHeirarchy;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.jimple.toolkits.typing.ClassHierarchy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExploreObjectHeirarchy {
    public static void exploreField(int level, int k, List<SootField> list, SootField sootField) {
        SootClass sootClass = Scene.v().getSootClassUnsafe(sootField.getType().toQuotedString());
        System.out.println(sootClass.getName() + ":" + sootClass.getFields().size());
        if (level <= k) {
            Iterator<SootField> it = sootClass.getFields().iterator();
            while (it.hasNext()){
                SootField field= it.next();
                if (!field.isFinal()) {
                    list.add(field);
                    exploreField(level+1, k, list, field);
                }
            }
        }
    }

    public static List<SootField> exploreClass(SootClass sootClass, int k) {
        List<SootField> fields = new ArrayList<>();
        System.out.println(sootClass.getName() + ":" + sootClass.getFields().size());
        Iterator<SootField> it = sootClass.getFields().iterator();
        while (it.hasNext()){
            SootField field= it.next();
            if (!field.isFinal()) {
                fields.add(field);
                exploreField(1, k, fields, field);
            }
        }
//        for (SootField field : sootClass.getFields()) {
//            if (!field.isFinal()) {
//                fields.add(field);
//                exploreField(1, k, fields, field);
//            }
//        }
        return fields;
    }


}
