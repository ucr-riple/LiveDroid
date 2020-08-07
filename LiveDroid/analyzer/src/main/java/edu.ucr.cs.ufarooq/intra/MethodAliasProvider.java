package edu.ucr.cs.ufarooq.intra;

import heros.InterproceduralCFG;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.HashMap;

public class MethodAliasProvider {
    private HashMap<Pair<SootMethod, Unit>, MethodAliasAnalysis> hashMap;
    private HashMap<Unit, MethodAliasAnalysis> contextSensitiveMap;
    private static MethodAliasProvider provider = null;

    public static MethodAliasProvider getInstance() {
        if (provider == null)
            provider = new MethodAliasProvider();
        return provider;
    }

    private MethodAliasProvider() {
        hashMap = new HashMap<Pair<SootMethod, Unit>, MethodAliasAnalysis>();
        contextSensitiveMap = new HashMap<Unit, MethodAliasAnalysis>();
    }

    private static void insert(SootMethod sootMethod, Unit unit, MethodAliasAnalysis result) {
        Pair<SootMethod, Unit> pair = new ImmutablePair<>(sootMethod, unit);
        getInstance().hashMap.putIfAbsent(pair, result);
    }

    public static MethodAliasAnalysis getAliasResult(SootMethod sootMethod, Unit unit, InterproceduralCFG<Unit, SootMethod> icfg) {
        return getAliasResult(sootMethod, unit, icfg, 0);
    }

    public synchronized static MethodAliasAnalysis getAliasResult(SootMethod sootMethod, Unit unit, InterproceduralCFG<Unit, SootMethod> icfg, int currentCount) {
        MethodAliasAnalysis aliasAnalysisResult = query(sootMethod, unit);
        if (aliasAnalysisResult != null) {
            return aliasAnalysisResult;
        } else {
            UnitGraph graph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
            MethodAliasAnalysis aliasAnalysis = new MethodAliasAnalysis(graph, icfg, currentCount);
            //if (currentCount != MethodAliasAnalysis.MAX_GETS)
            insert(sootMethod, unit, aliasAnalysis);
            return aliasAnalysis;
        }
    }

    public static MethodAliasAnalysis getAliasResultLastest(SootMethod sootMethod, Unit unit, InterproceduralCFG<Unit, SootMethod> icfg) {
        return getAliasResult(sootMethod, unit, icfg);
//        UnitGraph graph = new BriefUnitGraph(sootMethod.retrieveActiveBody());
//        MethodAliasAnalysis aliasAnalysis = new MethodAliasAnalysis(graph);
//
//        return aliasAnalysis;

    }

    private static MethodAliasAnalysis query(SootMethod sootMethod, Unit unit) {
        Pair<SootMethod, Unit> pair = new ImmutablePair<>(sootMethod, unit);
        if (getInstance().hashMap.containsKey(pair)) {
            return getInstance().hashMap.get(pair);
        }
        return null;
    }
}
