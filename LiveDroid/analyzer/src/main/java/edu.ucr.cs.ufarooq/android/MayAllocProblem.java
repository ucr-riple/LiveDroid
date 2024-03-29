package edu.ucr.cs.ufarooq.android;

import edu.ucr.cs.ufarooq.android.problems.BackwardsMayAllocProblem;
import edu.ucr.cs.ufarooq.accessPath.AccessPathAnalysisObject;
import heros.DefaultSeeds;
import heros.InterproceduralCFG;
import soot.NullType;
import soot.SootMethod;
import soot.Unit;
import soot.toDex.TemporaryRegisterLocal;

import java.util.Map;
import java.util.Set;

//import edu.ucr.cs.ufarooq.java.problems.BackwardsMayUseProblem;

public class MayAllocProblem extends BackwardsMayAllocProblem {
    private SootMethod entryMethod;

    public MayAllocProblem(InterproceduralCFG<Unit, SootMethod> icfg, SootMethod sootMethod) {
        super(icfg);
        this.entryMethod = sootMethod;
    }

    @Override
    protected AccessPathAnalysisObject createZeroValue() {
        return new AccessPathAnalysisObject(null, new TemporaryRegisterLocal(NullType.v()), null);
    }

    @Override
    public Map<Unit, Set<AccessPathAnalysisObject>> initialSeeds() {
        return DefaultSeeds.make(entryMethod.retrieveActiveBody().getUnits(), zeroValue());
    }


    public String getLable() {
        return "MayAllocProblem";
    }
}
