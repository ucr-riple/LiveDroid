package edu.ucr.cs.ufarooq.model;

import soot.Local;

public class LocalAndAnalysisObject {
    private Local local;
    private AnalysisObject analysisObject;

    public LocalAndAnalysisObject() {
        local = null;
        analysisObject = null;
    }

    public LocalAndAnalysisObject(Local local, AnalysisObject analysisObject) {
        this.local = local;
        this.analysisObject = analysisObject;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public AnalysisObject getAnalysisObject() {
        return analysisObject;
    }

    public void setAnalysisObject(AnalysisObject analysisObject) {
        this.analysisObject = analysisObject;
    }
}
