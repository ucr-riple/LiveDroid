package edu.ucr.cs.ufarooq.intra;

import edu.ucr.cs.ufarooq.model.AnalysisObject;
import edu.ucr.cs.ufarooq.model.ClassField;
import soot.Local;
import soot.Unit;


public class AliasesState {
    private AnalysisObject field;
    private Local local;
    private Local baseObject;
    private boolean readFlag;
    private Unit definitionStmt;
    private AliasType type;
    private Unit callSite; // only compare when type is InvokeBaseAlias
    private Unit exitSite; // only compare when returning, so can handle multiple returns
    private boolean isNewAllocation = false;
    private boolean isNewFindViewById = false;
    private int viewId = -1;


    public AliasesState() {
        field = null;
        local = null;
        readFlag = false;
        baseObject = null;
        definitionStmt = null;
        type = AliasType.NotDefined;
    }

//    public AliasesState(Object field, Local local) {
//        this.field = field;
//        this.local = local;
//        readFlag = false;
//        baseObject = null;
//    }

    public AliasesState(AnalysisObject field, Local local, Local baseObject, Unit unit, AliasType type) {
        this.field = field;
        this.local = local;
        readFlag = false;
        this.baseObject = baseObject;
        this.type = type;
        this.definitionStmt = unit;
    }

    public AliasesState(Local local) {
        this.field = null;
        this.local = local;
        readFlag = false;
        this.baseObject = null;
        this.definitionStmt = null;
    }

    public AliasesState(AliasesState aliasesState) {
        this.local = aliasesState.local;
        this.readFlag = aliasesState.readFlag;
        this.baseObject = aliasesState.baseObject;
        this.definitionStmt = aliasesState.definitionStmt;
        if (aliasesState.field instanceof ClassField) {
            ClassField classField = (ClassField) aliasesState.field;
            this.field = new ClassField(classField);
        }
    }

    public AnalysisObject getField() {
        return field;
    }

    public void setField(AnalysisObject field) {
        this.field = field;
    }

    public boolean isReadFlag() {
        return readFlag;
    }

    public void setReadFlag(boolean readFlag) {
        this.readFlag = readFlag;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Local getBaseObject() {
        return baseObject;
    }

    public void setBaseObject(Local baseObject) {
        this.baseObject = baseObject;
    }

    public AliasType getType() {
        return type;
    }

    public void setType(AliasType type) {
        this.type = type;
    }

    public Unit getCallSite() {
        return callSite;
    }

    public void setCallSite(Unit callSite) {
        this.callSite = callSite;
    }

    public Unit getExitSite() {
        return exitSite;
    }

    public void setExitSite(Unit exitSite) {
        this.exitSite = exitSite;
    }

    public Unit getDefinitionStmt() {
        return definitionStmt;
    }

    public void setDefinitionStmt(Unit definitionStmt) {
        this.definitionStmt = definitionStmt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AliasesState that = (AliasesState) o;
        if (local == null || that.local == null)
            return false;
        if (type == AliasType.InvokeBaseAlias || that.type == AliasType.InvokeBaseAlias) {// compare callsite to make sure
            if (callSite == null || that.callSite == null)
                return false;
            //if (!field.equals(that.field)) return false;
            return (callSite.equals(that.callSite) && local.equivTo(that.local));
            // return true;

        }

        /*if (type == AliasType.ReturnAlias || that.type == AliasType.ReturnAlias) {// compare return site to make sure
            if (exitSite == null || that.exitSite == null)
                return false;
            //if (!field.equals(that.field)) return false;
            return (exitSite.equals(that.exitSite) && local.equivTo(that.local));
            //   return true;

        }*/

        //if (!field.equals(that.field)) return false;

        return local.equivTo(that.local);
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (local != null)
            result = local.hashCode();
        //result = 31 * result + field.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getLocalString() + "-" + getFieldString();
    }

    private String getLocalString() {
        return local != null ? local.getName() : "<null>";
    }

    private String getFieldString() {
        return field != null ? field.toString() : "null";
    }

    public static AliasesState getNewAllocation(Local local) {
        AliasesState aliasesState = new AliasesState(local);
        aliasesState.isNewAllocation = true;
        aliasesState.field = null;
        return aliasesState;
    }

    public static boolean isNewAllocation(AliasesState aliasesState) {
        return (aliasesState.field == null && aliasesState.isNewAllocation);
    }

    public static AliasesState getNewFindView(Local local, int viewId) {
        AliasesState aliasesState = new AliasesState(local);
        aliasesState.isNewFindViewById = true;
        aliasesState.field = null;
        aliasesState.viewId = viewId;
        return aliasesState;
    }

    public static boolean isNewFindView(AliasesState aliasesState) {
        return (aliasesState.field == null && aliasesState.isNewFindViewById && aliasesState.viewId > -1);
    }
}
