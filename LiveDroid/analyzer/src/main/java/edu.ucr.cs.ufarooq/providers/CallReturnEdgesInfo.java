package edu.ucr.cs.ufarooq.providers;

import soot.SootMethod;
import soot.Unit;

import java.util.Objects;

public class CallReturnEdgesInfo {
    private Unit callSite;
    private SootMethod calleeMethod;
    private Unit exitStmt;
    private Unit returnSite;

    public CallReturnEdgesInfo() {
        this.callSite = null;
        this.calleeMethod = null;
        this.exitStmt = null;
        this.returnSite = null;
    }

    public CallReturnEdgesInfo(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
        this.callSite = callSite;
        this.calleeMethod = calleeMethod;
        this.exitStmt = exitStmt;
        this.returnSite = returnSite;
    }

    public Unit getIdentifier() {
        return callSite;
    }

    public Unit getCallSite() {
        return callSite;
    }

    public void setCallSite(Unit callSite) {
        this.callSite = callSite;
    }

    public SootMethod getCalleeMethod() {
        return calleeMethod;
    }

    public void setCalleeMethod(SootMethod calleeMethod) {
        this.calleeMethod = calleeMethod;
    }

    public Unit getExitStmt() {
        return exitStmt;
    }

    public void setExitStmt(Unit exitStmt) {
        this.exitStmt = exitStmt;
    }

    public Unit getReturnSite() {
        return returnSite;
    }

    public void setReturnSite(Unit returnSite) {
        this.returnSite = returnSite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallReturnEdgesInfo that = (CallReturnEdgesInfo) o;
        return Objects.equals(callSite, that.callSite) &&
                Objects.equals(calleeMethod, that.calleeMethod) &&
                Objects.equals(exitStmt, that.exitStmt) &&
                Objects.equals(returnSite, that.returnSite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callSite, calleeMethod, exitStmt, returnSite);
    }

    @Override
    public String toString() {
        return "CallReturnEdgesInfo{" +
                "callSite=" + callSite +
                ", calleeMethod=" + calleeMethod +
                ", exitStmt=" + exitStmt +
                ", returnSite=" + returnSite +
                '}';
    }
}
