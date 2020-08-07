package edu.ucr.cs.ufarooq.android.patch;

import edu.ucr.cs.ufarooq.android.results.ActivityAnalysisResult;
import edu.ucr.cs.ufarooq.android.results.ApplicationAnalysisResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import soot.Body;
import soot.G;
import soot.Local;
import soot.Modifier;
import soot.NullType;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.dexpler.DexType;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.jimple.toolkits.scalar.Evaluator;
import soot.options.Options;
import soot.tagkit.SignatureTag;
import soot.util.Chain;

public class Patcher {

  public static void main(String[] args)
      throws IOException, SAXException, ParserConfigurationException {
    // APK tool should be on path , all others will be loaded from config file
    Properties prop = new Properties();
    InputStream input = null;

    try {

      input = new FileInputStream(args[0]);
      // load a properties file
      prop.load(input);
      // get the property value and print it out
      System.out.println(prop.getProperty("classLocation"));
      System.out.println(prop.getProperty("outDirectory"));
      System.out.println(prop.getProperty("androidJar"));
      System.out.println(prop.getProperty("keyStore"));
      System.out.println(prop.getProperty("reports"));
      System.out.println(prop.getProperty("platformpk8"));
    } catch (IOException ex) {
      ex.printStackTrace();
      return;
    }

    String apkFile = args[1];
    String report = args[2];
    String classLocation = prop.getProperty("classLocation").trim();
    String outDirectory = prop.getProperty("outDirectory").trim();
    String androidJar = prop.getProperty("androidJar").trim();
    String signapkJar = prop.getProperty("signapkJar").trim();
    String platformPem = prop.getProperty("platformPem").trim();
    String platformpk8 = prop.getProperty("platformpk8").trim();
    String zipalign = prop.getProperty("zipalign").trim();
    String keyStore = prop.getProperty("keyStore").trim();

    if (apkFile.endsWith(".apk")) {
      Path apk = new File(apkFile).toPath();
      String apkDirectory = apk.getParent().toAbsolutePath().toString();
      long startTime = Calendar.getInstance().getTimeInMillis();
      Options.v().set_src_prec(Options.src_prec_apk);

      ArrayList<String> list = new ArrayList<>();
      list.add(apkFile);
      list.add(classLocation);

      Options.v().set_process_dir(list);
      Options.v().set_src_prec(Options.src_prec_apk);
      Options.v().set_process_multiple_dex(true);
      Options.v().set_android_jars(androidJar);
      Options.v().set_output_dir(outDirectory);
      Options.v().set_allow_phantom_refs(true);
      Options.v().set_process_multiple_dex(true);
      Options.v().set_whole_program(true);
      Options.v().set_force_overwrite(true);
      Options.v().set_no_writeout_body_releasing(true);
      // Options.v().set_android_api_version(23);
      // output as APK, too//-f J
      Options.v().set_output_format(Options.output_format_dex);
      // Scene.v().loadNecessaryClasses();

      String xmlFile = report;

      PackManager.v()
          .getPack("wjtp")
          .add(
              new Transform(
                  "wjtp.liveDroidPatcher",
                  new SceneTransformer() {
                    protected void internalTransform(String phaseName, Map options) {
                      // load whole program options first
                      ApplicationAnalysisResult application = null;
                      try {
                        application = ResultImporter.getInstance(xmlFile).parseXMLResults();
                      } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                      } catch (IOException e) {
                        e.printStackTrace();
                      } catch (SAXException e) {
                        e.printStackTrace();
                      }
                      for (ActivityAnalysisResult activityAnalysisResult :
                          application.getActivities()) {
                        // Set<SootClass> preCheckingSet =
                        // Utils.performPreChecking(activityAnalysisResult);
                        System.out.println(
                            "Activity: " + activityAnalysisResult.getActivityName().getName());
                        activityAnalysisResult
                            .getActivityName()
                            .getFields()
                            .forEach(
                                sootField -> {
                                  if (sootField.getTag(SignatureTag.NAME) != null) {
                                    SignatureTag fieldSignature =
                                        ((SignatureTag) sootField.getTag(SignatureTag.NAME));
                                    DexType dexType = new DexType(fieldSignature.getSignature());

                                    String sootType =
                                        DexType.toSootICAT(fieldSignature.getSignature());
                                    System.out.println(
                                        sootField.getName()
                                            + ":"
                                            + fieldSignature.getSignature()
                                            + ":"
                                            + dexType.toSoot().toQuotedString());
                                  }
                                });

                        SootClass gsonClass = Scene.v().getSootClassUnsafe("com.google.gson.Gson");
                        List<SootClass> gsonClasses =
                            Scene.v().getClasses().parallelStream()
                                .filter(sc -> sc.getName().startsWith("com.google.gson"))
                                .collect(Collectors.toList());
                        System.out.println("GSON :" + gsonClass.getName());
                        //
                        //            gsonClasses.forEach(gson ->
                        // System.out.println(gson.getName()));
                        //            SootClass aClass =
                        // Scene.v().getSootClassUnsafe("com.example.umarfarooq.livedroidtest.A");
                        //            Utils.insertTypeClass(aClass);

                        // SootClass testActivity =
                        // Scene.v().getSootClassUnsafe("edu.ucr.cs.ufarooq.livedroidpatchtest.MainActivity");
                        if (!activityAnalysisResult
                            .getActivityName()
                            .declaresMethod("void onSaveInstanceState(android.os.Bundle)")) {
                          Utils.insertOnSaveInstance(activityAnalysisResult);
                        }
                        if (!activityAnalysisResult
                            .getActivityName()
                            .declaresMethod("void onRestoreInstanceState(android.os.Bundle)")) {
                          Utils.insertOnRestoreInstance(activityAnalysisResult);
                          activityAnalysisResult
                              .getActivityName()
                              .getMethods()
                              .forEach(
                                  sootMethod -> {
                                    System.out.println("method: " + sootMethod.getSignature());
                                  });
                        }
                      }
                    }
                  }));

      // EventInstrumenter.v().processAnnotated();
      // PackManager.v().getPack("jtp").apply();
      // PackManager.v().runPacks();
      // PackManager.v().writeOutput();
      String mainArgs =
          "-android-api-version 23"; // "-process-dir " + apk.toFile().getAbsolutePath();

      soot.Main.main(mainArgs.split("\\s")); //

      long timeSpentUntil = Calendar.getInstance().getTimeInMillis() - startTime;
      System.out.println("Completed Refactoring:" + timeSpentUntil);
      // PackManager.v().runPacks();
      PackManager.v().writeOutput();
      String debugClass = "com.example.umarfarooq.livedroidtest.MainActivity";
      if (Scene.v().containsClass(debugClass)) {
        SootClass activity = Scene.v().getSootClassUnsafe(debugClass);

        SootMethod saveInstance = activity.getMethodByNameUnsafe("onSaveInstanceState");
        Body body = saveInstance.retrieveActiveBody();

        Iterator<Unit> bodyIt = body.getUnits().iterator();

        while (bodyIt.hasNext()) {
          Unit unit = bodyIt.next();
          System.out.println(unit);
        }

        SootMethod restoreInstance = activity.getMethodByNameUnsafe("onRestoreInstanceState");
        Body restoreInstancebody = restoreInstance.retrieveActiveBody();

        Iterator<Unit> restoreInstancebodyIt = restoreInstancebody.getUnits().iterator();

        while (restoreInstancebodyIt.hasNext()) {
          Unit unit = restoreInstancebodyIt.next();
          System.out.println(unit);
        }
      }
      //
      System.out.println(
          "Completed Writing:" + (Calendar.getInstance().getTimeInMillis() - startTime));
      long time = Calendar.getInstance().getTimeInMillis();

      // String apkAlignedFileName = apk.getFileName().toString().replace(".", "_") + time +
      // "aligned.apk";
      String apkSignedFileName =
          apk.getFileName().toString().replace(".", "_") + time + "signed.apk";

      String alignCommand =
          zipalign
              + " -v -p 4 "
              + outDirectory
              + File.separator
              + apk.getFileName()
              + " "
              + outDirectory
              + File.separator
              + apkSignedFileName;
      String alignCommandOutput = executeCommand(alignCommand);
      System.out.println(alignCommandOutput);
      // String signCommand = "apksigner sign --ks " + keyStore + " --out " + apkSignedFileName + "
      // " + apkAlignedFileName;
      String signCommand =
          "apksigner sign --cert "
              + platformPem
              + " --key "
              + platformpk8
              + " "
              + outDirectory
              + File.separator
              + apkSignedFileName;
      String signCommandOutput = executeCommand(signCommand);
      System.out.println(signCommandOutput);
      long timeSpent = time - startTime;
      File resultFile = new File(apkDirectory + File.separator + "LiveDroid.csv");
      String appResult = apk.getFileName() + "," + timeSpent;
      try {
        // FileUtils.forceDelete(new File(outDirectory + File.separator + apkAlignedFileName));
        FileUtils.forceDelete(
            new File(outDirectory + File.separator + apk.getFileName().toString()));
        FileUtils.writeStringToFile(resultFile, appResult + "\n", Charset.defaultCharset(), true);
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
      Iterator<Path> it =
          Files.newDirectoryStream(
                  Paths.get(apkDirectory), path -> path.toString().endsWith(".apk"))
              .iterator();
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
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return output.toString();
  }

  private static void updateXml(String xmlPath) {
    try {

      File fXmlFile = new File(xmlPath);
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(fXmlFile);
      // optional, but recommended
      // read this -
      // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
      doc.getDocumentElement().normalize();

      System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

      String appPackageName = doc.getDocumentElement().getAttribute("package");
      System.out.println("Package Name: " + appPackageName);

      doc.getDocumentElement().setAttribute("android:testOnly", "false");

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

            eElement.setAttribute(
                "android:configChanges",
                "keyboard|mnc|mcc|touchscreen|locale|navigation|fontScale|layoutDirection|keyboardHidden|orientation|screenLayout|uiMode|screenSize|smallestScreenSize");

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
      // write the content into xml file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(xmlPath));
      transformer.transform(source, result);

      System.out.println("Done");

    } catch (Exception pce) {
      pce.printStackTrace();
    }
  }

  private static boolean isSuperToClass(SootClass theClass, SootClass superClass) {
    while (theClass.hasSuperclass()) {
      if (theClass.getSuperclass().getName().equalsIgnoreCase(superClass.getName())) return true;
      theClass = theClass.getSuperclass();
    }
    return false;
  }

  /**
   * Make the given field final. Anywhere where the the given field is used in the given class,
   * inline the reference with the given value. Anywhere where the given field is illegally defined
   * in the given class, inline the definition to throw an exception. This happens unless the given
   * class is the defining class for the given field and the definition occurs within an initializer
   * (for instance fields) or a static initializer (for static fields). Note that this is rather
   * limited, since it is only really useful for constant values. In would be nice to specify a more
   * complex expression to inline, but I'm not sure how to do it.
   */
  public static void assertFinalField(SootClass theClass, SootField theField, Value newValue) {
    // First make the field final.
    theField.setModifiers(theField.getModifiers() | Modifier.FINAL);

    // Find any assignment to the field in the class and convert
    // them to Exceptions, unless they are in constructors,
    // in which case remove them.
    for (Iterator methods = theClass.getMethods().iterator(); methods.hasNext(); ) {
      SootMethod method = (SootMethod) methods.next();
      JimpleBody body = (JimpleBody) method.retrieveActiveBody();
      Chain units = body.getUnits();

      for (Iterator stmts = units.snapshotIterator(); stmts.hasNext(); ) {
        Stmt stmt = (Stmt) stmts.next();

        // Remove all the definitions.
        for (Iterator boxes = stmt.getDefBoxes().iterator(); boxes.hasNext(); ) {
          ValueBox box = (ValueBox) boxes.next();
          Value value = box.getValue();

          if (value instanceof FieldRef) {
            FieldRef ref = (FieldRef) value;

            if (ref.getField() == theField) {
              System.out.println("removing stmt = " + stmt);
              units.remove(stmt);
            }
          }
        }

        // Inline all the uses.
        if (Evaluator.isValueConstantValued(newValue)) {
          for (Iterator boxes = stmt.getUseBoxes().iterator(); boxes.hasNext(); ) {
            ValueBox box = (ValueBox) boxes.next();
            Value value = box.getValue();

            if (value instanceof FieldRef) {
              FieldRef ref = (FieldRef) value;

              if (ref.getField() == theField) {
                System.out.println("inlining stmt = " + stmt);

                box.setValue(Evaluator.getConstantValueOf(newValue));
              }
            }
          }
        }
      }
    }

    if (Modifier.isStatic(theField.getModifiers())) {
      SootMethod method;

      // create a class initializer if one does not already exist.
      if (theClass.declaresMethodByName("<clinit>")) {
        method = theClass.getMethodByName("<clinit>");
      } else {
        method = new SootMethod("<clinit>", new LinkedList(), NullType.v(), Modifier.PUBLIC);
        theClass.addMethod(method);
      }

      JimpleBody body = (JimpleBody) method.retrieveActiveBody();
      Chain units = body.getUnits();
      Stmt insertPoint = (Stmt) units.getLast();
      Local local = Jimple.v().newLocal("_CGTemp" + theField.getName(), theField.getType());
      body.getLocals().add(local);
      units.insertBefore(Jimple.v().newAssignStmt(local, newValue), insertPoint);

      FieldRef fieldRef = Jimple.v().newStaticFieldRef(theField.makeRef());
      units.insertBefore(Jimple.v().newAssignStmt(fieldRef, local), insertPoint);
    } else {
      for (Iterator methods = theClass.getMethods().iterator(); methods.hasNext(); ) {
        SootMethod method = (SootMethod) methods.next();

        // ignore things that aren't initializers.
        if (!method.getName().equals("<init>")) {
          continue;
        }

        JimpleBody body = (JimpleBody) method.retrieveActiveBody();
        Chain units = body.getUnits();
        Stmt insertPoint = (Stmt) units.getLast();
        Local local = Jimple.v().newLocal("_CGTemp" + theField.getName(), theField.getType());
        body.getLocals().add(local);
        units.insertBefore(Jimple.v().newAssignStmt(local, newValue), insertPoint);

        FieldRef fieldRef = Jimple.v().newInstanceFieldRef(body.getThisLocal(), theField.makeRef());
        units.insertBefore(Jimple.v().newAssignStmt(fieldRef, local), insertPoint);
      }
    }
  }

  /**
   * Search through all the methods in the given class and change all references to the old class to
   * references to the new class. This includes field references, type casts, this references, new
   * object instantiations and method invocations.
   *
   * @param theClass The class containing methods to modify.
   * @param oldClass The class to replace.
   * @param newClass The new class.
   */
  public static void changeTypesInMethods(
      SootClass theClass, SootClass oldClass, SootClass newClass) {
    System.out.println("fixing references on " + theClass);
    System.out.println("replacing " + oldClass + " with " + newClass);
    ArrayList methodList = new ArrayList(theClass.getMethods());

    for (Iterator methods = methodList.iterator(); methods.hasNext(); ) {
      SootMethod newMethod = (SootMethod) methods.next();

      // System.out.println("newMethod = " + newMethod.getSignature());
      Type returnType = newMethod.getReturnType();

      if (returnType instanceof RefType && (((RefType) returnType).getSootClass() == oldClass)) {
        newMethod.setReturnType(RefType.v(newClass));
      }

      List paramTypes = new LinkedList();

      for (Iterator oldParamTypes = newMethod.getParameterTypes().iterator();
          oldParamTypes.hasNext(); ) {
        Type type = (Type) oldParamTypes.next();

        if (type instanceof RefType && (((RefType) type).getSootClass() == oldClass)) {
          paramTypes.add(RefType.v(newClass));
        } else if (type instanceof RefType
            && (((RefType) type).getSootClass().getName().startsWith(oldClass.getName()))) {
          SootClass changeClass =
              _getInnerClassCopy(oldClass, ((RefType) type).getSootClass(), newClass);
          paramTypes.add(RefType.v(changeClass));
        } else {
          paramTypes.add(type);
        }
      }

      newMethod.setParameterTypes(paramTypes);

      // we have to do this seemingly useless
      // thing, since the scene caches a pointer
      // to the method based on it's parameter types.
      theClass.removeMethod(newMethod);
      theClass.addMethod(newMethod);

      Body newBody = newMethod.retrieveActiveBody();

      for (Iterator locals = newBody.getLocals().iterator(); locals.hasNext(); ) {
        Local local = (Local) locals.next();
        Type type = local.getType();

        if (type instanceof RefType && (((RefType) type).getSootClass() == oldClass)) {
          local.setType(RefType.v(newClass));
        }
      }

      Iterator j = newBody.getUnits().iterator();

      while (j.hasNext()) {
        Unit unit = (Unit) j.next();
        Iterator boxes = unit.getUseAndDefBoxes().iterator();
        //  System.out.println("unit = " + unit);

        while (boxes.hasNext()) {
          ValueBox box = (ValueBox) boxes.next();
          Value value = box.getValue();

          if (value instanceof FieldRef) {
            // Fix references to fields
            FieldRef r = (FieldRef) value;
            SootFieldRef fieldRef = r.getFieldRef();
            if (fieldRef.type() instanceof RefType) {
              RefType fieldType = (RefType) fieldRef.type();
              SootClass fieldClass = fieldType.getSootClass();
              if (fieldClass == oldClass) {
                r.setFieldRef(
                    Scene.v()
                        .makeFieldRef(
                            fieldRef.declaringClass(),
                            fieldRef.name(),
                            RefType.v(newClass),
                            fieldRef.isStatic()));
              }
              fieldRef = r.getFieldRef();
            }

            if (fieldRef.declaringClass() == oldClass) {
              // We might also have a reference to a field
              // which is not actually declared in the
              // oldclass, in which case, we just fix up
              // the ref to point to the new super class
              r.setFieldRef(
                  Scene.v()
                      .makeFieldRef(
                          newClass, fieldRef.name(), fieldRef.type(), fieldRef.isStatic()));
            } else if (fieldRef.declaringClass().getName().startsWith(oldClass.getName())) {
              SootClass changeClass =
                  _getInnerClassCopy(oldClass, r.getField().getDeclaringClass(), newClass);
              r.setFieldRef(changeClass.getFieldByName(r.getField().getName()).makeRef());
            } //  else if (r.getField().getDeclaringClass() == oldClass) {
            //                             r.setFieldRef(
            //                                     newClass.getFieldByName(
            //                                             r.getField().getName()).makeRef());

            //                             //   System.out.println("fieldRef = " +
            //                             //              box.getValue());
            //                         }

          } else if (value instanceof CastExpr) {
            // Fix casts
            CastExpr r = (CastExpr) value;
            Type type = r.getType();

            if (type instanceof RefType) {
              SootClass refClass = ((RefType) type).getSootClass();

              if (refClass == oldClass) {
                r.setCastType(RefType.v(newClass));

                // System.out.println("newValue = " +
                //        box.getValue());
              } else if (refClass.getName().startsWith(oldClass.getName())) {
                SootClass changeClass = _getInnerClassCopy(oldClass, refClass, newClass);
                r.setCastType(RefType.v(changeClass));
              }
            }
          } else if (value instanceof ThisRef) {
            // Fix references to 'this'
            ThisRef r = (ThisRef) value;
            Type type = r.getType();

            if (type instanceof RefType && (((RefType) type).getSootClass() == oldClass)) {
              box.setValue(Jimple.v().newThisRef(RefType.v(newClass)));
            }
          } else if (value instanceof ParameterRef) {
            // Fix references to a parameter
            ParameterRef r = (ParameterRef) value;
            Type type = r.getType();

            if (type instanceof RefType && (((RefType) type).getSootClass() == oldClass)) {
              box.setValue(Jimple.v().newParameterRef(RefType.v(newClass), r.getIndex()));
            }
          } else if (value instanceof InvokeExpr) {
            // Fix up the method invokes.
            InvokeExpr r = (InvokeExpr) value;
            SootMethodRef methodRef = r.getMethodRef();
            System.out.println("invoke = " + r);

            List newParameterTypes = new LinkedList();
            for (Iterator i = methodRef.parameterTypes().iterator(); i.hasNext(); ) {
              Type type = (Type) i.next();
              if (type instanceof RefType && (((RefType) type).getSootClass() == oldClass)) {
                System.out.println("matchedParameter = " + newClass);
                newParameterTypes.add(RefType.v(newClass));
              } else if (type instanceof RefType
                  && (((RefType) type).getSootClass().getName().startsWith(oldClass.getName()))) {
                System.out.println("matchedParameter = " + newClass);
                SootClass changeClass =
                    _getInnerClassCopy(oldClass, ((RefType) type).getSootClass(), newClass);
                newParameterTypes.add(RefType.v(changeClass));
              } else {
                newParameterTypes.add(type);
              }
            }

            Type newReturnType = methodRef.returnType();
            if (newReturnType instanceof RefType
                && (((RefType) newReturnType).getSootClass() == oldClass)) {
              newReturnType = RefType.v(newClass);
            }

            // Update the parameter types and the return type.
            methodRef =
                Scene.v()
                    .makeMethodRef(
                        methodRef.declaringClass(),
                        methodRef.name(),
                        newParameterTypes,
                        newReturnType,
                        methodRef.isStatic());
            r.setMethodRef(methodRef);

            if (methodRef.declaringClass() == oldClass) {
              r.setMethodRef(
                  Scene.v()
                      .makeMethodRef(
                          newClass,
                          methodRef.name(),
                          methodRef.parameterTypes(),
                          methodRef.returnType(),
                          methodRef.isStatic()));
              // System.out.println("newValue = " +
              // box.getValue());
            } else if (methodRef.declaringClass().getName().startsWith(oldClass.getName())) {
              SootClass changeClass =
                  _getInnerClassCopy(oldClass, methodRef.declaringClass(), newClass);
              r.setMethodRef(
                  Scene.v()
                      .makeMethodRef(
                          changeClass,
                          methodRef.name(),
                          methodRef.parameterTypes(),
                          methodRef.returnType(),
                          methodRef.isStatic()));
            }
          } else if (value instanceof NewExpr) {
            // Fix up the object creations.
            NewExpr r = (NewExpr) value;

            if (r.getBaseType().getSootClass() == oldClass) {
              r.setBaseType(RefType.v(newClass));

              //   System.out.println("newValue = " +
              //           box.getValue());
            } else if (r.getBaseType().getSootClass().getName().startsWith(oldClass.getName())) {
              SootClass changeClass =
                  _getInnerClassCopy(oldClass, r.getBaseType().getSootClass(), newClass);
              r.setBaseType(RefType.v(changeClass));
            }
          }

          //    System.out.println("value = " + value);
          //   System.out.println("class = " +
          //            value.getClass().getName());
        }
      }
    }
  }

  private static SootClass _getInnerClassCopy(
      SootClass oldOuterClass, SootClass oldInnerClass, SootClass newOuterClass) {
    String oldInnerClassName = oldInnerClass.getName();
    String oldInnerClassSpecifier = oldInnerClassName.substring(oldOuterClass.getName().length());

    // System.out.println("oldInnerClassSpecifier = " +
    //        oldInnerClassSpecifier);
    String newInnerClassName = newOuterClass.getName() + oldInnerClassSpecifier;
    SootClass newInnerClass;

    if (Scene.v().containsClass(newInnerClassName)) {
      newInnerClass = Scene.v().getSootClass(newInnerClassName);
    } else {
      oldInnerClass.setLibraryClass();

      //   System.out.println("copying "+ oldInnerClass +
      //           " to " + newInnerClassName);
      newInnerClass = copyClass(oldInnerClass, newInnerClassName);
      newInnerClass.setApplicationClass();
    }

    changeTypesOfFields(newInnerClass, oldOuterClass, newOuterClass);
    changeTypesInMethods(newInnerClass, oldOuterClass, newOuterClass);
    return newInnerClass;
  }

  /** Copy a class */
  public static SootClass copyClass(SootClass oldClass, String newClassName) {
    // System.out.println("SootClass.copyClass(" + oldClass + ", "
    //                   + newClassName + ")");
    // Create the new Class
    SootClass newClass = new SootClass(newClassName, oldClass.getModifiers());

    try {
      Scene.v().addClass(newClass);
    } catch (RuntimeException runtime) {
      throw new RuntimeException(
          "Perhaps you are calling the same " + "transform twice?: " + runtime);
    }

    // Set the Superclass.
    newClass.setSuperclass(oldClass.getSuperclass());

    // Copy the interface declarations
    //        newClass.getInterfaces().addAll(oldClass.getInterfaces());

    // Copy the fields.
    _copyFields(newClass, oldClass);

    // Copy the methods.
    Iterator methods = oldClass.getMethods().iterator();

    while (methods.hasNext()) {
      SootMethod oldMethod = (SootMethod) methods.next();

      SootMethod newMethod =
          new SootMethod(
              oldMethod.getName(),
              oldMethod.getParameterTypes(),
              oldMethod.getReturnType(),
              oldMethod.getModifiers(),
              oldMethod.getExceptions());
      newClass.addMethod(newMethod);

      JimpleBody body = Jimple.v().newBody(newMethod);
      body.importBodyContentsFrom(oldMethod.retrieveActiveBody());
      newMethod.setActiveBody(body);
    }

    changeTypesOfFields(newClass, oldClass, newClass);
    changeTypesInMethods(newClass, oldClass, newClass);
    return newClass;
  }

  /**
   * Search through all the fields in the given class and if the field is of class oldClass, then
   * change it to newClass.
   *
   * @param theClass The class containing fields to modify.
   * @param oldClass The class to replace.
   * @param newClass The new class.
   */
  public static void changeTypesOfFields(
      SootClass theClass, SootClass oldClass, SootClass newClass) {
    Iterator fields = theClass.getFields().snapshotIterator();

    while (fields.hasNext()) {
      SootField oldField = (SootField) fields.next();
      Type type = oldField.getType();

      //  System.out.println("field with type " + type);
      if (type instanceof RefType) {
        SootClass refClass = ((RefType) type).getSootClass();

        if (refClass == oldClass) {
          oldField.setType(RefType.v(newClass));

          // we have to do this seemingly useless
          // thing, since the scene caches a pointer
          // to the field based on it's parameter types.
          theClass.removeField(oldField);
          theClass.addField(oldField);
        } else if (refClass.getName().startsWith(oldClass.getName())) {
          SootClass changeClass = _getInnerClassCopy(oldClass, refClass, newClass);
          oldField.setType(RefType.v(changeClass));

          // we have to do this seemingly useless
          // thing, since the scene caches a pointer
          // to the field based on it's parameter types.
          theClass.removeField(oldField);
          theClass.addField(oldField);
        }
      }
    }
  }

  /**
   * Copy all the fields into the given class from the given old class.
   *
   * @return A list of fields in the new class that were created whose names collide with fields
   *     already there.
   */
  private static List _copyFields(SootClass newClass, SootClass oldClass) {
    List list = new LinkedList();
    Iterator fields = oldClass.getFields().iterator();

    while (fields.hasNext()) {
      SootField oldField = (SootField) fields.next();

      if (newClass.declaresFieldByName(oldField.getName())) {
        // FIXME
        throw new RuntimeException(
            "Field "
                + oldField
                + " cannot be folded into "
                + newClass
                + " because its name is the same as "
                + newClass.getFieldByName(oldField.getName()));
      }

      SootField newField =
          new SootField(oldField.getName(), oldField.getType(), oldField.getModifiers());
      newClass.addField(newField);
    }

    return list;
  }

  /*
  Set<ViewResult> uiResults = new HashSet<>();

          Set<FieldOnlyAccessPath> mayUseForEventListener = new HashSet<>();
          Set<FieldOnlyAccessPath> mayModifyForEventListener = new HashSet<>();
          Set<AbsConnection> connections = new HashSet<>();
          Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> mayPointsToForEventListener = new HashMap<>();
          SootClass activityName = Scene.v().getSootClassUnsafe("edu.ucr.cs.ufarooq.livedroidpatchtest.MainActivity");

          SootClass aClass = Scene.v().getSootClassUnsafe("edu.ucr.cs.ufarooq.livedroidpatchtest.A");
          SootField aField = activityName.getFieldUnsafe("a", aClass.getType());
          SootField stringList = activityName.getFieldByNameUnsafe("stringList");
          SootField mayPointsTo = activityName.getFieldByNameUnsafe("mayPointsTo");
          SootField map = activityName.getFieldByNameUnsafe("map");

          Field field = new Field(aField);
          Field stringListField = new Field(stringList);
          Field mayPointsToField = new Field(mayPointsTo);
          Field mapField = new Field(map);
          FieldOnlyAccessPath fieldOnlyAccessPath = new FieldOnlyAccessPath(Collections.singleton(field));
          FieldOnlyAccessPath stringListAccessPath = new FieldOnlyAccessPath(Collections.singleton(stringListField));
          FieldOnlyAccessPath mayPointsToAccessPath = new FieldOnlyAccessPath(Collections.singleton(mayPointsToField));
          FieldOnlyAccessPath mapAccessPath = new FieldOnlyAccessPath(Collections.singleton(mapField));
          mayModifyForEventListener.add(fieldOnlyAccessPath);
          mayUseForEventListener.add(fieldOnlyAccessPath);
          mayModifyForEventListener.add(stringListAccessPath);
          mayUseForEventListener.add(stringListAccessPath);
          mayModifyForEventListener.add(mapAccessPath);
          mayUseForEventListener.add(mapAccessPath);

          Set<FieldOnlyAccessPath> stringListPointsTo = new HashSet<>();
          stringListPointsTo.add(mayPointsToAccessPath);
          mayPointsToForEventListener.put(stringListAccessPath, stringListPointsTo);

          ActivityAnalysisResult activityAnalysisResult = new ActivityAnalysisResult(uiResults, mayUseForEventListener, mayModifyForEventListener, mayPointsToForEventListener, connections, activityName);

   */
}
