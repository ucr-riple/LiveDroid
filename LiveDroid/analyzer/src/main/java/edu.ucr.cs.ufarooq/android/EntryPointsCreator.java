package edu.ucr.cs.ufarooq.android;

import edu.ucr.cs.ufarooq.android.layout.ViewControlProvider;
import edu.ucr.cs.ufarooq.android.problems.Utils;
import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.AbstractCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.DefaultCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.FastCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.filters.AlienFragmentFilter;
import soot.jimple.infoflow.android.callbacks.filters.AlienHostComponentFilter;
import soot.jimple.infoflow.android.callbacks.filters.ApplicationCallbackFilter;
import soot.jimple.infoflow.android.callbacks.filters.UnreachableConstructorFilter;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointCreator;
import soot.jimple.infoflow.android.iccta.IccInstrumenter;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.memory.FlowDroidMemoryWatcher;
import soot.jimple.infoflow.memory.FlowDroidTimeoutWatcher;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.values.IValueProvider;
import soot.options.Options;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EntryPointsCreator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected MultiMap<SootClass, AndroidCallbackDefinition> callbackMethods = new HashMultiMap<>();
    protected MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();

    protected InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();

    protected Set<SootClass> entrypoints = null;
    protected Set<String> callbackClasses = null;
    protected AndroidEntryPointCreator entryPointCreator = null;
    protected IccInstrumenter iccInstrumenter = null;

    protected ARSCFileParser resources = null;
    protected ProcessManifest manifest = null;
    protected IValueProvider valueProvider = null;

    protected boolean forceAndroidJar;

    protected String callbackFile = "AndroidCallbacks.txt";
    protected SootClass scView = null;

    protected Set<PreAnalysisHandler> preprocessors = new HashSet<>();

    protected IInfoflowConfig sootConfig = new SootConfigForAndroid();
    protected IIPCManager ipcManager = null;

    public EntryPointsCreator(InfoflowAndroidConfiguration config) {
        this(config, null);
    }


    public EntryPointsCreator(InfoflowAndroidConfiguration config, IIPCManager ipcManager) {
        this.config = config;
        this.ipcManager = ipcManager;

        // We can use either a specific platform JAR file or automatically
        // select the right one
        if (config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.CreateNewInstace) {
            String platformDir = config.getAnalysisFileConfig().getAndroidPlatformDir();
            if (platformDir == null || platformDir.isEmpty())
                throw new RuntimeException("Android platform directory not specified");
            File f = new File(platformDir);
            this.forceAndroidJar = f.isFile();
        } else {
            this.forceAndroidJar = false;
        }
    }

    /**
     * Creates a basic data flow configuration with only the input files set
     *
     * @param androidJar      The path to the Android SDK's "platforms" directory if
     *                        Soot shall automatically select the JAR file to be
     *                        used or the path to a single JAR file to force one.
     * @param apkFileLocation The path to the APK file to be analyzed
     * @return The new configuration
     */
    private static InfoflowAndroidConfiguration getConfig(String androidJar, String apkFileLocation) {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(apkFileLocation);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidJar);
        return config;
    }

    public void prepareForAndroid(File apk) {
        // Reset our object state

        config.getAnalysisFileConfig().setTargetAPKFile(apk.getAbsolutePath());
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.CreateNewInstace);
        // Perform some sanity checks on the configuration
        if (config.getSourceSinkConfig().getEnableLifecycleSources() && config.getIccConfig().isIccEnabled()) {
            logger.warn("ICC model specified, automatically disabling lifecycle sources");
            config.getSourceSinkConfig().setEnableLifecycleSources(false);
        }

        // Start a new Soot instance
        if (config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.CreateNewInstace) {
            G.reset();
            initializeSoot();
        }

        // Perform basic app parsing
        try {
            parseAppResources();
        } catch (IOException | XmlPullParserException e) {
            logger.error("Callgraph construction failed", e);
            throw new RuntimeException("Callgraph construction failed", e);
        }

        //MultiRunResultAggregator resultAggregator = new MultiRunResultAggregator();

        // We need at least one entry point
        if (entrypoints == null || entrypoints.isEmpty()) {
            logger.warn("No entry points");
            return;
        }

        // In one-component-at-a-time, we do not have a single entry point
        // creator. For every entry point, run the data flow analysis.
        if (config.getOneComponentAtATime()) {
            List<SootClass> entrypointWorklist = new ArrayList<>(entrypoints);
            while (!entrypointWorklist.isEmpty()) {
                SootClass entrypoint = entrypointWorklist.remove(0);
                processEntryPoint(/*sourcesAndSinks, resultAggregator,*/  entrypoint);
            }
        } else
            processEntryPoint(/*sourcesAndSinks, resultAggregator,*/ null);

       /* // Write the results to disk if requested
        serializeResults(resultAggregator.getAggregatedResults(), resultAggregator.getLastICFG());

        // We return the aggregated results
        this.infoflow = null;
        resultAggregator.clearLastResults();
        return resultAggregator.getAggregatedResults();*/
    }

    protected void processEntryPoint(SootClass entrypoint) {
        long beforeEntryPoint = System.nanoTime();

        // Get rid of leftovers from the last entry point
        //     resultAggregator.clearLastResults();

        // Perform basic app parsing
        long callbackDuration = System.nanoTime();
        try {
            if (config.getOneComponentAtATime())
                calculateCallbacks(/*sourcesAndSinks,*/ entrypoint);
            else
                calculateCallbacks(/*sourcesAndSinks*/);
        } catch (IOException | XmlPullParserException e) {
            logger.error("Callgraph construction failed: " + e.getMessage(), e);
            throw new RuntimeException("Callgraph construction failed", e);
        }
        callbackDuration = Math.round((System.nanoTime() - callbackDuration) / 1E9);
        logger.info(
                String.format("Collecting callbacks and building a callgraph took %d seconds", (int) callbackDuration));

/*        final Set<SourceSinkDefinition> sources = getSources();
       final Set<SourceSinkDefinition> sinks = getSinks();
        final String apkFileLocation = config.getAnalysisFileConfig().getTargetAPKFile();

        if (config.getOneComponentAtATime())
            logger.info("Running data flow analysis on {} (component {}/{}: {}) with {} sources and {} sinks...",
                    apkFileLocation, (entrypoints.size() - numEntryPoints), entrypoints.size(), entrypoint,
                    sources == null ? 0 : sources.size(), sinks == null ? 0 : sinks.size());
        else
            logger.info("Running data flow analysis on {} with {} sources and {} sinks...", apkFileLocation,
                    sources == null ? 0 : sources.size(), sinks == null ? 0 : sinks.size());*/

        // Create a new entry point and compute the flows in it. If we
        // analyze all components together, we do not need a new callgraph,
        // but can reuse the one from the callback collection phase.
        if (config.getOneComponentAtATime() && needsToBuildCallgraph()) {
            createMainMethod(entrypoint);
            constructCallgraphInternal();
        }
/*
        // Create and run the data flow tracker
        infoflow = createInfoflow();
        infoflow.addResultsAvailableHandler(resultAggregator);
        infoflow.runAnalysis(sourceSinkManager, entryPointCreator.getGeneratedMainMethod());

        // Update the statistics
        if (config.getLogSourcesAndSinks() && infoflow.getCollectedSources() != null)
            this.collectedSources.addAll(infoflow.getCollectedSources());
        if (config.getLogSourcesAndSinks() && infoflow.getCollectedSinks() != null)
            this.collectedSinks.addAll(infoflow.getCollectedSinks());

        // Print out the found results
        {
            int resCount = resultAggregator.getLastResults() == null ? 0 : resultAggregator.getLastResults().size();
            if (config.getOneComponentAtATime())
                logger.info("Found {} leaks for component {}", resCount, entrypoint);
            else
                logger.info("Found {} leaks", resCount);
        }

        // Update the performance object with the real data
        {
            InfoflowResults lastResults = resultAggregator.getLastResults();
            if (lastResults != null) {
                InfoflowPerformanceData perfData = lastResults.getPerformanceData();
                perfData.setCallgraphConstructionSeconds((int) callbackDuration);
                perfData.setTotalRuntimeSeconds((int) Math.round((System.nanoTime() - beforeEntryPoint) / 1E9));
            }
        }*/

        // We don't need the computed callbacks anymore
        this.callbackMethods.clear();
        this.fragmentClasses.clear();

        /*// Notify our result handlers
        for (ResultsAvailableHandler handler : resultsAvailableHandlers)
            handler.onResultsAvailable(resultAggregator.getLastICFG(), resultAggregator.getLastResults());*/
    }


    /**
     * Parses common app resources such as the manifest file
     *
     * @throws IOException            Thrown if the given source/sink file could not
     *                                be read.
     * @throws XmlPullParserException Thrown if the Android manifest file could not
     *                                be read.
     */
    protected void parseAppResources() throws IOException, XmlPullParserException {
        final String targetAPK = config.getAnalysisFileConfig().getTargetAPKFile();

        // To look for callbacks, we need to start somewhere. We use the Android
        // lifecycle methods for this purpose.
        this.manifest = new ProcessManifest(targetAPK);
        Set<String> entryPoints = manifest.getEntryPointClasses();
        this.entrypoints = new HashSet<>(entryPoints.size());
        for (String className : entryPoints) {
            SootClass sc = Scene.v().getSootClassUnsafe(className);
            if (sc != null)
                this.entrypoints.add(sc);
        }

        // Parse the resource file
        long beforeARSC = System.nanoTime();
        this.resources = new ARSCFileParser();
        this.resources.parse(targetAPK);
        logger.info("ARSC file parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds");
    }

    /**
     * Initializes soot for running the soot-based phases of the application
     * metadata analysis
     */
    private void initializeSoot() {
        logger.info("Initializing Soot...");

        final String androidJar = config.getAnalysisFileConfig().getAndroidPlatformDir();
        final String apkFileLocation = config.getAnalysisFileConfig().getTargetAPKFile();

        // Clean up any old Soot instance we may have
        G.reset();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        if (config.getWriteOutputFiles())
            Options.v().set_output_format(Options.output_format_jimple);
        else
            Options.v().set_output_format(Options.output_format_none);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
        if (forceAndroidJar)
            Options.v().set_force_android_jar(androidJar);
        else
            Options.v().set_android_jars(androidJar);
        Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
        Options.v().set_keep_line_number(false);
        Options.v().set_keep_offset(false);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        //Options.v().set_process_multiple_dex(config.getMergeDexFiles());
        Options.v().set_process_multiple_dex(true);
        Options.v().set_ignore_resolution_errors(true);

        // Set the Soot configuration options. Note that this will needs to be
        // done before we compute the classpath.
        if (sootConfig != null)
            sootConfig.setSootOptions(Options.v(), config);

        Options.v().set_soot_classpath(getClasspath());
        Main.v().autoSetOptions();

        configureCallgraph();

        // Load whatever we need
        logger.info("Loading dex files...");
        Scene.v().loadNecessaryClasses();

        // Make sure that we have valid Jimple bodies
        PackManager.v().getPack("wjpp").apply();

        // apply callgraph--custom, to make aligned with SPARK
        //PackManager.v().getPack("cg").apply();
        // Patch the callgraph to support additional edges. We do this now,
        // because during callback discovery, the context-insensitive callgraph
        // algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
    }


    /**
     * Builds the classpath for this analysis
     *
     * @return The classpath to be used for the taint analysis
     */
    private String getClasspath() {
        final String androidJar = config.getAnalysisFileConfig().getAndroidPlatformDir();
        final String apkFileLocation = config.getAnalysisFileConfig().getTargetAPKFile();
        final String additionalClasspath = config.getAnalysisFileConfig().getAdditionalClasspath();

        String classpath = forceAndroidJar ? androidJar : Scene.v().getAndroidJarPath(androidJar, apkFileLocation);
        if (additionalClasspath != null && !additionalClasspath.isEmpty())
            classpath += File.pathSeparator + additionalClasspath;
        logger.debug("soot classpath: " + classpath);
        return classpath;
    }

    /**
     * Releases the callgraph and all intermediate objects associated with it
     */
    protected void releaseCallgraph() {
        // If we are configured to use an existing callgraph, we may not release
        // it
//        if (config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingCallgraph)
//            return;

        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseReachableMethods();
        G.v().resetSpark();
    }


    /**
     * Creates the main method based on the current callback information, injects it
     * into the Soot scene.
     *
     * @param component class name of a component to create a main method containing only
     *                  that component, or null to create main method for all components
     */
    private void createMainMethod(SootClass component) {
        // There is no need to create a main method if we don't want to generate
        // a callgraph
        if (config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingCallgraph)
            return;

        // Always update the entry point creator to reflect the newest set
        // of callback methods
        entryPointCreator = createEntryPointCreator(component);
        SootMethod dummyMainMethod = entryPointCreator.createDummyMain();
        Scene.v().setEntryPoints(Collections.singletonList(dummyMainMethod));
        if (!dummyMainMethod.getDeclaringClass().isInScene())
            Scene.v().addClass(dummyMainMethod.getDeclaringClass());

        // addClass() declares the given class as a library class. We need to
        // fix this.
        //System.out.println("Dummy::" + dummyMainMethod.getDeclaringClass() + ":" + dummyMainMethod.getName());
        dummyMainMethod.getDeclaringClass().setApplicationClass();
        if (!Scene.v().getEntryPoints().contains(dummyMainMethod)) {
            Scene.v().getEntryPoints().add(dummyMainMethod);
            //Scene.v().setEntryPoints(Collections.singletonList(dummyMainMethod));
        }
    }

    /**
     * Creates the {@link AndroidEntryPointCreator} instance which will later create
     * the dummy main method for the analysis
     *
     * @param component The single component to include in the dummy main method.
     *                  Pass null to include all components in the dummy main
     *                  method.
     * @return The {@link AndroidEntryPointCreator} responsible for generating the
     * dummy main method
     */
    private AndroidEntryPointCreator createEntryPointCreator(SootClass component) {
        Set<SootClass> components = getComponentsToAnalyze(component);

        // If we we already have an entry point creator, we make sure to clean up our
        // leftovers from previous runs
        if (entryPointCreator == null)
            entryPointCreator = new AndroidEntryPointCreator(manifest, components);
        else {
            entryPointCreator.removeGeneratedMethods(false);
            entryPointCreator.reset();
        }

        MultiMap<SootClass, SootMethod> callbackMethodSigs = new HashMultiMap<>();
        if (component == null) {
            // Get all callbacks for all components
            for (SootClass sc : this.callbackMethods.keySet()) {
                Set<AndroidCallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
                if (callbackDefs != null)
                    for (AndroidCallbackDefinition cd : callbackDefs)
                        callbackMethodSigs.put(sc, cd.getTargetMethod());
            }
        } else {
            // Get the callbacks for the current component only
            for (SootClass sc : components) {
                Set<AndroidCallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
                if (callbackDefs != null)
                    for (AndroidCallbackDefinition cd : callbackDefs)
                        callbackMethodSigs.put(sc, cd.getTargetMethod());
            }
        }
        entryPointCreator.setCallbackFunctions(callbackMethodSigs);
        entryPointCreator.setFragments(fragmentClasses);
        entryPointCreator.setComponents(components);
        return entryPointCreator;
    }

    /**
     * Gets the components to analyze. If the given component is not null, we assume
     * that only this component and the application class (if any) shall be
     * analyzed. Otherwise, all components are to be analyzed.
     *
     * @param component A component class name to only analyze this class and the
     *                  application class (if any), or null to analyze all classes.
     * @return The set of classes to analyze
     */
    private Set<SootClass> getComponentsToAnalyze(SootClass component) {
        if (component == null)
            return this.entrypoints;
        else {
            // We always analyze the application class together with each
            // component
            // as there might be interactions between the two
            Set<SootClass> components = new HashSet<>(2);
            components.add(component);

            String applicationName = manifest.getApplicationName();
            if (applicationName != null && !applicationName.isEmpty())
                components.add(Scene.v().getSootClassUnsafe(applicationName));
            return components;
        }
    }

    /**
     * Triggers the callgraph construction in Soot
     */
    private void constructCallgraphInternal() {
        // If we are configured to use an existing callgraph, we may not replace
        // it. However, we must make sure that there really is one.
        if (config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingCallgraph) {
            if (!Scene.v().hasCallGraph())
                throw new RuntimeException("FlowDroid is configured to use an existing callgraph, but there is none");
            return;
        }

        // Do we need ICC instrumentation?
        if (config.getIccConfig().isIccEnabled()) {
            if (iccInstrumenter == null)
                iccInstrumenter = createIccInstrumenter();
            iccInstrumenter.onBeforeCallgraphConstruction();
        }

        // Run the preprocessors
        for (PreAnalysisHandler handler : this.preprocessors)
            handler.onBeforeCallgraphConstruction();

        // Make sure that we don't have any weird leftovers
        releaseCallgraph();

        // If we didn't create the Soot instance, we can't be sure what its callgraph
        // configuration is
        if (config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance)
            configureCallgraph();

        // Construct the actual callgraph
        logger.info("Constructing the callgraph...");
        PackManager.v().getPack("cg").apply();

        // ICC instrumentation
        if (iccInstrumenter != null)
            iccInstrumenter.onAfterCallgraphConstruction();

        // Run the preprocessors
        for (PreAnalysisHandler handler : this.preprocessors)
            handler.onAfterCallgraphConstruction();

        // Make sure that we have a hierarchy
        Scene.v().getOrMakeFastHierarchy();
    }

    /**
     * Creates the ICC instrumentation class
     *
     * @return An instance of the class for instrumenting the app's code for
     * inter-component communication
     */
    protected IccInstrumenter createIccInstrumenter() {
        IccInstrumenter iccInstrumenter;
        iccInstrumenter = new IccInstrumenter(config.getIccConfig().getIccModel(),
                entryPointCreator.getGeneratedMainMethod().getDeclaringClass(),
                entryPointCreator.getComponentToEntryPointInfo());
        return iccInstrumenter;
    }

    /**
     * Configures the callgraph options for Soot according to FlowDroid's settings
     */
    protected void configureCallgraph() {
        // Configure the callgraph algorithm
        switch (config.getCallgraphAlgorithm()) {
            case AutomaticSelection:
            case SPARK:
                Options.v().setPhaseOption("cg.spark", "on");
                break;
            case GEOM:
                Options.v().setPhaseOption("cg.spark", "on");
                AbstractInfoflow.setGeomPtaSpecificOptions();
                break;
            case CHA:
                Options.v().setPhaseOption("cg.cha", "on");
                break;
            case RTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "rta:true");
                Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
                break;
            case VTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "vta:true");
                break;
            default:
                throw new RuntimeException("Invalid callgraph algorithm");
        }
        if (config.getEnableReflection())
            Options.v().setPhaseOption("cg", "types-for-invoke:true");
    }

    /**
     * Calculates the sets of sources, sinks, entry points, and callbacks methods
     * for the given APK file.
     *
     * @throws IOException            Thrown if the given source/sink file could not
     *                                be read.
     * @throws XmlPullParserException Thrown if the Android manifest file could not
     *                                be read.
     */
    private void calculateCallbacks()
            throws IOException, XmlPullParserException {
        calculateCallbacks(null);
    }

    /**
     * Calculates the sets of sources, sinks, entry points, and callbacks methods
     * for the entry point in the given APK file.
     *
     * @param entryPoint The entry point for which to calculate the callbacks.
     *                   Pass null to calculate callbacks for all entry points.
     * @throws IOException            Thrown if the given source/sink file could not
     *                                be read.
     * @throws XmlPullParserException Thrown if the Android manifest file could not
     *                                be read.
     */
    private void calculateCallbacks(SootClass entryPoint)
            throws IOException, XmlPullParserException {
        // Add the callback methods
        LayoutFileParser lfp = null;
        if (config.getCallbackConfig().getEnableCallbacks()) {
            if (callbackClasses != null && callbackClasses.isEmpty()) {
                logger.warn("Callback definition file is empty, disabling callbacks");
            } else {
                lfp = createLayoutFileParser();
                switch (config.getCallbackConfig().getCallbackAnalyzer()) {
                    case Fast:
                        calculateCallbackMethodsFast(lfp, entryPoint);
                        break;
                    case Default:
                        calculateCallbackMethods(lfp, entryPoint);
                        break;
                    default:
                        throw new RuntimeException("Unknown callback analyzer");
                }
            }
        } else if (needsToBuildCallgraph()) {
            // Create the new iteration of the main method
            createMainMethod(null);
            constructCallgraphInternal();
        }

        logger.info("Entry point calculation done.");

        /*if (this.sourceSinkProvider != null) {
            // Get the callbacks for the current entry point
            Set<CallbackDefinition> callbacks;
            if (entryPoint == null)
                callbacks = this.callbackMethods.values();
            else
                callbacks = this.callbackMethods.get(entryPoint);

            // Create the SourceSinkManager
            sourceSinkManager = createSourceSinkManager(lfp, callbacks);
        }*/
    }

    /**
     * Calculates the set of callback methods declared in the XML resource files or
     * the app's source code. This method prefers performance over precision and
     * scans the code including unreachable methods.
     *
     * @param lfp       The layout file parser to be used for analyzing UI controls
     * @param component The entry point for which to calculate the callbacks. Pass
     *                  null to calculate callbacks for all entry points.
     * @throws IOException Thrown if a required configuration cannot be read
     */
    private void calculateCallbackMethodsFast(LayoutFileParser lfp, SootClass component) throws IOException {
        // Construct the current callgraph
        releaseCallgraph();
        createMainMethod(component);
        constructCallgraphInternal();

        // Get the classes for which to find callbacks
        Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

        // Collect the callback interfaces implemented in the app's
        // source code
        AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
                ? new FastCallbackAnalyzer(config, entryPointClasses, callbackFile)
                : new FastCallbackAnalyzer(config, entryPointClasses, callbackClasses);
        if (valueProvider != null)
            jimpleClass.setValueProvider(valueProvider);
        jimpleClass.collectCallbackMethods();

        // Collect the results
        this.callbackMethods.putAll(jimpleClass.getCallbackMethods());

        this.entrypoints.addAll(jimpleClass.getDynamicManifestComponents());

        // Find the user-defined sources in the layout XML files. This
        // only needs to be done once, but is a Soot phase.
        lfp.parseLayoutFileDirect(config.getAnalysisFileConfig().getTargetAPKFile());

        // Collect the XML-based callback methods
        collectXmlBasedCallbackMethods(lfp, jimpleClass);

        // Construct the final callgraph
        releaseCallgraph();
        createMainMethod(component);
        constructCallgraphInternal();
    }

    /**
     * Creates a new layout file parser. Derived classes can override this method to
     * supply their own parser.
     *
     * @return The newly created layout file parser.
     */
    protected LayoutFileParser createLayoutFileParser() {
        return new LayoutFileParser(this.manifest.getPackageName(), this.resources);
    }


    /**
     * Calculates the set of callback methods declared in the XML resource files or
     * the app's source code
     *
     * @param lfp       The layout file parser to be used for analyzing UI controls
     * @param component The Android component for which to compute the callbacks.
     *                  Pass null to compute callbacks for all components.
     * @throws IOException Thrown if a required configuration cannot be read
     */
    private void calculateCallbackMethods(LayoutFileParser lfp, SootClass component) throws IOException {
        final InfoflowAndroidConfiguration.CallbackConfiguration callbackConfig = config.getCallbackConfig();

        // Load the APK file
        if (needsToBuildCallgraph())
            releaseCallgraph();

        // Make sure that we don't have any leftovers from previous runs
        PackManager.v().getPack("wjtp").remove("wjtp.lfp");
        PackManager.v().getPack("wjtp").remove("wjtp.ajc");

        // Get the classes for which to find callbacks
        Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

        // Collect the callback interfaces implemented in the app's
        // source code. Note that the filters should know all components to
        // filter out callbacks even if the respective component is only
        // analyzed later.
        AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
                ? new DefaultCallbackAnalyzer(config, entryPointClasses, callbackFile)
                : new DefaultCallbackAnalyzer(config, entryPointClasses, callbackClasses);
        if (valueProvider != null)
            jimpleClass.setValueProvider(valueProvider);
        jimpleClass.addCallbackFilter(new AlienHostComponentFilter(entrypoints));
        jimpleClass.addCallbackFilter(new ApplicationCallbackFilter(entrypoints));
        jimpleClass.addCallbackFilter(new UnreachableConstructorFilter());
        jimpleClass.collectCallbackMethods();

        // Find the user-defined sources in the layout XML files. This
        // only needs to be done once, but is a Soot phase.
        lfp.parseLayoutFile(config.getAnalysisFileConfig().getTargetAPKFile());

        // Watch the callback collection algorithm's memory consumption
        FlowDroidMemoryWatcher memoryWatcher = null;
        FlowDroidTimeoutWatcher timeoutWatcher = null;
        if (jimpleClass instanceof IMemoryBoundedSolver) {
            memoryWatcher = new FlowDroidMemoryWatcher(config.getMemoryThreshold());
            memoryWatcher.addSolver((IMemoryBoundedSolver) jimpleClass);

            // Make sure that we don't spend too much time in the callback
            // analysis
            if (callbackConfig.getCallbackAnalysisTimeout() > 0) {
                timeoutWatcher = new FlowDroidTimeoutWatcher(callbackConfig.getCallbackAnalysisTimeout());
                timeoutWatcher.addSolver((IMemoryBoundedSolver) jimpleClass);
                timeoutWatcher.start();
            }
        }

        try {
            int depthIdx = 0;
            boolean hasChanged = true;
            boolean isInitial = true;
            while (hasChanged) {
                hasChanged = false;

                // Check whether the solver has been aborted in the meantime
                if (jimpleClass instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) jimpleClass).isKilled())
                        break;
                }

                // Create the new iteration of the main method
                createMainMethod(component);

                // Since the gerenation of the main method can take some time,
                // we check again whether we need to stop.
                if (jimpleClass instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) jimpleClass).isKilled())
                        break;
                }

                if (!isInitial) {
                    // Reset the callgraph
                    releaseCallgraph();

                    // We only want to parse the layout files once
                    PackManager.v().getPack("wjtp").remove("wjtp.lfp");
                }
                isInitial = false;

                //todo: dummy load instructions creator
                this.callbackMethods.forEach(v -> {
                    System.out.println("CallbackMethods: " + v.getO1().getName() + ":" + v.getO2().getTargetMethod().getSubSignature());
                    Utils.addDummyToCallback(v.getO2().getTargetMethod());
                });

                // Run the soot-based operations
                constructCallgraphInternal();
                if (!Scene.v().hasCallGraph())
                    throw new RuntimeException("No callgraph in Scene even after creating one. That's very sad "
                            + "and should never happen.");
                PackManager.v().getPack("wjtp").apply();

                // Creating all callgraph takes time and memory. Check whether
                // the solver has been aborted in the meantime
                if (jimpleClass instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
                        logger.warn("Aborted callback collection because of low memory");
                        break;
                    }
                }

                // Collect the results of the soot-based phases
                if (this.callbackMethods.putAll(jimpleClass.getCallbackMethods()))
                    hasChanged = true;

                if (entrypoints.addAll(jimpleClass.getDynamicManifestComponents()))
                    hasChanged = true;

                // Collect the XML-based callback methods
                if (collectXmlBasedCallbackMethods(lfp, jimpleClass))
                    hasChanged = true;

                // Avoid callback overruns. If we are beyond the callback limit
                // for one entry point, we may not collect any further callbacks
                // for that entry point.
                if (callbackConfig.getMaxCallbacksPerComponent() > 0) {
                    for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt
                            .hasNext(); ) {
                        SootClass callbackComponent = componentIt.next();
                        if (this.callbackMethods.get(callbackComponent).size() > callbackConfig
                                .getMaxCallbacksPerComponent()) {
                            componentIt.remove();
                            jimpleClass.excludeEntryPoint(callbackComponent);
                        }
                    }
                }

                // Check depth limiting
                depthIdx++;
                if (callbackConfig.getMaxAnalysisCallbackDepth() > 0
                        && depthIdx >= callbackConfig.getMaxAnalysisCallbackDepth())
                    break;

                // If we work with an existing callgraph, the callgraph never
                // changes and thus it doesn't make any sense to go multiple
                // rounds
                if (config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingCallgraph)
                    break;
            }
        } catch (Exception ex) {
            logger.error("Could not calculate callback methods", ex);
            throw ex;
        } finally {
            // Shut down the watchers
            if (timeoutWatcher != null)
                timeoutWatcher.stop();
            if (memoryWatcher != null)
                memoryWatcher.close();
        }

        // Filter out callbacks that belong to fragments that are not used by
        // the host activity
        AlienFragmentFilter fragmentFilter = new AlienFragmentFilter(invertMap(fragmentClasses));
        fragmentFilter.reset();
        for (Iterator<Pair<SootClass, AndroidCallbackDefinition>> cbIt = this.callbackMethods.iterator(); cbIt.hasNext(); ) {
            Pair<SootClass, AndroidCallbackDefinition> pair = cbIt.next();

            // Check whether the filter accepts the given mapping
            if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod()))
                cbIt.remove();
            else if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod().getDeclaringClass())) {
                cbIt.remove();
            }
        }

        // Avoid callback overruns
        if (callbackConfig.getMaxCallbacksPerComponent() > 0) {
            for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt.hasNext(); ) {
                SootClass callbackComponent = componentIt.next();
                if (this.callbackMethods.get(callbackComponent).size() > callbackConfig.getMaxCallbacksPerComponent())
                    componentIt.remove();
            }
        }

        // Make sure that we don't retain any weird Soot phases
        PackManager.v().getPack("wjtp").remove("wjtp.lfp");
        PackManager.v().getPack("wjtp").remove("wjtp.ajc");

        // Warn the user if we had to abort the callback analysis early
        boolean abortedEarly = false;
        if (jimpleClass instanceof IMemoryBoundedSolver) {
            if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
                logger.warn("Callback analysis aborted early due to time or memory exhaustion");
                abortedEarly = true;
            }
        }
        if (!abortedEarly)
            logger.info("Callback analysis terminated normally");
    }

    /**
     * Collects the XML-based callback methods, e.g., Button.onClick() declared in
     * layout XML files
     *
     * @param lfp         The layout file parser
     * @param jimpleClass The analysis class that gives us a mapping between layout
     *                    IDs and components
     * @return True if at least one new callback method has been added, otherwise
     * false
     */
    private boolean collectXmlBasedCallbackMethods(LayoutFileParser lfp, AbstractCallbackAnalyzer jimpleClass) {
        SootMethod smViewOnClick = Scene.v()
                .grabMethod("<android.view.View$OnClickListener: void onClick(android.view.View)>");

        // Collect the XML-based callback methods
        boolean hasNewCallback = false;
        for (final SootClass callbackClass : jimpleClass.getLayoutClasses().keySet()) {
            if (jimpleClass.isExcludedEntryPoint(callbackClass))
                continue;

            Set<Integer> classIds = jimpleClass.getLayoutClasses().get(callbackClass);
            for (Integer classId : classIds) {
                ARSCFileParser.AbstractResource resource = this.resources.findResource(classId);
                if (resource instanceof ARSCFileParser.StringResource) {
                    final String layoutFileName = ((ARSCFileParser.StringResource) resource).getValue();

                    // Add the callback methods for the given class
                    Set<String> callbackMethods = lfp.getCallbackMethods().get(layoutFileName);
                    if (callbackMethods != null) {
                        for (String methodName : callbackMethods) {
                            final String subSig = "void " + methodName + "(android.view.View)";

                            // The callback may be declared directly in the
                            // class or in one of the superclasses
                            SootClass currentClass = callbackClass;
                            while (true) {
                                SootMethod callbackMethod = currentClass.getMethodUnsafe(subSig);
                                if (callbackMethod != null) {
                                    if (this.callbackMethods.put(callbackClass,
                                            new AndroidCallbackDefinition(callbackMethod, smViewOnClick, AndroidCallbackDefinition.CallbackType.Widget)))
                                        hasNewCallback = true;
                                    break;
                                }
                                SootClass sclass = currentClass.getSuperclassUnsafe();
                                if (sclass == null) {
                                    logger.error(String.format("Callback method %s not found in class %s", methodName,
                                            callbackClass.getName()));
                                    break;
                                }
                                currentClass = sclass;
                            }
                        }
                    }

                    // Add the fragments for this class
                    Set<SootClass> fragments = lfp.getFragments().get(layoutFileName);
                    if (fragments != null)
                        for (SootClass fragment : fragments)
                            if (fragmentClasses.put(callbackClass, fragment))
                                hasNewCallback = true;

                    // For user-defined views, we need to emulate their
                    // callbacks
                    Set<AndroidLayoutControl> controls = lfp.getUserControls().get(layoutFileName);
                    ViewControlProvider.getInstance().insert(classId, controls);
                    if (controls != null) {
                        for (AndroidLayoutControl lc : controls)
                            if (!SystemClassHandler.v().isClassInSystemPackage(lc.getViewClass().getName()))
                                registerCallbackMethodsForView(callbackClass, lc);
                    }
                } else
                    logger.error("Unexpected resource type for layout class");
            }
        }

        // Collect the fragments, merge the fragments created in the code with
        // those declared in Xml files
        if (fragmentClasses.putAll(jimpleClass.getFragmentClasses())) // Fragments
            // declared
            // in
            // code
            hasNewCallback = true;

        return hasNewCallback;
    }

    /**
     * Registers the callback methods in the given layout control so that they are
     * included in the dummy main method
     *
     * @param callbackClass The class with which to associate the layout callbacks
     * @param lc            The layout control whose callbacks are to be associated
     *                      with the given class
     */
    private void registerCallbackMethodsForView(SootClass callbackClass, AndroidLayoutControl lc) {
        // Ignore system classes
        if (SystemClassHandler.v().isClassInSystemPackage(callbackClass.getName()))
            return;

        // Get common Android classes
        if (scView == null)
            scView = Scene.v().getSootClass("android.view.View");

        // Check whether the current class is actually a view
        if (!Scene.v().getOrMakeFastHierarchy().canStoreType(lc.getViewClass().getType(), scView.getType()))
            return;

        // There are also some classes that implement interesting callback
        // methods.
        // We model this as follows: Whenever the user overwrites a method in an
        // Android OS class, we treat it as a potential callback.
        SootClass sc = lc.getViewClass();
        Map<String, SootMethod> systemMethods = new HashMap<>(10000);
        for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sc)) {
            if (parentClass.getName().startsWith("android."))
                for (SootMethod sm : parentClass.getMethods())
                    if (!sm.isConstructor())
                        systemMethods.put(sm.getSubSignature(), sm);
        }

        // Scan for methods that overwrite parent class methods
        for (SootMethod sm : sc.getMethods()) {
            if (!sm.isConstructor()) {
                SootMethod parentMethod = systemMethods.get(sm.getSubSignature());
                if (parentMethod != null)
                    // This is a real callback method
                    this.callbackMethods.put(callbackClass,
                            new AndroidCallbackDefinition(sm, parentMethod, AndroidCallbackDefinition.CallbackType.Widget));
            }
        }
    }

    /**
     * Inverts the given {@link MultiMap}. The keys become values and vice versa
     *
     * @param original The map to invert
     * @return An inverted copy of the given map
     */
    private <K, V> MultiMap<K, V> invertMap(MultiMap<V, K> original) {
        MultiMap<K, V> newTag = new HashMultiMap<>();
        for (V key : original.keySet())
            for (K value : original.get(key))
                newTag.put(value, key);
        return newTag;
    }

    boolean needsToBuildCallgraph() {
        return config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.CreateNewInstace || config.getSootIntegrationMode() == InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance;
    }

}
