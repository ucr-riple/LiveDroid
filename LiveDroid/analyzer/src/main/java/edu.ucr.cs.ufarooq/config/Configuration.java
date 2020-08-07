package edu.ucr.cs.ufarooq.config;

import soot.jimple.spark.SparkTransformer;
import soot.options.Options;

import java.util.HashMap;

public class Configuration {
    private static Configuration instance;

    public static Configuration v() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private boolean enableArrayAnalysis = true;
    private boolean enableStaticFieldsAnalysis = true;
    private boolean enableLiveTrackReferenceOnly = true; // true means, only make specific reference/object live.
    private boolean enableKillTrackPointer = true; // kill analysis will be on pointer, false will make it reference
    private boolean enableParametersTrackReference = true; // kill analysis will be on pointer, false will make it reference
    private boolean enableDebug = false;
    private boolean enableSparkDebug = false; // log spark output
    private boolean enablePointsTo = false;
    private int returnDepth = 1000;
    private int accessPathSize = Integer.MAX_VALUE;

    public int getReturnDepth() {
        return returnDepth;
    }

    public void setReturnDepth(int returnDepth) {
        this.returnDepth = returnDepth;
    }

    public boolean isEnableArrayAnalysis() {
        return enableArrayAnalysis;
    }

    public void setEnableArrayAnalysis(boolean enableArrayAnalysis) {
        this.enableArrayAnalysis = enableArrayAnalysis;
    }

    public boolean isEnableStaticFieldsAnalysis() {
        return enableStaticFieldsAnalysis;
    }

    public void setEnableStaticFieldsAnalysis(boolean enableStaticFieldsAnalysis) {
        this.enableStaticFieldsAnalysis = enableStaticFieldsAnalysis;
    }

    public boolean isEnableKillTrackPointer() {
        return enableKillTrackPointer;
    }

    public void setEnableKillTrackPointer(boolean enableKillTrackPointer) {
        this.enableKillTrackPointer = enableKillTrackPointer;
    }

    public boolean isEnableLiveTrackReferenceOnly() {
        return enableLiveTrackReferenceOnly;
    }

    public void setEnableLiveTrackReferenceOnly(boolean enableLiveTrackReferenceOnly) {
        this.enableLiveTrackReferenceOnly = enableLiveTrackReferenceOnly;
    }

    public boolean isEnableParametersTrackReference() {
        return enableParametersTrackReference;
    }

    public void setEnableParametersTrackReference(boolean enableParametersTrackReference) {
        this.enableParametersTrackReference = enableParametersTrackReference;
    }

    public boolean isEnableDebug() {
        return enableDebug;
    }

    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }

    public boolean isEnableSparkDebug() {
        return enableSparkDebug;
    }

    public void setEnableSparkDebug(boolean enableSparkDebug) {
        this.enableSparkDebug = enableSparkDebug;
    }

    public boolean isEnablePointsTo() {
        return enablePointsTo;
    }

    public void setEnablePointsTo(boolean enablePointsTo) {
        this.enablePointsTo = enablePointsTo;
    }

    public int getAccessPathSize() {
        return accessPathSize;
    }

    public void setAccessPathSize(int accessPathSize) {
        this.accessPathSize = accessPathSize;
    }

    public void setSparkPointsToAnalysis() {
        Options.v().set_keep_line_number(true);
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg", "verbose:true");

        System.out.println("[spark] Starting analysis ...");
        HashMap opt = new HashMap();
        opt.put("enabled", "true");
        opt.put("verbose", "true");
        opt.put("ignore-types", "false");
        opt.put("force-gc", "false");
        opt.put("pre-jimplify", "false");
        opt.put("vta", "false");
        opt.put("rta", "false");
        opt.put("field-based", "false");
        opt.put("types-for-sites", "false");
        opt.put("merge-stringbuffer", "true");
        opt.put("string-constants", "false");
        opt.put("simulate-natives", "true");
        opt.put("simple-edges-bidirectional", "false");
        opt.put("on-fly-cg", "true");
        opt.put("simplify-offline", "false");
        opt.put("simplify-sccs", "false");
        opt.put("ignore-types-for-sccs", "false");
        opt.put("propagator", "worklist");
        opt.put("set-impl", "double");
        opt.put("double-set-old", "hybrid");
        opt.put("double-set-new", "hybrid");
        opt.put("dump-html", "false");
        opt.put("dump-pag", "false");
        opt.put("dump-solution", "false");
        opt.put("topo-sort", "false");
        opt.put("dump-types", "true");
        opt.put("class-method-var", "true");
        opt.put("dump-answer", "false");
        opt.put("add-tags", "false");
        opt.put("set-mass", "false");

        SparkTransformer.v().transform("", opt);

        System.out.println("[spark] Done!");
    }
}
