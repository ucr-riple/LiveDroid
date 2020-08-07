package edu.ucr.cs.ufarooq.android.connection;

import soot.SootMethod;
import soot.Type;
import soot.Unit;

import java.util.List;
import java.util.Objects;

public abstract class AbsConnection {
    protected SootMethod openingCallback;
    protected Unit openingStmt;
    protected SootMethod closingCallback;
    protected Unit closingStmt;
    protected List<SootMethod> openingCompletePath;
    protected List<SootMethod> closingCompletePath;
    protected Type connectionType;

    public enum Connection {NONE, OPENING, CLOSING}


    public AbsConnection(SootMethod openingCallback, Unit openingStmt, Connection opening) {
        if (opening == Connection.OPENING) {
            this.openingCallback = openingCallback;
            this.openingStmt = openingStmt;
        } else {
            this.closingCallback = openingCallback;
            this.closingStmt = openingStmt;
        }
    }

    public SootMethod getOpeningCallback() {
        return openingCallback;
    }

    public void setOpeningCallback(SootMethod openingCallback) {
        this.openingCallback = openingCallback;
    }

    public Unit getOpeningStmt() {
        return openingStmt;
    }

    public void setOpeningStmt(Unit openingStmt) {
        this.openingStmt = openingStmt;
    }

    public SootMethod getClosingCallback() {
        return closingCallback;
    }

    public void setClosingCallback(SootMethod closingCallback) {
        this.closingCallback = closingCallback;
    }

    public Unit getClosingStmt() {
        return closingStmt;
    }

    public void setClosingStmt(Unit closingStmt) {
        this.closingStmt = closingStmt;
    }

    public List<SootMethod> getOpeningCompletePath() {
        return openingCompletePath;
    }

    public void setOpeningCompletePath(List<SootMethod> openingCompletePath) {
        this.openingCompletePath = openingCompletePath;
    }

    public List<SootMethod> getClosingCompletePath() {
        return closingCompletePath;
    }

    public void setClosingCompletePath(List<SootMethod> closingCompletePath) {
        this.closingCompletePath = closingCompletePath;
    }

    public Type getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(Type connectionType) {
        this.connectionType = connectionType;
    }

    public boolean isConnectionComplete() {
        return (openingCallback != null && openingStmt != null && closingCallback != null && closingStmt != null);
    }

    public Connection getMissingType() {
        if (openingCallback == null && openingStmt == null)
            return Connection.OPENING;
        else if (closingCallback == null && closingStmt == null)
            return Connection.CLOSING;
        return Connection.NONE;
    }

    public abstract String getType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbsConnection that = (AbsConnection) o;
        return Objects.equals(openingCallback, that.openingCallback) &&
                Objects.equals(openingStmt, that.openingStmt) &&
                Objects.equals(closingCallback, that.closingCallback) &&
                Objects.equals(closingStmt, that.closingStmt) &&
                Objects.equals(openingCompletePath, that.openingCompletePath) &&
                Objects.equals(closingCompletePath, that.closingCompletePath) &&
                Objects.equals(connectionType, that.connectionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(openingCallback, openingStmt, closingCallback, closingStmt, openingCompletePath, closingCompletePath, connectionType);
    }
}
