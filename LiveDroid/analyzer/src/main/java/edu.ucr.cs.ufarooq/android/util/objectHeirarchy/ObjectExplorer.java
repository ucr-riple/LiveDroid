package edu.ucr.cs.ufarooq.android.util.objectHeirarchy;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import fj.data.Array;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.jimple.toolkits.scalar.Evaluator;
import soot.options.Options;
import soot.util.Chain;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ObjectExplorer {
    private static final String ANDROID_MANIFEST = "AndroidManifest.xml";

    public static void main(String[] args) {
//APK tool should be on path , all others will be loaded from config file

        String apkDirectory = "/Users/umarfarooq/UCR/Research/Research/ReverseEngineering/tests/objects";
        String outPath = "/Users/umarfarooq/UCR/Research/Research/ReverseEngineering/tests/objects";
        String androidJar = "/Users/umarfarooq/Library/Android/sdk/platforms";

        ArrayList<Path> apkList = getAPKList(apkDirectory);

        for (Path apk : apkList) {
            System.out.println(apk.getFileName().toString());
            String extractionDir = apkDirectory + File.separator + apk.getFileName().toString().replace(".", "_") + "_" + Calendar.getInstance().getTimeInMillis();

            String commandToExtract = "apktool d " + apk.toString() + " -o " + extractionDir;
            System.out.println(commandToExtract);
            String extractionOutput = executeCommand(commandToExtract);
            System.out.println(extractionOutput);

            String xmlPath = extractionDir + File.separator + ANDROID_MANIFEST;
            Application application = parseXml(xmlPath);
            System.out.println("Activities: " + application.getPackageName() + ":" + application.activitiesClasses.size());
            if (application != null) {
                //prefer Android APK files// -src-prec apk
                Options.v().set_src_prec(Options.src_prec_apk);

                ArrayList<String> list = new ArrayList<>();
                list.add(apk.toString());

                Options.v().set_process_dir(list);
                Options.v().set_android_jars(androidJar);
                Options.v().set_allow_phantom_refs(true);
                Options.v().set_process_multiple_dex(true);
                Options.v().set_whole_program(true);
                Options.v().set_force_overwrite(true);
                Options.v().set_android_api_version(23);
                //output as APK, too//-f J
                Options.v().set_output_format(Options.output_format_dex);

                Scene.v().loadNecessaryClasses();
                application.process();
                List<SootField> allFields = new ArrayList<>();
                for (SootClass activityName : application.activities) {
                    System.out.println("Activity: " + activityName.getName());
                    List<SootField> fields = ExploreObjectHeirarchy.exploreClass(activityName, 3);
                    System.out.println("Fields Size: " + fields.size());
                    allFields.addAll(fields);
                }
                System.out.println("Fields Size: " + allFields.size());
                File resultFile = new File(outPath + File.separator + "LiveDroid.csv");
                String appResult = application.getPackageName() + "," + application.activities.size() + "," + allFields.size();
                try {
                    FileUtils.writeStringToFile(resultFile, appResult + "\n", Charset.defaultCharset(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileUtils.deleteDirectory(new File(extractionDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
            G.reset();
            G.v().resetSpark();
        }


    }


    private static ArrayList<Path> getAPKList(String apkDirectory) {
        ArrayList<Path> apks = new ArrayList<Path>();
        try {
            Iterator<Path> it = Files.newDirectoryStream(Paths.get(apkDirectory),
                    path -> path.toString().endsWith(".apk")).iterator();
            while (it.hasNext()) {
                Path path = it.next();
                System.out.println(path.toString());
                apks.add(path);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return apks;
    }

    private static String executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }

    private static Application parseXml(String xmlPath) {
        try {

            File fXmlFile = new File(xmlPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            String appPackageName = doc.getDocumentElement().getAttribute("package");
            System.out.println("Package Name: " + appPackageName);

            Node application = doc.getElementsByTagName("application").item(0);
            NodeList nList = application.getChildNodes();

            System.out.println("----------------------------");
            ArrayList<String> activities = new ArrayList<>();
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    System.out.println(nNode.getNodeName());
                    if (nNode.getNodeName().equalsIgnoreCase("activity")) {
                        Element eElement = (Element) nNode;
                        String activity = eElement.getAttribute("android:name");
                        String className = null;
                        if (appPackageName != null) {
                            if (activity.startsWith(".")) {
                                className = appPackageName + activity;
                            } else if (activity.startsWith(appPackageName)) {
                                className = activity;
                            } else if (!activity.contains(".")) {
                                className = appPackageName + "." + activity;
                            } else {
                                className = activity;
                            }
                        }
                        System.out.println("Activity Name : " + className);
                        activities.add(className);
                    }
                }
            }
            return new Application(appPackageName, activities);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}