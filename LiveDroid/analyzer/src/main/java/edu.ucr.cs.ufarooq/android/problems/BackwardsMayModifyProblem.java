package edu.ucr.cs.ufarooq.android.problems;

import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import edu.ucr.cs.ufarooq.accessPath.AccessPathAnalysisObject;
import edu.ucr.cs.ufarooq.accessPath.Val;
import edu.ucr.cs.ufarooq.config.Configuration;
import edu.ucr.cs.ufarooq.pointsto.SparkPointsTo;
import edu.ucr.cs.ufarooq.providers.CallReturnEdgesInfo;
import edu.ucr.cs.ufarooq.providers.CallReturnEdgesProvider;
import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;
import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.toDex.TemporaryRegisterLocal;

import java.util.*;

public class BackwardsMayModifyProblem extends DefaultJimpleIFDSTabulationProblem<AccessPathAnalysisObject, InterproceduralCFG<Unit, SootMethod>> {

    private InterproceduralCFG<Unit, SootMethod> icfg;

    public BackwardsMayModifyProblem(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
        this.icfg = icfg;
    }

    @Override
    public FlowFunctions<Unit, AccessPathAnalysisObject, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, AccessPathAnalysisObject, SootMethod>() {
            @Override
            public FlowFunction<AccessPathAnalysisObject> getNormalFlowFunction(Unit curr, Unit next) {
                if (curr.getUseAndDefBoxes().isEmpty()) {
                    return Identity.v();
                }

                if (Utils.isLoadInst(curr)) {// todo: we might need to treat specially, add only if children not in set
                    /*
                    $u1 = $u0.<edu.ucr.cs.ufarooq.simpleflow.A: edu.ucr.cs.ufarooq.simpleflow.C c>
                    $u5.<edu.ucr.cs.ufarooq.simpleflow.MainActivity: edu.ucr.cs.ufarooq.simpleflow.C c1> = $u1*/
                    return Identity.v();
                }

                SootMethod sootMethod = interproceduralCFG().getMethodOf(curr);
                Set<Local> paramAndThisLocal = new HashSet<>();
                Local thisLocal = null;
                if (!sootMethod.isStatic()) {
                    thisLocal = sootMethod.retrieveActiveBody().getThisLocal();
                    paramAndThisLocal.add(thisLocal);
                }

                paramAndThisLocal.addAll(sootMethod.retrieveActiveBody().getParameterLocals());

                Set<AccessPathAnalysisObject> defs = new HashSet<>();
                final boolean isStoreStmt = Utils.isStoreInst(curr);

                if (curr instanceof DefinitionStmt) {
                    DefinitionStmt definitionStmt = (DefinitionStmt) curr;
                    Value leftOp = definitionStmt.getLeftOp();
                    Value rightOp = definitionStmt.getRightOp();
                    System.out.println("Current: " + definitionStmt);

                    if (isStoreStmt) {
                        System.out.println("Store: " + curr);
                        //l0.<BoomerangExampleTarget: int zz> = $stack29,  kill field
                        Local local = Utils.getLocalForFieldRef(((DefinitionStmt) curr).getFieldRef());
                        if (local != null) {
                            AccessPath accessPath = Utils.getAccessPathUsingMapping(local, sootMethod, curr, false, icfg);
                            System.out.println("Store (AccessPath): " + accessPath + " : " + local);
                            if (accessPath != null) {
                                SootField sootField = definitionStmt.getFieldRef().getField();
                                AccessPath newAccessPath = Utils.concatenateField(accessPath, sootField);
                                AccessPathAnalysisObject accessPathAnalysisObject = new AccessPathAnalysisObject(newAccessPath, local, curr);
                                defs.add(accessPathAnalysisObject);
                            }

                        }

                    }

                }


                return new FlowFunction<AccessPathAnalysisObject>() {
                    public Set<AccessPathAnalysisObject> computeTargets(AccessPathAnalysisObject source) {
                        Set<AccessPathAnalysisObject> killVars = new HashSet<AccessPathAnalysisObject>();
                        if (source != null) {
                            for (AccessPathAnalysisObject def : defs) {
                                System.out.println("IsPrefix: " + def + ":" + source);
                                boolean isPrefix = Utils.isPrefixAccessPath(def.getAccessPath(), source.getAccessPath());
                                if (isPrefix)
                                    return Collections.emptySet();
                            }
                        }
                        if (!defs.isEmpty()) {
                            killVars.addAll(defs);
                        }

                        if (source != null) {
                            System.out.println("NormalFlow (Source): " + source);
                            killVars.add(source);
                        }
                        return killVars;
                    }
                };
            }

            @Override
            public FlowFunction<AccessPathAnalysisObject> getCallFlowFunction(Unit callStmt, final SootMethod destinationMethod) {
                // Info: pass information from Call-site to method entry, in backward -- call to method return node
                System.out.println("CallFlow: " + callStmt);
                SootMethod callerMethod = interproceduralCFG().getMethodOf(callStmt);
                //Set<Local> argumentAndThisLocal = new HashSet<>();
                Local thisLocal = null;
                if (!callerMethod.isStatic()) {
                    try {
                        thisLocal = callerMethod.retrieveActiveBody().getThisLocal();
                        //argumentAndThisLocal.add(thisLocal);
                    } catch (Exception e) {
                        thisLocal = null;
                    }
                }
                Stmt stmt = (Stmt) callStmt;
                Local localForInvoke = null;
                AccessPath invokedAccessPath = null;
                if (!destinationMethod.isStatic()) {
                    localForInvoke = Utils.getLocalForInvoke(stmt.getInvokeExpr());
                    invokedAccessPath = Utils.getAccessPathUsingMapping(localForInvoke, callerMethod, callStmt, false, icfg);
                }
                //List<Value> argumentLocals = stmt.getInvokeExpr().getArgs();  //callerMethod.retrieveActiveBody().getParameterLocals();
                //argumentAndThisLocal.addAll(argumentLocals);
                List<AccessPath> argumentAccessPaths = Utils.getArgumentAccessPaths(stmt, callerMethod, icfg);
                List<Local> argsLocals = Utils.getArgumentLocals(stmt);

                if (Configuration.v().isEnablePointsTo()) {
                    for (Local arg : argsLocals) {
                        Set<AccessPath> aliases = SparkPointsTo.query(stmt, callerMethod, arg, icfg);
                        aliases.forEach(x -> System.out.println("Call Aliases: " + arg + ":" + x));
                    }
                }

                List<Local> paramLocals = Utils.getParameterLocals(destinationMethod);
                final AccessPath finalInvokedAccessPath = invokedAccessPath;
                final Local finalLocalForInvoke = localForInvoke;

                return new FlowFunction<AccessPathAnalysisObject>() {
                    public Set<AccessPathAnalysisObject> computeTargets(AccessPathAnalysisObject source) {
                        Set<AccessPathAnalysisObject> liveVars = new HashSet<AccessPathAnalysisObject>();
                        if (!destinationMethod.getName().equals("<clinit>") && !destinationMethod.getSubSignature().equals("void run()")) {
                            if (source != null && source.getAccessPath() != null) {
                                Value sourceBase = source.getAccessPath().getBase().value();
                                if (sourceBase instanceof Local) {
                                    Local baseLocal = (Local) sourceBase;

                                    for (int i = 0; i < argumentAccessPaths.size(); i++) {
                                        AccessPath argAccessPath = argumentAccessPaths.get(i);
                                        if (argAccessPath != null) {
                                            boolean isPrefix = Utils.isPrefixAccessPath(argAccessPath, source.getAccessPath());
                                            if (isPrefix) {
                                                System.out.println("Param Prefix: " + isPrefix);
                                                //split and remove prefixed path
                                                Local paramLocal = paramLocals.get(i);
                                                //Local argLocal = finalArgsLocals.get(i);
                                                AccessPath accessPath = Utils.removePrefix(argAccessPath, source.getAccessPath(), paramLocal, destinationMethod);
                                                AccessPathAnalysisObject formalParam = new AccessPathAnalysisObject(accessPath, paramLocal, callStmt);
                                                System.out.println("Formal Param: " + formalParam.getAccessPath());
                                                liveVars.add(formalParam);
                                                continue;
                                            }
                                        }
                                    }
                                    if (!callerMethod.isStatic() && !destinationMethod.isStatic() && finalInvokedAccessPath != null) {
                                        if (baseLocal.equivTo(finalLocalForInvoke)) {
                                            System.out.println("Prefix:" + finalLocalForInvoke + ":" + source.getAccessPath());
                                            Local destThisLocal = destinationMethod.retrieveActiveBody().getThisLocal();
                                            Val valBase = new Val(destThisLocal, destinationMethod);
                                            AccessPath accessPath = new AccessPath(valBase, source.getAccessPath().getFields());
                                            AccessPathAnalysisObject formalField = new AccessPathAnalysisObject(accessPath, baseLocal, callStmt);
                                            liveVars.add(formalField);
                                        } else {
                                            //add based on finalInvokedAccessPath
                                            boolean isPrefix = Utils.isPrefixAccessPath(finalInvokedAccessPath, source.getAccessPath());
                                            if (isPrefix) {
                                                System.out.println("Prefix:" + finalInvokedAccessPath + ":" + source.getAccessPath());
                                                Local destThisLocal = destinationMethod.retrieveActiveBody().getThisLocal();
                                                AccessPath accessPath = Utils.removePrefix(finalInvokedAccessPath, source.getAccessPath(), destThisLocal, destinationMethod);
                                                System.out.println("Prefix(removed):" + accessPath);
                                                AccessPathAnalysisObject formalSubField = new AccessPathAnalysisObject(accessPath, baseLocal, callStmt);
                                                liveVars.add(formalSubField);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return liveVars;
                    }
                };

                //return KillAll.v();
            }

            @Override
            public FlowFunction<AccessPathAnalysisObject> getReturnFlowFunction(final Unit callSite, SootMethod
                    calleeMethod, final Unit exitStmt, Unit returnSite) {
                // connects exit to return-site, applies results based on this,
                // in liveness, entry node to return site
                CallReturnEdgesInfo returnEdgesInfo = new CallReturnEdgesInfo(callSite, calleeMethod, exitStmt, returnSite);
                CallReturnEdgesProvider.insert(returnEdgesInfo);
                System.out.println("ReturnFlow: " + callSite + ":" + exitStmt);
                SootMethod callerMethod = interproceduralCFG().getMethodOf(callSite);
                //Set<Local> argumentAndThisLocal = new HashSet<>();
                Local thisLocal = null;
                if (!callerMethod.isStatic()) {
                    try {
                        thisLocal = calleeMethod.retrieveActiveBody().getThisLocal();
                        //argumentAndThisLocal.add(thisLocal);
                    } catch (Exception e) {
                        thisLocal = null;
                    }
                }
                Stmt stmt = (Stmt) callSite;
                Local localForInvoke = null;
                AccessPath invokedAccessPath = null;
                if (!calleeMethod.isStatic() && thisLocal != null) {
                    localForInvoke = Utils.getLocalForInvoke(stmt.getInvokeExpr());
                    invokedAccessPath = Utils.getAccessPathUsingMapping(localForInvoke, callerMethod, callSite, false, icfg);
                }
                //List<Value> argumentLocals = stmt.getInvokeExpr().getArgs();  //callerMethod.retrieveActiveBody().getParameterLocals();
                //argumentAndThisLocal.addAll(argumentLocals);
                List<AccessPath> argumentAccessPaths = Utils.getArgumentAccessPaths(stmt, callerMethod, icfg);
                List<Local> argsLocals = Utils.getArgumentLocals(stmt);
                List<Local> paramLocals = Utils.getParameterLocals(calleeMethod);
                Local finalThisLocal = thisLocal;
                Local finalLocalForInvoke = localForInvoke;
                AccessPath finalInvokedAccessPath = invokedAccessPath;

                return new FlowFunction<AccessPathAnalysisObject>() {
                    public Set<AccessPathAnalysisObject> computeTargets(AccessPathAnalysisObject source) {
                        Set<AccessPathAnalysisObject> liveVars = new HashSet<AccessPathAnalysisObject>();
                        if (!calleeMethod.getName().equals("<clinit>") && !calleeMethod.getSubSignature().equals("void run()")) {
                            if (source != null && source.getAccessPath() != null) {
                                Value sourceBase = source.getAccessPath().getBase().value();
                                System.out.println("ReturnFlow: " + source.getAccessPath() + ":" + finalThisLocal + ":" + sourceBase);
                                if (sourceBase instanceof Local) {
                                    Local baseLocal = (Local) sourceBase;

                                    if (paramLocals.contains(baseLocal)) {
                                        int parameterIndex = paramLocals.indexOf(baseLocal);
                                        //Local argLocal = argsLocals.get(parameterIndex);
                                        if(argumentAccessPaths!=null && parameterIndex< argumentAccessPaths.size()) {
                                            AccessPath argAccessPath = argumentAccessPaths.get(parameterIndex);

                                            AccessPath convertedAccessPath = Utils.formalToActualParameterConversion(argAccessPath, source.getAccessPath());
                                            AccessPathAnalysisObject convertedField = new AccessPathAnalysisObject(convertedAccessPath, baseLocal, exitStmt);
                                            liveVars.add(convertedField);
                                        }
                                    }

                                    if (!callerMethod.isStatic() && !calleeMethod.isStatic() && finalInvokedAccessPath != null) {
                                        System.out.println("ReturnFlow: " + finalThisLocal + ":" + baseLocal + ":" + baseLocal.equivTo(finalThisLocal));
                                        if (baseLocal.equivTo(finalThisLocal) && finalInvokedAccessPath != null) {
                                            System.out.println("Prefix:" + finalLocalForInvoke + ":" + source.getAccessPath() + ":" + finalInvokedAccessPath);

                                            AccessPath fieldAccessPath = Utils.formalToActualSubfieldConversion(finalInvokedAccessPath, source.getAccessPath(), callerMethod);
                                            AccessPathAnalysisObject actualField = new AccessPathAnalysisObject(fieldAccessPath, baseLocal, callSite);
                                            liveVars.add(actualField);
                                        } else if (baseLocal.equivTo(finalThisLocal) && callerMethod.getDeclaringClass().equals(calleeMethod.getDeclaringClass())) {
                                            Local callerThis = callerMethod.retrieveActiveBody().getThisLocal();
                                            Val base = new Val(callerThis, callerMethod);
                                            AccessPath accessPath = new AccessPath(base, source.getAccessPath().getFields());
                                            AccessPathAnalysisObject newField = new AccessPathAnalysisObject(accessPath, baseLocal, callSite);
                                            liveVars.add(newField);
                                        }
                                    }
                                }
                            }
                        }
                        return liveVars;
                    }
                };
            }

            @Override
            public FlowFunction<AccessPathAnalysisObject> getCallToReturnFlowFunction(Unit callSite, Unit
                    returnSite) {
                SootMethod callerMethod = interproceduralCFG().getMethodOf(callSite);
                //Set<Local> argumentAndThisLocal = new HashSet<>();
                Local thisLocal = null;
                if (!callerMethod.isStatic()) {
                    try {
                        thisLocal = callerMethod.retrieveActiveBody().getThisLocal();
                        //argumentAndThisLocal.add(thisLocal);
                    } catch (Exception e) {
                        thisLocal = null;
                    }
                }
                Stmt stmt = (Stmt) callSite;
                SootMethod destinationMethod = stmt.getInvokeExpr().getMethod();
                Local localForInvoke = null;
                AccessPath invokedAccessPath = null;
                if (!destinationMethod.isStatic() && thisLocal != null) {
                    localForInvoke = Utils.getLocalForInvoke(stmt.getInvokeExpr());
                    invokedAccessPath = Utils.getAccessPathUsingMapping(localForInvoke, callerMethod, callSite, false, icfg);
                }
                //List<Value> argumentLocals = stmt.getInvokeExpr().getArgs();  //callerMethod.retrieveActiveBody().getParameterLocals();
                //argumentAndThisLocal.addAll(argumentLocals);
                List<AccessPath> argumentAccessPaths = Utils.getArgumentAccessPaths(stmt, callerMethod, icfg);
                List<Local> argsLocals = Utils.getArgumentLocals(stmt);
                //List<Local> paramLocals = Utils.getParameterLocals(destinationMethod);
                final AccessPath finalInvokedAccessPath = invokedAccessPath;
                final Local finalLocalForInvoke = localForInvoke;

                return new FlowFunction<AccessPathAnalysisObject>() {
                    public Set<AccessPathAnalysisObject> computeTargets(AccessPathAnalysisObject source) {
                        Set<AccessPathAnalysisObject> liveVars = new HashSet<AccessPathAnalysisObject>();
                        if (!destinationMethod.getName().equals("<clinit>") && !destinationMethod.getSubSignature().equals("void run()")) {
                            if (source != null && source.getAccessPath() != null) {
                                Value sourceBase = source.getAccessPath().getBase().value();
                                if (sourceBase instanceof Local) {
                                    Local baseLocal = (Local) sourceBase;
                                    if (Configuration.v().isEnableParametersTrackReference()) {
                                        for (int i = 0; i < argumentAccessPaths.size(); i++) {
                                            AccessPath argAccessPath = argumentAccessPaths.get(i);
                                            if (argAccessPath != null) {
                                                boolean isPrefix = Utils.isPrefixAccessPath(argAccessPath, source.getAccessPath());
                                                if (isPrefix) {
                                                    System.out.println("CallToReturn: " + source.getAccessPath() + ":" + argAccessPath);
                                                    return Collections.emptySet();
                                                }
                                            }
                                        }
                                    } else {//track pointers, find all pointers and pass along the path

                                    }
                                    if (!callerMethod.isStatic() && !destinationMethod.isStatic() && finalInvokedAccessPath != null) {
                                        if (baseLocal.equivTo(finalLocalForInvoke)) {
                                            return Collections.emptySet();
                                        } else {
                                            //add based on finalInvokedAccessPath
                                            boolean isPrefix = Utils.isPrefixAccessPath(finalInvokedAccessPath, source.getAccessPath());
                                            if (isPrefix) {
                                                Collections.emptySet();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        liveVars.add(source);
                        return liveVars;
                    }
                };

                //return KillAll.v();
            }
        };
    }

    @Override
    protected AccessPathAnalysisObject createZeroValue() {
        return new AccessPathAnalysisObject(null, new TemporaryRegisterLocal(NullType.v()), null);
    }

    @Override
    public Map<Unit, Set<AccessPathAnalysisObject>> initialSeeds() {
        return DefaultSeeds.make(interproceduralCFG().getStartPointsOf(Scene.v().getMainMethod()), zeroValue());
    }


    public String getLable() {
        return "MayModifyProblem";
    }
}
