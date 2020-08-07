package edu.ucr.cs.ufarooq.pointsto;

import com.google.common.graph.*;
import edu.ucr.cs.ufarooq.config.Configuration;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.options.Options;
import soot.tagkit.LineNumberTag;

import java.util.*;

public class PointsToAnalysis {

    // Make sure we get line numbers and whole program analysis
    static {
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().setPhaseOption("cg", "verbose:true");
    }

    private static SootClass loadClass(String name, boolean main) {
        SootClass c = Scene.v().loadClassAndSupport(name);
        c.setApplicationClass();
        if (main) Scene.v().setMainClass(c);
        return c;
    }

    private static void setupSoot(String sootClassPath, String mainClass) {
        G.v().reset();
        Options.v().set_whole_program(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().setPhaseOption("cg", "verbose:true");
        List<String> includeList = new LinkedList<String>();
        //includeList.add("java.lang.*");
        //includeList.add("java.util.*");
        //includeList.add("java.io.*");
        //includeList.add("sun.misc.*");
        //includeList.add("java.net.*");
        //includeList.add("javax.servlet.*");
        //includeList.add("javax.crypto.*");

        Options.v().set_include(includeList);
        Options.v().setPhaseOption("jb", "use-original-names:true");

        Options.v().set_soot_classpath(sootClassPath);
        Options.v().set_prepend_classpath(true);
        // Options.v().set_main_class(this.getTargetClass());
        Scene.v().loadNecessaryClasses();
        SootClass c = Scene.v().forceResolve(mainClass, SootClass.BODIES);
        if (c != null) {
            c.setApplicationClass();
        }
        for (SootMethod m : c.getMethods()) {
            System.out.println(m);
        }
    }

    public static void main(String[] args) {

        String sootClassPath = "/Users/umarfarooq/UCR/Research/Research/ReverseEngineering/LivenessAnalysis/ifds/spark/test/out"; //getSootClassPath();
        String mainClass = "Test1";
        setupSoot(sootClassPath, mainClass);
        Configuration.v().setEnableSparkDebug(true);
        setSparkPointsToAnalysis();

        SootClass c = Scene.v().getMainClass();
        //SootField f = getField("Container", "item");
        Map/*<Local>*/ ls = getLocals(c, "go", "B");
        //Map/*<Local>*/ items = getLocals(c, "go", "Item");
        print(c, "go");
        printLocalIntersects(ls);
        SootMethod sootMethod = c.getMethodByName("go");
        printPointsTo(sootMethod);
        //printPointsTo(sootMethod);
        //printFieldIntersects(ls, f);
        //printReachingField(f);
    }


    private static void print(SootClass c, String method) {
        SootMethod methodByName = c.getMethodByName(method);
        for (Unit unit : methodByName.method().retrieveActiveBody().getUnits()) {
            int lineNumber = getLineNumber(unit);
            System.out.println("[" + lineNumber + "]\t" + unit);
        }
    }

    static void setSparkPointsToAnalysis() {
        System.out.println("[spark] Starting analysis ...");

        HashMap opt = new HashMap();
        opt.put("enabled", "true");
        opt.put("verbose", "true");
        opt.put("ignore-types", "false");
        opt.put("force-gc", "false");
        opt.put("pre-jimplify", "false");
        opt.put("vta", "false");
        opt.put("rta", "false");
        opt.put("field-based", "false");
        opt.put("types-for-sites", "false");
        opt.put("merge-stringbuffer", "true");
        opt.put("string-constants", "false");
        opt.put("simulate-natives", "true");
        opt.put("simple-edges-bidirectional", "false");
        opt.put("on-fly-cg", "true");
        opt.put("simplify-offline", "false");
        opt.put("simplify-sccs", "false");
        opt.put("ignore-types-for-sccs", "false");
        opt.put("propagator", "worklist");
        opt.put("set-impl", "double");
        opt.put("double-set-old", "hybrid");
        opt.put("double-set-new", "hybrid");
        opt.put("dump-html", "false");
        opt.put("dump-pag", "false");
        opt.put("dump-solution", "false");
        opt.put("topo-sort", "false");
        opt.put("dump-types", "true");
        opt.put("class-method-var", "true");
        opt.put("dump-answer", "false");
        opt.put("add-tags", "false");
        opt.put("set-mass", "false");

        SparkTransformer.v().transform("", opt);

        System.out.println("[spark] Done!");
    }


    private static int getLineNumber(Unit s) {
        Iterator ti = s.getTags().iterator();
        while (ti.hasNext()) {
            Object o = ti.next();
            if (o instanceof LineNumberTag)
                return Integer.parseInt(o.toString());
        }
        return -1;
    }

    private static SootField getField(String classname, String fieldname) {
        SootClass sootClass = Scene.v().getSootClass(classname);
        if (sootClass != null)
            return sootClass.getFieldByName(fieldname);
//        Collection app = Scene.v().getApplicationClasses();
//        Iterator ci = app.iterator();
//        while (ci.hasNext()) {
//            SootClass sc = (SootClass) ci.next();
//            System.out.println("Class:" + sc.getName());
//            if (sc.getName().equals(classname))
//                return sc.getFieldByName(fieldname);
//        }
        throw new RuntimeException("Field " + fieldname + " was not found in class " + classname);
    }


    private static Map/*<Integer,Local>*/ getLocals(SootClass sc, String methodname, String typename) {
        Map res = new HashMap();
        Iterator mi = sc.getMethods().iterator();
        while (mi.hasNext()) {
            SootMethod sm = (SootMethod) mi.next();
            System.err.println(sm.getName());
            if (true && sm.getName().equals(methodname) && sm.isConcrete()) {
                JimpleBody jb = (JimpleBody) sm.retrieveActiveBody();
                List<Local> paramLocals = jb.getParameterLocals();
                Iterator ui = jb.getUnits().iterator();
                while (ui.hasNext()) {
                    Stmt s = (Stmt) ui.next();
                    int line = getLineNumber(s);
                    // find definitions
                    Iterator bi = s.getDefBoxes().iterator();
                    while (bi.hasNext()) {
                        Object o = bi.next();
                        if (o instanceof ValueBox) {
                            Value v = ((ValueBox) o).getValue();
                            if (v.getType().toString().equals(typename) && v instanceof Local) {
                                if (paramLocals.contains(v)) {
                                    line = paramLocals.indexOf(v);
                                }
                                res.put(new Integer(line), v);
                            }
                        }
                    }
                }
            }
        }

        return res;
    }

    private static void printPointsTo(SootMethod sootMethod) {
        JimpleBody jb = (JimpleBody) sootMethod.retrieveActiveBody();
        List<Local> paramLocals = jb.getParameterLocals();
        Iterator ui = jb.getUnits().iterator();
        while (ui.hasNext()) {
            Stmt s = (Stmt) ui.next();
            Iterator<ValueBox> bi = s.getUseBoxes().iterator();
            while (bi.hasNext()) {
                ValueBox box = bi.next();
                if (box.getValue() instanceof Local) {
                    Local local = (Local) box.getValue();
                    if (/*!paramLocals.contains(local) &&*/ !jb.getThisLocal().equivTo(local)) {
                       // Set<AccessPath> accessPaths = SparkPointsTo.query(s, sootMethod, local);
                       // System.out.println(local + "\t" + accessPaths);
                    }
                }
            }
        }
    }

    private static void printLocalIntersects(Map/*<Integer,Local>*/ ls) {
        soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        Iterator i1 = ls.entrySet().iterator();
        MutableGraph<Local> graph = GraphBuilder.undirected().build();
        while (i1.hasNext()) {
            Map.Entry e1 = (Map.Entry) i1.next();
            int p1 = ((Integer) e1.getKey()).intValue();
            Local l1 = (Local) e1.getValue();
            PointsToSet r1 = pta.reachingObjects(l1);
            Iterator i2 = ls.entrySet().iterator();
            while (i2.hasNext()) {
                Map.Entry e2 = (Map.Entry) i2.next();
                int p2 = ((Integer) e2.getKey()).intValue();
                Local l2 = (Local) e2.getValue();
                PointsToSet r2 = pta.reachingObjects(l2);
                if (p1 <= p2) {
                    if (!l1.equivTo(l2))
                        graph.putEdge(l1, l2);
                    System.out.println("[" + p1 + "," + p2 + "]\t[" + l1 + ":" + l2 + "]\t Container intersect? " + r1.hasNonEmptyIntersection(r2));
                }
            }
        }
        System.out.println("transitiveClosure");
        Graph<Local> transitiveGraph = Graphs.transitiveClosure(graph);

        for (EndpointPair<Local> pair : transitiveGraph.edges()) {
            System.out.println(pair.nodeU() + "\t->" + pair.nodeV());
        }

    }


    private static void printFieldIntersects(Map/*<Integer,Local>*/ ls, SootField f) {

        soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        Iterator i1 = ls.entrySet().iterator();
        while (i1.hasNext()) {
            Map.Entry e1 = (Map.Entry) i1.next();
            int p1 = ((Integer) e1.getKey()).intValue();
            Local l1 = (Local) e1.getValue();
            PointsToSet r1 = pta.reachingObjects(l1, f);
            //printPts(r1);
            Iterator i2 = ls.entrySet().iterator();
            while (i2.hasNext()) {
                Map.Entry e2 = (Map.Entry) i2.next();
                int p2 = ((Integer) e2.getKey()).intValue();
                Local l2 = (Local) e2.getValue();
                PointsToSet r2 = pta.reachingObjects(l2, f);
                //printPts(r2);
                if (p1 <= p2)
                    System.out.println("[" + p1 + "," + p2 + "]\t[" + e1.getValue() + ":" + e2.getValue() + "]\t Container.item intersect? " + r1.hasNonEmptyIntersection(r2));
            }
        }
    }

    private static void printPts(PointsToSet pts_origin) {
        if (!pts_origin.isEmpty()) {
            PointsToSetInternal pts = (PointsToSetInternal) pts_origin;
            pts.forall(new P2SetVisitor() {

                @Override
                public void visit(Node n) {
                    // TODO Auto-generated method stub
                    System.err.print(n + "\n");
                }
            });
        }
    }

    private static void printReachingField(SootField field) {
        soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        PointsToSet pts_origin = pta.reachingObjects(field);
        if (!pts_origin.isEmpty()) {
            PointsToSetInternal pts = (PointsToSetInternal) pts_origin;
            pts.forall(new P2SetVisitor() {

                @Override
                public void visit(Node n) {
                    // TODO Auto-generated method stub
                    System.err.print(n + "\n");
                }
            });
        }
    }

}