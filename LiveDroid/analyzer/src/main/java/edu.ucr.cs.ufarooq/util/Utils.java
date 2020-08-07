package edu.ucr.cs.ufarooq.util;


import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import edu.ucr.cs.ufarooq.accessPath.Field;
import edu.ucr.cs.ufarooq.accessPath.Val;
import edu.ucr.cs.ufarooq.intra.AliasAnalysisResult;
import edu.ucr.cs.ufarooq.intra.AliasesState;
import edu.ucr.cs.ufarooq.intra.MethodAliasAnalysis;
import edu.ucr.cs.ufarooq.intra.MethodAliasProvider;
import edu.ucr.cs.ufarooq.accessPath.AccessPathAnalysisObject;
import edu.ucr.cs.ufarooq.model.AnalysisObject;
import edu.ucr.cs.ufarooq.model.ClassField;
import edu.ucr.cs.ufarooq.model.ParameterAnalysisObject;
import edu.ucr.cs.ufarooq.model.ReturnAnalysisObject;
import heros.InterproceduralCFG;
import pathexpression.IRegEx;
import pathexpression.RegEx;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocalBox;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;

import java.util.*;

public class Utils {

    public static AnalysisObject getClassFieldOnAssignment(DefinitionStmt assignStmt, Local thisLocal, FlowSet<AliasesState> aliases) {
        AnalysisObject classField = null;
        //Local local = (Local) assignStmt.getLeftOp();
        FieldRef fieldRef = (FieldRef) assignStmt.getLeftOp();
        Local localForFieldRef = getLocalForFieldRef(fieldRef);
        if (localForFieldRef != null && thisLocal != null) {
            if (localForFieldRef.equivTo(thisLocal)) {//class field
                classField = new ClassField(fieldRef.getField());
            } else if (aliases.contains(new AliasesState(localForFieldRef))) {//field of field
                classField = findAliasInSet(localForFieldRef, aliases);
                classField.insert(fieldRef.getField());
            }
        } else {//static access
            classField = new ClassField(fieldRef.getField());

        }
        return classField;
    }

    public static Local getLocalForFieldRef(FieldRef fieldRef) {
        for (ValueBox box : fieldRef.getUseBoxes()) {
            if (box instanceof JimpleLocalBox) {
                JimpleLocalBox jimpleLocalBox = (JimpleLocalBox) box;
                if (jimpleLocalBox.getValue() instanceof Local) {
                    Local l = (Local) jimpleLocalBox.getValue();
                    return l;
                }
            }
        }
        return null;
    }

    public static Local getLocalForInvoke(InvokeStmt invokeStmt) {
        for (ValueBox vb : invokeStmt.getUseBoxes()) {
            if (vb instanceof JimpleLocalBox) {
                JimpleLocalBox jimpleLocalBox = (JimpleLocalBox) vb;
                if (jimpleLocalBox.getValue() instanceof Local) {
                    Local l = (Local) jimpleLocalBox.getValue();
                    return l;
                }
            }
        }
        return null;
    }

    public static Local getLocalForInvoke(InvokeExpr invokeStmt) {
        for (ValueBox vb : invokeStmt.getUseBoxes()) {
            if (vb instanceof JimpleLocalBox) {
                JimpleLocalBox jimpleLocalBox = (JimpleLocalBox) vb;
                if (jimpleLocalBox.getValue() instanceof Local) {
                    Local l = (Local) jimpleLocalBox.getValue();
                    return l;
                }
            }
        }
        return null;
    }

    //for alias analysis
    public static AnalysisObject findAliasInSet(Local local, FlowSet<AliasesState> aliases) {
        if (aliases.contains(new AliasesState(local))) {
            List<AliasesState> list = aliases.toList();
            int indexOfObject = list.indexOf(new AliasesState(local));
            AnalysisObject classField = list.get(indexOfObject).getField();
            return classField;
        }
        return null;
    }

    public static AnalysisObject findAliasInSet(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(sootMethod, curr, icfg).getFlowAfter(curr).getAliasesStateFlowSet();
        //System.out.println(aliases);
        if (aliases.contains(new AliasesState(local))) {
            List<AliasesState> list = aliases.toList();
            int indexOfObject = list.indexOf(new AliasesState(local));
            AnalysisObject classField = list.get(indexOfObject).getField();
            //System.out.println("Index of Object: " + indexOfObject + ":" + classField);
            return classField;
        }
        return null;
    }


    public static Unit findAliasedPoint(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(sootMethod, curr, icfg).getFlowAfter(curr).getAliasesStateFlowSet();
        if (aliases.contains(new AliasesState(local))) {
            List<AliasesState> list = aliases.toList();
            int indexOfObject = list.indexOf(new AliasesState(local));
            Unit unit = list.get(indexOfObject).getDefinitionStmt();
            return unit;
        }
        return null;
    }

    public static AccessPath getAccessPathViaFieldMapping(AccessPath x, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        Value baseValue = x.getBase().value();
        if (baseValue instanceof Local) {
            Local baseLocal = (Local) baseValue;
            AnalysisObject analysisObject = Utils.findAliasInSet(baseLocal, sootMethod, curr, icfg);
            if (analysisObject != null) {
                AccessPath accessPath = Utils.analysisObjectToAccessPath(analysisObject, sootMethod);
                if (accessPath != null) {
                    List<Field> fields = accessPath.getFields();
                    for (Field field : x.getFields()) {
                        fields.add(field); //concatenate fields accordingly
                    }
                    AccessPath newAccessed = new AccessPath(accessPath.getBase(), fields);
                    return newAccessed;
                }
            }
        }
        return null;
    }

    public static AccessPath concatenateField(AccessPath base, AccessPath accessPath) {
        int size = base.getFields().size() + accessPath.getFields().size();
        List<Field> newFields = new ArrayList<>(size);
        newFields.addAll(0, base.getFields());
        newFields.addAll(base.getFields().size(), accessPath.getFields());
//        Collection<Field> fields = base.getFields();
//        for (Field field : accessPath.getFields()) {
//            fields.add(field);
//        }
        AccessPath newObj = new AccessPath(base.getBase(), newFields);
        return newObj;
    }

    public static AccessPath concatenateField(AccessPath accessPath, SootField sootField) {
        List<Field> fields = new ArrayList<>();
        if (accessPath.getFields() != null) {
            fields = accessPath.getFields();
        }
        if (sootField != null)
            fields.add(new Field(sootField));
        AccessPath newObj = new AccessPath(accessPath.getBase(), fields);
        return newObj;
    }

    public static AnalysisObject getClassFieldIfExistsOnLeft(SootMethod method, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        DefinitionStmt definition = (DefinitionStmt) curr;
        AnalysisObject classField = null;
        if (definition.getLeftOp() instanceof FieldRef) {
            Local thisLocal = null;
            UnitGraph graph = new BriefUnitGraph(method.retrieveActiveBody());
            if (!graph.getBody().getMethod().isStatic())
                thisLocal = graph.getBody().getThisLocal();
            MethodAliasAnalysis aliasResult = MethodAliasProvider.getAliasResult(method, curr, icfg);
            AliasAnalysisResult analysisResult = aliasResult.getFlowAfter(curr);
            classField = Utils.getClassFieldOnAssignment(definition, thisLocal, analysisResult.getAliasesStateFlowSet());
        }
        return classField;

    }

    public static AnalysisObject getClassFieldIfExistsOnRight(SootMethod method, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        DefinitionStmt definition = (DefinitionStmt) curr;

        AnalysisObject classField = null;
        if (definition.getRightOp() instanceof Local) {
            Local readLocal = (Local) definition.getRightOp();
            FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(method, curr, icfg).getFlowAfter(curr).getAliasesStateFlowSet();
            classField = findAliasInSet(readLocal, aliases);
        }
        return classField;

    }

    public static boolean isDefinitionForLocal(Unit unit, SootMethod sootMethod, InterproceduralCFG<Unit, SootMethod> icfg) {
        FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(sootMethod, unit, icfg).getFlowAfter(unit).getAliasesStateFlowSet();
        if (unit instanceof DefinitionStmt) {
            DefinitionStmt assignStmt = (DefinitionStmt) unit;
            if (assignStmt.getLeftOp() instanceof Local && assignStmt.getRightOp() instanceof FieldRef) {
                Local l = (Local) assignStmt.getLeftOp();
                FieldRef ref = (FieldRef) assignStmt.getRightOp();
                //System.out.println("aliases:" + aliases);
                //System.out.println("Def Check:" + l + ":" + ref + ":" + aliases.contains(new AliasesState(l)) + ":" + locals.contains(l));
                if (aliases.contains(new AliasesState(l))) {
                    AnalysisObject classField = findAliasInSet(l, aliases);
                    //System.out.println("Def Found:" + foundRef);
                    return classField.getTargetClassField().equals(ref.getField());
                }
            }
        }
        return false;
    }

    public static AliasesState getReturnAliasesState(SootMethod sootMethod, InterproceduralCFG<Unit, SootMethod> icfg, int currentCount) {
        Iterator<Unit> unitsIt = sootMethod.retrieveActiveBody().getUnits().iterator();

        while (unitsIt.hasNext()) {
            Unit unit = unitsIt.next();
            if (unit instanceof ReturnStmt) {
                ReturnStmt returnStmt = (ReturnStmt) unit;
                if (returnStmt.getOp() instanceof Local) {
                    Local local = (Local) returnStmt.getOp();
                    AliasesState aliasState = findAliasStateInSet(local, sootMethod, unit, icfg, currentCount);
                    return aliasState;
                }
            }
        }
        return null;
    }

    public static AliasesState findAliasStateInSet(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg, int currentCount) {
        FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(sootMethod, curr, icfg).getFlowAfter(curr).getAliasesStateFlowSet();
        //System.out.println(aliases);
        if (aliases.contains(new AliasesState(local))) {
            List<AliasesState> list = aliases.toList();
            int indexOfObject = list.indexOf(new AliasesState(local));
            AliasesState aliasesState = list.get(indexOfObject);
            //System.out.println("Index of Object: " + indexOfObject + ":" + classField);
            return aliasesState;
        }
        return null;
    }

    public static AliasesState findAliasStateInSet(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(sootMethod, curr, icfg).getFlowAfter(curr).getAliasesStateFlowSet();
        //System.out.println(aliases);
        if (aliases.contains(new AliasesState(local))) {
            List<AliasesState> list = aliases.toList();
            int indexOfObject = list.indexOf(new AliasesState(local));
            AliasesState aliasesState = list.get(indexOfObject);
            //System.out.println("Index of Object: " + indexOfObject + ":" + classField);
            return aliasesState;
        }
        return null;
    }

    public static boolean isDefinitionForLocal(Unit unit) {
        if (unit instanceof DefinitionStmt) {
            DefinitionStmt assignStmt = (DefinitionStmt) unit;
            return (assignStmt.getLeftOp() instanceof Local && assignStmt.getRightOp() instanceof FieldRef);
        }

        return false;
    }

    public static boolean isLoadInst(Unit unit) {
        if (unit instanceof DefinitionStmt) {
            DefinitionStmt assignStmt = (DefinitionStmt) unit;
            return (assignStmt.getLeftOp() instanceof Local && assignStmt.getRightOp() instanceof FieldRef);
        }
        return false;
    }

    public static boolean isStoreInst(Unit unit) {
        if (unit instanceof DefinitionStmt) {
            DefinitionStmt assignStmt = (DefinitionStmt) unit;
            return (assignStmt.getLeftOp() instanceof FieldRef && assignStmt.getRightOp() instanceof Local);
        }
        return false;
    }

    public static boolean isPrimitiveType(Type type) {
        if (type instanceof PrimType)
            return true;
        if (type instanceof ByteType || type instanceof CharType || type instanceof DoubleType || type instanceof FloatType ||
                type instanceof IntType || type instanceof LongType || type instanceof ShortType || type instanceof BooleanType) {
            return true;
        }
        if(type.toString().equalsIgnoreCase("java.lang.String"))
            return true;
        return false;
    }

    public static boolean isVoidReturn(SootMethod method) {
        return method.getReturnType() instanceof VoidType;
    }

    public static boolean canReturnReference(SootMethod sootMethod) {
        return !(isVoidReturn(sootMethod) || isPrimitiveType(sootMethod.getReturnType()));
    }

    public static boolean isPrefixObject(AnalysisObject prefix, AnalysisObject flowObject) {
        if (prefix instanceof ClassField && flowObject instanceof ClassField) {
            ClassField flowClassField = (ClassField) flowObject;
            return flowClassField.comparePrefix((ClassField) prefix);
        } else if (prefix instanceof ParameterAnalysisObject && flowObject instanceof ParameterAnalysisObject) {
            ParameterAnalysisObject flowParam = (ParameterAnalysisObject) flowObject;
            return flowParam.comparePrefix((ParameterAnalysisObject) prefix);
        } else if (prefix instanceof ReturnAnalysisObject && flowObject instanceof ReturnAnalysisObject) {
            return isPrefixStringMatch(prefix, flowObject);
        }
        return false;
    }

    private static boolean isPrefixStringMatch(AnalysisObject prefix, AnalysisObject flow) {
        String baseObjectString = prefix.toString();
        String baseSub = baseObjectString.substring(0, baseObjectString.length() - 1);
        return (flow.toString().startsWith(baseSub));
    }

    public static AccessPath getAccessPathUsingMapping(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        AnalysisObject mappedObject = findAliasInSet(local, sootMethod, curr, icfg);
        if (mappedObject != null) {
            return analysisObjectToAccessPath(mappedObject, sootMethod);
        }
        return null;
    }

    public static AccessPath analysisObjectToAccessPath(AnalysisObject analysisObject, SootMethod method) {
        if (analysisObject instanceof ClassField) {
            if (!method.isStatic()) {
                Local thisLocal = method.retrieveActiveBody().getThisLocal();
                Val base = new Val(thisLocal, method);
                List<Field> fields = analysisObject.getAsListField();
                AccessPath accessPath = new AccessPath(base, fields);
                return accessPath;

            }
        } else if (analysisObject instanceof ParameterAnalysisObject) {
            ParameterAnalysisObject parameterAnalysisObject = (ParameterAnalysisObject) analysisObject;
            Val base = new Val(parameterAnalysisObject.getLocal(), method);
            List<Field> fields = analysisObject.getAsListField();
            AccessPath accessPath = new AccessPath(base, fields);
            return accessPath;
        }
        return null;
    }

    public static AccessPath analysisObjectToAccessPath(AnalysisObject analysisObject, SootMethod method, SootField field) {
        AccessPath accessPath = analysisObjectToAccessPath(analysisObject, method);
        if (accessPath != null) {
            List<Field> fields = accessPath.getFields();
            fields.add(new Field(field));
            AccessPath newAccessPath = new AccessPath(accessPath.getBase(), fields);
            return newAccessPath;
        }
        return null;
    }

    public static void processKills(AccessPath x, Set<AccessPathAnalysisObject> defs, Unit curr, SootMethod sootMethod, SootField sootField, Local local) {
//        System.out.println("AccessPath: " + x + ":" + x.getBase().value());
//        AccessPath accessPathViaFieldMapping = Utils.getAccessPathViaFieldMapping(x, sootMethod, curr);
//        if (accessPathViaFieldMapping != null) {
//            System.out.println("accessPathViaFieldMapping: " + accessPathViaFieldMapping);
//            AccessPath concatenateField = Utils.concatenateField(accessPathViaFieldMapping, sootField);
//            AccessPathAnalysisObject aliasedAnalysisObject = new AccessPathAnalysisObject(concatenateField, local, curr);
//            defs.add(aliasedAnalysisObject);
//            System.out.println("Aliases(FieldMapping): " + aliasedAnalysisObject);
//        }
        AccessPath newObj = Utils.concatenateField(x, sootField);
        AccessPathAnalysisObject analysisObject = new AccessPathAnalysisObject(newObj, local, curr);
        defs.add(analysisObject);
    }

    public static void processKillFieldOfLocal(Local local, Unit curr, SootMethod sootMethod, SootField sootField, Set<AccessPathAnalysisObject> defs) {
        AccessPath accessPath = new AccessPath(new Val(local, sootMethod), new Field(sootField));
        System.out.println("Only LocalField: " + accessPath);
        defs.add(new AccessPathAnalysisObject(accessPath, local, curr));
    }

    public static void processReferenceUse(Local defLocal, Unit defPoint, SootMethod sootMethod, Local reason, Unit curr, Set<AccessPathAnalysisObject> uses, InterproceduralCFG<Unit, SootMethod> icfg) {
        AnalysisObject analysisObject = Utils.findAliasInSet(defLocal, sootMethod, defPoint, icfg);
        AccessPath accessPath = Utils.analysisObjectToAccessPath(analysisObject, sootMethod);
        if (accessPath != null) {
            AccessPathAnalysisObject accessPathAnalysisObject = new AccessPathAnalysisObject(accessPath, reason, curr);
            uses.add(accessPathAnalysisObject);
        }
    }

    public static boolean isPrefixAccessPath(AccessPath prefix, AccessPath accessPath) {
        if (prefix != null && accessPath != null && prefix.getFields() != null && accessPath.getFields() != null) {
            if (prefix.getFields().size() < accessPath.getFields().size()) {
                int prefixSize = prefix.getFields().size();
                Collection<Field> accessPathFields = accessPath.getFields();
                List<Field> fieldList = new ArrayList<>(accessPathFields);
                List<Field> subList = fieldList.subList(0, prefixSize);
                List<Field> prefixList = new ArrayList<>(prefix.getFields());
                for (int i = 0; i < prefixList.size(); i++) {
                    if (!prefixList.get(i).equals(subList.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isPrefixAccessPath(AccessPath prefix, AccessPath accessPath, boolean matchBase) {
        if (matchBase) {
            if (prefix.getBase().equals(accessPath.getBase()))
                return isPrefixAccessPath(prefix, accessPath);
            return false;
        } else {
            return isPrefixAccessPath(prefix, accessPath);
        }
    }

    public static AccessPath removePrefix(AccessPath prefix, AccessPath accessPath, Local paramLocal, SootMethod destinationMethod) {
        // assuming argument object is prefix
        int prefixSize = prefix.getFields().size();
        Collection<Field> accessPathFields = accessPath.getFields();
        List<Field> fieldList = new ArrayList<>(accessPathFields);
        List<Field> subList = fieldList.subList(prefixSize, fieldList.size());
        Val base = new Val(paramLocal, destinationMethod);
        AccessPath removedPrefix = new AccessPath(base, subList);
        return removedPrefix;
    }

    public static List<AccessPath> getArgumentAccessPaths(Stmt stmt, SootMethod callerMethod, InterproceduralCFG<Unit, SootMethod> icfg) {
        List<AccessPath> argumentAccessPaths = new ArrayList<>(stmt.getInvokeExpr().getArgCount());
        for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
            Value argVal = stmt.getInvokeExpr().getArg(i);
            if (argVal instanceof Local) {
                Local argLocal = (Local) argVal;
                AccessPath argAccessPath = Utils.getAccessPathUsingMapping(argLocal, callerMethod, stmt, icfg);

                argumentAccessPaths.add(i, argAccessPath);
            }
        }
        return argumentAccessPaths;
    }

    public static AccessPath formalToActualParameterConversion(AccessPath argument, AccessPath parameter) {
        // assuming argument object is prefix
        int size = argument.getFields().size() + parameter.getFields().size();
        List<Field> newFields = new ArrayList<>(size);
        newFields.addAll(0, argument.getFields());
        newFields.addAll(argument.getFields().size(), parameter.getFields());
        AccessPath newObj = new AccessPath(argument.getBase(), newFields);
        return newObj;
    }

    public static AccessPath formalToActualSubfieldConversion(AccessPath invokeAccessPath, AccessPath field, SootMethod callerMethod) {
        // assuming argument object is prefix
        Local invokeLocal = (Local) invokeAccessPath.getBase().value();
        List<Field> argPathFields = new ArrayList<>(invokeAccessPath.getFields());
        Field firstField = argPathFields.get(0);
        List<Field> fieldList = new ArrayList<>(field.getFields().size() + 1);
        fieldList.add(firstField);
        fieldList.addAll(1, field.getFields());
        Val base = new Val(invokeLocal, callerMethod);
        AccessPath addedPrefix = new AccessPath(base, fieldList);
        return addedPrefix;
        //return formalToActualParameterConversion(invokeAccessPath, field, invokeLocal, callerMethod);
    }

    public static List<Val> getThisAndParameterVal(SootMethod sootMethod) {
        List<Val> locals = new ArrayList<>();
        if (!sootMethod.isStatic()) {
            locals.add(new Val(sootMethod.retrieveActiveBody().getThisLocal(), sootMethod));
        }
        for (int i = 0; i < sootMethod.getParameterCount(); i++) {
            Local parameterLocal = sootMethod.retrieveActiveBody().getParameterLocal(i);
            Val paramVal = new Val(parameterLocal, sootMethod);
            locals.add(paramVal);
        }

        return locals;
    }

    public static List<Local> getArgumentLocals(Stmt stmt) {
        List<Local> argsLocals = new ArrayList<>(stmt.getInvokeExpr().getArgCount());
        for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
            Value argVal = stmt.getInvokeExpr().getArg(i);
            if (argVal instanceof Local) {
                Local argLocal = (Local) argVal;
                argsLocals.add(i, argLocal);
            }
        }
        return argsLocals;
    }

    public static List<Local> getParameterLocals(SootMethod sootMethod) {
        List<Local> paramLocals = new ArrayList<>(sootMethod.getParameterCount());
        for (int i = 0; i < sootMethod.getParameterCount(); i++) {
            Local parameterLocal = sootMethod.retrieveActiveBody().getParameterLocal(i);
            paramLocals.add(i, parameterLocal);
        }
        return paramLocals;
    }



    private static IRegEx<Field> getPlainRegEx(AccessPath accessPath) {
        if (!accessPath.getFields().isEmpty()) {
            List<Field> list = new ArrayList(accessPath.getFields());
            IRegEx<Field> fieldIRegEx = new RegEx.Plain<Field>(list.get(0));
            if (accessPath.getFields().size() == 1)
                return fieldIRegEx;

            for (int i = 1; i < list.size(); i++) {
                IRegEx<Field> currentEx = new RegEx.Plain<Field>(list.get(i));
                fieldIRegEx = RegEx.concatenate(fieldIRegEx, currentEx);
            }
            return fieldIRegEx;
        }
        return new RegEx.EmptySet<Field>();
    }

    public static void processNonPointTo(Local local, SootMethod sootMethod, SootField sootField, Unit curr, Set<AccessPathAnalysisObject> defs, InterproceduralCFG<Unit, SootMethod> icfg) {
        AccessPath accessPath = getAccessPathUsingMapping(local, sootMethod, curr, icfg);
        if (accessPath != null) {
            AccessPath newAccessPath = concatenateField(accessPath, sootField);
            defs.add(new AccessPathAnalysisObject(newAccessPath, local, curr));
        }
    }

    public static void processNonPointToUses(Local local, SootMethod sootMethod, Unit curr, Set<AccessPathAnalysisObject> uses, InterproceduralCFG<Unit, SootMethod> icfg) {
        AccessPath accessPath = getAccessPathUsingMapping(local, sootMethod, curr, icfg);
        if (accessPath != null) {
            uses.add(new AccessPathAnalysisObject(accessPath, local, curr));
        }
    }
}