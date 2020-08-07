package edu.ucr.cs.ufarooq.liveDroid.actions;

import com.android.builder.model.SourceProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import edu.ucr.cs.ufarooq.liveDroid.Utils;
import edu.ucr.cs.ufarooq.liveDroid.staticResults.ActivityAnalysisResult;
import edu.ucr.cs.ufarooq.liveDroid.staticResults.ApplicationAnalysisResult;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.manifest.Activity;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.jps.android.model.impl.JpsAndroidModuleProperties;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class LiveDroidAppAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {

        final Project project = e.getData(PlatformDataKeys.PROJECT);
        final Module module = e.getData(LangDataKeys.MODULE);
        PsiManager psiManager = PsiManager.getInstance(project);

        AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) {
            Messages.showMessageDialog("Facet is NULL", "Facet", Messages.getInformationIcon());
        } else {

            for (Activity activity : facet.getManifest().getApplication().getActivities()) {
                AndroidAttributeValue<PsiClass> activityClass = activity.getActivityClass();
                Messages.showMessageDialog("Activity: " + activity.getActivityClass().getStringValue(), "Activity", Messages.getInformationIcon());
            }
        }

        JpsAndroidModuleProperties androidModuleProperties = facet.getProperties();
        SourceProvider mainSourceProvider = facet.getMainSourceProvider();
        File selectedFile = Utils.browseFile();
        if (selectedFile != null) {
            Utils.showMessage(selectedFile.getAbsolutePath());
            try {
                ApplicationAnalysisResult applicationAnalysisResult = Utils.parseXMLResults(selectedFile, psiManager);
                for (ActivityAnalysisResult activityAnalysisResult : applicationAnalysisResult.getActivityAnalysisResultSet()) {
                    LiveDroidActivityAction.processActivity(project, psiManager, mainSourceProvider, activityAnalysisResult);
                }
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
}
