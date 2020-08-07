package edu.ucr.cs.ufarooq.android.results;

import edu.ucr.cs.ufarooq.accessPath.AccessPathAnalysisObject;
import soot.jimple.infoflow.data.AccessPath;

import java.util.Map;
import java.util.Set;

public class CallbackResult {
    private Set<AccessPathAnalysisObject> myaUseResults;
    private Set<AccessPathAnalysisObject> mayModifyResults;
    private Set<AccessPathAnalysisObject> mayAllocateResults;
    private Map<AccessPath, Set<AccessPath>> mayPointsResults;// should be one way points;
    //add views to activity results;



}
