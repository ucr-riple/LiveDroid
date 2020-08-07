package edu.ucr.cs.ufarooq.android.XMLPrefs;

import soot.jimple.infoflow.android.axml.AXmlNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreferencesProvider {
    private HashMap<Integer, Set<AXmlNode>> nodes;
    private HashMap<Integer, String> idXMLNameMap;
    private Set<String> killablePrefSet;
    private static PreferencesProvider _instance = null;

    private static final String DialogPreference = "DialogPreference";
    private static final String EditTextPreference = "EditTextPreference";
    private static final String ListPreference = "ListPreference";
    private static final String MultiSelectListPreference = "MultiSelectListPreference";
    private static final String RingtonePreference = "RingtonePreference";
    private static final String Preference = "Preference";

    public static PreferencesProvider getInstance() {
        if (_instance == null)
            _instance = new PreferencesProvider();
        return _instance;
    }

    private PreferencesProvider() {
        idXMLNameMap = new HashMap<>();
        nodes = new HashMap<>();
        killablePrefSet = new HashSet<>();
        killablePrefSet.add(DialogPreference);
        killablePrefSet.add(EditTextPreference);
        killablePrefSet.add(ListPreference);
        killablePrefSet.add(MultiSelectListPreference);
        killablePrefSet.add(RingtonePreference);
        killablePrefSet.add(Preference);

    }

    public HashMap<Integer, String> getIdXMLNameMap() {
        return idXMLNameMap;
    }

    public void insertXMLNames(Integer id, String name) {
        idXMLNameMap.putIfAbsent(id, name);

    }

    public void removeXMLName(Integer id) {
        if (idXMLNameMap.containsKey(id))
            idXMLNameMap.remove(id);

    }

    public String getNameForId(Integer id) {
        if (idXMLNameMap.containsKey(id)) {
            return idXMLNameMap.get(id);
        }
        return null;
    }

    public void insertNodes(Integer id, List<AXmlNode> preferenceScreenNodes) {
        Set<AXmlNode> allNodes = new HashSet<>();
        for (AXmlNode node : preferenceScreenNodes) {
            processChildNode(node, allNodes);
        }
        nodes.putIfAbsent(id, allNodes);
    }

    private void processChildNode(AXmlNode childNode, Set<AXmlNode> allNodes) {
        if (childNode.getTag().equalsIgnoreCase("PreferenceCategory") || childNode.getTag().equalsIgnoreCase("PreferenceScreen")) {
            for (AXmlNode node : childNode.getChildren()) {
                if (node.getChildren().size() > 0) {
                    processChildNode(node, allNodes);
                } else {
                    allNodes.add(node);
                }
            }
        }
    }


    public Set<AXmlNode> getNodes(Integer id) {
        if (nodes.containsKey(id)) {
            return nodes.get(id);
        }
        return new HashSet<>();
    }

    public Set<AXmlNode> getKilledSet(Integer id) {
        Set<AXmlNode> killedSet = new HashSet<>();
        Set<AXmlNode> allNodes = getNodes(id);

        for (AXmlNode node : allNodes) {
            if (killablePrefSet.contains(node.getTag())) {
                killedSet.add(node);
            }
        }
        return killedSet;
    }
}
