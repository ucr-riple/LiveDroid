package edu.ucr.cs.ufarooq.android.connection;

import soot.SootMethod;
import soot.Unit;

import java.util.Objects;

public class ReachableConnectionMethodWrapper {
    private SootMethod method;
    private SootMethod callback;
    private Unit callSite;
    private SootMethod caller;


    public ReachableConnectionMethodWrapper(SootMethod callback, SootMethod method, Unit callSite, SootMethod caller) {
        this.callback = callback;
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

    public SootMethod getCallback() {
        return callback;
    }

    public void setCallback(SootMethod callback) {
        this.callback = callback;
    }

    public void setCaller(SootMethod caller) {
        this.caller = caller;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReachableConnectionMethodWrapper wrapper = (ReachableConnectionMethodWrapper) o;
        return method.equals(wrapper.method) &&
                Objects.equals(callSite, wrapper.callSite) &&
                caller.equals(wrapper.caller);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, callSite, caller);
    }
}
