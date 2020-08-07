package edu.ucr.cs.ufarooq.android.layout.controls;

import soot.SootMethod;
import soot.Unit;

import java.util.Objects;

public class SetterMethod {
    private final SootMethod sootMethod;
    private final boolean isPreferred;
    private final String property;
    private Unit callSite;

    public SetterMethod(SootMethod sootMethod, boolean isPreferred, String property) {
        this.sootMethod = sootMethod;
        this.isPreferred = isPreferred;
        this.property = property;
        this.callSite = null;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public boolean isPreferred() {
        return isPreferred;
    }

    public String getProperty() {
        return property;
    }

    public Unit getCallSite() {
        return callSite;
    }

    public void setCallSite(Unit callSite) {
        this.callSite = callSite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetterMethod that = (SetterMethod) o;
        return isPreferred == that.isPreferred &&
                Objects.equals(sootMethod, that.sootMethod) &&
                Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootMethod, isPreferred, property);
    }
}
