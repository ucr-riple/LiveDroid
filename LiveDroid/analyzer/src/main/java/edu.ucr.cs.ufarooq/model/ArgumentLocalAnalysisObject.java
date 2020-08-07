package edu.ucr.cs.ufarooq.model;

import soot.Local;

public class ArgumentLocalAnalysisObject {

    private int index;
    private Local argLocal;
    private Local paramLocal;
    private AnalysisObject analysisObject;

    public ArgumentLocalAnalysisObject() {
        index = -1;
        argLocal = null;
        paramLocal = null;
        analysisObject = null;
    }

    public ArgumentLocalAnalysisObject(int index, Local argLocal, Local paramLocal, AnalysisObject analysisObject) {
        this.index = index;
        this.argLocal = argLocal;
        this.paramLocal = paramLocal;
        this.analysisObject = analysisObject;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Local getArgLocal() {
        return argLocal;
    }

    public void setArgLocal(Local argLocal) {
        this.argLocal = argLocal;
    }

    public Local getParamLocal() {
        return paramLocal;
    }

    public void setParamLocal(Local paramLocal) {
        this.paramLocal = paramLocal;
    }

    public AnalysisObject getAnalysisObject() {
        return analysisObject;
    }

    public void setAnalysisObject(AnalysisObject analysisObject) {
        this.analysisObject = analysisObject;
    }
}
