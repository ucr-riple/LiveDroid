package edu.ucr.cs.ufarooq.model;

import soot.Local;

import java.util.Objects;

public class NewFindViewAnalysisObject extends AnalysisObject {

    private int viewId;// view id used in findViewById
    private Local baseLocal;

    public NewFindViewAnalysisObject(int viewId) {
        this.sootField = null;
        this.fieldsMap = null;
        this.targetClassField = null;
        this.baseLocal = null;
        this.viewId = viewId;
    }

    public NewFindViewAnalysisObject(Local baseLocal, int viewId) {
        this.sootField = null;
        this.fieldsMap = null;
        this.targetClassField = null;
        this.baseLocal = baseLocal;
        this.viewId = viewId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewFindViewAnalysisObject that = (NewFindViewAnalysisObject) o;
        return viewId == that.viewId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewId);
    }
}
