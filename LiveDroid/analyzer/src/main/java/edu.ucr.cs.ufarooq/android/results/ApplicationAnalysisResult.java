package edu.ucr.cs.ufarooq.android.results;

import com.google.common.collect.Sets;
import edu.ucr.cs.ufarooq.accessPath.Field;
import edu.ucr.cs.ufarooq.android.layout.ViewResult;
import edu.ucr.cs.ufarooq.android.model.FieldOnlyAccessPath;
import edu.ucr.cs.ufarooq.android.problems.Utils;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

public class ApplicationAnalysisResult {
    private final String name;
    private final String packageName;
    private final String version;
    private Set<ActivityAnalysisResult> activities;
    private final long startTime;

    public ApplicationAnalysisResult(String name, String packageName, String version, long startTime) {
        this.name = name;
        this.packageName = packageName;
        this.version = version;
        this.activities = new HashSet<>();
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Set<ActivityAnalysisResult> getActivities() {
        return activities;
    }

    public void setActivities(Set<ActivityAnalysisResult> activities) {
        this.activities = activities;
    }

    public void addActivity(ActivityAnalysisResult activity) {
        this.activities.add(activity);
    }

    public String getPackageName() {
        return packageName;
    }

    public void writeXML(String path) {
        try {
            DocumentBuilderFactory dbFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            final long runtime = Calendar.getInstance().getTimeInMillis() - this.startTime;
            Element appElement = XMLFileHelper.getAppElement(doc, this.packageName, this.version, runtime);
            Element activitiesXML = XMLFileHelper.getItemWithValue(doc, "Activities", this.activities.size());

            List<ViewResult> uiResults = new ArrayList<>();
            List<FieldOnlyAccessPath> mayUseForEventListener = new ArrayList<>();
            List<FieldOnlyAccessPath> mayModifyForEventListener = new ArrayList<>();
            //List<FieldOnlyAccessPath> mayAllocForEventListener = new ArrayList<>();
            List<FieldOnlyAccessPath> criticalData = new ArrayList<>();
            Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> mayPointsToForEventListener = new HashMap<>();
            int criticalActivitiesCount = 0;
            int connectionsCount = 0;
            for (ActivityAnalysisResult activityResult : this.activities) {
                Element activityXMLElement = activityResult.getActivityXMLElement(doc);
                activitiesXML.appendChild(activityXMLElement);
                uiResults.addAll(activityResult.getUiResults());
                Sets.SetView<FieldOnlyAccessPath> intersectionResult = Sets.intersection(activityResult.getMayUseForEventListener(), activityResult.getMayModifyForEventListener());
                criticalData.addAll(intersectionResult.immutableCopy());
                mayUseForEventListener.addAll(activityResult.getMayUseForEventListener());
                mayModifyForEventListener.addAll(activityResult.getMayModifyForEventListener());
                //mayAllocForEventListener.addAll(activityResult.getMayAllocForEventListener());
                mayPointsToForEventListener.putAll(activityResult.getMayPointsToForEventListener());

                if (!intersectionResult.immutableCopy().isEmpty() /*|| !activityResult.getMayAllocForEventListener().isEmpty()*/ || !activityResult.getUiResults().isEmpty() || !activityResult.getMayPointsToForEventListener().isEmpty()) {
                    criticalActivitiesCount++;
                }
                connectionsCount += activityResult.getConnections().size();
            }
            appElement.appendChild(activitiesXML);
            doc.appendChild(appElement);
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            String xmlFile = this.name + "_" + Calendar.getInstance().getTimeInMillis() + ".xml";

            if (criticalActivitiesCount > 0) {
                File outDirectory = new File(path + File.separator + "out-NonEmpty");
                if (!outDirectory.exists())
                    outDirectory.mkdir();
                StreamResult result = new StreamResult(new File(outDirectory.getAbsolutePath() + File.separator + xmlFile));
                transformer.transform(source, result);
            } else {
                File outDirectory = new File(path + File.separator + "out-Empty");
                if (!outDirectory.exists())
                    outDirectory.mkdir();
                StreamResult result = new StreamResult(new File(outDirectory.getAbsolutePath() + File.separator + xmlFile));
                transformer.transform(source, result);
            }
            int totalUIProperties = 0;
            for (ViewResult ui : uiResults) {
                totalUIProperties += ui.getProperties().size();
            }
            List<FieldOnlyAccessPath> pointsTo = new ArrayList<>();
            for (Set<FieldOnlyAccessPath> accessPathSet : mayPointsToForEventListener.values()) {
                pointsTo.addAll(accessPathSet);
            }

            int criticalReferenceCount = 0;
            for (FieldOnlyAccessPath d : criticalData) {
                Field field = d.getFields().get(d.getFields().size() - 1);
                if (!Utils.isPrimitiveType(field.getSootField().getType())) {
                    criticalReferenceCount++;
                }
            }

            String appResult = this.name + "," + this.packageName + "," + this.version + "," + xmlFile + "," + activities.size() + "," + criticalActivitiesCount + "," + uiResults.size() + "," + totalUIProperties + "," + criticalData.size() + "," + criticalReferenceCount + "," + mayUseForEventListener.size() + "," + mayModifyForEventListener.size() + "," /*+ mayAllocForEventListener.size() + ","*/ + mayPointsToForEventListener.size() + "," + pointsTo.size() + "," + connectionsCount + "," + runtime;
            System.out.println(appResult);
            File resultFile = new File(path + File.separator + "LiveDroid.csv");
            FileUtils.writeStringToFile(resultFile, appResult + "\n", Charset.defaultCharset(), true);

            // Output to console for testing
            //StreamResult consoleResult = new StreamResult(System.out);
            //transformer.transform(source, consoleResult);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
