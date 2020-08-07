package edu.ucr.cs.ufarooq.android;

import edu.ucr.cs.ufarooq.config.Configuration;
import soot.PackManager;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.options.Options;

import java.util.LinkedList;
import java.util.List;

public class SootConfigForAndroid implements IInfoflowConfig {

    @Override
    public void setSootOptions(Options options, InfoflowConfiguration config) {
        // explicitly include packages for shorter runtime:
        List<String> excludeList = new LinkedList<String>();
        //excludeList.add("java.*");
        excludeList.add("sun.*");
        //excludeList.add("android.*");
        excludeList.add("org.apache.*");
        excludeList.add("org.eclipse.*");
        excludeList.add("soot.*");
        excludeList.add("javax.*");
        excludeList.add("android.content.*");
        excludeList.add("android.support.*");
        options.set_exclude(excludeList);
        Options.v().set_no_bodies_for_excluded(true);

        List<String> includeList = new LinkedList<String>();
        includeList.add("java.*");
//		includeList.add("java.util.*");
        includeList.add("android.app.*");
//		includeList.add("java.nio.charset.*");
//		includeList.add("sun.util.*");
//		includeList.add("sun.nio.cs.*");
//		includeList.add("java.io.");
//		includeList.add("java.security.*");
        // includeList.add("sun.misc.");
        //includeList.add("java.net.*");
        // includeList.add("javax.servlet.");
        // includeList.add("javax.crypto.");

        includeList.add("android.app.*");
        includeList.add("android.widget.*");
        includeList.add("android.widget.TextView");
        includeList.add("android.widget.CompoundButton");
        includeList.add("android.widget.Switch");
        includeList.add("android.widget.ToggleButton");
        includeList.add("android.widget.RadioButton");
        includeList.add("android.widget.CheckedTextView");
        includeList.add("android.widget.SeekBar");
        includeList.add("android.widget.AbsSeekBar");
        includeList.add("android.widget.RatingBar");
        includeList.add("android.widget.ScrollView");
        includeList.add("android.widget.ListView");

        includeList.add("android.widget.Button");
        //includeList.add("android.graphics.*");
        //includeList.add("android.util.*");
        //includeList.add("android.content.*");
//		includeList.add("org.apache.http.");
        //includeList.add("soot.*");
        // includeList.add("com.example.");
        // includeList.add("com.jakobkontor.");
        // includeList.add("libcore.icu.");
        // includeList.add("securibench.");
        //includeList.add("com.example.umarfarooq.livedroidtest.*");
        //options.set_no_bodies_for_excluded(false);
        options.set_allow_phantom_refs(true);
        options.set_include(includeList);
        options.set_output_format(Options.output_format_none);
        options.setPhaseOption("jb", "use-original-names:true");
        options.set_ignore_classpath_errors(true);
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg.spark", "on");
        PackManager.v().getPack("cg").apply();
        Options.v().set_process_multiple_dex(true);
        Configuration.v().setSparkPointsToAnalysis();
        //BoomerangPretransformer.v().apply();
    }

}