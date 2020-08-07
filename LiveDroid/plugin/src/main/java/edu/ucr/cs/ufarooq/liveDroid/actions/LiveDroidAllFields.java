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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.refactoring.psi.SearchUtils;
import edu.ucr.cs.ufarooq.liveDroid.Utils;
import edu.ucr.cs.ufarooq.liveDroid.staticResults.*;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.manifest.Activity;
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
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.android.tools.idea.gradle.dsl.api.dependencies.CommonConfigurationNames.COMPILE;
import static edu.ucr.cs.ufarooq.liveDroid.Utils.DUMMY_PACKAGE_NAME;
import static edu.ucr.cs.ufarooq.liveDroid.Utils.isView;
import static edu.ucr.cs.ufarooq.liveDroid.actions.LiveDroidActivityAction.importGsonToGradle;

public class LiveDroidAllFields extends AnAction {
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
                Messages.showMessageDialog("Facet is NULL, Cannot continue", "Facet", Messages.getErrorIcon());
            }

            JpsAndroidModuleProperties androidModuleProperties = facet.getProperties();
            SourceProvider mainSourceProvider = facet.getMainSourceProvider();
            IdeaSourceProvider ideaSourceProvider = facet.getMainIdeaSourceProvider();
            //Promise<Void> promise = importGsonToGradle(project, module);
            // promise.onSuccess(c -> {
            PsiClass layoutTraverserClass = Utils.findClass(psiManager, DUMMY_PACKAGE_NAME + ".LayoutTraverser");// project, module);

            if (layoutTraverserClass == null) {
                PsiDirectory dummyDirectory = Utils.createPackage(project, mainSourceProvider, DUMMY_PACKAGE_NAME);
                PsiFile configElement = PsiFileFactory.getInstance(project).createFileFromText("LayoutTraverser.java", JavaFileType.INSTANCE, Utils.CLASS_DEF_LayoutTraverser);
                PsiElement createdDummyClass = dummyDirectory.add(configElement);
            }

            for (Activity activity : facet.getManifest().getApplication().getActivities()) {
                PsiClass activityClass = activity.getActivityClass().getValue();
                Messages.showMessageDialog("Activity: " + activity.getActivityClass().getStringValue(), "Activity", Messages.getInformationIcon());

                processActivity(project, psiManager, mainSourceProvider, activityClass);
                //Messages.showMessageDialog("Activity: " + activity.getActivityClass().getStringValue(), "Activity", Messages.getInformationIcon());

            }
            //});
//            PsiField[] allFields = psiClass.getFields(); //getAllFields return all super fields
//            PsiMethod[] allMethods = psiClass.getMethods();
//
//            for (PsiField field : allFields) {
//                PsiType fieldType = field.getType();
//
//                Optional<PsiClass> classType = Utils.resolveFieldType(field);
//                PsiClass fieldClass = null;
//                if (classType.isPresent()) {
//                    fieldClass = classType.get();
//                }
//                //Utils.showMessage(field.getNameIdentifier().getText() + ":" + fieldType.toString());
//            }
//
//            for (PsiMethod psiMethod : allMethods) {
//                //Utils.showMessage(psiMethod.getNameIdentifier().getText());
//            }
//
//            PsiClass textView = Utils.findClass(psiClass.getManager(), "android.widget.TextView");


        }
        // }
    }

    public static void processActivity(Project project, PsiManager psiManager, SourceProvider sourceProvider, PsiClass activity) {
        PsiField[] allFields = activity.getFields(); //getAllFields return all super fields
        PsiMethod[] allMethods = activity.getMethods();

        StringBuilder saveCodeSnippet = new StringBuilder();
        StringBuilder restoreCodeSnippet = new StringBuilder();

        PsiClass androidView = Utils.findClass(psiManager, "android.view.View");
        PsiClass javaObject = Utils.findClass(psiManager, "java.lang.Object");

        String startTimeStmt = "long liveDroidStartTime = java.util.Calendar.getInstance().getTimeInMillis();\n";
        String endTimeStmt = "long liveDroidEndTime = java.util.Calendar.getInstance().getTimeInMillis();\n" +
                "        long liveDroidTimeSpent = liveDroidEndTime - liveDroidStartTime;\n" +
                "        android.util.Log.v(\"%s\", java.lang.String.valueOf(liveDroidTimeSpent));";
        String printBundleSize = "int bundleSizeInBytes = edu.cs.ucr.dummy.LayoutTraverser.getBundleSizeInBytes(outState);\n" +
                "        android.util.Log.v(\"LiveDroidSaveSize \", java.lang.String.valueOf(bundleSizeInBytes));\n";
        saveCodeSnippet.append(startTimeStmt);
        restoreCodeSnippet.append(startTimeStmt);

        final String gsonLocal = "gson";
        String gsonDeclaration = "Gson " + gsonLocal + " = new Gson();";
        PsiClass gsonClass = Utils.findClass(psiManager, "com.google.gson.Gson");
        if (activity != null && activity.getContainingFile().isWritable()) {
            Utils.importClass(project, activity, gsonClass);
            Set<String> existingVariables = new HashSet<>();
            saveCodeSnippet.append(gsonDeclaration + "\n");
            restoreCodeSnippet.append(gsonDeclaration + "\n");

            for (PsiField psiField : allFields) {
                boolean isFinal = psiField.hasModifierProperty(PsiModifier.FINAL);
                PsiClass fieldType = Utils.findClass(psiManager, psiField.getType().getCanonicalText());
                boolean isView = isView(androidView, fieldType, javaObject);
                if (!isFinal && !isView) {
                    String saveCode = processSingleFieldSave(psiField, gsonLocal, activity);
                    saveCodeSnippet.append(saveCode + "\n");

                    //restore
                    String restoreCode = processSingleFieldRestore(psiField, gsonLocal, activity);
                    restoreCodeSnippet.append(restoreCode + "\n");

                }
            }
        }
        saveCodeSnippet.append(Utils.SAVE_VIEWS);
        saveCodeSnippet.append(printBundleSize);

        restoreCodeSnippet.append(Utils.RESTORE_VIEW);

        saveCodeSnippet.append(String.format(endTimeStmt, "LiveDroidSaveTime"));
        restoreCodeSnippet.append(String.format(endTimeStmt, "LiveDroidRestoreTime"));

        Utils.insertSaveRestoreMethods(project, activity, saveCodeSnippet.toString(), restoreCodeSnippet.toString());

    }

    public static String processSingleFieldSave(PsiField accessPath, String gsonLocal, PsiClass activity) {
        StringBuilder saveCodeSnippet = new StringBuilder();
        PsiField field = accessPath;//.getFields().get(accessPath.getFields().size() - 1);
        if (field != null) {
            String fieldLocal = field.getName() + "Json";
            //todo use getter in case field in coming from Super class

            String toStringStmt = "String " + fieldLocal + " = " + gsonLocal + ".toJson(this." + field.getName() + ");";
            String putStmt = Utils.saveCallbackParameter + ".putString(" + "\"" + accessPath.getName() + "\"," + fieldLocal + ");";
//            saveCodeSnippet.append(toStringStmt + "\n");
//            saveCodeSnippet.append(putStmt + "\n");
            if (!activity.equals(field.getContainingClass())) {
                PsiMethod getter = PropertyUtil.findGetterForField(field);

                if (getter != null) {
                    toStringStmt = "String " + fieldLocal + " = " + gsonLocal + ".toJson(" + getter.getName() + "());";
                } else {
                    String toStringStmtInitComment = "// todo (Fix: LiveDroid): add getter for " + field.getName() + "\n";
                    saveCodeSnippet.append(toStringStmtInitComment + "\n");
                }
                saveCodeSnippet.append(toStringStmt + "\n");
                saveCodeSnippet.append(putStmt + "\n");
            } else {
                saveCodeSnippet.append(toStringStmt + "\n");
                saveCodeSnippet.append(putStmt + "\n");
            }
            //PropertyUtil.findSetterForField();

        }
        return saveCodeSnippet.toString();
    }

    public static String processSingleFieldRestore(PsiField accessPath, String gsonLocal, PsiClass activity) {
        StringBuilder restoreCodeSnippet = new StringBuilder();
        PsiField field = accessPath;//.getFields().get(accessPath.getFields().size() - 1);
        if (field != null) {
            String fieldLocal = field.getName() + "Json";
            String typeLocal = fieldLocal + "Type";
            String getStmt = "String " + fieldLocal + "=" + Utils.restoreCallbackParameter + ".getString(" + "\"" + accessPath.getName() + "\");";
            String typeStmt = "java.lang.reflect.Type " + typeLocal + " = new com.google.gson.reflect.TypeToken<" + field.getType().getCanonicalText() + ">() {}.getType();";
            String setFieldVariable = "this." + field.getName() + "=";
            String RBrace = ");";
            if (!field.getContainingClass().equals(activity)) {
                PsiMethod setter = PropertyUtil.findSetterForField(field);
                if (setter != null) {
                    setFieldVariable = setter.getName() + "(";
                    RBrace = "));";
                } else {
                    String toStringStmtInitComment = "// todo (Fix: LiveDroid): add setter for " + field.getName() + "\n";
                    restoreCodeSnippet.append(toStringStmtInitComment + "\n");
                }
            }
            String setFieldStmt = setFieldVariable + gsonLocal + ".fromJson(" + fieldLocal + "," + typeLocal + RBrace;
            if (TypeConversionUtil.isPrimitive(field.getType().getCanonicalText())) {
                typeStmt = "";
                setFieldStmt = setFieldVariable + gsonLocal + ".fromJson(" + fieldLocal + "," + field.getType().getCanonicalText() + ".class" + RBrace;
            }
            restoreCodeSnippet.append(getStmt + "\n");
            restoreCodeSnippet.append(typeStmt + "\n");
            restoreCodeSnippet.append(setFieldStmt + "\n");
        }
        return restoreCodeSnippet.toString();
    }
}
