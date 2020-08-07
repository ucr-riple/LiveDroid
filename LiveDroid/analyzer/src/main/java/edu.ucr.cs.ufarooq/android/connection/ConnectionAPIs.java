package edu.ucr.cs.ufarooq.android.connection;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import java.util.HashSet;
import java.util.Set;

public class ConnectionAPIs {
    public static final String bind1 = "boolean bindService(android.content.Intent,android.content.ServiceConnection,int)";
    public static final String bind2 = "boolean bindService(android.content.Intent,int,java.util.concurrent.Executor,android.content.ServiceConnection)";
    public static final String bind3 = "boolean bindIsolatedService(android.content.Intent,int,java.lang.String,java.util.concurrent.Executor,android.content.ServiceConnection)";

    public static final String unbind1 = "void unbindService(android.content.ServiceConnection)";


    public static final String register1 = "android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)";
    public static final String register2 = "android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,int)";
    public static final String register3 = "android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler,int)";
    public static final String register4 = "android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler)";


    public static final String unregister1 = "void unregisterReceiver(android.content.BroadcastReceiver)";

    public final Set<SootMethod> bindingMethods;
    public final Set<SootMethod> unBindingMethods;

    public final Set<SootMethod> registerMethods;
    public final Set<SootMethod> unregisterMethods;

    private static ConnectionAPIs instance = null;

    private ConnectionAPIs() {
        SootClass contextClass = Scene.v().getSootClassUnsafe("android.content.Context");
        SootClass contextWrapperClass = Scene.v().getSootClassUnsafe("android.content.ContextWrapper");
        this.bindingMethods = new HashSet<>();
        this.unBindingMethods = new HashSet<>();

        bindingMethods.add(contextWrapperClass.getMethod(bind1));
        //bindingMethods.add(contextWrapperClass.getMethod(bind2));
        //bindingMethods.add(contextWrapperClass.getMethod(bind3));

        unBindingMethods.add(contextWrapperClass.getMethod(unbind1));

        registerMethods = new HashSet<>();
        unregisterMethods = new HashSet<>();

        registerMethods.add(contextClass.getMethod(register1));
        registerMethods.add(contextClass.getMethod(register2));
        registerMethods.add(contextClass.getMethod(register3));
        registerMethods.add(contextClass.getMethod(register4));

        unregisterMethods.add(contextClass.getMethod(unregister1));
    }

    public static ConnectionAPIs getInstance(){
        if(instance == null){
            instance = new ConnectionAPIs();
        }
        return instance;
    }
}
