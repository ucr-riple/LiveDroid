package edu.ucr.cs.ufarooq.android.util;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

import java.util.Iterator;

public class AndroidHelper {

    public static FlowSet<SootMethod> getStartingLifecycleCallbacks(SootClass activityClass) {
        FlowSet<SootMethod> sootMethodList = new ArraySparseSet<>();
        if (activityClass.declaresMethod("void onResume()")) {
            sootMethodList.add(activityClass.getMethod("void onResume()"));
        }
        if (activityClass.declaresMethod("void onRestart()")) {
            sootMethodList.add(activityClass.getMethod("void onRestart()"));
        }
        if (activityClass.declaresMethod("void onCreate(android.os.Bundle)")) {
            sootMethodList.add(activityClass.getMethod("void onCreate(android.os.Bundle)"));
        }
        if (activityClass.declaresMethod("void onStart()")) {
            sootMethodList.add(activityClass.getMethod("void onStart()"));
        }
        if (activityClass.declaresMethod("void onPostResume()")) {
            sootMethodList.add(activityClass.getMethod("void onPostResume()"));
        }
        if (activityClass.declaresMethod("void onPostCreate(android.os.Bundle)")) {
            sootMethodList.add(activityClass.getMethod("void onPostCreate(android.os.Bundle)"));
        }
        return sootMethodList;
    }

    public static FlowSet<SootMethod> getPostResumeLifecycleCallbacks(SootClass activityClass) {
        FlowSet<SootMethod> sootMethodList = new ArraySparseSet<>();
        if (activityClass.declaresMethod("void onPause()")) {
            sootMethodList.add(activityClass.getMethod("void onPause()"));
        }
        if (activityClass.declaresMethod("void onStop()")) {
            sootMethodList.add(activityClass.getMethod("void onStop()"));
        }
        if (activityClass.declaresMethod("void onDestroy()")) {
            sootMethodList.add(activityClass.getMethod("void onDestroy()"));
        }
        if (activityClass.declaresMethod("void onBackPressed()")) {
            sootMethodList.add(activityClass.getMethod("void onBackPressed()"));
        }
        if (activityClass.declaresMethod("boolean onOptionsItemSelected(android.view.MenuItem)")) {
            sootMethodList.add(activityClass.getMethod("boolean onOptionsItemSelected(android.view.MenuItem)"));
        }
        if (activityClass.declaresMethod("boolean onContextItemSelected(android.view.MenuItem)")) {
            sootMethodList.add(activityClass.getMethod("boolean onContextItemSelected(android.view.MenuItem)"));
        }
        return sootMethodList;
    }

    /**
     * @param activity
     * @return >1 if layout attached, 0 if no layout file attached but dynamic layout attached. -1 if setContentView not called.
     */
    public static int getLayoutForActivity(SootClass activity) {
        if (activity.declaresMethod("void onCreate(android.os.Bundle)")) {
            SootMethod onCreate = activity.getMethod("void onCreate(android.os.Bundle)");
            try {
                UnitGraph graph = new BriefUnitGraph(onCreate.retrieveActiveBody());
                for (Iterator<Unit> iterator = graph.iterator(); iterator.hasNext(); ) {
                    Unit next = iterator.next();
                    if (next instanceof Stmt && ((Stmt) next).containsInvokeExpr()) {
                        InvokeExpr invoke = ((Stmt) next).getInvokeExpr();
                        if (invoke.getMethod().getName().equals("setContentView")) {
                            if (invoke.getMethod().getSubSignature().equals("void setContentView(int)")) {
                                if (invoke.getArg(0) instanceof Constant) {
                                    IntConstant constant = (IntConstant) invoke.getArg(0);
                                    return constant.value;
                                }
                            } else if (invoke.getMethod().getSubSignature().equals("void setContentView(android.view.View)")) {
                                return 0;
                            }
                        }
                    }
                }
                return -1;
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        }
        return -1;
    }
}
