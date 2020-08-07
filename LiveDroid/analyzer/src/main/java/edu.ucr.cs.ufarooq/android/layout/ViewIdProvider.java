package edu.ucr.cs.ufarooq.android.layout;

import java.util.HashMap;

public class ViewIdProvider {
    private HashMap<Integer, String> idNameMap;
    private static ViewIdProvider _instance = null;


    public static ViewIdProvider getInstance() {
        if (_instance == null)
            _instance = new ViewIdProvider();
        return _instance;
    }

    private ViewIdProvider() {
        idNameMap = new HashMap<>();

    }

    public HashMap<Integer, String> getIdNameMap() {
        return idNameMap;
    }

    public void insertName(Integer id, String name) {
        idNameMap.putIfAbsent(id, name);

    }

    public void removeName(Integer id) {
        if (idNameMap.containsKey(id))
            idNameMap.remove(id);

    }

    public String getNameForId(Integer id) {
        if (idNameMap.containsKey(id)) {
            return idNameMap.get(id);
        }
        return null;
    }

}
