package edu.ucr.cs.ufarooq.android.connection;

import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;

import java.util.List;
import java.util.Objects;

public class ServiceConnection extends AbsConnection {
    // bindings
    private final AccessPath serviceConnection;
    private SootClass serviceClass;


    public ServiceConnection(AccessPath serviceConnection, SootMethod openingCallback, Unit openingStmt, Connection opening) {
        super(openingCallback, openingStmt, opening);
        this.serviceConnection = serviceConnection;
        Type serviceConnectionType = serviceConnection.getFields().get(serviceConnection.getFields().size() - 1).getSootField().getType();
        setConnectionType(serviceConnectionType);
    }

    public ServiceConnection(Type serviceConnectionType, SootMethod openingCallback, Unit openingStmt, Connection opening) {
        super(openingCallback, openingStmt, opening);
        setConnectionType(serviceConnectionType);
        this.serviceConnection = null;
    }

    public AccessPath getServiceConnection() {
        return serviceConnection;
    }

    public SootClass getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(SootClass serviceClass) {
        this.serviceClass = serviceClass;
    }


    @Override
    public String getType() {
        return "ServiceConnection";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ServiceConnection that = (ServiceConnection) o;
        return Objects.equals(serviceConnection, that.serviceConnection) &&
                Objects.equals(serviceClass, that.serviceClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), serviceConnection, serviceClass);
    }
}
