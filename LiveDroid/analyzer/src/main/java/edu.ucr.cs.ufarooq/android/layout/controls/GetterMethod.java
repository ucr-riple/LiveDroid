package edu.ucr.cs.ufarooq.android.layout.controls;

import soot.SootMethod;

import java.util.Objects;

public class GetterMethod {
    private final SootMethod sootMethod;
    private final boolean isToStringRequired;
    private final String property;

    public GetterMethod(SootMethod sootMethod, boolean isToStringRequired, String property) {
        this.sootMethod = sootMethod;
        this.isToStringRequired = isToStringRequired;
        this.property = property;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public boolean isToStringRequired() {
        return isToStringRequired;
    }

    public String getProperty() {
        return property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetterMethod that = (GetterMethod) o;
        return isToStringRequired == that.isToStringRequired &&
                Objects.equals(sootMethod, that.sootMethod) &&
                Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootMethod, isToStringRequired, property);
    }
}
