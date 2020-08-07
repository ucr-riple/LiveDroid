package edu.ucr.cs.ufarooq.intra;

import edu.ucr.cs.ufarooq.config.Configuration;
import edu.ucr.cs.ufarooq.model.AnalysisObject;
import edu.ucr.cs.ufarooq.model.ClassField;
import edu.ucr.cs.ufarooq.model.ParameterAnalysisObject;
import edu.ucr.cs.ufarooq.model.ReturnAnalysisObject;
import edu.ucr.cs.ufarooq.util.Utils;
import heros.InterproceduralCFG;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.NewExpr;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.Collection;
import java.util.List;

// not return objects, no parameter analysis
public class MethodAliasAnalysis
        extends ForwardFlowAnalysis<Unit, AliasAnalysisResult> {

    AliasAnalysisResult allLocals;
    UnitGraph unitGraph;
    InterproceduralCFG<Unit, SootMethod> icfg;
    private int currentGetCount;
    private Local thisLocal;

    public MethodAliasAnalysis(UnitGraph g, InterproceduralCFG<Unit, SootMethod> icfg) {
        super(g);
        unitGraph = g;
        allLocals = new AliasAnalysisResult();
        this.icfg = icfg;
        currentGetCount = 0;
//        for (Value ref : g.getBody().getParameterRefs()) {
//            System.out.println("ref: " + ref);
//        }
//        for (Local loc : g.getBody().getLocals()) {
//            allLocals.add(new AliasesState(loc));
//        }
        thisLocal = null;
        if (!unitGraph.getBody().getMethod().isStatic())
            thisLocal = unitGraph.getBody().getThisLocal();
//        if (unitGraph.getBody().getMethod().getDeclaringClass().isInnerClass()) {
//            SootClass outterClass = unitGraph.getBody().getMethod().getDeclaringClass().getOuterClass();
//            Chain<Local> locals = unitGraph.getBody().getLocals();
//            for (Local local : locals) {
//                if (Utils.isPrimitiveType(local.getType())) {
//                    SootClass localClass = Scene.v().getSootClass(local.getType().toString());
//                    if (localClass == outterClass) {
//                        thisLocal = local;
//                        break;
//                    }
//                }
//            }
//        }
        doAnalysis();
    }

    public MethodAliasAnalysis(UnitGraph g, InterproceduralCFG<Unit, SootMethod> icfg, int currentCount) {
        this(g, icfg);
        currentGetCount = currentCount;
    }

    @Override
    protected AliasAnalysisResult entryInitialFlow() {
        return new AliasAnalysisResult();
    }

    @Override
    protected AliasAnalysisResult newInitialFlow() {

        FlowSet<AliasesState> ret = new ArraySparseSet<AliasesState>();
        allLocals.getAliasesStateFlowSet().copy(ret);
        return new AliasAnalysisResult(ret);
    }

    @Override
    protected void flowThrough(AliasAnalysisResult in, Unit unit, AliasAnalysisResult out) {
        in.getAliasesStateFlowSet().copy(out.getAliasesStateFlowSet());

//        Local thisLocal = null;
//        if (!unitGraph.getBody().getMethod().isStatic())
//            thisLocal = unitGraph.getBody().getThisLocal();

        if (unit instanceof DefinitionStmt) {
            DefinitionStmt assignStmt = (DefinitionStmt) unit;
            //remove if local is re-defined, remove from to watch
            if (assignStmt.getLeftOp() instanceof Local) {
                Local local = (Local) assignStmt.getLeftOp();
                if (out.getAliasesStateFlowSet().contains(new AliasesState(local))) {
                    out.getAliasesStateFlowSet().remove(new AliasesState(local));
                }
            }

            if (assignStmt.getRightOp() instanceof NewExpr) {
                //System.out.println("NewExpr (MethodAlias): " + unit);
                Local leftLocal = (Local) assignStmt.getLeftOp();
                AliasesState newAllocationState = AliasesState.getNewAllocation(leftLocal);
                out.getAliasesStateFlowSet().add(newAllocationState);
            }
            if (assignStmt.containsInvokeExpr() && currentGetCount < Configuration.v().getReturnDepth()) {
                if (assignStmt.getLeftOp() instanceof Local) {
                    Local assignLocal = (Local) assignStmt.getLeftOp();
                    if (!Utils.isPrimitiveType(assignLocal.getType())) {
                        Local invokeLocal = Utils.getLocalForInvoke(assignStmt.getInvokeExpr());
                        if (out.getAliasesStateFlowSet().contains(new AliasesState(invokeLocal))) {
                            Collection<SootMethod> callees = icfg.getCalleesOfCallAt(assignStmt);
                            for (SootMethod callee : callees) {
                                AliasesState aliasesState = Utils.getReturnAliasesState(callee, icfg, currentGetCount + 1);
                                if (aliasesState != null) {
                                    List<AliasesState> inList = in.getAliasesStateFlowSet().toList();
                                    int index = inList.indexOf(new AliasesState(invokeLocal));
                                    AliasesState invokeAliasesState = inList.get(index);
                                    if (invokeAliasesState.getField() instanceof ClassField && aliasesState.getField() instanceof ClassField) {
                                        ClassField invokeClassField = new ClassField((ClassField) invokeAliasesState.getField());
                                        ClassField returnClassField = new ClassField((ClassField) aliasesState.getField());

                                        returnClassField.appendAtStartField(invokeClassField);
                                        System.out.println(unit);
                                        System.out.println("ReturnedAliased: " + returnClassField.toString());
                                        AliasesState newReturnState = new AliasesState(returnClassField, assignLocal, assignLocal, unit, AliasType.ReturnAlias);
                                        out.getAliasesStateFlowSet().add(newReturnState);
                                    }

                                }
                            }
                        }
                    }
//                SootMethod callee = assignStmt.getInvokeExpr().getMethod();
//                if (Utils.canReturnReference(callee) && assignStmt.getLeftOp() instanceof Local) {
//                    Local local = (Local) assignStmt.getLeftOp();
//                    ReturnAnalysisObject returnAnalysisObject = new ReturnAnalysisObject(local, unit);
//                    out.getAliasesStateFlowSet().add(new AliasesState(returnAnalysisObject, local, local, unit, AliasType.ReturnAlias));
//                }
                }
            }
            if (assignStmt.getLeftOp() instanceof Local && assignStmt.getRightOp() instanceof FieldRef) {
                Local local = (Local) assignStmt.getLeftOp();
                FieldRef fieldRef = (FieldRef) assignStmt.getRightOp();
                Local localForFieldRef = Utils.getLocalForFieldRef(fieldRef);
                if (localForFieldRef != null && !localForFieldRef.equivTo(local)) {
                    if (localForFieldRef.equivTo(thisLocal)) {//class field
                        ClassField classField = new ClassField(fieldRef.getField());
                        out.getAliasesStateFlowSet().add(new AliasesState(classField, local, localForFieldRef, unit, AliasType.FieldAlias));
                    } else if (out.getAliasesStateFlowSet().contains(new AliasesState(localForFieldRef))) {//field of field
                        AnalysisObject classField = Utils.findAliasInSet(localForFieldRef, out.getAliasesStateFlowSet());
                        // keep separate type for each field based on parent/container
                        if (classField instanceof ReturnAnalysisObject) {
                            ReturnAnalysisObject retrievedObject = (ReturnAnalysisObject) classField;
                            ReturnAnalysisObject returnAnalysisObject = new ReturnAnalysisObject(retrievedObject);
                            returnAnalysisObject.insert(fieldRef.getField());
                            out.getAliasesStateFlowSet().add(new AliasesState(returnAnalysisObject, local, localForFieldRef, unit, AliasType.ReturnAlias));
                        } else if (classField instanceof ParameterAnalysisObject) {
                            ParameterAnalysisObject retrievedObject = (ParameterAnalysisObject) classField;
                            ParameterAnalysisObject parameterAnalysisObject = new ParameterAnalysisObject(retrievedObject);
                            parameterAnalysisObject.insert(fieldRef.getField());
                            out.getAliasesStateFlowSet().add(new AliasesState(parameterAnalysisObject, local, localForFieldRef, unit, AliasType.ParameterLocal));
                        } else if (classField instanceof ClassField) {
                            ClassField retrievedObject = (ClassField) classField;
                            ClassField newClassField = new ClassField(retrievedObject);
                            newClassField.insert(fieldRef.getField());
                            out.getAliasesStateFlowSet().add(new AliasesState(newClassField, local, localForFieldRef, unit, AliasType.FieldFieldAlias));
                        }
                    } else if (unitGraph.getBody().getParameterLocals().contains(localForFieldRef) && !Utils.isPrimitiveType(localForFieldRef.getType())) {// parameters local
                        int paramIndex = unitGraph.getBody().getParameterLocals().indexOf(localForFieldRef);
                        System.out.println();
                        ParameterAnalysisObject parameterAnalysisObject = new ParameterAnalysisObject(localForFieldRef, paramIndex, fieldRef.getField());
                        out.getAliasesStateFlowSet().add(new AliasesState(parameterAnalysisObject, local, localForFieldRef, unit, AliasType.ParameterLocal));
                    }
                } else {//static access
                    ClassField classField = new ClassField(fieldRef.getField());
                    out.getAliasesStateFlowSet().add(new AliasesState(classField, local, localForFieldRef, unit, AliasType.FieldAlias));
                }
            }
        }

    }

    @Override
    protected void merge
            (AliasAnalysisResult in1, AliasAnalysisResult in2, AliasAnalysisResult out) {
        in1.getAliasesStateFlowSet().union(in2.getAliasesStateFlowSet(), out.getAliasesStateFlowSet());

    }

    @Override
    protected void copy(AliasAnalysisResult source, AliasAnalysisResult dest) {
        source.getAliasesStateFlowSet().copy(dest.getAliasesStateFlowSet());

    }


}

