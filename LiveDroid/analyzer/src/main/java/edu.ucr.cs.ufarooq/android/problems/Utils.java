package edu.ucr.cs.ufarooq.android.problems;

import com.google.common.collect.Sets;
import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import edu.ucr.cs.ufarooq.accessPath.Field;
import edu.ucr.cs.ufarooq.accessPath.Val;
import edu.ucr.cs.ufarooq.android.model.FieldOnlyAccessPath;
import edu.ucr.cs.ufarooq.intra.AliasAnalysisResult;
import edu.ucr.cs.ufarooq.intra.AliasesState;
import edu.ucr.cs.ufarooq.intra.MethodAliasAnalysis;
import edu.ucr.cs.ufarooq.intra.MethodAliasProvider;
import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import edu.ucr.cs.ufarooq.accessPath.AccessPathAnalysisObject;
import edu.ucr.cs.ufarooq.accessPath.Val;
import edu.ucr.cs.ufarooq.model.AnalysisObject;
import edu.ucr.cs.ufarooq.model.ClassField;
import edu.ucr.cs.ufarooq.model.NewAllocAnalysisObject;
import edu.ucr.cs.ufarooq.model.ParameterAnalysisObject;
import edu.ucr.cs.ufarooq.model.ReturnAnalysisObject;
import heros.InterproceduralCFG;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.toolkits.ide.JimpleIFDSSolver;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.util.Chain;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    public static final String DUMMY_LOCAL_PREFIX = "livedroid_";

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

    private static AnalysisObject findAliasInSet(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(sootMethod, curr, icfg).getFlowBefore(curr).getAliasesStateFlowSet();
        //System.out.println(aliases);
        if (aliases.contains(new AliasesState(local))) {
            List<AliasesState> list = aliases.toList();
            int indexOfObject = list.indexOf(new AliasesState(local));
            AliasesState aliaseState = list.get(indexOfObject);
            if (!AliasesState.isNewAllocation(aliaseState)) {
                AnalysisObject classField = list.get(indexOfObject).getField();
                //System.out.println("Index of Object: " + indexOfObject + ":" + classField);
                return classField;
            } else {
                return new NewAllocAnalysisObject();
            }
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

    public static AccessPath concatenateReplaceFirstField(AccessPath base, AccessPath accessPath) {
        System.out.println("Pre: " + base + ":" + accessPath);
        int size = base.getFields().size() + accessPath.getFields().size();
        List<Field> newFields = new ArrayList<>(size);
        newFields.addAll(0, base.getFields());
        List<Field> tmp = new ArrayList<>(accessPath.getFields());
        tmp.remove(0);//remove first
        newFields.addAll(base.getFields().size(), tmp);
//        Collection<Field> fields = base.getFields();
//        for (Field field : accessPath.getFields()) {
//            fields.add(field);
//        }
        AccessPath newObj = new AccessPath(base.getBase(), newFields);
        System.out.println("concatenateReplaceFirstField: " + newObj);
        return newObj;
    }

    public static AccessPath ReplaceReferenceAssignment(AccessPath left, AccessPath right, AccessPath accessPath) {
        System.out.println("Pre: " + right + ":" + left + ":" + accessPath);
        int leftSize = left.getFields().size();
        int rightSize = right.getFields().size();

        int sizeDiff = accessPath.getFields().size() - leftSize;
        assert sizeDiff > 0;

        int newSize = rightSize + sizeDiff;

        List<Field> newFields = new ArrayList<>(newSize);
        newFields.addAll(0, right.getFields());
        AccessPath removedPrefix = removePrefix(left, accessPath);
        newFields.addAll(removedPrefix.getFields());

        AccessPath newObj = new AccessPath(right.getBase(), newFields);
        System.out.println("ReplaceReferenceAssignment: " + newObj);
        return newObj;
    }

    public static AccessPath removePrefix(AccessPath prefix, AccessPath accessPath) {
        // assuming argument object is prefix
        int prefixSize = prefix.getFields().size();
        Collection<Field> accessPathFields = accessPath.getFields();
        List<Field> fieldList = new ArrayList<>(accessPathFields);
        List<Field> subList = fieldList.subList(prefixSize, fieldList.size());
        AccessPath removedPrefix = new AccessPath(prefix.getBase(), subList);
        return removedPrefix;
    }

    public static Set<AccessPathAnalysisObject> processInnerClassResults(Set<AccessPathAnalysisObject> results, SootClass activityClass) {
        Set<AccessPathAnalysisObject> retResults = new HashSet<>();

        for (AccessPathAnalysisObject analysisObject : results) {
            ArrayList<Field> accessPaths = new ArrayList(analysisObject.getAccessPath().getFields());
            for (int i = 0; i < accessPaths.size(); i++) {
                Field curr = accessPaths.get(i);
                if (curr.getSootField().getType().toString().equals(activityClass.getName())) {
                    int newSize = accessPaths.size() - i - 1;
                    List<Field> newFields = new ArrayList<>(newSize);
                    List<Field> subAccessPath = accessPaths.subList(i + 1, accessPaths.size());
                    newFields.addAll(0, subAccessPath);
                    AccessPath accessPath = new AccessPath(analysisObject.getAccessPath().getBase(), newFields);
                    AccessPathAnalysisObject newAnalysisObject = new AccessPathAnalysisObject(accessPath);
                    retResults.add(newAnalysisObject);
                }
            }
        }

        return retResults;
    }

    public static AccessPath concatenateField(AccessPath accessPath, SootField sootField) {
        List<Field> fields = new ArrayList<>();
        if (accessPath.getFields() != null) {
            fields = accessPath.getFields();
        }
        if (sootField != null)
            fields.add(new Field(sootField));
        AccessPath newObj = new AccessPath(accessPath.getBase(), fields);
        System.out.println("concatenateField : " + newObj);
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
            return (assignStmt.getLeftOp() instanceof FieldRef && (assignStmt.getRightOp() instanceof Local || assignStmt.getRightOp() instanceof Constant));
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

    public static AccessPath getAccessPathUsingMapping(Local local, SootMethod sootMethod, Unit curr, boolean queryNew, InterproceduralCFG<Unit, SootMethod> icfg) {
        if (!sootMethod.isStatic()) {
            Local thisLocal = sootMethod.retrieveActiveBody().getThisLocal();
            if (local != null && local.equivTo(thisLocal)) {
                AccessPath accessPath = new AccessPath(new Val(thisLocal, sootMethod));
                return accessPath;
            }
        }
        AnalysisObject mappedObject = findAliasInSet(local, sootMethod, curr, icfg);
        if (mappedObject != null) {
            if (queryNew) {
                if (mappedObject instanceof NewAllocAnalysisObject) {
                    AccessPath accessPath = new AccessPath(Val.zero());
                    return accessPath;
                }
                return null;
            } else {
                return analysisObjectToAccessPath(mappedObject, sootMethod);
            }
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
                AccessPath argAccessPath = Utils.getAccessPathUsingMapping(argLocal, callerMethod, stmt, false, icfg);
                if (argAccessPath != null && i < argumentAccessPaths.size())
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
        if (!argPathFields.isEmpty()) {
            Field firstField = argPathFields.get(0);
            List<Field> fieldList = new ArrayList<>(field.getFields().size() + 1);
            fieldList.add(firstField);
            fieldList.addAll(1, field.getFields());
            Val base = new Val(invokeLocal, callerMethod);
            AccessPath addedPrefix = new AccessPath(base, fieldList);
            return addedPrefix;
        } else {
//            List<Field> fieldList = new ArrayList<>(field.getFields().size());
//            fieldList.addAll(0, field.getFields());
            Val base = new Val(invokeLocal, callerMethod);
            AccessPath addedPrefix = new AccessPath(base, field.getFields());
            return addedPrefix;
        }

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
                if (argLocal != null && i < argsLocals.size())
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


    public static void printResultsForMethod(JimpleIFDSSolver<?, InterproceduralCFG<Unit, SootMethod>> solver, InterproceduralCFG<Unit, SootMethod> icfg, SootMethod method) {

        System.out.println("Printing Results at:: " + method.getSubSignature());
        for (Unit unit : method.retrieveActiveBody().getUnits()) {
            Set<?> result = solver.ifdsResultsAt(unit);
            System.out.print(unit + "\t>>\t" + result);
            //FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResult(method).getFlowAfter(unit).getAliasesStateFlowSet();
            //System.out.print("\t Aliases::" + aliases);
            System.out.println();
        }
        System.out.println("\t*********\t*************\t***********");
    }

    /**
     * @param results calculated by the callback
     * @return Apply JOIN-Rule (part to remove parents if child is included) and return the set
     */
    public static Set<FieldOnlyAccessPath> removeParentIfChildExist(Set<FieldOnlyAccessPath> results) {
        Set<FieldOnlyAccessPath> toRemove = new HashSet<>();
        // n to n comparison, avoid self comparison
        for (FieldOnlyAccessPath analysisObjectOuter : results) {
            for (FieldOnlyAccessPath analysisObjectInner : results) {
                if (analysisObjectOuter.equals(analysisObjectInner))
                    continue;
                if (isPrefixAccessPath(analysisObjectOuter, analysisObjectInner)) {
                    toRemove.add(analysisObjectOuter);
                }
            }
        }
        Set<FieldOnlyAccessPath> tmp = new HashSet<>(results);
        tmp.removeAll(toRemove);
        return tmp;
    }

    public static Set<FieldOnlyAccessPath> performSpecialUnion(Set<FieldOnlyAccessPath> set1, Set<AccessPathAnalysisObject> param2, boolean skipChildRemoval) {

        Set<FieldOnlyAccessPath> set2 = new HashSet<>();
        param2.forEach(ap -> {
            FieldOnlyAccessPath newAp = new FieldOnlyAccessPath(ap.getAccessPath().getFields());
            set2.add(newAp);
        });

        Set<FieldOnlyAccessPath> set1AfterChildRemoved = set1;
        if (!skipChildRemoval)// if set1 is empty or already action performed, just skip it.
            set1AfterChildRemoved = removeParentIfChildExist(set1);
        Set<FieldOnlyAccessPath> set2AfterChildRemoved = removeParentIfChildExist(set2);

        Set<FieldOnlyAccessPath> unionSet = new HashSet<>();
        Sets.SetView<FieldOnlyAccessPath> resultUnion = Sets.union(set1AfterChildRemoved, set2AfterChildRemoved);
        resultUnion.copyInto(unionSet);

        Set<FieldOnlyAccessPath> result = removeParentIfChildExist(unionSet);
        return result;
    }

    public static <T> Set<T> clone(Set<T> original) {
        Set<T> copy = original.stream()
                .collect(Collectors.toSet());
        return copy;
    }

    public static boolean isPrefixAccessPath(FieldOnlyAccessPath prefix, FieldOnlyAccessPath accessPath) {
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

    public static Set<FieldOnlyAccessPath> plainUnion(Set<FieldOnlyAccessPath> set1, Set<AccessPathAnalysisObject> param2) {
        Set<FieldOnlyAccessPath> set2 = new HashSet<>();
        param2.forEach(ap -> {
            FieldOnlyAccessPath newAp = new FieldOnlyAccessPath(ap.getAccessPath().getFields());
            set2.add(newAp);
        });

        Set<FieldOnlyAccessPath> ret = new HashSet<>();
        Sets.SetView<FieldOnlyAccessPath> result = Sets.union(set1, set2);
        result.copyInto(ret);
        return ret;
    }


    public static Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> unionPointsTo(Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> set1, Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> set2) {
        Sets.SetView<FieldOnlyAccessPath> allKeys = Sets.union(set1.keySet(), set2.keySet());
        Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> result = new HashMap<>();

        for (FieldOnlyAccessPath key : allKeys) {
            Set<FieldOnlyAccessPath> set1Val = new HashSet<>();
            if (set1.containsKey(key)) {
                set1Val = set1.get(key);
            }

            Set<FieldOnlyAccessPath> set2Val = new HashSet<>();
            if (set2.containsKey(key)) {
                set2Val = set2.get(key);
            }

            Sets.SetView<FieldOnlyAccessPath> allVal = Sets.union(set1Val, set2Val);
            Set<FieldOnlyAccessPath> set = new HashSet<>(allVal);
            if (set.contains(key))
                set.remove(key);
            if (!set.isEmpty())
                result.put(key, set);
        }
        return result;
    }

    public static void createDummyNew(SootClass sootClass) {
        Type voidType = VoidType.v();
        SootMethod sootMethod = new SootMethod("dummy_PtsSpark", new LinkedList(), voidType);
        sootClass.addMethod(sootMethod);
        JimpleBody newBody = Jimple.v().newBody(sootMethod);

        sootMethod.setActiveBody(newBody);
        newBody.insertIdentityStmts();

        Chain units = newBody.getUnits();
        Unit insertPoint = (Unit) units.getLast();
        Local thisLocal = newBody.getThisLocal();
        Iterator<SootField> fieldsIt = sootClass.getFields().iterator();
        while (fieldsIt.hasNext()) {
            SootField field = fieldsIt.next();
            InstanceFieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef());
            Local fieldLocal = Jimple.v().newLocal("local_" + field.getName(), field.getType());
            newBody.getLocals().add(fieldLocal);
            AssignStmt loadStmt = Jimple.v().newAssignStmt(fieldLocal, fieldRef);
            units.insertAfter(loadStmt, insertPoint);
            insertPoint = newBody.getUnits().getSuccOf(insertPoint);
            SootClass fieldClass = Scene.v().getSootClass(field.getType().toString());
            if (fieldClass != null && fieldClass.getName().startsWith("edu.ucr.cs.ufarooq.simpleflow")) {
                Iterator<SootField> fieldFieldsIt = fieldClass.getFields().iterator();
                while (fieldFieldsIt.hasNext()) {
                    SootField fieldField = fieldFieldsIt.next();
                    System.out.println("Instance: " + fieldLocal + field.getName() + ":" + fieldField.getName());
                    InstanceFieldRef fieldFieldRef = Jimple.v().newInstanceFieldRef(fieldLocal, fieldField.makeRef());
                    Local fieldFieldLocal = Jimple.v().newLocal("local_" + field.getName() + "_" + fieldField.getName(), fieldFieldRef.getType());
                    newBody.getLocals().add(fieldFieldLocal);
                    AssignStmt loadFieldStmt = Jimple.v().newAssignStmt(fieldFieldLocal, fieldFieldRef);
                    units.insertAfter(loadFieldStmt, insertPoint);
                    insertPoint = newBody.getUnits().getSuccOf(insertPoint);
                }
            }
        }

        ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
        units.insertAfter(returnStmt, insertPoint);
    }

    public static void addDummyToCallback(SootMethod sootMethod) {

        JimpleBody newBody = (JimpleBody) sootMethod.retrieveActiveBody();//Jimple.v().newBody(sootMethod);
        Chain units = newBody.getUnits();
        Unit insertPoint = (Unit) units.getLast();
        Local thisLocal = null;
        try {
            thisLocal = newBody.getThisLocal();
        } catch (Exception e) {
            thisLocal = null;
        }
        if (thisLocal != null) {
            Chain<SootField> fields = sootMethod.getDeclaringClass().getFields();
            if (!fields.isEmpty()) {
                SootField firstField = fields.getFirst();
                Local firstLocal = Jimple.v().newLocal("local_" + firstField.getName(), firstField.getType());
                boolean insertedAlready = newBody.getLocals().contains(firstLocal);
                for (Local l : newBody.getLocals()) {
                    if (l.getName().equalsIgnoreCase(firstLocal.getName())) {
                        insertedAlready = true;
                        break;
                    }
                }
                System.out.println("insertedAlready: " + insertedAlready);
                if (!insertedAlready) {
                    Iterator<SootField> fieldsIt = sootMethod.getDeclaringClass().getFields().iterator();
                    while (fieldsIt.hasNext()) {
                        SootField field = fieldsIt.next();
                        if (!isPrimitiveType(field.getType()) && !field.isStatic()) {
                            if (!field.getType().toString().startsWith("android.") && !field.getType().toString().startsWith("java.")) {
                                InstanceFieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef());
                                Local fieldLocal = Jimple.v().newLocal(DUMMY_LOCAL_PREFIX + field.getName(), field.getType());
                                newBody.getLocals().add(fieldLocal);
                                AssignStmt loadStmt = Jimple.v().newAssignStmt(fieldLocal, fieldRef);
                                units.insertBefore(loadStmt, insertPoint);
                                Set<Pair<SootClass, SootField>> existingSet = new HashSet<>();
                                existingSet.add(new ImmutablePair<>(field.getDeclaringClass(), field));
                                createAndInsert(newBody, units, insertPoint, fieldLocal, field, 1, existingSet);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void createAndInsert(JimpleBody newBody, Chain units, Unit insertPoint, Local fieldLocal, SootField field, int level, Set<Pair<SootClass, SootField>> existingSet) {
        SootClass fieldClass = Scene.v().getSootClass(field.getType().toString());
        // avoid java and Android objects, h
        /*SootClass activityClass = Scene.v().getSootClass("android.app.Activity");
        SootClass broadcastReceiverClass = Scene.v().getSootClass("android.content.BroadcastReceiver");
        boolean isActivity = SootUtilities.derivesFrom(fieldClass, activityClass);
        boolean isBroadCast = SootUtilities.derivesFrom(fieldClass, broadcastReceiverClass);
        if(!isActivity && !isBroadCast) {*/
        System.out.println("createAndInsert: " + field.getName() + ":" + field.getDeclaringClass());
        if (fieldClass != null && !field.getType().toString().startsWith("android.") && !field.getType().toString().startsWith("java.")) {
            Iterator<SootField> fieldFieldsIt = fieldClass.getFields().iterator();
            while (fieldFieldsIt.hasNext()) {
                SootField fieldField = fieldFieldsIt.next();
                Pair<SootClass, SootField> pair = new ImmutablePair<>(fieldField.getDeclaringClass(), fieldField);
                if (!existingSet.contains(pair)) {
                    if (!isPrimitiveType(fieldField.getType()) && !fieldField.isStatic()) {
                        if (!fieldField.getType().toString().startsWith("android.") && !fieldField.getType().toString().startsWith("java.")) {
                            InstanceFieldRef fieldFieldRef = Jimple.v().newInstanceFieldRef(fieldLocal, fieldField.makeRef());
                            Local fieldFieldLocal = Jimple.v().newLocal(fieldLocal.getName() + "_" + fieldField.getName(), fieldFieldRef.getType());
                            if (!fieldField.getName().startsWith("this$") && !fieldFieldRef.getType().equals(field.getType()) && !newBody.getLocals().contains(fieldFieldLocal)) {
                                System.out.println("fieldField: " + fieldField.getName() + ":" + fieldField.getType() + ":" + fieldFieldLocal);
                                newBody.getLocals().add(fieldFieldLocal);
                                AssignStmt loadFieldStmt = Jimple.v().newAssignStmt(fieldFieldLocal, fieldFieldRef);
                                units.insertBefore(loadFieldStmt, insertPoint);
                                existingSet.add(new ImmutablePair<>(fieldField.getDeclaringClass(), fieldField));
                                //if (level < 10)
                                createAndInsert(newBody, units, insertPoint, fieldFieldLocal, fieldField, level++, existingSet);
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    public static AccessPath getAccessPathUsingMappingLatest(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        if (!sootMethod.isStatic()) {
            Local thisLocal = sootMethod.retrieveActiveBody().getThisLocal();
            if (local.equivTo(thisLocal)) {
                AccessPath accessPath = new AccessPath(new Val(thisLocal, sootMethod));
                return accessPath;
            }
        }
        AnalysisObject mappedObject = findAliasInSetLatest(local, sootMethod, curr, icfg);
        if (mappedObject != null) {
            return analysisObjectToAccessPath(mappedObject, sootMethod);

        }
        return null;
    }

    private static AnalysisObject findAliasInSetLatest(Local local, SootMethod sootMethod, Unit curr, InterproceduralCFG<Unit, SootMethod> icfg) {
        FlowSet<AliasesState> aliases = MethodAliasProvider.getAliasResultLastest(sootMethod, curr, icfg).getFlowBefore(curr).getAliasesStateFlowSet();
        //System.out.println(aliases);
        if (aliases.contains(new AliasesState(local))) {
            List<AliasesState> list = aliases.toList();
            int indexOfObject = list.indexOf(new AliasesState(local));
            AliasesState aliaseState = list.get(indexOfObject);
            if (!AliasesState.isNewAllocation(aliaseState)) {
                AnalysisObject classField = list.get(indexOfObject).getField();
                //System.out.println("Index of Object: " + indexOfObject + ":" + classField);
                return classField;
            } else {
                return new NewAllocAnalysisObject();
            }
        }
        return null;
    }
}