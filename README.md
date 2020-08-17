[![Actions Status](https://github.com/ucr-riple/LiveDroid/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/ucr-riple/LiveDroid/actions)

## Getting Started
#### Overview
- ``/LiveDroid`` contains source code for static analyzer, patching tool and IDE plugin.
- ``/LiveDroidTestApp`` has a sample file to test basic configurations. 
- ``/LiveDroid-Patching`` contains the config file and other helper tools for the patching tool.

#### Prerequisites
- Java installation and should be on path.
- Android SDK installation, and set ``$ANDROID_HOME``  to Android SDK.
- APKTool should be on path (required for patching tool).

#### Compile
- Clone this repo and move to LiveDroid source directory ``cd /LiveDroid``
- Compile Static analyzer using ``./gradlew :analyzer:shadowJar``
- Similarly, compile patching tool using  ``./gradlew :patcher:shadowJar``
- And finally build Android Studio (IntelliJ) IDE Plugin with ``./gradlew :plugin:buildPlugin``
- If all of the above succeeded, two jar files for analyzer and patcher should be at ``analyzer/build/libs/analyzer-1.0-SNAPSHOT-all.jar`` and ``patcher/build/libs/patcher-1.0-SNAPSHOT-all.jar`` respectively. IDE Plugin build should produce a zip file at ``plugin/build/distributions/plugin-1.0-SNAPSHOT.zip``.

#### Configure (almost there!)
We provide a ``config.properties``  under /LiveDroid-Patching.  Which points to tools inside the directory and some in Android SDK. Which looks like as:  *(Please, replace relative paths with absolute paths)*
```bash
classLocation=../LiveDroid-Patching/gson-2.8.6.dex  
outDirectory= /patch-out  
signapkJar=../LiveDroid-Patching/tools/signapk.jar  
platformPem=../LiveDroid-Patching/tools/platform.x509.pem  
platformpk8=../LiveDroid-Patching/tools/platform.pk8  
keyStore=../LiveDroid-Patching/keystore.jks  
zipalign=ANDROID_HOME/build-tools/27.0.0/zipalign  
androidJar=ANDROID_HOME/platforms
```
Here, one of the most important is outDirectory, where a patched APK file will be produced.
#### Build Basic Android App
move to LiveDroidTestApp directory, and build using ``./gradlew :livedroidtest:assembleDebug`` or build APK in Android Studio/IntelliJ IDE.
APK file should be available at: ``LiveDroidTestApp/livedroidtest/build/outputs/apk/debug/livedroidtest-debug.apk ``
#### Run 
First run static analysis and then use an analysis report for IDE plugin or Patching tool.

#####  Analyzer
Move to ``/LiveDroid`` and execute the following command to test analysis for generated APK. (parameter -a can also be a directory to process all APK files inside). All other FlowDroid's configurations are also available to run analysis faster.

```bash
java -cp "analyzer/build/libs/analyzer-1.0-SNAPSHOT-all.jar" edu.ucr.cs.ufarooq.android.MainClass -p $ANDROID_HOME/platforms -a ../LiveDroidTestApp/livedroidtest/build/outputs/apk/debug/livedroidtest-debug.apk -s SourcesAndSinks.txt -ap 5	
```
If the analysis went well, it will generate the following files under the APK file directory (i.e., LiveDroidTestApp/livedroidtest/build/outputs/apk/debug/).

- ``LiveDroid.csv`` is a summary for the analysis results.
- When analysis identifies App States, it will place a detailed xml file under ``out-NonEmpty``, otherwise it will go under ``out-Empty``. 
- Sample static analysis XML report file.

```xml
<CriticalData>  
 <AccessPath length="3">  
	 <Field class="com.example.umarfarooq.livedroidtest.MainActivity" index="0" name="a"  
	  primitive="false" type="com.example.umarfarooq.livedroidtest.A"/>  
	 <Field class="com.example.umarfarooq.livedroidtest.A" index="1" name="b" primitive="false"  
	  type="com.example.umarfarooq.livedroidtest.B"/>  
	 <Field class="com.example.umarfarooq.livedroidtest.B" index="2" name="f" primitive="true"  
	  type="int"/>  
 </AccessPath>
</CriticalData>
...
<Views>  
 <View id="2131165263" name="maintextView" type="android.widget.TextView">  
	 <Property getter="java.lang.CharSequence getText()" name="Text"  
	  setter="void setText(java.lang.CharSequence)" toString="true"/>  
 </View>
</Views>
```

##### Patching Tool
You can use the patching tool to generate patched APK based on static analysis results. Patching tool takes 3 parameters.

- A config.properties discussed above.
- An APK file which was used for static analyzer. 
- XML report generated by static analyzer.

You can invoke the patching tool using the following command and replace "xml-file-name" with your xml file name.

``` bash
java -cp "patcher/build/libs/patcher-1.0-SNAPSHOT-all.jar" edu.ucr.cs.ufarooq.android.patch.Patcher ../LiveDroid-Patching/config.properties ../LiveDroidTestApp/livedroidtest/build/outputs/apk/debug//processed/livedroidtest-debug.apk ../LiveDroidTestApp/livedroidtest/build/outputs/apk/debug/out-NonEmpty/<xml-file-name>
```
If successful, this will generate a patched APK file inside ``outDirectory`` as configured in config.properties.

##### Plugin in IntelliJ
*Note: For now please use IntelliJ Idea 2019.3.3 or run `` ./gradlew :plugin:runIde`` to run in sandbox mode.*

You can install Plugin inside IntelliJ/Android Studio by going to plugin and "Install from Disk", browse .zip file generated under plugin.

Open an Android App (e.g., LiveDroidTestApp), select an Activity class and select Menu "Refactor>LiveDroid>"Apply LiveDroid on Selected Activity". It will ask to select static analysis (XML) report, browse generated XML file and it will further ask to generate "getter/setter", "default constructor" if needed and generate the saving and restoring routines. 

Note: if browse dialog does not appear please select class name and then select Menu options.
