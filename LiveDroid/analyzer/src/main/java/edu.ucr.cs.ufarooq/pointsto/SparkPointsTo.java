package edu.ucr.cs.ufarooq.pointsto;

import com.google.common.graph.*;
import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import edu.ucr.cs.ufarooq.config.Configuration;
import edu.ucr.cs.ufarooq.util.Utils;
import heros.InterproceduralCFG;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;

import java.util.*;

public class SparkPointsTo {

    private static int getLineNumber(Unit s) {
        Iterator ti = s.getTags().iterator();
        while (ti.hasNext()) {
            Object o = ti.next();
            if (o instanceof LineNumberTag)
                return Integer.parseInt(o.toString());
        }
        return -1;
    }

    public static Set<AccessPath> query(Stmt stmt, SootMethod sootMethod, Local queryVariable, InterproceduralCFG<Unit, SootMethod> icfg) {
        Set<AccessPath> result = new HashSet<>();
        Set<Local> pointsToResult = getPointsSet(stmt, sootMethod, queryVariable);
        if (Configuration.v().isEnableSparkDebug()) {
            System.out.println(sootMethod.getName() + "[" + queryVariable + "]\t" + pointsToResult);
        }
        AccessPath mappingAlias = Utils.getAccessPathUsingMapping(queryVariable, sootMethod, stmt,icfg);
        if (mappingAlias != null)
            result.add(mappingAlias);
        for (Local pt : pointsToResult) {
            AccessPath ptAccessPath = Utils.getAccessPathUsingMapping(pt, sootMethod, stmt,icfg);
            if (ptAccessPath != null) {
                result.add(ptAccessPath);
                if (Configuration.v().isEnableSparkDebug()) {
                    System.out.println(sootMethod.getName() + "[" + queryVariable + "]" + "\t" + ptAccessPath);
                }
            }
        }
        return result;
    }

    private static Set<Local> getPointsSet(Stmt stmt, SootMethod sootMethod, Local local) {
        if (Configuration.v().isEnableSparkDebug())
            System.out.println("getPointsTo: " + local);
        int currentLine = getLineNumber(stmt);
        Map<Integer, Local> locals = getLocals(sootMethod, local.getType());
        soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        Set<Local> pointsToResult = new HashSet<>();
        MutableGraph<Local> graph = GraphBuilder.undirected().build();

        Iterator i1 = locals.entrySet().iterator();
        while (i1.hasNext()) {
            Map.Entry e1 = (Map.Entry) i1.next();
            int p1 = ((Integer) e1.getKey()).intValue();
            Local l1 = (Local) e1.getValue();
            PointsToSet r1 = pta.reachingObjects(l1);
            Iterator i2 = locals.entrySet().iterator();
            while (i2.hasNext()) {
                Map.Entry e2 = (Map.Entry) i2.next();
                int p2 = ((Integer) e2.getKey()).intValue();
                Local l2 = (Local) e2.getValue();
                PointsToSet r2 = pta.reachingObjects(l2);
                if (!l1.equivTo(l2) && p1 <= currentLine && p2 <= currentLine && p1 <= p2 && r1.hasNonEmptyIntersection(r2)) {
                    graph.putEdge(l1, l2);
                    if (Configuration.v().isEnableSparkDebug()) {
                        System.out.println("[" + p1 + "," + p2 + "]\t[" + e1.getValue() + ":" + e2.getValue() + "]\t intersect? " + r1.hasNonEmptyIntersection(r2));
                    }
                }
            }
        }
        Graph<Local> transitiveGraph = Graphs.transitiveClosure(graph);
        if (Configuration.v().isEnableSparkDebug()) {
            for (EndpointPair<Local> pair : transitiveGraph.edges()) {
                System.out.println(pair.nodeU() + "->" + pair.nodeV());
            }
        }
        for (Local curr : locals.values()) {
            if (transitiveGraph.hasEdgeConnecting(local, curr)) {
                pointsToResult.add(curr);
            }
        }

        return pointsToResult;
    }

    private static Map<Integer, Local> getLocals(SootMethod sootMethod, Type typename) {
        Map res = new HashMap<Integer, Local>();
        JimpleBody jb = (JimpleBody) sootMethod.retrieveActiveBody();
        List<Local> paramLocals = jb.getParameterLocals();
        Iterator ui = jb.getUnits().iterator();
        while (ui.hasNext()) {
            Stmt s = (Stmt) ui.next();
            int line = getLineNumber(s);// helps to maintain ordering
            // find definitions
            Iterator bi = s.getDefBoxes().iterator();
            while (bi.hasNext()) {
                Object o = bi.next();
                if (o instanceof ValueBox) {
                    Value v = ((ValueBox) o).getValue();
                    if (v.getType().equals(typename) && v instanceof Local) {
                        if (paramLocals.contains(v)) {
                            line = paramLocals.indexOf(v);
                        }
                        res.put(new Integer(line), v);
                    }
                }
            }
        }
        return res;
    }
}
