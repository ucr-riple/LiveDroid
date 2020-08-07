package edu.ucr.cs.ufarooq.android.connection;

import edu.ucr.cs.ufarooq.accessPath.AccessPath;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;

import java.util.List;
import java.util.Objects;

public class BroadcastConnection extends AbsConnection {

    private final AccessPath broadcast;


    public BroadcastConnection(AccessPath broadcast, SootMethod openingCallback, Unit openingStmt, Connection opening) {
        super(openingCallback, openingStmt, opening);
        this.broadcast = broadcast;
        Type broadcastType = broadcast.getFields().get(broadcast.getFields().size() - 1).getSootField().getType();
        setConnectionType(broadcastType);
    }

    public BroadcastConnection(Type broadcastType, SootMethod openingCallback, Unit openingStmt, Connection opening) {
        super(openingCallback, openingStmt, opening);
        this.broadcast = null;
        setConnectionType(broadcastType);
    }

    public AccessPath getBroadcast() {
        return broadcast;
    }


    @Override
    public String getType() {
        return "BroadcastConnection";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BroadcastConnection that = (BroadcastConnection) o;
        return Objects.equals(broadcast, that.broadcast);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), broadcast);
    }
}
