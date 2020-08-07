package edu.ucr.cs.ufarooq.model;

import java.util.Objects;

public class NewAllocAnalysisObject extends AnalysisObject {

    private boolean isNewAlloc;// used dummy to make sure equals never go wrong;

    public NewAllocAnalysisObject() {
        this.sootField = null;
        this.fieldsMap = null;
        this.targetClassField = null;
        isNewAlloc = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewAllocAnalysisObject that = (NewAllocAnalysisObject) o;
        return isNewAlloc == that.isNewAlloc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isNewAlloc);
    }
}
