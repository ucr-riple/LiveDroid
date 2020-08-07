package edu.ucr.cs.ufarooq.android.XMLPrefs;

import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.ApkHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ProcessPrefXMLFile {
    protected ApkHandler apk;
    protected AXmlHandler axml;
    protected List<AXmlNode> preferenceScreens;
    protected AXmlNode application;
    private int xmlId;


    public ProcessPrefXMLFile(String apkPath, int xmlId) throws IOException, XmlPullParserException {
        this(new File(apkPath), xmlId);
    }

    public ProcessPrefXMLFile(File apkFile, int xmlId) throws IOException, XmlPullParserException {
        this.apk = null;
        this.xmlId = xmlId;
        if (!apkFile.exists()) {
            throw new RuntimeException(String.format("The given APK file %s does not exist", apkFile.getCanonicalPath()));
        } else {
            this.apk = new ApkHandler(apkFile);
            InputStream is = null;

            try {
                String xml = PreferencesProvider.getInstance().getNameForId(xmlId);
                is = this.apk.getInputStream("res/xml/" + xml + ".xml");
//                if (is == null) {
//                    throw new FileNotFoundException(String.format("The file %s does not contain % .xml File", apkFile.getAbsolutePath(), xml));
//                }

                this.handle(is);
            } finally {
                if (is != null) {
                    is.close();
                }

            }

        }
    }

    public ProcessPrefXMLFile(InputStream manifestIS) throws IOException, XmlPullParserException {
        this.apk = null;
        this.handle(manifestIS);
    }

    protected void handle(InputStream manifestIS) throws IOException, XmlPullParserException {
        if (manifestIS != null) {
            this.axml = new AXmlHandler(manifestIS);
            preferenceScreens = this.axml.getNodesWithTag("PreferenceScreen");
            PreferencesProvider.getInstance().insertNodes(xmlId, preferenceScreens);
        }
    }


    public ApkHandler getApk() {
        return this.apk;
    }

    public List<AXmlNode> getPreferenceScreens() {
        return this.preferenceScreens;
    }
}