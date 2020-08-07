package edu.ucr.cs.ufarooq.android.results;

import com.google.common.collect.Sets;
import edu.ucr.cs.ufarooq.android.connection.AbsConnection;
import edu.ucr.cs.ufarooq.android.layout.ViewResult;
import edu.ucr.cs.ufarooq.android.model.FieldOnlyAccessPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import soot.SootClass;

import java.util.Map;
import java.util.Set;

public class ActivityAnalysisResult {
    private final Set<ViewResult> uiResults;
    private final Set<FieldOnlyAccessPath> mayUseForEventListener;
    private final Set<FieldOnlyAccessPath> mayModifyForEventListener;
    private final Set<AbsConnection> connections;
    //private final Set<FieldOnlyAccessPath> mayAllocForEventListener;
    private final Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> mayPointsToForEventListener;
    private final SootClass activityName;

    public ActivityAnalysisResult(Set<ViewResult> uiResults, Set<FieldOnlyAccessPath> mayUseForEventListener, Set<FieldOnlyAccessPath> mayModifyForEventListener, /*Set<FieldOnlyAccessPath> mayAllocForEventListener,*/ Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> mayPointsToForEventListener, Set<AbsConnection> connections, SootClass activityName) {
        this.uiResults = uiResults;
        this.mayUseForEventListener = mayUseForEventListener;
        this.mayModifyForEventListener = mayModifyForEventListener;
        //this.mayAllocForEventListener = mayAllocForEventListener;
        this.mayPointsToForEventListener = mayPointsToForEventListener;
        this.connections = connections;
        this.activityName = activityName;
    }

    public Set<ViewResult> getUiResults() {
        return uiResults;
    }

    public Set<FieldOnlyAccessPath> getMayUseForEventListener() {
        return mayUseForEventListener;
    }

    public Set<FieldOnlyAccessPath> getMayModifyForEventListener() {
        return mayModifyForEventListener;
    }

//    public Set<FieldOnlyAccessPath> getMayAllocForEventListener() {
//        return mayAllocForEventListener;
//    }

    public Map<FieldOnlyAccessPath, Set<FieldOnlyAccessPath>> getMayPointsToForEventListener() {
        return mayPointsToForEventListener;
    }

    public Set<AbsConnection> getConnections() {
        return connections;
    }

    public SootClass getActivityName() {
        return activityName;
    }

    public Element getActivityXMLElement(Document doc) {

        Element activityElement = XMLFileHelper.getItemWithName(doc, "Activity", activityName.getName());

        Element mayUse = XMLFileHelper.getCompleteSetXMLValue(doc, "MayUse", this.mayUseForEventListener);
        activityElement.appendChild(mayUse);

        Element mayModify = XMLFileHelper.getCompleteSetXMLValue(doc, "MayModify", this.mayModifyForEventListener);
        activityElement.appendChild(mayModify);

//        Element mayAllocate = XMLFileHelper.getCompleteSetXMLValue(doc, "MayAllocate", this.mayAllocForEventListener);
//        activityElement.appendChild(mayAllocate);

        Sets.SetView<FieldOnlyAccessPath> intersectionResult = Sets.intersection(this.mayUseForEventListener, this.mayModifyForEventListener);
        System.out.println("CriticalData" + intersectionResult.immutableCopy());
        Element criticalData = XMLFileHelper.getCompleteSetXMLValue(doc, "CriticalData", intersectionResult.immutableCopy());
        activityElement.appendChild(criticalData);

        Element mayPointsTo = XMLFileHelper.getMayPointsToXML(doc, this.mayPointsToForEventListener);
        activityElement.appendChild(mayPointsTo);

        Element uiXML = XMLFileHelper.getViewsElement(doc, this.uiResults);
        activityElement.appendChild(uiXML);

        Element connectionXML = XMLFileHelper.getConnectionElement(doc, this.connections);
        activityElement.appendChild(connectionXML);

        return activityElement;
    }
}
