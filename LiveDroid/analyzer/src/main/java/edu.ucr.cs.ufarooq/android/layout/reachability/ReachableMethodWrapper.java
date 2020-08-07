package edu.ucr.cs.ufarooq.android.layout.reachability;

import soot.SootMethod;
import soot.Unit;

import java.util.Objects;

public class ReachableMethodWrapper {
    private SootMethod method;
    private Unit callSite;
    private SootMethod caller;


    public ReachableMethodWrapper(SootMethod method, Unit callSite, SootMethod caller) {
        this.method = method;
        this.callSite = callSite;
        this.caller = caller;
    }

    public SootMethod getMethod() {
        return method;
    }

    public void setMethod(SootMethod method) {
        this.method = method;
    }

    public Unit getCallSite() {
        return callSite;
    }

    public void setCallSite(Unit callSite) {
        this.callSite = callSite;
    }

    public SootMethod getCaller() {
        return caller;
    }

    public void setCaller(SootMethod caller) {
        this.caller = caller;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReachableMethodWrapper wrapper = (ReachableMethodWrapper) o;
        return method.equals(wrapper.method) &&
                Objects.equals(callSite, wrapper.callSite) &&
                caller.equals(wrapper.caller);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, callSite, caller);
    }
}
