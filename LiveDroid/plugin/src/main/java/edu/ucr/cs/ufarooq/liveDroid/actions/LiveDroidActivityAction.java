package edu.ucr.cs.ufarooq.liveDroid.actions;

import com.android.builder.model.SourceProvider;
import com.android.tools.idea.gradle.AndroidGradleJavaProjectModelModifier;
import com.android.tools.idea.gradle.dsl.api.GradleBuildModel;
import com.android.tools.idea.gradle.dsl.api.GradleModelProvider;
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencyModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencySpec;
import com.android.tools.idea.gradle.dsl.api.dependencies.DependenciesModel;
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.android.tools.idea.gradle.project.sync.GradleSyncListener;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.generation.ConstructorBodyGenerator;
import com.intellij.codeInsight.generation.GenerateConstructorHandler;
import com.intellij.codeInsight.generation.GenerateMembersUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UnexpectedUndoException;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.refactoring.psi.SearchUtils;
import com.siyeh.ig.psiutils.ClassUtils;
import com.siyeh.ig.psiutils.ConstructionUtils;
import com.siyeh.ig.psiutils.TypeUtils;
import edu.ucr.cs.ufarooq.liveDroid.Utils;
import edu.ucr.cs.ufarooq.liveDroid.staticResults.*;
import javax.swing.JOptionPane;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.IdeaSourceProvider;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.jps.android.model.impl.JpsAndroidModuleProperties;
import org.xml.sax.SAXException;

import static com.android.tools.idea.gradle.dsl.api.dependencies.CommonConfigurationNames.IMPLEMENTATION;
import static com.google.wireless.android.sdk.stats.GradleSyncStats.Trigger.TRIGGER_MODIFIER_ACTION_UNDONE;
import static com.google.wireless.android.sdk.stats.GradleSyncStats.Trigger.TRIGGER_MODIFIER_ADD_MODULE_DEPENDENCY;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.android.tools.idea.gradle.dsl.api.dependencies.CommonConfigurationNames.COMPILE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;

public class LiveDroidActivityAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {

    final PsiClass psiClass = Utils.getPsiClassFromContext(e);
    if (psiClass != null && Utils.isActivity(psiClass)) {
      final Project project = e.getData(PlatformDataKeys.PROJECT);
      // if (AndroidUtils.hasAndroidFacets(project)) {
      PsiManager psiManager = PsiManager.getInstance(project);
      final Module module = e.getData(LangDataKeys.MODULE);

      AndroidFacet facet = AndroidFacet.getInstance(module);
      if (facet == null) {
        Messages.showMessageDialog(
            "Facet is NULL, Cannot continue", "Facet", Messages.getErrorIcon());
      }

      // JpsAndroidModuleProperties androidModuleProperties = facet.getProperties();
      SourceProvider mainSourceProvider = facet.getMainSourceProvider();
      // IdeaSourceProvider ideaSourceProvider = facet.getMainIdeaSourceProvider();

      PsiField[] allFields = psiClass.getFields(); // getAllFields return all super fields
      PsiMethod[] allMethods = psiClass.getMethods();

      for (PsiField field : allFields) {
        PsiType fieldType = field.getType();

        Optional<PsiClass> classType = Utils.resolveFieldType(field);
        PsiClass fieldClass = null;
        if (classType.isPresent()) {
          fieldClass = classType.get();
        }
        // Utils.showMessage(field.getNameIdentifier().getText() + ":" + fieldType.toString());
      }

      for (PsiMethod psiMethod : allMethods) {
        // Utils.showMessage(psiMethod.getNameIdentifier().getText());
      }

      PsiClass textView = Utils.findClass(psiClass.getManager(), "android.widget.TextView");

      boolean gsonExists = gsonArtifactsExists(module);
      if (!gsonExists) {
        Promise<Void> promise = importGsonToGradle(project, module);

        promise.onSuccess(
            c -> {
              browseReport(psiManager, project, mainSourceProvider, psiClass);
            });
      } else {
        browseReport(psiManager, project, mainSourceProvider, psiClass);
      }
    }
    // }
  }

  private static void browseReport(
      PsiManager psiManager, Project project, SourceProvider sourceProvider, PsiClass psiClass) {
    File selectedFile = Utils.browseFile();
    if (selectedFile != null) {
      Utils.showMessage(selectedFile.getAbsolutePath());
      try {
        long startTime = Calendar.getInstance().getTimeInMillis();
        ApplicationAnalysisResult applicationAnalysisResult =
            Utils.parseXMLResults(selectedFile, psiManager);
        for (ActivityAnalysisResult activityAnalysisResult :
            applicationAnalysisResult.getActivityAnalysisResultSet()) {
          if (activityAnalysisResult.getActivityName().equals(psiClass)) {
            FileEditorManager.getInstance(project)
                .openTextEditor(
                    new OpenFileDescriptor(project, psiClass.getContainingFile().getVirtualFile()),
                    true // request focus to editor
                    );
            preProcessActivity(project, psiManager, sourceProvider, activityAnalysisResult);
          }
        }
        long endTime = Calendar.getInstance().getTimeInMillis();
        long timeSpent = endTime - startTime;
        //        Messages.showMessageDialog(
        //            "Time Spent: " + timeSpent, "Processing Time", Messages.getErrorIcon());
      } catch (ParserConfigurationException e1) {
        e1.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      } catch (SAXException e1) {
        e1.printStackTrace();
      }
    } else {
      Utils.showMessage("File is NULL");
    }
  }

  public static void preProcessActivity(
      Project project,
      PsiManager psiManager,
      SourceProvider sourceProvider,
      ActivityAnalysisResult activityAnalysisResult) {
    Set<AccessPath> noSelectionNeed = new HashSet<>();
    StringBuilder generationMessage = new StringBuilder();
    Map<AccessPath, Set<FieldGenerationHandler>> accessPathSetMap = new HashMap<>();
    // first collect what needs to be generated
    for (AccessPath accessPath : activityAnalysisResult.getCriticalData()) {
      System.out.println("AccessPath: " + accessPath.getKey());
      if (!accessPath.isSingleField()) { // Single field is inside the class itself
        int accessPathSize = accessPath.getFields().size();
        PsiField topField = accessPath.getFields().get(accessPathSize - 1);
        if (ignoreToSaveRestore(topField.getContainingClass())) continue;
        Map<PsiField, FieldGenerationHandler> requireToGenerateAccess = new HashMap<>();
        // go in reverse until the base (field in activity) which is already accessible
        for (int i = accessPathSize - 1; i > 0; i--) {
          PsiField field = accessPath.getFields().get(i);
          PsiClass containerClass = field.getContainingClass();
          boolean hasDefaultConstructor = false; // empty parameter constructor
          for (PsiMethod constructor : containerClass.getConstructors()) {
            if (constructor.getParameterList().isEmpty()) {
              hasDefaultConstructor = true;
              break;
            }
          }
          PsiMethod getter = PropertyUtil.findGetterForField(field);
          PsiMethod setter = PropertyUtil.findSetterForField(field);
          System.out.println("Has Default Constructor: " + hasDefaultConstructor);
          FieldGenerationHandler generationHandler =
              new FieldGenerationHandler(
                  field, !hasDefaultConstructor, getter == null, setter == null);

          if (generationHandler.requiresGeneration()) {
            requireToGenerateAccess.put(field, generationHandler);
            String msg = generationHandler.getMessage();
            System.out.println("Message: " + msg);
            generationMessage.append(msg);
          }
        }
        if (requireToGenerateAccess.isEmpty()) {
          noSelectionNeed.add(accessPath);
        } else {
          accessPathSetMap.put(accessPath, new HashSet<>(requireToGenerateAccess.values()));
        }
      } else {
        noSelectionNeed.add(accessPath);
      }
    }
    // ask for user to select
    if (!accessPathSetMap.isEmpty()) {
      String message =
          "LiveDroid requires to generate following Setter/Getter(s) and Constructor(s): \n"
              + generationMessage.toString()
              + "\nDo you want to Generate?";
      int ret =
          JOptionPane.showConfirmDialog(
              null,
              message,
              "Require Code Modification(s)",
              YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
      System.out.println("Selection: " + ret);

      if (ret == YES_OPTION) {
        for (AccessPath accessPath : accessPathSetMap.keySet()) {
          Set<FieldGenerationHandler> fieldGenerations = accessPathSetMap.get(accessPath);
          for (FieldGenerationHandler handler : fieldGenerations) {
            PsiMethod getter = PropertyUtil.findGetterForField(handler.getField());
            PsiMethod setter = PropertyUtil.findSetterForField(handler.getField());
            //            FileEditorManager fileEditorManager =
            // FileEditorManager.getInstance(project);
            //            Editor editor = null;
            //
            //            fileEditorManager.openFile(
            //
            // handler.getField().getContainingClass().getContainingFile().getVirtualFile(), true);
            //            editor = fileEditorManager.getSelectedTextEditor();

            FileEditorManager.getInstance(project)
                .openTextEditor(
                    new OpenFileDescriptor(
                        project,
                        handler
                            .getField()
                            .getContainingClass()
                            .getContainingFile()
                            .getVirtualFile()),
                    true // request focus to editor
                    );
            if (getter == null) {
              WriteCommandAction.runWriteCommandAction(
                  project,
                  () -> {
                    PsiMethod newGetter =
                        GenerateMembersUtil.generateGetterPrototype(handler.getField());
                    handler
                        .getField()
                        .getContainingClass()
                        .addBefore(newGetter, handler.getField().getContainingClass().getRBrace());
                    System.out.println(newGetter.getName());
                  });
            }
            if (setter == null) {
              WriteCommandAction.runWriteCommandAction(
                  project,
                  () -> {
                    PsiMethod newSetter =
                        GenerateMembersUtil.generateSetterPrototype(handler.getField());
                    handler
                        .getField()
                        .getContainingClass()
                        .addBefore(newSetter, handler.getField().getContainingClass().getRBrace());
                    System.out.println(newSetter.getName());
                  });
            }
            if (handler.isConstructorRequired()) {
              WriteCommandAction.runWriteCommandAction(
                  project,
                  () -> {
                    PsiField[] fields = {};
                    PsiMethod constructor =
                        GenerateConstructorHandler.generateConstructorPrototype(
                            handler.getField().getContainingClass(), null, false, fields);
                    System.out.println(constructor.getName());
                    handler
                        .getField()
                        .getContainingClass()
                        .addBefore(
                            constructor, handler.getField().getContainingClass().getRBrace());
                  });
              noSelectionNeed.add(accessPath);
            }
          }
        }
      } else {
        for (AccessPath accessPath : accessPathSetMap.keySet()) {
          PsiField base = accessPath.getFields().get(0); // only base AP
          AccessPath newAP = new AccessPath(base);
          noSelectionNeed.add(newAP);
        }
      }
    }

    activityAnalysisResult.setCriticalData(noSelectionNeed);
    processActivity(project, psiManager, sourceProvider, activityAnalysisResult);
  }

  public static void processActivity(
      Project project,
      PsiManager psiManager,
      SourceProvider sourceProvider,
      ActivityAnalysisResult activityAnalysisResult) {
    StringBuilder saveCodeSnippet = new StringBuilder();
    StringBuilder restoreCodeSnippet = new StringBuilder();

    PsiClass androidView = Utils.findClass(psiManager, "android.view.View");
    PsiClass javaObject = Utils.findClass(psiManager, "java.lang.Object");

    final String gsonLocal = "gson";
    String gsonDeclaration = "Gson " + gsonLocal + " = new Gson();";
    PsiClass gsonClass = Utils.findClass(psiManager, "com.google.gson.Gson");

    if (activityAnalysisResult.getActivityName() != null
        && activityAnalysisResult.getActivityName().getContainingFile().isWritable()) {
      Utils.importClass(project, activityAnalysisResult.getActivityName(), gsonClass);
      Set<String> existingVariables = new HashSet<>();

      saveCodeSnippet.append(gsonDeclaration + "\n");
      restoreCodeSnippet.append(gsonDeclaration + "\n");

      Set<AccessPath> criticalData = activityAnalysisResult.getCriticalData();
      Utils.showMessage(
          "Critical Data Size: "
              + criticalData.size()
              + "\n"
              + activityAnalysisResult.getMayPointsToForEventListener().size());
      for (AccessPath ap : criticalData) {
        // Utils.showMessage("Critical: " + accessPath.toString());
        if (activityAnalysisResult.getProcessedUsingPointsTo().contains(ap)) continue;
        AccessPath accessPath = ap.getSaveRestoreablePath(ap);
        PsiField topField = accessPath.getFields().get(ap.getFields().size() - 1);

        Optional<PsiClass> type = Utils.resolveFieldType(topField);
        if (type.isPresent()) {
          if (ignoreToSaveRestore(type.get())) {
            continue;
          }
          if (Utils.isView(androidView, type.get(), javaObject)) {
            continue;
          }
        }
        if (accessPath.isSingleField()) {
          String saveCode =
              processSingleFieldSave(
                  psiManager, accessPath, gsonLocal, activityAnalysisResult.getActivityName());
          saveCodeSnippet.append(saveCode + "\n");

          // restore
          String restoreCode =
              processSingleFieldRestore(
                  psiManager, accessPath, gsonLocal, activityAnalysisResult.getActivityName());
          restoreCodeSnippet.append(restoreCode + "\n");
        } else {
          String saveCode = processMultiLevelFieldSave(project, psiManager, accessPath, gsonLocal);
          saveCodeSnippet.append(saveCode + "\n");

          // restore
          String restoreCode =
              processMultiLevelFieldRestore(
                  project,
                  psiManager,
                  sourceProvider,
                  activityAnalysisResult.getActivityName(),
                  existingVariables,
                  true,
                  accessPath,
                  gsonLocal);
          restoreCodeSnippet.append(restoreCode + "\n");
        }
        // runtime alias checking
        boolean hasAlias =
            activityAnalysisResult.getMayPointsToForEventListener().containsKey(accessPath);
        // Utils.showMessage(accessPath.getKey()+":"+hasAlias);
        if (hasAlias) {
          Utils.showMessage("Alias for " + accessPath.getVariableName());
          String conditionLHS = "";
          if (accessPath.isSingleField()) conditionLHS = accessPath.getFields().get(0).getName();
          else conditionLHS = MultiLevelFieldGetting(project, accessPath);

          Set<AccessPath> pointsToSet =
              activityAnalysisResult.getMayPointsToForEventListener().get(accessPath);
          for (AccessPath pointsTo : pointsToSet) {
            String aliasKey = "Alias" + accessPath.getKey() + ":" + pointsTo.getKey();
            String conditionRHS = "";
            if (pointsTo.isSingleField())
              conditionRHS = "this." + pointsTo.getFields().get(0).getName();
            else conditionRHS = MultiLevelFieldGetting(project, pointsTo);
            String ifCondition =
                "if("
                    + conditionLHS
                    + "=="
                    + conditionRHS
                    + "){\n"
                    + Utils.saveCallbackParameter
                    + ".putBoolean(\""
                    + aliasKey
                    + "\",true);\n}";
            String restoreIf =
                "if(" + Utils.restoreCallbackParameter + ".getBoolean(\"" + aliasKey + "\",false))";
            String restoreElse = "";
            String pointsToSave = "";
            String restoreIfBody = "";
            if (pointsTo.isSingleField()) {
              pointsToSave =
                  processSingleFieldSave(
                      psiManager, pointsTo, gsonLocal, activityAnalysisResult.getActivityName());
              restoreIfBody =
                  "{\n" + pointsTo.getFields().get(0).getName() + "=" + conditionLHS + ";\n}";
              restoreElse =
                  processSingleFieldRestore(
                      psiManager, pointsTo, gsonLocal, activityAnalysisResult.getActivityName());
            } else {
              pointsToSave = processMultiLevelFieldSave(project, psiManager, pointsTo, gsonLocal);
              restoreIfBody =
                  "{\n"
                      + MultiLevelFieldSetting(
                          project,
                          psiManager,
                          sourceProvider,
                          activityAnalysisResult.getActivityName(),
                          pointsTo,
                          existingVariables,
                          conditionLHS)
                      + "\n}";
              restoreElse =
                  processMultiLevelFieldRestore(
                      project,
                      psiManager,
                      sourceProvider,
                      activityAnalysisResult.getActivityName(),
                      existingVariables,
                      true,
                      pointsTo,
                      gsonLocal);
            }
            String elseBlock = "else{\n" + pointsToSave + "\n}";
            saveCodeSnippet.append(ifCondition + "\n");
            saveCodeSnippet.append(elseBlock + "\n");
            String elseRestoreBlock = "else{\n" + restoreElse + "\n}";
            restoreCodeSnippet.append(restoreIf + restoreIfBody + "\n");
            restoreCodeSnippet.append(elseRestoreBlock + "\n");
          }
        }
      }

      for (ViewResult viewResult : activityAnalysisResult.getUiResults()) {
        Utils.showMessage("View Result: " + viewResult.getViewType() + ":" + viewResult.getName());
        PsiClass viewClass = Utils.findClass(psiManager, viewResult.getViewType());
        Utils.importClass(project, activityAnalysisResult.getActivityName(), viewClass);

        String viewLocal = Utils.variableNameForView(viewResult);
        String viewIdRetrieve = "R.id." + viewResult.getName();
        if (viewResult.getName().isEmpty()) { // when there is no name
          viewIdRetrieve = String.valueOf(viewResult.getId());
        }
        String viewDeclaration =
            viewClass.getName()
                + " "
                + viewLocal
                + " = ("
                + viewClass.getName()
                + ")findViewById("
                + viewIdRetrieve
                + ");";

        saveCodeSnippet.append(viewDeclaration + "\n");
        restoreCodeSnippet.append(viewDeclaration + "\n");

        for (ViewPropertyResult property : viewResult.getProperties()) {
          String propertyLocal = viewLocal + property.getPropertyName();
          String getter = Utils.extractMethodName(property.getGetterSignature());
          String returnType = Utils.extractMethodReturn(property.getGetterSignature());

          PsiClass returnClass = ClassUtil.findPsiClass(psiManager, returnType);

          PsiClassType apiReturnType = TypeUtils.getType(returnClass);
          if (canPutTypeIntoBundle(psiManager, apiReturnType)) {
            String putAPI = getAPItoPutInBundle(psiManager, apiReturnType);
            String putStmt =
                Utils.saveCallbackParameter
                    + "."
                    + putAPI
                    + "("
                    + "\""
                    + propertyLocal
                    + "\","
                    + viewLocal
                    + "."
                    + getter
                    + "());";
            saveCodeSnippet.append(putStmt);

            String getAPI = getAPItoGetFromBundle(psiManager, apiReturnType);
            String getStmt =
                apiReturnType.getClassName()
                    + " "
                    + propertyLocal
                    + " = "
                    + Utils.restoreCallbackParameter
                    + "."
                    + getAPI
                    + "("
                    + "\""
                    + propertyLocal
                    + "\");";
            String setter = Utils.extractMethodName(property.getSetterSignature());
            String setPropertyStmt = viewLocal + "." + setter + "(" + propertyLocal + ");";
            restoreCodeSnippet.append(getStmt);
            restoreCodeSnippet.append(setPropertyStmt);
          } else {
            String propertyDeclaration =
                "String "
                    + propertyLocal
                    + "="
                    + gsonLocal
                    + ".toJson("
                    + viewLocal
                    + "."
                    + getter
                    + "());";
            String putStmt =
                Utils.saveCallbackParameter
                    + ".putString("
                    + "\""
                    + propertyLocal
                    + "\","
                    + propertyLocal
                    + ");";

            saveCodeSnippet.append(propertyDeclaration + "\n");
            saveCodeSnippet.append(putStmt + "\n");

            // restoring part
            String getStmt =
                "String "
                    + propertyLocal
                    + "="
                    + Utils.restoreCallbackParameter
                    + ".getString("
                    + "\""
                    + propertyLocal
                    + "\");";
            String gsonConvert =
                returnType
                    + " gson"
                    + propertyLocal
                    + "="
                    + gsonLocal
                    + ".fromJson("
                    + propertyLocal
                    + ","
                    + returnType
                    + ".class);";
            String setter = Utils.extractMethodName(property.getSetterSignature());
            String setPropertyStmt = viewLocal + "." + setter + "(gson" + propertyLocal + ");";
            restoreCodeSnippet.append(getStmt + "\n");
            restoreCodeSnippet.append(gsonConvert + "\n");
            restoreCodeSnippet.append(setPropertyStmt + "\n");
          }
        }

        // Utils.showMessage(viewDeclaration);
      }

      // generate code
      Utils.insertSaveRestoreMethods(
          project,
          activityAnalysisResult.getActivityName(),
          saveCodeSnippet.toString(),
          restoreCodeSnippet.toString());
    }
  }

  public static void findReferences(Project project, PsiField psiField) {
    GlobalSearchScope useScope = GlobalSearchScope.projectScope(project);
    Iterable<PsiReference> references = SearchUtils.findAllReferences(psiField, useScope);
    Iterator<PsiReference> it = references.iterator();
    while (it.hasNext()) {
      PsiReference next = it.next();
      PsiElement element = next.resolve();

      System.out.println(
          "Reference: " + element.getText() + ":" + next.getElement().getParent().getText());
    }
  }

  public static String processSingleFieldSave(
      PsiManager psiManager, AccessPath accessPath, String gsonLocal, PsiClass activity) {
    StringBuilder saveCodeSnippet = new StringBuilder();
    PsiField field = accessPath.getFields().get(accessPath.getFields().size() - 1);
    if (field != null) {
      if (canPutTypeIntoBundle(psiManager, field.getType())) {
        String putAPI = getAPItoPutInBundle(psiManager, field.getType());
        String putStmt =
            Utils.saveCallbackParameter
                + "."
                + putAPI
                + "("
                + "\""
                + accessPath.getKey()
                + "\", this."
                + field.getName()
                + ");";
        return putStmt;
      } else {
        String fieldLocal = field.getName() + "Json";
        // todo use getter in case field in coming from Super class

        String toStringStmt =
            "String " + fieldLocal + " = " + gsonLocal + ".toJson(this." + field.getName() + ");";
        String putStmt =
            Utils.saveCallbackParameter
                + ".putString("
                + "\""
                + accessPath.getKey()
                + "\","
                + fieldLocal
                + ");";
        //            saveCodeSnippet.append(toStringStmt + "\n");
        //            saveCodeSnippet.append(putStmt + "\n");
        if (!activity.equals(field.getContainingClass())) {
          PsiMethod getter = PropertyUtil.findGetterForField(field);

          if (getter != null) {
            toStringStmt =
                "String " + fieldLocal + " = " + gsonLocal + ".toJson(" + getter.getName() + "());";
          } else {
            String toStringStmtInitComment =
                "// todo (Fix: LiveDroid): add getter for " + field.getName() + "\n";
            saveCodeSnippet.append(toStringStmtInitComment + "\n");
          }
          saveCodeSnippet.append(toStringStmt + "\n");
          saveCodeSnippet.append(putStmt + "\n");
        } else {
          saveCodeSnippet.append(toStringStmt + "\n");
          saveCodeSnippet.append(putStmt + "\n");
        }
        // PropertyUtil.findSetterForField();
      }
    }
    return saveCodeSnippet.toString();
  }

  public static String processSingleFieldRestore(
      PsiManager psiManager, AccessPath accessPath, String gsonLocal, PsiClass activity) {
    StringBuilder restoreCodeSnippet = new StringBuilder();
    PsiField field = accessPath.getFields().get(accessPath.getFields().size() - 1);
    if (field != null) {
      if (canPutTypeIntoBundle(psiManager, field.getType())) {
        String getAPI = getAPItoGetFromBundle(psiManager, field.getType());
        String getStmt =
            "this."
                + field.getName()
                + " = "
                + Utils.restoreCallbackParameter
                + "."
                + getAPI
                + "("
                + "\""
                + accessPath.getKey()
                + "\");";
        return getStmt;
      } else {
        String fieldLocal = field.getName() + "Json";
        String typeLocal = fieldLocal + "Type";
        String getStmt =
            "String "
                + fieldLocal
                + "="
                + Utils.restoreCallbackParameter
                + ".getString("
                + "\""
                + accessPath.getKey()
                + "\");";
        String typeStmt =
            "java.lang.reflect.Type "
                + typeLocal
                + " = new com.google.gson.reflect.TypeToken<"
                + field.getType().getCanonicalText()
                + ">() {}.getType();";
        String setFieldVariable = "this." + field.getName() + "=";
        String RBrace = ");";
        if (!field.getContainingClass().equals(activity)) {
          PsiMethod setter = PropertyUtil.findSetterForField(field);
          if (setter != null) {
            setFieldVariable = setter.getName() + "(";
            RBrace = "));";
          } else {
            String toStringStmtInitComment =
                "// todo (Fix: LiveDroid): add setter for " + field.getName() + "\n";
            restoreCodeSnippet.append(toStringStmtInitComment + "\n");
          }
        }
        String setFieldStmt =
            setFieldVariable + gsonLocal + ".fromJson(" + fieldLocal + "," + typeLocal + RBrace;
        if (TypeConversionUtil.isPrimitive(field.getType().getCanonicalText())) {
          typeStmt = "";
          setFieldStmt =
              setFieldVariable
                  + gsonLocal
                  + ".fromJson("
                  + fieldLocal
                  + ","
                  + field.getType().getCanonicalText()
                  + ".class"
                  + RBrace;
        }
        restoreCodeSnippet.append(getStmt + "\n");
        restoreCodeSnippet.append(typeStmt + "\n");
        restoreCodeSnippet.append(setFieldStmt + "\n");
      }
    }
    return restoreCodeSnippet.toString();
  }

  public static String MultiLevelFieldGetting(Project project, AccessPath accessPath) {
    PsiField activityField = accessPath.getFields().get(0);
    PsiField lastField = accessPath.getFields().get(accessPath.getFields().size() - 1);
    StringBuilder gettingSnippet = new StringBuilder();
    gettingSnippet.append(activityField.getName());
    for (int i = 1; i < accessPath.getFields().size(); i++) {
      PsiField currField = accessPath.getFields().get(i);
      Utils.generateGetterAndSetterIfNotExist(project, currField);
      gettingSnippet.append("." + Utils.getGetterName(currField) + "()");
    }
    gettingSnippet.append("");
    return gettingSnippet.toString();
  }

  public static String MultiLevelFieldSetting(
      Project project,
      PsiManager psiManager,
      SourceProvider sourceProvider,
      PsiClass activity,
      AccessPath accessPath,
      Set<String> existingVariables,
      String rhs) {
    StringBuilder restoreCodeSnippet = new StringBuilder();
    String lastLocalName = rhs;

    for (int i = accessPath.getFields().size() - 2; i >= 0; i--) {
      PsiField currField = accessPath.getFields().get(i);
      PsiField prevField = accessPath.getFields().get(i + 1);
      Optional<PsiClass> classType = Utils.resolveFieldType(currField);
      if (classType.isPresent()) {
        final String localVar = "local" + accessPath.getVariableNameUntil(currField);
        if (!existingVariables.contains(localVar)) {
          String dummyInit =
              classType.get().getQualifiedName()
                  + " "
                  + localVar
                  + "= new "
                  + classType.get().getQualifiedName()
                  + "();";
          restoreCodeSnippet.append(dummyInit + "\n");
          // existingVariables.add(dummyLocal);
        }
        String setterName = Utils.getSetterName(prevField);
        String dummySetter = localVar + "." + setterName + "(" + lastLocalName + ");";
        restoreCodeSnippet.append(dummySetter + "\n");
        lastLocalName = localVar;
        if (currField.getContainingClass().isEquivalentTo(activity)) {
          String assignField = "this." + currField.getName() + "=" + localVar + ";";
          restoreCodeSnippet.append(assignField + "\n");
        }
      }
      // gettingSnippet.append("." + Utils.getGetterName(currField) + "()");
    }
    return restoreCodeSnippet.toString();
  }

  public static String processMultiLevelFieldSave(
      Project project, PsiManager psiManager, AccessPath accessPath, String gsonLocal) {
    StringBuilder saveCodeSnippet = new StringBuilder();
    if (!accessPath.getFields().isEmpty()) {
      PsiField activityField = accessPath.getFields().get(0);
      PsiField lastField = accessPath.getFields().get(accessPath.getFields().size() - 1);

      StringBuilder gettingSnippet = new StringBuilder();
      gettingSnippet.append(
          lastField.getType().getCanonicalText()
              + " "
              + accessPath.getVariableName()
              + "="
              + activityField.getName());
      for (int i = 1; i < accessPath.getFields().size(); i++) {
        PsiField currField = accessPath.getFields().get(i);
        if (currField.getContainingClass().isWritable()) {
          // Utils.generateGetterAndSetterIfNotExist(project, currField);
          gettingSnippet.append("." + Utils.getGetterName(currField) + "()");
        }
      }
      gettingSnippet.append(";\n");
      saveCodeSnippet.append(gettingSnippet);
      PsiField topField = accessPath.getFields().get(accessPath.getFields().size() - 1);
      if (canPutTypeIntoBundle(psiManager, topField.getType())) {
        String putAPI = getAPItoPutInBundle(psiManager, topField.getType());
        String putStmt =
            Utils.saveCallbackParameter
                + "."
                + putAPI
                + "("
                + "\""
                + accessPath.getKey()
                + "\","
                + accessPath.getVariableName()
                + ");";
        saveCodeSnippet.append(putStmt + "\n");
      } else {
        String fieldLocal = accessPath.getVariableName() + "Json";
        String toStringStmt =
            "String "
                + fieldLocal
                + " = "
                + gsonLocal
                + ".toJson("
                + accessPath.getVariableName()
                + ");";
        String putStmt =
            Utils.saveCallbackParameter
                + ".putString("
                + "\""
                + accessPath.getKey()
                + "\","
                + fieldLocal
                + ");";
        saveCodeSnippet.append(toStringStmt + "\n");
        saveCodeSnippet.append(putStmt + "\n");
      }
    }
    return saveCodeSnippet.toString();
  }

  public static String processMultiLevelFieldRestore(
      Project project,
      PsiManager psiManager,
      SourceProvider sourceProvider,
      PsiClass activity,
      Set<String> existingVariables,
      boolean addInSet,
      AccessPath accessPath,
      String gsonLocal) {
    StringBuilder restoreCodeSnippet = new StringBuilder();
    if (!accessPath.getFields().isEmpty()) {
      PsiField activityField = accessPath.getFields().get(0);
      String lastLocalName = accessPath.getVariableName();
      PsiField lastField = accessPath.getFields().get(accessPath.getFields().size() - 1);
      if (canPutTypeIntoBundle(psiManager, lastField.getType())) {
        String getAPI = getAPItoGetFromBundle(psiManager, lastField.getType());
        String getStmt =
            lastField.getType().getCanonicalText()
                + " "
                + accessPath.getVariableName()
                + " = "
                + Utils.restoreCallbackParameter
                + "."
                + getAPI
                + "("
                + "\""
                + accessPath.getKey()
                + "\");";
        restoreCodeSnippet.append(getStmt + "\n");
      } else {
        String fieldLocal = accessPath.getVariableName() + "Json";
        String getStmt =
            "String "
                + fieldLocal
                + "="
                + Utils.restoreCallbackParameter
                + ".getString("
                + "\""
                + accessPath.getKey()
                + "\");";
        String setFieldStmt =
            lastField.getType().getCanonicalText()
                + " "
                + accessPath.getVariableName()
                + "="
                + gsonLocal
                + ".fromJson("
                + fieldLocal
                + ","
                + lastField.getType().getCanonicalText()
                + ".class);";
        restoreCodeSnippet.append(getStmt + "\n");
        restoreCodeSnippet.append(setFieldStmt + "\n");
      }
      // build Dummy parents here
      for (int i = accessPath.getFields().size() - 2; i >= 0; i--) {
        PsiField currField = accessPath.getFields().get(i);
        PsiField prevField = accessPath.getFields().get(i + 1);
        Optional<PsiClass> classType = Utils.resolveFieldType(currField);
        if (classType.isPresent()) {
          if (classType.get().isWritable()) {
            //            Utils.generateDummyConstructIfNotExist(
            //                project, psiManager, sourceProvider, classType.get());
            final String variableLocal = "local" + accessPath.getVariableNameUntil(currField);
            if (!existingVariables.contains(variableLocal)) {
              String dummyInit =
                  classType.get().getQualifiedName()
                      + " "
                      + variableLocal
                      + "= new "
                      + classType.get().getQualifiedName()
                      + "();"; // default Constructor
              restoreCodeSnippet.append(dummyInit + "\n");
              if (addInSet) {
                existingVariables.add(variableLocal);
              }
            }
            String setterName = Utils.getSetterName(prevField);
            String dummySetter = variableLocal + "." + setterName + "(" + lastLocalName + ");";
            restoreCodeSnippet.append(dummySetter + "\n");
            lastLocalName = variableLocal;
            if (currField.getContainingClass().isEquivalentTo(activity)) {
              String assignField = "this." + currField.getName() + "=" + variableLocal + ";";
              restoreCodeSnippet.append(assignField + "\n");
            }
          }
        }
        // gettingSnippet.append("." + Utils.getGetterName(currField) + "()");
      }
    }
    return restoreCodeSnippet.toString();
  }

  public static boolean gsonArtifactsExists(Module module) {

    GradleBuildModel buildModel = GradleModelProvider.get().getBuildModel(module);

    String gsonGroup = "com.google.code.gson";
    boolean gsonArtifactExists = false;
    DependenciesModel dependencies = buildModel.dependencies();
    for (ArtifactDependencyModel artifact : dependencies.artifacts()) {
      if (artifact.compactNotation().startsWith(gsonGroup)) {
        gsonArtifactExists = true;
      }
    }
    return gsonArtifactExists;
  }

  public static Promise<Void> importGsonToGradle(Project project, Module module) {
    //        AndroidGradleJavaProjectModelModifier modelModifier = new
    // AndroidGradleJavaProjectModelModifier();
    //        Collection<Module> modules = Collections.singleton(module);
    //        GsonLibraryDescriptor descriptor = new GsonLibraryDescriptor();
    //        modelModifier.addExternalLibraryDependency(modules, descriptor,
    // DependencyScope.COMPILE);

    //        GradleBuildModel buildModel =
    // GradleModelProvider.get().getBuildModel(module);//GradleBuildModel.get(module);
    //
    //        DependenciesModel dependenciesModel = buildModel.dependencies();
    //
    ////        for (ArtifactDependencyModel artifactDependencyModel :
    // dependenciesModel.artifacts()) {
    ////
    ////        }
    //        ArtifactDependencySpec newDependency = ArtifactDependencySpec.create("gson",
    // "com.google.code.gson", "2.8.5");
    //        buildModel.dependencies().addArtifact(COMPILE, newDependency);
    //
    //        //dependenciesModel.addArtifact(COMPILE, newDependency);
    //
    //        runWriteCommandAction(project, buildModel::applyChanges);
    //        requestProjectSync().waitForGradleProjectSyncToFinish();

    //        new WriteCommandAction(project, "Add Gradle Gson Dependency") {
    //            @Override
    //            protected void run(@NotNull Result result) throws Throwable {
    //
    //                buildModel.applyChanges();
    //            }
    //        }.execute();

    GradleBuildModel buildModel = GradleModelProvider.get().getBuildModel(module);

    String gsonGroup = "com.google.code.gson";
    DependenciesModel dependencies = buildModel.dependencies();

    ArtifactDependencySpec dependencySpec =
        ArtifactDependencySpec.create("gson", gsonGroup, "2.8.5");
    dependencies.addArtifact(IMPLEMENTATION, dependencySpec);
    runWriteCommandAction(
        project,
        () -> {
          buildModel.applyChanges();
          registerUndoAction(project);
        });

    return requestProjectSync(project);
  }

  private static void registerUndoAction(@NotNull Project project) {
    UndoManager.getInstance(project)
        .undoableActionPerformed(
            new BasicUndoableAction() {
              @Override
              public void undo() throws UnexpectedUndoException {
                requestProjectSync(project);
              }

              @Override
              public void redo() throws UnexpectedUndoException {
                requestProjectSync(project);
              }
            });
  }

  @NotNull
  private static Promise<Void> requestProjectSync(@NotNull Project project) {
    AsyncPromise<Void> promise = new AsyncPromise<>();
    GradleSyncInvoker.Request request =
        new GradleSyncInvoker.Request(TRIGGER_MODIFIER_ADD_MODULE_DEPENDENCY);
    request.generateSourcesOnSuccess = false;

    GradleSyncInvoker.getInstance()
        .requestProjectSync(
            project,
            request,
            new GradleSyncListener() {
              @Override
              public void syncSucceeded(@NotNull Project project) {
                promise.setResult(null);
              }

              @Override
              public void syncFailed(@NotNull Project project, @NotNull String errorMessage) {
                promise.setError(errorMessage);
              }
            });

    return promise;
  }

  public static boolean canPutTypeIntoBundle(PsiManager psiManager, PsiType type) {
    if (ClassUtils.isPrimitive(type)) return true;
    PsiClass stringClass = Utils.findClass(psiManager, "java.lang.String");
    PsiClass charSequenceClass = Utils.findClass(psiManager, "java.lang.CharSequence");
    PsiClass typeClass = PsiTypesUtil.getPsiClass(type);
    if (stringClass.equals(typeClass)) {
      return true;
    } else if (charSequenceClass.equals(typeClass)) {
      return true;
    } else if (PsiType.BOOLEAN.equals(type)
        || PsiType.BOOLEAN.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    } else if (PsiType.BYTE.equals(type)
        || PsiType.BYTE.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    } else if (PsiType.SHORT.equals(type)
        || PsiType.SHORT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    } else if (PsiType.INT.equals(type)
        || PsiType.INT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    } else if (PsiType.LONG.equals(type)
        || PsiType.LONG.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    } else if (PsiType.FLOAT.equals(type)
        || PsiType.FLOAT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    } else if (PsiType.DOUBLE.equals(type)
        || PsiType.DOUBLE.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    } else if (PsiType.CHAR.equals(type)
        || PsiType.CHAR.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return true;
    }
    return false;
  }

  public static String getAPItoPutInBundle(PsiManager psiManager, PsiType type) {
    PsiClass stringClass = Utils.findClass(psiManager, "java.lang.String");
    PsiClass charSequenceClass = Utils.findClass(psiManager, "java.lang.CharSequence");
    PsiClass typeClass = PsiTypesUtil.getPsiClass(type);

    if (stringClass.equals(typeClass)) {
      return "putString";
    } else if (charSequenceClass.equals(typeClass)) {
      return "putCharSequence";
    } else if (PsiType.BOOLEAN.equals(type)
        || PsiType.BOOLEAN.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putBoolean";
    } else if (PsiType.BYTE.equals(type)
        || PsiType.BYTE.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putByte";
    } else if (PsiType.SHORT.equals(type)
        || PsiType.SHORT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putShort";
    } else if (PsiType.INT.equals(type)
        || PsiType.INT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putInt";
    } else if (PsiType.LONG.equals(type)
        || PsiType.LONG.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putLong";
    } else if (PsiType.FLOAT.equals(type)
        || PsiType.FLOAT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putFloat";
    } else if (PsiType.DOUBLE.equals(type)
        || PsiType.DOUBLE.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putDouble";
    } else if (PsiType.CHAR.equals(type)
        || PsiType.CHAR.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "putChar";
    }
    return null;
  }

  public static String getAPItoGetFromBundle(PsiManager psiManager, PsiType type) {
    PsiClass stringClass = Utils.findClass(psiManager, "java.lang.String");
    PsiClass charSequenceClass = Utils.findClass(psiManager, "java.lang.CharSequence");
    PsiClass typeClass = PsiTypesUtil.getPsiClass(type);
    if (stringClass.equals(typeClass)) {
      return "getString";
    } else if (charSequenceClass.equals(typeClass)) {
      return "getCharSequence";
    } else if (PsiType.BOOLEAN.equals(type)
        || PsiType.BOOLEAN.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getBoolean";
    } else if (PsiType.BYTE.equals(type)
        || PsiType.BYTE.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getByte";
    } else if (PsiType.SHORT.equals(type)
        || PsiType.SHORT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getShort";
    } else if (PsiType.INT.equals(type)
        || PsiType.INT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getInt";
    } else if (PsiType.LONG.equals(type)
        || PsiType.LONG.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getLong";
    } else if (PsiType.FLOAT.equals(type)
        || PsiType.FLOAT.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getFloat";
    } else if (PsiType.DOUBLE.equals(type)
        || PsiType.DOUBLE.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getDouble";
    } else if (PsiType.CHAR.equals(type)
        || PsiType.CHAR.equals(PsiPrimitiveType.getUnboxedType(type))) {
      return "getChar";
    }
    return null;
  }

  private static boolean ignoreToSaveRestore(PsiClass typeClass) {
    if (typeClass.getQualifiedName().startsWith("android.os")) {
      return true;
    }
    if (typeClass.getQualifiedName().startsWith("android.app")) return true;
    if (typeClass.getQualifiedName().startsWith("android.content")) return true;
    if (typeClass.getQualifiedName().startsWith("android.database")) return true;
    if (typeClass.getQualifiedName().startsWith("android.widget")) return true;
    if (typeClass.getQualifiedName().startsWith("android.webkit")) return true;
    return false;
  }
}
