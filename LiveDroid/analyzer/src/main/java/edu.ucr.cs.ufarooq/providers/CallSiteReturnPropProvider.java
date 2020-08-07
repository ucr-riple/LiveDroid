package edu.ucr.cs.ufarooq.providers;

import edu.ucr.cs.ufarooq.model.AnalysisObject;
import soot.Unit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CallSiteReturnPropProvider {
    private HashMap<Unit, Set<AnalysisObject>> callsiteMap;
    private static CallSiteReturnPropProvider _instance = null;

    private CallSiteReturnPropProvider() {
        callsiteMap = new HashMap<>();
    }

    public static CallSiteReturnPropProvider getInstance() {
        if (_instance == null)
            _instance = new CallSiteReturnPropProvider();
        return _instance;
    }

    public static Set<AnalysisObject> getResultsForCallSite(Unit callSite) {
        if (isResultAvailable(callSite)) {
            return getInstance().callsiteMap.get(callSite);
        }
        return null;
    }

    public static boolean isResultAvailable(Unit callSite) {
        return getInstance().callsiteMap.containsKey(callSite);
    }

    public static void insert(Unit callSite, AnalysisObject analysisObject) {
        if (isResultAvailable(callSite)) {
            Set<AnalysisObject> resultSet = getResultsForCallSite(callSite);
            resultSet.add(analysisObject);
            getInstance().callsiteMap.replace(callSite, resultSet);
        } else {
            Set<AnalysisObject> resultSet = new HashSet<AnalysisObject>();
            resultSet.add(analysisObject);
            getInstance().callsiteMap.put(callSite, resultSet);
        }
    }
}
