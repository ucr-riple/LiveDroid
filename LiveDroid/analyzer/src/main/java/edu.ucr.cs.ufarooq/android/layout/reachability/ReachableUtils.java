package edu.ucr.cs.ufarooq.android.layout.reachability;

import edu.ucr.cs.ufarooq.android.layout.controls.ViewControlsAPIProviders;
import edu.ucr.cs.ufarooq.android.problems.Utils;
import org.xml.sax.SAXException;
import soot.Local;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

public class ReachableUtils {

    private final SootMethod method;
    private final Set<SootMethod> viewChangeableAPIs;
    private final JimpleBasedInterproceduralCFG icfg;
    private List<String> avoidCallsFrom;


    public ReachableUtils(SootMethod method, Set<SootMethod> viewChangeableAPIs, JimpleBasedInterproceduralCFG icfg) {
        this.method = method;
        this.viewChangeableAPIs = viewChangeableAPIs;
        this.icfg = icfg;
        avoidCallsFrom = new ArrayList<>();
        avoidCallsFrom.add("void <init>()");
        avoidCallsFrom.add("void run()");
    }

    public FlowSet<ReachableMethodWrapper> putEdgesInStack(SootMethod method, FlowSet<ReachableMethodWrapper> edgeFlowSet) {
        //stack.push(method);
        //Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(method);
        if (method.hasActiveBody()) {
            Set<Unit> calls = icfg.getCallsFromWithin(method);
            for (Unit callSite : calls) {
                Stmt stmt = (Stmt) callSite;
                SootMethod sootMethod = stmt.getInvokeExpr().getMethod();
                //System.out.println("Callsite: " + callSite + " -> " + sootMethod.getSignature());
                // todo: temp fix for Android calls
                ReachableMethodWrapper tmp = new ReachableMethodWrapper(sootMethod, callSite, method);
                putInSet(tmp, edgeFlowSet);
                //Edge edge = edges.next();
                Collection<SootMethod> callees = icfg.getCalleesOfCallAt(callSite);
                for (SootMethod callee : callees) {
                    ReachableMethodWrapper wrapper = new ReachableMethodWrapper(callee, callSite, method);
                    //System.out.println("Callee: " + callee.getSignature());
                    putInSet(wrapper, edgeFlowSet);
                }
            }
        }
        return edgeFlowSet;
    }

    private void putInSet(ReachableMethodWrapper wrapper, FlowSet<ReachableMethodWrapper> edgeFlowSet) {
        if (!edgeFlowSet.contains(wrapper)) {
            //System.out.println("Callee: " + wrapper.getMethod().getSignature());
            if (!wrapper.getMethod().getName().equals("<init>") && !wrapper.getMethod().getSubSignature().equals("void run()")) {
                edgeFlowSet.add(wrapper);
                putEdgesInStack(wrapper.getMethod(), edgeFlowSet);
            }
        }
    }

    public FlowSet<ReachableMethodWrapper> getReachableViewEdges() {
        FlowSet<ReachableMethodWrapper> reachableViewEdges = new ArraySparseSet<>();
        FlowSet<ReachableMethodWrapper> edgeFlowSet = new ArraySparseSet<>();
        FlowSet<ReachableMethodWrapper> edges = putEdgesInStack(method, edgeFlowSet);
        edges.forEach(edge -> {
            SootMethod sootMethod = edge.getMethod();
            if (viewChangeableAPIs.contains(sootMethod)) {
                reachableViewEdges.add(edge);
            }
        });
        return reachableViewEdges;
    }

    public static Map<Type, Set<ReachableMethodWrapper>> printReachableViewEdges(SootMethod method, JimpleBasedInterproceduralCFG icfg) throws ParserConfigurationException, SAXException, IOException {
        System.out.println(method.getName());
//        SootClass textView = Scene.v().getSootClass("android.widget.TextView");
//        SootMethod seText = textView.getMethodUnsafe("void setText(java.lang.CharSequence)");
//        SootMethod seTextInt = textView.getMethodUnsafe("void setText(int)");
//        SootMethod seTextColor = textView.getMethodUnsafe("void setTextColor(int)");
//        Set<SootMethod> set = new HashSet<>();
//        set.add(seText);
//        set.add(seTextInt);
//        set.add(seTextColor);

        Set<SootMethod> set = ViewControlsAPIProviders.v().getAllAPIEditables();
//        set.forEach(sootMethod -> {
//            System.out.println("APIEditable: " + sootMethod.getSubSignature());
//        });
        ReachableUtils reachableUtils = new ReachableUtils(method, set, icfg);
        FlowSet<ReachableMethodWrapper> edges = reachableUtils.getReachableViewEdges();
        Map<Type, Set<ReachableMethodWrapper>> typeReachableMethodWrapperHashMap = new HashMap<>();

        edges.forEach(edge -> {
            Local localForInvoke = Utils.getLocalForInvoke(((Stmt) edge.getCallSite()).getInvokeExpr());
            if (typeReachableMethodWrapperHashMap.containsKey(localForInvoke.getType())) {
                Set<ReachableMethodWrapper> foundEgdes = typeReachableMethodWrapperHashMap.get(localForInvoke.getType());
                Set<ReachableMethodWrapper> newEdges = Utils.clone(foundEgdes);
                newEdges.add(edge);
                typeReachableMethodWrapperHashMap.replace(localForInvoke.getType(), newEdges);
            } else {
                Set<ReachableMethodWrapper> foundEgdes = new HashSet<ReachableMethodWrapper>();
                foundEgdes.add(edge);
                typeReachableMethodWrapperHashMap.put(localForInvoke.getType(), foundEgdes);
            }
            System.out.println("Type: " + localForInvoke.getType());

            System.out.println("View Edge::" + edge.getCaller().getName() + "->" + edge.getCallSite() + ":" + edge.getMethod().getDeclaringClass().getName());
        });

        return typeReachableMethodWrapperHashMap;

    }
}
