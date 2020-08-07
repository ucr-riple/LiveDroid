package edu.ucr.cs.ufarooq.accessPath;

import edu.ucr.cs.ufarooq.config.Configuration;
import soot.Local;
import soot.Unit;

import java.util.*;

public class AccessPathAnalysisObject {
    private final AccessPath accessPath;
    private final Local reason; // object that was originally effected during analysis
    private final Unit statement;
    private boolean hasEvent;

    public AccessPathAnalysisObject(AccessPath accessPath, Local reason, Unit unit) {
        this.accessPath = accessPath;
        this.reason = reason;
        this.statement = unit;
        this.hasEvent = false;
    }

    public AccessPathAnalysisObject(AccessPath accessPath) {
        this.accessPath = accessPath;
        this.reason = null;
        this.statement = null;
        this.hasEvent = false;
    }

    public AccessPathAnalysisObject(AccessPath accessPath, boolean hasEvent, Local reason, Unit unit) {
        this.accessPath = accessPath;
        this.reason = reason;
        this.statement = unit;
        this.hasEvent = hasEvent;
    }

    public AccessPath getAccessPath() {
        return accessPath;
    }

    public AccessPath getDelegate() {
        return accessPath;
    }

    public boolean hasEvent() {
        return hasEvent;
    }

    public int getFieldCount() {
        return accessPath.getFields().size();
    }

    public Local getReason() {
        return reason;
    }

    public Unit getStatement() {
        return statement;
    }

    public AccessPathAnalysisObject deriveWithoutEvent() {
        return new AccessPathAnalysisObject(accessPath, false, reason, statement);
    }

    public AccessPathAnalysisObject deriveWithEvent() {
        return new AccessPathAnalysisObject(accessPath, true, reason, statement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessPathAnalysisObject that = (AccessPathAnalysisObject) o;
        return Objects.equals(accessPath, that.accessPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessPath);
    }

    @Override
    public String toString() {
        if (Configuration.v().isEnableDebug()) {
            return "{" +
                    "accessPath=" + accessPath +
                    ", reason=" + reason +
                    ", Stmt=" + statement +
                    '}';
        } else {
            return "{" +
                    "accessPath=" + accessPath +
                    '}';
        }

    }

    public Field getLastField() {
        if (accessPath != null) {
            int size = accessPath.getFields().size();
            List<Field> fields = new ArrayList<>(size);
            fields.addAll(0, accessPath.getFields());
            return fields.get(size - 1);
        }
        return null;
    }

    public Collection<Field> popLastField() {
        if (accessPath != null) {
            int size = accessPath.getFields().size();
            if (size > 1) {
                List<Field> fields = new ArrayList<>(size);
                fields.addAll(0, accessPath.getFields());
                return fields.subList(0, size - 1);
            } else {
                return Collections.emptySet();
            }
        }
        return null;
    }
}
