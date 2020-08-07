package edu.ucr.cs.ufarooq.providers;

import soot.Unit;

import java.util.HashMap;

public class CallReturnEdgesProvider {
    private HashMap<Unit, CallReturnEdgesInfo> hashMap;
    private static CallReturnEdgesProvider provider = null;

    public static CallReturnEdgesProvider getInstance() {
        if (provider == null)
            provider = new CallReturnEdgesProvider();
        return provider;
    }

    private CallReturnEdgesProvider() {
        hashMap = new HashMap<Unit, CallReturnEdgesInfo>();
    }

    public static void insert(CallReturnEdgesInfo result) {
        getInstance().hashMap.putIfAbsent(result.getIdentifier(), result);
    }

    public static CallReturnEdgesInfo query(Unit callsite) {
        if (getInstance().hashMap.containsKey(callsite)) {
            return getInstance().hashMap.get(callsite);
        }
        return null;
    }
}
