package edu.ucr.cs.ufarooq.android.connection;

import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import edu.ucr.cs.ufarooq.accessPath.Field;
import edu.ucr.cs.ufarooq.android.problems.Utils;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

import java.util.*;

public class ConnectionReachableUtils {

    private final SootMethod method;

    private final JimpleBasedInterproceduralCFG icfg;
    private List<String> avoidCallsFrom;


    public ConnectionReachableUtils(SootMethod method, JimpleBasedInterproceduralCFG icfg) {
        this.method = method;
        this.icfg = icfg;

        avoidCallsFrom = new ArrayList<>();
        avoidCallsFrom.add("void <init>()");
        avoidCallsFrom.add("void run()");
    }

    public FlowSet<ReachableConnectionMethodWrapper> putEdgesInStack(SootMethod method, FlowSet<ReachableConnectionMethodWrapper> edgeFlowSet) {
        //stack.push(method);
        //Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(method);
        if (method.hasActiveBody()) {
            Set<Unit> calls = icfg.getCallsFromWithin(method);
            for (Unit callSite : calls) {
                Stmt stmt = (Stmt) callSite;
                SootMethod sootMethod = stmt.getInvokeExpr().getMethod();
                //System.out.println("Callsite: " + callSite + " -> " + sootMethod.getSignature());
                // todo: temp fix for Android calls
                ReachableConnectionMethodWrapper tmp = new ReachableConnectionMethodWrapper(this.method, sootMethod, callSite, method);
                putInSet(tmp, edgeFlowSet);
                //Edge edge = edges.next();
                Collection<SootMethod> callees = icfg.getCalleesOfCallAt(callSite);
                for (SootMethod callee : callees) {
                    ReachableConnectionMethodWrapper wrapper = new ReachableConnectionMethodWrapper(this.method, callee, callSite, method);
                    //System.out.println("Callee: " + callee.getSignature());
                    putInSet(wrapper, edgeFlowSet);
                }
            }
        }
        return edgeFlowSet;
    }

    private void putInSet(ReachableConnectionMethodWrapper wrapper, FlowSet<ReachableConnectionMethodWrapper> edgeFlowSet) {
        if (!edgeFlowSet.contains(wrapper)) {
            //System.out.println("Callee: " + wrapper.getMethod().getSignature());
            if (!wrapper.getMethod().getName().equals("<init>") && !wrapper.getMethod().getSubSignature().equals("void run()")) {
                edgeFlowSet.add(wrapper);
                putEdgesInStack(wrapper.getMethod(), edgeFlowSet);
            }
        }
    }

    public Set<ReachableConnectionMethodWrapper> getReachableConnectionEdges() {
        Set<ReachableConnectionMethodWrapper> reachableViewEdges = new HashSet<>();
        FlowSet<ReachableConnectionMethodWrapper> edgeFlowSet = new ArraySparseSet<>();
        Set<SootMethod> allConnectionRelated = new HashSet<>(ConnectionAPIs.getInstance().bindingMethods.size() + ConnectionAPIs.getInstance().unBindingMethods.size() + ConnectionAPIs.getInstance().registerMethods.size() + ConnectionAPIs.getInstance().unregisterMethods.size());

        allConnectionRelated.addAll(ConnectionAPIs.getInstance().bindingMethods);
        allConnectionRelated.addAll(ConnectionAPIs.getInstance().unBindingMethods);
        allConnectionRelated.addAll(ConnectionAPIs.getInstance().registerMethods);
        allConnectionRelated.addAll(ConnectionAPIs.getInstance().unregisterMethods);
        FlowSet<ReachableConnectionMethodWrapper> edges = putEdgesInStack(method, edgeFlowSet);
        edges.forEach(edge -> {
            SootMethod sootMethod = edge.getMethod();
            if (allConnectionRelated.contains(sootMethod)) {
                reachableViewEdges.add(edge);
            }
        });
        return reachableViewEdges;
    }

    public static Set<AbsConnection> getReachableConnections(FlowSet<SootMethod> methods, JimpleBasedInterproceduralCFG icfg) {
        //System.out.println(method.getName());
//        SootClass textView = Scene.v().getSootClass("android.widget.TextView");
//        SootMethod seText = textView.getMethodUnsafe("void setText(java.lang.CharSequence)");
//        SootMethod seTextInt = textView.getMethodUnsafe("void setText(int)");
//        SootMethod seTextColor = textView.getMethodUnsafe("void setTextColor(int)");
//        Set<SootMethod> set = new HashSet<>();
//        set.add(seText);
//        set.add(seTextInt);
//        set.add(seTextColor);

        Set<ReachableConnectionMethodWrapper> edges = new HashSet<>();
        methods.forEach(method -> {
            ConnectionReachableUtils reachableUtils = new ConnectionReachableUtils(method, icfg);
            //all edges related to connection, further filter them out
            Set<ReachableConnectionMethodWrapper> currentEdges = reachableUtils.getReachableConnectionEdges();
            System.out.println("Reachable Edges: " + method.getName() + ": " + currentEdges.size());
            currentEdges.forEach(e -> {
                System.out.println(e.getMethod() + ":" + e.getCallSite().toString());
            });

            edges.addAll(currentEdges);
        });


        Set<AbsConnection> connections = new HashSet<>();

        // process closing
        edges.forEach(edge -> {
            if (ConnectionAPIs.getInstance().unBindingMethods.contains(edge.getMethod())) {

                Unit callSite = edge.getCallSite();
                SootMethod invokingMethod = icfg.getMethodOf(callSite);
                Stmt stmt = (Stmt) callSite;

                List<AccessPath> argumentAccessPaths = new ArrayList<>(stmt.getInvokeExpr().getArgCount());
                for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
                    Value argVal = stmt.getInvokeExpr().getArg(i);
                    if (argVal instanceof Local) {
                        Local argLocal = (Local) argVal;
                        System.out.println("Arg Local:" + argLocal);
                        AccessPath argAccessPath = Utils.getAccessPathUsingMapping(argLocal, invokingMethod, stmt, true, icfg);
                        if (argAccessPath != null && i < argumentAccessPaths.size())
                            argumentAccessPaths.add(i, argAccessPath);
                    }
                }
                //List<AccessPath> argumentAccessPaths = Utils.getArgumentAccessPaths(stmt, invokingMethod, icfg);

                argumentAccessPaths.forEach(ap -> {
                    System.out.println("Access Path:" + ap.toString());
                });

                if (stmt.getInvokeExpr().getArgCount() == 1) {
                    if (!argumentAccessPaths.isEmpty()) {
                        AccessPath arg0 = argumentAccessPaths.get(0);
                        System.out.println("Single Argument: " + arg0.toString());
                        ServiceConnection serviceConnection = new ServiceConnection(arg0, edge.getCallback(), callSite, AbsConnection.Connection.CLOSING);
                        connections.add(serviceConnection);
                    } else {
                        if (stmt.getInvokeExpr().getArgs().get(0) instanceof Local) {
                            Local local = (Local) stmt.getInvokeExpr().getArgs().get(0);
                            ServiceConnection serviceConnection = new ServiceConnection(local.getType(), edge.getCallback(), callSite, AbsConnection.Connection.CLOSING);
                            connections.add(serviceConnection);
                        }
                    }
                }
            } else if (ConnectionAPIs.getInstance().unregisterMethods.contains(edge.getMethod())) {
                Unit callSite = edge.getCallSite();
                SootMethod invokingMethod = icfg.getMethodOf(callSite);
                Stmt stmt = (Stmt) callSite;
                List<AccessPath> argumentAccessPaths = Utils.getArgumentAccessPaths(stmt, invokingMethod, icfg);

                if (stmt.getInvokeExpr().getArgCount() == 1) {
                    if (!argumentAccessPaths.isEmpty()) {
                        AccessPath arg0 = argumentAccessPaths.get(0);
                        BroadcastConnection serviceConnection = new BroadcastConnection(arg0, edge.getCallback(), callSite, AbsConnection.Connection.CLOSING);
                        connections.add(serviceConnection);
                    } else {
                        if (stmt.getInvokeExpr().getArgs().get(0) instanceof Local) {
                            Local local = (Local) stmt.getInvokeExpr().getArgs().get(0);
                            BroadcastConnection serviceConnection = new BroadcastConnection(local.getType(), edge.getCallback(), callSite, AbsConnection.Connection.CLOSING);
                            connections.add(serviceConnection);
                        }
                    }
                }
            }
        });

        //process only opening and connect with closing parts
        edges.forEach(edge -> {
            if (ConnectionAPIs.getInstance().bindingMethods.contains(edge.getMethod())) {
                Unit callSite = edge.getCallSite();
                SootMethod invokingMethod = icfg.getMethodOf(callSite);
                Stmt stmt = (Stmt) callSite;
                InvokeExpr invokeExpr = stmt.getInvokeExpr();

                connections.forEach(connection -> {
                    if (!connection.isConnectionComplete() && connection instanceof ServiceConnection) {
                        ServiceConnection serviceConnection = (ServiceConnection) connection;
                        Type connectionType = serviceConnection.getConnectionType();
                        for (Value value : stmt.getInvokeExpr().getArgs()) {
                            if (value instanceof Local) {
                                Local local = (Local) value;
                                if (local.getType().equals(connectionType)) {
                                    serviceConnection.setOpeningStmt(callSite);
                                    serviceConnection.setOpeningCallback(edge.getCallback());
                                }
                            }
                        }
                    }
                });

            } else if (ConnectionAPIs.getInstance().registerMethods.contains(edge.getMethod())) {
                Unit callSite = edge.getCallSite();

                Stmt stmt = (Stmt) callSite;
                connections.forEach(connection -> {
                    if (!connection.isConnectionComplete() && connection instanceof BroadcastConnection) {
                        BroadcastConnection broadcastConnection = (BroadcastConnection) connection;
                        Type connectionType = broadcastConnection.getConnectionType();
                        for (Value value : stmt.getInvokeExpr().getArgs()) {
                            if (value instanceof Local) {
                                Local local = (Local) value;
                                if (local.getType().equals(connectionType)) {
                                    broadcastConnection.setOpeningStmt(callSite);
                                    broadcastConnection.setOpeningCallback(edge.getCallback());
                                }
                            }
                        }
                    }
                });
            }
        });

        Set<AbsConnection> completeConnection = new HashSet<>();

        System.out.println("All Connections Size: " + connections.size());
        connections.forEach(connection -> {
            System.out.println(connection.toString());
            if (connection.isConnectionComplete()) {
                completeConnection.add(connection);
            }
        });

        return completeConnection;

    }
}
