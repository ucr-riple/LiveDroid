package edu.ucr.cs.ufarooq.intra;

import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

import java.util.Objects;

public class AliasAnalysisResult {
    private FlowSet<AliasesState> aliasesStateFlowSet;

    public AliasAnalysisResult() {
        aliasesStateFlowSet = new ArraySparseSet<AliasesState>();
    }

    public AliasAnalysisResult(FlowSet<AliasesState> aliasesStateFlowSet) {
        this.aliasesStateFlowSet = aliasesStateFlowSet;
    }

    public FlowSet<AliasesState> getAliasesStateFlowSet() {
        return aliasesStateFlowSet;
    }

    public void setAliasesStateFlowSet(FlowSet<AliasesState> aliasesStateFlowSet) {
        this.aliasesStateFlowSet = aliasesStateFlowSet;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AliasAnalysisResult that = (AliasAnalysisResult) o;
        return Objects.equals(aliasesStateFlowSet, that.aliasesStateFlowSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aliasesStateFlowSet);
    }
}
