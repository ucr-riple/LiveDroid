package edu.ucr.cs.ufarooq.android.layout;

import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ViewControlProvider {

    private HashMap<Integer, Set<AndroidLayoutControl>> viewControlsMap;
    private HashMap<Integer, AndroidLayoutControl> idControlMap;
    private static ViewControlProvider instance = null;

    public static ViewControlProvider getInstance() {
        if (instance == null)
            instance = new ViewControlProvider();
        return instance;
    }

    private ViewControlProvider() {
        viewControlsMap = new HashMap<>();
        idControlMap = new HashMap<>();
    }

    public void insert(Integer layout, Set<AndroidLayoutControl> controls) {
        viewControlsMap.putIfAbsent(layout, controls);
        controls.stream().filter(c -> c.getID() > 1).forEach(c -> idControlMap.putIfAbsent(c.getID(), c));
    }

    public Set<AndroidLayoutControl> getControls(Integer layout) {
        if (viewControlsMap.containsKey(layout)) {
            Set<AndroidLayoutControl> controls = viewControlsMap.get(layout);
            return controls;
        }
        return new HashSet<>();
    }

    public Set<AndroidLayoutControl> getControlsWithNoId(Integer layout) {
        if (viewControlsMap.containsKey(layout)) {
            Set<AndroidLayoutControl> controls = viewControlsMap.get(layout);
            return controls.stream().filter(c -> c.getID() == -1).collect(Collectors.toSet());
        }
        return new HashSet<>();
    }
}