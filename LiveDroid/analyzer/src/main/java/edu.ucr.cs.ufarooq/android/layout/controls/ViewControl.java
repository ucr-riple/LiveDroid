package edu.ucr.cs.ufarooq.android.layout.controls;

import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewControl {
    private final SootClass viewClass;
    private List<GetterMethod> getterMethodList;
    private Set<SetterMethod> setterMethods;
    private Set<SetterMethod> preferredSetterMethods;
    private List<SootMethod> allSetAPIs;

    public ViewControl(SootClass viewClass) {
        this.viewClass = viewClass;//Scene.v().getSootClassUnsafe(viewClass);
        this.getterMethodList = new ArrayList<>();
        this.setterMethods = new HashSet<>();
        this.preferredSetterMethods = new HashSet<>();
        this.allSetAPIs = new ArrayList<>();
    }

    public void addSetter(Set<SetterMethod> setterMethods) {
        this.setterMethods.addAll(setterMethods);
        for (SetterMethod setter : setterMethods) {
            this.allSetAPIs.add(setter.getSootMethod());
            if (setter.isPreferred())
                this.preferredSetterMethods.add(setter);
        }
    }

    public void addGetter(GetterMethod getterMethod) {
        this.getterMethodList.add(getterMethod);
    }

    public GetterMethod findGetterForAPI(SootMethod sootMethod) {
        if (allSetAPIs.contains(sootMethod)) {
            SetterMethod found = setterMethods.stream().filter(setter -> setter.getSootMethod().getSubSignature().equalsIgnoreCase(sootMethod.getSubSignature())).
                    findAny()                                      // If 'findAny' then return found
                    .orElse(null);
            if (found != null) {
                GetterMethod getterMethod = getterMethodList.stream().filter(getter -> getter.getProperty().equalsIgnoreCase(found.getProperty())).
                        findAny().orElse(null);
                return getterMethod;
            }
        }
        return null;
    }

    public ImmutablePair<SetterMethod, GetterMethod> findPairForAPI(SootMethod sootMethod, Unit callSite) {
        if (allSetAPIs.contains(sootMethod)) {
            SetterMethod found = setterMethods.stream().filter(setter -> setter.getSootMethod().getSubSignature().equalsIgnoreCase(sootMethod.getSubSignature())).
                    findAny()                                      // If 'findAny' then return found
                    .orElse(null);
            if (found != null) {
                found.setCallSite(callSite);
                GetterMethod getterMethod = getterMethodList.stream().filter(getter -> getter.getProperty().equalsIgnoreCase(found.getProperty())).
                        findAny().orElse(null);
                return new ImmutablePair<>(found, getterMethod);
            }
        }
        return null;
    }

    public SootClass getViewClass() {
        return viewClass;
    }

    public List<GetterMethod> getGetterMethodList() {
        return getterMethodList;
    }

    public Set<SetterMethod> getSetterMethods() {
        return setterMethods;
    }

    public List<SootMethod> getAllSetAPIs() {
        return allSetAPIs;
    }

    public Set<SetterMethod> getPreferredSetterMethods() {
        return preferredSetterMethods;
    }
}
