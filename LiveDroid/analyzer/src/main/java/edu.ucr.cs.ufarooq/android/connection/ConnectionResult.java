package edu.ucr.cs.ufarooq.android.connection;

import java.util.HashSet;
import java.util.Set;

public class ConnectionResult {
    private Set<AbsConnection> connectionSet;
    private static ConnectionResult _instance;

    public static ConnectionResult getInstance() {
        if (_instance == null)
            _instance = new ConnectionResult();
        return _instance;
    }

    private ConnectionResult() {
        this.connectionSet = new HashSet<>();
    }

    public Set<AbsConnection> getConnectionSet() {
        return connectionSet;
    }

    public static void insertNewResult(ServiceConnection serviceConnection) {
        getInstance().getConnectionSet().add(serviceConnection);
    }
}
