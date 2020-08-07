package edu.ucr.cs.ufarooq.android.layout;

import edu.ucr.cs.ufarooq.android.layout.controls.GetterMethod;
import edu.ucr.cs.ufarooq.android.layout.controls.SetterMethod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;

import java.util.Set;

public class ViewResult {
    private final AndroidLayoutControl control;
    private final String idVariableName;
    private final Set<ImmutablePair<SetterMethod, GetterMethod>> properties; //save/restore properties

    public ViewResult(AndroidLayoutControl control, Set<ImmutablePair<SetterMethod, GetterMethod>> properties) {
        this.control = control;
        this.properties = properties;
        this.idVariableName = ViewIdProvider.getInstance().getNameForId(control.getID());
    }

    public ViewResult(Set<ImmutablePair<SetterMethod, GetterMethod>> properties, int viewId) {
        this.control = null;
        this.properties = properties;
        this.idVariableName = String.valueOf(viewId);//ViewIdProvider.getInstance().getNameForId(control.getID());
    }
    public AndroidLayoutControl getControl() {
        return control;
    }

    public Set<ImmutablePair<SetterMethod, GetterMethod>> getProperties() {
        return properties;
    }

    public String getIdVariableName() {
        return idVariableName;
    }

    public void printProperties() {
        properties.forEach(property -> {
            System.out.println(idVariableName + "(" + control.getID() + "):" + property.left.getProperty() + ":" + property.left.getSootMethod().getSubSignature() + ":" + property.right.getSootMethod().getSubSignature());
        });
    }
}
