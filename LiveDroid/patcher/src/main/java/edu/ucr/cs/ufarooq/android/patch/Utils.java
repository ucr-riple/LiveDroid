package edu.ucr.cs.ufarooq.android.patch;

import com.google.common.collect.Sets;
import edu.ucr.cs.ufarooq.accessPath.Field;
import edu.ucr.cs.ufarooq.android.layout.ViewResult;
import edu.ucr.cs.ufarooq.android.layout.controls.GetterMethod;
import edu.ucr.cs.ufarooq.android.layout.controls.SetterMethod;
import edu.ucr.cs.ufarooq.android.model.FieldOnlyAccessPath;
import edu.ucr.cs.ufarooq.android.results.ActivityAnalysisResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.dexpler.DexType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.EqExpr;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NopStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.tagkit.SignatureTag;
import soot.util.Chain;

class Utils {
  public static final String TYPES_PACKAGE = "edu.cs.ucr.liveDroid.types.";
  public static final String typeParentClass = "com.google.gson.reflect.TypeToken";
  public static final String BUNDLE = "android.os.Bundle";
  public static final String INIT_METHOD = "void <init>()";
  public static final String INIT_NAME = "<init>";
  public static final String GSON_CLASS = "com.google.gson.Gson";
  public static final String GSON_BUILDER_CLASS = "com.google.gson.GsonBuilder";
  public static final String STRING_CLASS = "java.lang.String";
  public static final String PUT_STRING =
      "<android.os.BaseBundle: void putString(java.lang.String,java.lang.String)>";
  public static final String PUT_BOOLEAN =
      "<android.os.BaseBundle: void putBoolean(java.lang.String,boolean)>";
  public static final String PUT_BYTE =
      "<android.os.BaseBundle: void putByte(java.lang.String,byte)>";
  public static final String PUT_CHAR =
      "<android.os.BaseBundle: void putChar(java.lang.String,char)>";
  public static final String PUT_CHAR_SEQUENCE =
      "<android.os.BaseBundle: void putCharSequence(java.lang.String,java.lang.CharSequence)>";
  public static final String PUT_DOUBLE =
      "<android.os.BaseBundle: void putDouble(java.lang.String,double)>";
  public static final String PUT_FLOAT =
      "<android.os.BaseBundle: void putFloat(java.lang.String,float)>";
  public static final String PUT_INT = "<android.os.BaseBundle: void putInt(java.lang.String,int)>";
  public static final String PUT_SHORT =
      "<android.os.BaseBundle: void putShort(java.lang.String,short)>";
  public static final String PUT_LONG =
      "<android.os.BaseBundle: void putLong(java.lang.String,long)>";

  public static final String GET_STRING =
      "<android.os.BaseBundle: java.lang.String getString(java.lang.String)>";
  public static final String GET_BOOLEAN =
      "<android.os.BaseBundle: boolean getBoolean(java.lang.String,boolean)>";

  public static final String GET_BOOLEAN_NO_DEFAULT =
      "<android.os.BaseBundle: boolean getBoolean(java.lang.String)>";
  public static final String GET_BYTE = "<android.os.BaseBundle: byte getByte(java.lang.String)>";
  public static final String GET_CHAR = "<android.os.BaseBundle: char getChar(java.lang.String)>";
  public static final String GET_CHAR_SEQUENCE =
      "<android.os.BaseBundle: java.lang.CharSequence getCharSequence(java.lang.String)>";
  public static final String GET_DOUBLE =
      "<android.os.BaseBundle: double getDouble(java.lang.String)>";
  public static final String GET_FLOAT =
      "<android.os.BaseBundle: float getFloat(java.lang.String)>";
  public static final String GET_INT = "<android.os.BaseBundle: int getInt(java.lang.String)>";
  public static final String GET_SHORT =
      "<android.os.BaseBundle: short getShort(java.lang.String)>";
  public static final String GET_LONG = "<android.os.BaseBundle: long getLong(java.lang.String)>";

  public static final String VIEW_CLASS = "android.view.View";
  public static final String ACTIVITY_CLASS = "android.app.Activity";
  public static final String FIND_VIEW_BY_ID =
      "<android.app.Activity: android.view.View findViewById(int)>";

  static void insertOnSaveInstance(ActivityAnalysisResult activityAnalysisResult) {
    // SootClass bundleClass = Scene.v().getSootClassUnsafe(BUNDLE);
    SootClass activity = activityAnalysisResult.getActivityName();
    Set<FieldOnlyAccessPath> criticalData =
        Sets.intersection(
                activityAnalysisResult.getMayModifyForEventListener(),
                activityAnalysisResult.getMayUseForEventListener())
            .immutableCopy();

    SootMethod method =
        new SootMethod(
            "onSaveInstanceState",
            Arrays.asList(RefType.v(BUNDLE)),
            VoidType.v(),
            Modifier.PROTECTED);
    activity.addMethod(method);

    JimpleBody body = Jimple.v().newBody(method);
    method.setActiveBody(body);
    body.insertIdentityStmts();
    Chain units = body.getUnits();
    ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
    units.addLast(returnStmt);
    // super call
    Local thisLocal = body.getThisLocal();
    Local parameterLocal = body.getParameterLocal(0);

    SootMethod parentOnSaveMethod = getSuperOnSave(activity);
    SpecialInvokeExpr superInvoke =
        Jimple.v().newSpecialInvokeExpr(thisLocal, parentOnSaveMethod.makeRef(), parameterLocal);
    InvokeStmt superInvokeStmt = Jimple.v().newInvokeStmt(superInvoke);
    units.insertBefore(superInvokeStmt, returnStmt);

    // insert and initialize GSON
    SootClass gsonClass = Scene.v().getSootClassUnsafe(GSON_CLASS);
    Local gsonLocal = generateNewLocal(body, RefType.v(gsonClass));
    NewExpr gsonNew = Jimple.v().newNewExpr(gsonClass.getType());
    AssignStmt gsonNewStmt = Jimple.v().newAssignStmt(gsonLocal, gsonNew);
    SootMethod gsonInitMethod = gsonClass.getMethodUnsafe(INIT_METHOD);
    SpecialInvokeExpr gsonInitInvoke =
        Jimple.v().newSpecialInvokeExpr(gsonLocal, gsonInitMethod.makeRef());
    InvokeStmt gsonInitInvokeStmt = Jimple.v().newInvokeStmt(gsonInitInvoke);

    units.insertBefore(gsonNewStmt, returnStmt);
    units.insertBefore(gsonInitInvokeStmt, returnStmt);
    SootMethod toJsonMethod =
        gsonClass.getMethodUnsafe("java.lang.String toJson(java.lang.Object)");
    SootMethod putStringMethod = Scene.v().getMethod(PUT_STRING);

    for (ViewResult viewResult : activityAnalysisResult.getUiResults()) {
      if (viewResult.getControl() != null && viewResult.getControl().getID() > 0) {
        IntConstant viewId =
            IntConstant.v(viewResult.getControl().getID()); // IntConstant.v(0x7f0700b3);
        SootMethod findViewById = Scene.v().getMethod(FIND_VIEW_BY_ID);
        VirtualInvokeExpr findViewByIdInvoke =
            Jimple.v().newVirtualInvokeExpr(thisLocal, findViewById.makeRef(), viewId);
        Local viewLocal = generateNewLocal(body, RefType.v(VIEW_CLASS));
        AssignStmt findViewAssign = Jimple.v().newAssignStmt(viewLocal, findViewByIdInvoke);
        units.insertBefore(findViewAssign, returnStmt);

        // SootClass textView = Scene.v().getSootClassUnsafe("android.widget.TextView");
        Local viewTypeLocal =
            generateNewLocal(body, RefType.v(viewResult.getControl().getViewClass()));
        // Local viewTypeLocal = generateNewLocal(body, RefType.v(textView));
        AssignStmt castAssignment =
            Jimple.v()
                .newAssignStmt(
                    viewTypeLocal,
                    Jimple.v()
                        .newCastExpr(viewLocal, RefType.v(viewResult.getControl().getViewClass())));
        // AssignStmt castAssignment = Jimple.v().newAssignStmt(viewTypeLocal,
        // Jimple.v().newCastExpr(viewLocal, RefType.v(textView)));

        units.insertBefore(castAssignment, returnStmt);

        // get property
        for (ImmutablePair<SetterMethod, GetterMethod> pair : viewResult.getProperties()) {
          SootMethod getterMethod = pair.getRight().getSootMethod();
          Local propertyLocal = generateNewLocal(body, getterMethod.getReturnType());
          AssignStmt propertyAssign =
              Jimple.v()
                  .newAssignStmt(
                      propertyLocal,
                      Jimple.v().newVirtualInvokeExpr(viewTypeLocal, getterMethod.makeRef()));
          units.insertBefore(propertyAssign, returnStmt);
          String propertyKey =
              viewResult.getControl().getID() + "#" + pair.getRight().getProperty();
          StringConstant bundleKey = StringConstant.v(propertyKey);
          if (Utils.canPutTypeIntoBundle(getterMethod.getReturnType())) {
            // no need to use Gson
            SootMethod putMethod = Utils.getAPItoPutInBundle(getterMethod.getReturnType());
            VirtualInvokeExpr putMethodExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(
                        parameterLocal, putMethod.makeRef(), bundleKey, propertyLocal);
            InvokeStmt putInvoke = Jimple.v().newInvokeStmt(putMethodExpr);
            units.insertBefore(putInvoke, returnStmt);
          } else {
            // toJson
            VirtualInvokeExpr viewPropertyToJsonExpr =
                Jimple.v().newVirtualInvokeExpr(gsonLocal, toJsonMethod.makeRef(), propertyLocal);
            Local viewPropertyJsonLocal = generateNewLocal(body, RefType.v(STRING_CLASS));
            AssignStmt viewPropertyToJsonStmt =
                Jimple.v().newAssignStmt(viewPropertyJsonLocal, viewPropertyToJsonExpr);
            units.insertBefore(viewPropertyToJsonStmt, returnStmt);

            VirtualInvokeExpr putPropertyStringExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(
                        parameterLocal,
                        putStringMethod.makeRef(),
                        bundleKey,
                        viewPropertyJsonLocal);
            InvokeStmt putPropertyStringInvoke = Jimple.v().newInvokeStmt(putPropertyStringExpr);
            units.insertBefore(putPropertyStringInvoke, returnStmt);
          }
        }
      }
    }
    for (FieldOnlyAccessPath field : criticalData) {
      SootField aField = field.getFields().get(field.getFields().size() - 1).getSootField();
      SootClass aClass = Scene.v().getSootClassUnsafe(aField.getType().toString());
      if (ignoreToSaveRestore(aClass)) continue;
      Local aLocal = generateNewLocal(body, RefType.v(aClass));

      FieldRef aFieldRef;
      if (aField.isStatic()) {
        aFieldRef = Jimple.v().newStaticFieldRef(aField.makeRef());
      } else {
        aFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, aField.makeRef());
      }

      AssignStmt aAssignment = Jimple.v().newAssignStmt(aLocal, aFieldRef);
      units.insertBefore(aAssignment, returnStmt);
      Local objectLocal = aLocal;
      if (Utils.canPutTypeIntoBundle(aField.getType())) {
        // no need to use Gson
        SootMethod putMethod = Utils.getAPItoPutInBundle(aField.getType());
        StringConstant stringKey = StringConstant.v(Utils.getBundleKey(field));
        VirtualInvokeExpr putMethodExpr =
            Jimple.v()
                .newVirtualInvokeExpr(parameterLocal, putMethod.makeRef(), stringKey, objectLocal);
        InvokeStmt putStringInvoke = Jimple.v().newInvokeStmt(putMethodExpr);
        units.insertBefore(putStringInvoke, returnStmt);
      } else {
        // toJson
        VirtualInvokeExpr aToJsonExpr =
            Jimple.v().newVirtualInvokeExpr(gsonLocal, toJsonMethod.makeRef(), objectLocal);
        Local aJsonLocal = generateNewLocal(body, RefType.v(STRING_CLASS));
        AssignStmt aToJsonStmt = Jimple.v().newAssignStmt(aJsonLocal, aToJsonExpr);
        units.insertBefore(aToJsonStmt, returnStmt);

        StringConstant stringKey = StringConstant.v(Utils.getBundleKey(field));
        VirtualInvokeExpr putStringExpr =
            Jimple.v()
                .newVirtualInvokeExpr(
                    parameterLocal, putStringMethod.makeRef(), stringKey, aJsonLocal);
        InvokeStmt putStringInvoke = Jimple.v().newInvokeStmt(putStringExpr);
        units.insertBefore(putStringInvoke, returnStmt);

        // handle pointsTo
        if (activityAnalysisResult.getMayPointsToForEventListener().containsKey(field)) {
          Set<FieldOnlyAccessPath> mayPointsToSet =
              activityAnalysisResult.getMayPointsToForEventListener().get(field);
          for (FieldOnlyAccessPath mayPointsTo : mayPointsToSet) {
            SootField pointsToField =
                mayPointsTo.getFields().get(mayPointsTo.getFields().size() - 1).getSootField();
            SootClass pointsToClass =
                Scene.v().getSootClassUnsafe(pointsToField.getType().toString());
            Local pointsToLocal = generateNewLocal(body, pointsToField.getType());
            FieldRef bFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, pointsToField.makeRef());
            AssignStmt bAssignment = Jimple.v().newAssignStmt(pointsToLocal, bFieldRef);
            units.insertBefore(bAssignment, returnStmt);

            NeExpr abCompareExpr = Jimple.v().newNeExpr(objectLocal, pointsToLocal);
            NopStmt newNopStmt = Jimple.v().newNopStmt();

            Local bElsLocal = generateNewLocal(body, pointsToField.getType());
            FieldRef bElsFieldRef =
                Jimple.v().newInstanceFieldRef(thisLocal, pointsToField.makeRef());
            AssignStmt bElsAssignment = Jimple.v().newAssignStmt(bElsLocal, bElsFieldRef);

            IfStmt ifStmt = Jimple.v().newIfStmt(abCompareExpr, bElsAssignment);
            units.insertBefore(ifStmt, returnStmt);

            SootMethod putBooleanMethod = Scene.v().getMethod(PUT_BOOLEAN);
            String pointsKey = Utils.getBundleKey(field, mayPointsTo);
            StringConstant booleanKey = StringConstant.v(pointsKey);
            VirtualInvokeExpr putBooleanExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(
                        parameterLocal, putBooleanMethod.makeRef(), booleanKey, IntConstant.v(1));
            InvokeStmt putBooleanInvoke = Jimple.v().newInvokeStmt(putBooleanExpr);
            units.insertBefore(putBooleanInvoke, returnStmt);
            GotoStmt gotoNop = Jimple.v().newGotoStmt(newNopStmt);
            units.insertBefore(gotoNop, returnStmt);

            units.insertBefore(bElsAssignment, returnStmt);
            // toJson
            VirtualInvokeExpr bToJsonExpr =
                Jimple.v().newVirtualInvokeExpr(gsonLocal, toJsonMethod.makeRef(), bElsLocal);
            Local bJsonLocal = generateNewLocal(body, RefType.v(STRING_CLASS));
            AssignStmt bToJsonStmt = Jimple.v().newAssignStmt(bJsonLocal, bToJsonExpr);
            units.insertAfter(bToJsonStmt, bElsAssignment);

            StringConstant bStringKey = StringConstant.v(Utils.getBundleKey(mayPointsTo));
            VirtualInvokeExpr bPutStringExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(
                        parameterLocal, putStringMethod.makeRef(), bStringKey, bJsonLocal);
            InvokeStmt bPutStringInvoke = Jimple.v().newInvokeStmt(bPutStringExpr);
            units.insertAfter(bPutStringInvoke, bToJsonStmt);

            GotoStmt gotoElseNop = Jimple.v().newGotoStmt(newNopStmt);
            units.insertBefore(gotoElseNop, returnStmt);

            units.insertBefore(newNopStmt, returnStmt);
          }
        }
      }
    }
    body.validate();
    body.getUnits()
        .forEach(
            unit -> {
              System.out.println(unit);
            });
  }

  static void insertOnRestoreInstance(ActivityAnalysisResult activityAnalysisResult) {
    // SootClass bundleClass = Scene.v().getSootClassUnsafe(BUNDLE);
    SootClass activity = activityAnalysisResult.getActivityName();
    Set<FieldOnlyAccessPath> criticalData =
        Sets.intersection(
                activityAnalysisResult.getMayModifyForEventListener(),
                activityAnalysisResult.getMayUseForEventListener())
            .immutableCopy();

    SootMethod method =
        new SootMethod(
            "onRestoreInstanceState",
            Arrays.asList(RefType.v(BUNDLE)),
            VoidType.v(),
            Modifier.PROTECTED);
    activity.addMethod(method);

    JimpleBody body = Jimple.v().newBody(method);
    method.setActiveBody(body);
    body.insertIdentityStmts();
    Chain units = body.getUnits();
    ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
    units.addLast(returnStmt);
    // super call
    Local thisLocal = body.getThisLocal();
    Local parameterLocal = body.getParameterLocal(0);

    SootMethod parentOnRestoreMethod = getSuperOnRestore(activity);
    parentOnRestoreMethod.makeRef();
    SpecialInvokeExpr superInvoke =
        Jimple.v().newSpecialInvokeExpr(thisLocal, parentOnRestoreMethod.makeRef(), parameterLocal);
    InvokeStmt superInvokeStmt = Jimple.v().newInvokeStmt(superInvoke);
    units.insertBefore(superInvokeStmt, returnStmt);

    // insert and initialize GSON
    SootClass gsonBuilderClass = Scene.v().getSootClassUnsafe(GSON_BUILDER_CLASS);
    Local gsonBuilderLocal = generateNewLocal(body, RefType.v(gsonBuilderClass));
    NewExpr gsonBuilderNew = Jimple.v().newNewExpr(RefType.v(gsonBuilderClass));
    AssignStmt gsonBuilderNewStmt = Jimple.v().newAssignStmt(gsonBuilderLocal, gsonBuilderNew);
    SootMethod gsonBuilderInitMethod = gsonBuilderClass.getMethodUnsafe(INIT_METHOD);
    SpecialInvokeExpr gsonBuilderInitInvoke =
        Jimple.v().newSpecialInvokeExpr(gsonBuilderLocal, gsonBuilderInitMethod.makeRef());
    InvokeStmt gsonBuilderInitInvokeStmt = Jimple.v().newInvokeStmt(gsonBuilderInitInvoke);
    units.insertBefore(gsonBuilderNewStmt, returnStmt);
    units.insertBefore(gsonBuilderInitInvokeStmt, returnStmt);

    // register adapters e.g. CharSequenceAdapter
    SootMethod registerTypeMethod = gsonBuilderClass.getMethodByNameUnsafe("registerTypeAdapter");
    SootClass charSequence = Scene.v().getSootClassUnsafe("java.lang.CharSequence");
    ClassConstant charSquenceClass = ClassConstant.fromType(RefType.v(charSequence));
    SootClass charSequenceDeserializer =
        Scene.v()
            .getSootClassUnsafe("edu.ucr.cs.ufarooq.gsonasdex.adapters.CharSequenceDeserializer");
    Local charSequenceDeserializerLocal =
        generateNewLocal(body, RefType.v(charSequenceDeserializer));
    NewExpr charSequenceDeserializerNew =
        Jimple.v().newNewExpr(RefType.v(charSequenceDeserializer));
    AssignStmt charSequenceDeserializerNewStmt =
        Jimple.v().newAssignStmt(charSequenceDeserializerLocal, charSequenceDeserializerNew);
    units.insertBefore(charSequenceDeserializerNewStmt, returnStmt);
    SootMethod charSequenceDeserializerInitMethod =
        charSequenceDeserializer.getMethodUnsafe(INIT_METHOD);
    SpecialInvokeExpr charSequenceDeserializerInitInvoke =
        Jimple.v()
            .newSpecialInvokeExpr(
                charSequenceDeserializerLocal, charSequenceDeserializerInitMethod.makeRef());
    InvokeStmt charSequenceDeserializerInitInvokeStmt =
        Jimple.v().newInvokeStmt(charSequenceDeserializerInitInvoke);
    units.insertBefore(charSequenceDeserializerInitInvokeStmt, returnStmt);

    InvokeStmt registerAdapterStmt =
        Jimple.v()
            .newInvokeStmt(
                Jimple.v()
                    .newVirtualInvokeExpr(
                        gsonBuilderLocal,
                        registerTypeMethod.makeRef(),
                        charSquenceClass,
                        charSequenceDeserializerLocal));
    units.insertBefore(registerAdapterStmt, returnStmt);

    SootClass gsonClass = Scene.v().getSootClassUnsafe(GSON_CLASS);
    Local gsonLocal = generateNewLocal(body, RefType.v(gsonClass));
    SootMethod createMethod = gsonBuilderClass.getMethodByNameUnsafe("create");
    AssignStmt gsonCreateAssign =
        Jimple.v()
            .newAssignStmt(
                gsonLocal,
                Jimple.v().newVirtualInvokeExpr(gsonBuilderLocal, createMethod.makeRef()));

    units.insertBefore(gsonCreateAssign, returnStmt);
    SootMethod getStringMethod = Scene.v().getMethod(GET_STRING);
    SootMethod fromJsonClassMethod =
        gsonClass.getMethodUnsafe("java.lang.Object fromJson(java.lang.String,java.lang.Class)");

    for (ViewResult viewResult : activityAnalysisResult.getUiResults()) {
      if (viewResult.getControl()!=null && viewResult.getControl().getID() > 0) {
        IntConstant viewId =
            IntConstant.v(viewResult.getControl().getID()); // IntConstant.v(0x7f0700b3);
        SootMethod findViewById = Scene.v().getMethod(FIND_VIEW_BY_ID);
        VirtualInvokeExpr findViewByIdInvoke =
            Jimple.v().newVirtualInvokeExpr(thisLocal, findViewById.makeRef(), viewId);
        Local viewLocal = generateNewLocal(body, RefType.v(VIEW_CLASS));
        AssignStmt findViewAssign = Jimple.v().newAssignStmt(viewLocal, findViewByIdInvoke);
        units.insertBefore(findViewAssign, returnStmt);

        // SootClass textView = Scene.v().getSootClassUnsafe("android.widget.TextView");
        Local viewTypeLocal =
            generateNewLocal(body, RefType.v(viewResult.getControl().getViewClass()));
        // Local viewTypeLocal = generateNewLocal(body, RefType.v(textView));
        AssignStmt castAssignment =
            Jimple.v()
                .newAssignStmt(
                    viewTypeLocal,
                    Jimple.v()
                        .newCastExpr(viewLocal, RefType.v(viewResult.getControl().getViewClass())));
        // AssignStmt castAssignment = Jimple.v().newAssignStmt(viewTypeLocal,
        // Jimple.v().newCastExpr(viewLocal, RefType.v(textView)));

        units.insertBefore(castAssignment, returnStmt);

        // from Json
        for (ImmutablePair<SetterMethod, GetterMethod> pair : viewResult.getProperties()) {
          String propertyKey =
              viewResult.getControl().getID() + "#" + pair.getRight().getProperty();
          StringConstant bundleKey = StringConstant.v(propertyKey);
          SootMethod getterMethod = pair.getRight().getSootMethod();

          if (Utils.canPutTypeIntoBundle(getterMethod.getReturnType())) {
            Local aLocal = generateNewLocal(body, getterMethod.getReturnType());
            SootMethod getMethod = Utils.getAPItoGetFromBundle(getterMethod.getReturnType());

            VirtualInvokeExpr getMethodExpr =
                Jimple.v().newVirtualInvokeExpr(parameterLocal, getMethod.makeRef(), bundleKey);
            AssignStmt getBundleAssign = Jimple.v().newAssignStmt(aLocal, getMethodExpr);
            units.insertBefore(getBundleAssign, returnStmt);

            SootMethod setterMethod = pair.getRight().getSootMethod();
            InvokeStmt setInvoke =
                Jimple.v()
                    .newInvokeStmt(
                        Jimple.v()
                            .newVirtualInvokeExpr(viewTypeLocal, setterMethod.makeRef(), aLocal));
            units.insertBefore(setInvoke, returnStmt);
          } else {
            VirtualInvokeExpr getStringExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(parameterLocal, getStringMethod.makeRef(), bundleKey);
            Local stringLocal = generateNewLocal(body, RefType.v(STRING_CLASS));
            AssignStmt getJsonAssign = Jimple.v().newAssignStmt(stringLocal, getStringExpr);
            units.insertBefore(getJsonAssign, returnStmt);

            ClassConstant bundleStoredClass = ClassConstant.fromType(getterMethod.getReturnType());
            VirtualInvokeExpr aFromJsonExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(
                        gsonLocal, fromJsonClassMethod.makeRef(), stringLocal, bundleStoredClass);
            Local objectLocal = generateNewLocal(body, RefType.v("java.lang.Object"));
            AssignStmt aFromJsonStmt = Jimple.v().newAssignStmt(objectLocal, aFromJsonExpr);
            units.insertBefore(aFromJsonStmt, returnStmt);

            Local castLocal = generateNewLocal(body, getterMethod.getReturnType());
            AssignStmt castAssign =
                Jimple.v()
                    .newAssignStmt(
                        castLocal,
                        Jimple.v().newCastExpr(objectLocal, getterMethod.getReturnType()));
            units.insertBefore(castAssign, returnStmt);
            // set property

            SootMethod setterMethod = pair.getRight().getSootMethod();
            InvokeStmt setInvoke =
                Jimple.v()
                    .newInvokeStmt(
                        Jimple.v()
                            .newVirtualInvokeExpr(
                                viewTypeLocal, setterMethod.makeRef(), castLocal));
            units.insertBefore(setInvoke, returnStmt);
          }
        }
      }
    }

    for (FieldOnlyAccessPath field : criticalData) {
      SootField aField = field.getFields().get(field.getFields().size() - 1).getSootField();
      SootClass aClass = Scene.v().getSootClassUnsafe(aField.getType().toString());
      if (ignoreToSaveRestore(aClass)) continue;
      // fetch from Bundle

      StringConstant stringKey = StringConstant.v(Utils.getBundleKey(field));
      if (Utils.canPutTypeIntoBundle(aField.getType())) {
        Local aLocal = generateNewLocal(body, aField.getType());
        SootMethod getMethod = Utils.getAPItoGetFromBundle(aField.getType());

        VirtualInvokeExpr getMethodExpr =
            Jimple.v().newVirtualInvokeExpr(parameterLocal, getMethod.makeRef(), stringKey);
        AssignStmt getBundleAssign = Jimple.v().newAssignStmt(aLocal, getMethodExpr);
        units.insertBefore(getBundleAssign, returnStmt);
        FieldRef aFieldRef;
        if (aField.isStatic()) {
          aFieldRef = Jimple.v().newStaticFieldRef(aField.makeRef());
        } else {
          aFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, aField.makeRef());
        }

        AssignStmt aAssignment = Jimple.v().newAssignStmt(aFieldRef, aLocal);
        units.insertBefore(aAssignment, returnStmt);

      } else {
        VirtualInvokeExpr getStringExpr =
            Jimple.v().newVirtualInvokeExpr(parameterLocal, getStringMethod.makeRef(), stringKey);
        Local stringLocal = generateNewLocal(body, RefType.v(STRING_CLASS));
        AssignStmt getJsonAssign = Jimple.v().newAssignStmt(stringLocal, getStringExpr);
        units.insertBefore(getJsonAssign, returnStmt);

        ClassConstant classConstant = ClassConstant.fromType(aClass.getType());
        Local objectLocal = generateNewLocal(body, RefType.v("java.lang.Object"));
        List<Value> argumentsList = new ArrayList<>();
        // argumentsList.add(stringLocal);

        if (aField.getTag(SignatureTag.NAME) != null) {
          SignatureTag fieldSignature = ((SignatureTag) aField.getTag(SignatureTag.NAME));
          String[] splits = fieldSignature.getSignature().split("<"); // split with type parameters
          String baseTypeString = splits[0];
          ClassConstant baseType =
              ClassConstant.fromType(new DexType(baseTypeString + ";").toSoot());
          argumentsList.add(baseType);
          for (int i = 1; i < splits.length; i++) {
            String part = splits[i];
            System.out.println("Type: " + splits[i]);
            String[] subParts = part.split(";");
            int arrayLength = subParts.length - 1; // avoiding ;>;
            // todo: handle multiple params
            NewArrayExpr arrayExpr =
                Jimple.v()
                    .newNewArrayExpr(
                        RefType.v("java.lang.reflect.Type"), IntConstant.v(arrayLength));
            Local typeArrayLocal = generateNewLocal(body, arrayExpr.getType());
            units.insertBefore(Jimple.v().newAssignStmt(typeArrayLocal, arrayExpr), returnStmt);
            for (int j = 0; j < arrayLength; j++) {
              String subPart = subParts[j];
              DexType dexType = new DexType(subPart + ";");
              System.out.println("Type: " + dexType.toSoot().toQuotedString());
              ArrayRef leftSide = Jimple.v().newArrayRef(typeArrayLocal, IntConstant.v(j));
              ClassConstant stringClass = ClassConstant.fromType(dexType.toSoot());
              units.insertBefore(Jimple.v().newAssignStmt(leftSide, stringClass), returnStmt);
            }
            // add array to list
            argumentsList.add(typeArrayLocal);
          }
          SootClass typeTokenClass =
              Scene.v().getSootClassUnsafe("com.google.gson.reflect.TypeToken");
          SootMethod getParameterizedMethod =
              typeTokenClass.getMethodByNameUnsafe("getParameterized");
          StaticInvokeExpr getParameterizedInvoke =
              Jimple.v().newStaticInvokeExpr(getParameterizedMethod.makeRef(), argumentsList);
          Local typeTokenLocal = generateNewLocal(body, RefType.v(typeTokenClass));
          AssignStmt getParameterizedAssign =
              Jimple.v().newAssignStmt(typeTokenLocal, getParameterizedInvoke);
          units.insertBefore(getParameterizedAssign, returnStmt);

          SootMethod getTypeMethod =
              typeTokenClass.getMethodUnsafe("java.lang.reflect.Type getType()");
          Local reflectTypeLocal = generateNewLocal(body, RefType.v("java.lang.reflect.Type"));
          AssignStmt getTypeAssign =
              Jimple.v()
                  .newAssignStmt(
                      reflectTypeLocal,
                      Jimple.v().newVirtualInvokeExpr(typeTokenLocal, getTypeMethod.makeRef()));
          units.insertBefore(getTypeAssign, returnStmt);

          SootMethod fromJsonMethod =
              gsonClass.getMethodUnsafe(
                  "java.lang.Object fromJson(java.lang.String,java.lang.reflect.Type)");
          VirtualInvokeExpr aFromJsonExpr =
              Jimple.v()
                  .newVirtualInvokeExpr(
                      gsonLocal, fromJsonMethod.makeRef(), stringLocal, reflectTypeLocal);
          AssignStmt aFromJsonStmt = Jimple.v().newAssignStmt(objectLocal, aFromJsonExpr);
          units.insertBefore(aFromJsonStmt, returnStmt);
        } else {
          SootMethod fromJsonMethod =
              gsonClass.getMethodUnsafe(
                  "java.lang.Object fromJson(java.lang.String,java.lang.Class)");
          VirtualInvokeExpr aFromJsonExpr =
              Jimple.v()
                  .newVirtualInvokeExpr(
                      gsonLocal, fromJsonMethod.makeRef(), stringLocal, classConstant);
          AssignStmt aFromJsonStmt = Jimple.v().newAssignStmt(objectLocal, aFromJsonExpr);
          units.insertBefore(aFromJsonStmt, returnStmt);
        }
        // cast and assign
        Local aCast = generateNewLocal(body, RefType.v(aClass));
        AssignStmt castAssign =
            Jimple.v().newAssignStmt(aCast, Jimple.v().newCastExpr(objectLocal, RefType.v(aClass)));
        units.insertBefore(castAssign, returnStmt);
        FieldRef aFieldRef;
        if (aField.isStatic()) {
          aFieldRef = Jimple.v().newStaticFieldRef(aField.makeRef());
        } else {
          aFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, aField.makeRef());
        }

        AssignStmt aAssignment = Jimple.v().newAssignStmt(aFieldRef, aCast);
        units.insertBefore(aAssignment, returnStmt);

        if (activityAnalysisResult.getMayPointsToForEventListener().containsKey(field)) {
          Set<FieldOnlyAccessPath> mayPointsToSet =
              activityAnalysisResult.getMayPointsToForEventListener().get(field);
          for (FieldOnlyAccessPath mayPointsTo : mayPointsToSet) {
            SootField pointsToField =
                mayPointsTo.getFields().get(mayPointsTo.getFields().size() - 1).getSootField();
            SootClass pointsToClass =
                Scene.v().getSootClassUnsafe(pointsToField.getType().toString());

            SootMethod getBooleanMethod = Scene.v().getMethod(GET_BOOLEAN);

            Local getPointsLocal = generateNewLocal(body, IntType.v());
            StringConstant pointsToKey = StringConstant.v(Utils.getBundleKey(field, mayPointsTo));
            VirtualInvokeExpr getBooleanExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(
                        parameterLocal, getBooleanMethod.makeRef(), pointsToKey, IntConstant.v(0));
            AssignStmt getBooleanAssign = Jimple.v().newAssignStmt(getPointsLocal, getBooleanExpr);
            units.insertBefore(getBooleanAssign, returnStmt);

            EqExpr abCompareExpr =
                Jimple.v().newEqExpr(getPointsLocal, IntConstant.v(0)); // if pointsTo is true
            NopStmt newNopStmt = Jimple.v().newNopStmt();

            StringConstant bStringKey = StringConstant.v(Utils.getBundleKey(mayPointsTo));
            VirtualInvokeExpr getBStringExpr =
                Jimple.v()
                    .newVirtualInvokeExpr(parameterLocal, getStringMethod.makeRef(), bStringKey);
            Local bStringLocal = generateNewLocal(body, RefType.v(STRING_CLASS));
            AssignStmt getElsJsonAssign = Jimple.v().newAssignStmt(bStringLocal, getBStringExpr);
            // units.insertBefore(getElsJsonAssign, returnStmt);

            IfStmt ifStmt = Jimple.v().newIfStmt(abCompareExpr, getElsJsonAssign);
            units.insertBefore(ifStmt, returnStmt);

            FieldRef bFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, pointsToField.makeRef());
            AssignStmt ifAssignment = Jimple.v().newAssignStmt(bFieldRef, aCast);
            units.insertBefore(ifAssignment, returnStmt);

            GotoStmt gotoNop = Jimple.v().newGotoStmt(newNopStmt);
            units.insertBefore(gotoNop, returnStmt);
            units.insertBefore(getElsJsonAssign, returnStmt);

            ClassConstant bClassConstant = ClassConstant.fromType(pointsToField.getType());
            Local bObjectLocal = generateNewLocal(body, RefType.v("java.lang.Object"));
            List<Value> bArgumentsList = new ArrayList<>();
            AssignStmt bFromJsonStmt;
            // argumentsList.add(stringLocal);
            if (pointsToField.getTag(SignatureTag.NAME) != null) {
              SignatureTag fieldSignature =
                  ((SignatureTag) pointsToField.getTag(SignatureTag.NAME));
              String[] splits =
                  fieldSignature.getSignature().split("<"); // split with type parameters
              String baseTypeString = splits[0];
              ClassConstant baseType =
                  ClassConstant.fromType(new DexType(baseTypeString + ";").toSoot());
              bArgumentsList.add(baseType);
              for (int i = 1; i < splits.length; i++) {
                String part = splits[i];
                System.out.println("Type: " + splits[i]);
                String[] subParts = part.split(";");
                int arrayLength = subParts.length - 1; // avoiding ;>;
                // todo: handle multiple params
                NewArrayExpr arrayExpr =
                    Jimple.v()
                        .newNewArrayExpr(
                            RefType.v("java.lang.reflect.Type"), IntConstant.v(arrayLength));
                Local typeArrayLocal = generateNewLocal(body, arrayExpr.getType());
                units.insertBefore(Jimple.v().newAssignStmt(typeArrayLocal, arrayExpr), returnStmt);
                for (int j = 0; j < arrayLength; j++) {
                  String subPart = subParts[j];
                  DexType dexType = new DexType(subPart + ";");
                  System.out.println("Type: " + dexType.toSoot().toQuotedString());
                  ArrayRef leftSide = Jimple.v().newArrayRef(typeArrayLocal, IntConstant.v(j));
                  ClassConstant stringClass = ClassConstant.fromType(dexType.toSoot());
                  units.insertBefore(Jimple.v().newAssignStmt(leftSide, stringClass), returnStmt);
                }
                // add array to list
                bArgumentsList.add(typeArrayLocal);
              }
              SootClass typeTokenClass =
                  Scene.v().getSootClassUnsafe("com.google.gson.reflect.TypeToken");
              SootMethod getParameterizedMethod =
                  typeTokenClass.getMethodByNameUnsafe("getParameterized");
              StaticInvokeExpr getParameterizedInvoke =
                  Jimple.v().newStaticInvokeExpr(getParameterizedMethod.makeRef(), bArgumentsList);
              Local typeTokenLocal = generateNewLocal(body, RefType.v(typeTokenClass));
              AssignStmt getParameterizedAssign =
                  Jimple.v().newAssignStmt(typeTokenLocal, getParameterizedInvoke);
              units.insertBefore(getParameterizedAssign, returnStmt);

              SootMethod getTypeMethod =
                  typeTokenClass.getMethodUnsafe("java.lang.reflect.Type getType()");
              Local reflectTypeLocal = generateNewLocal(body, RefType.v("java.lang.reflect.Type"));
              AssignStmt getTypeAssign =
                  Jimple.v()
                      .newAssignStmt(
                          reflectTypeLocal,
                          Jimple.v().newVirtualInvokeExpr(typeTokenLocal, getTypeMethod.makeRef()));
              units.insertBefore(getTypeAssign, returnStmt);

              SootMethod fromJsonMethod =
                  gsonClass.getMethodUnsafe(
                      "java.lang.Object fromJson(java.lang.String,java.lang.reflect.Type)");
              VirtualInvokeExpr bFromJsonExpr =
                  Jimple.v()
                      .newVirtualInvokeExpr(
                          gsonLocal, fromJsonMethod.makeRef(), bStringLocal, reflectTypeLocal);
              bFromJsonStmt = Jimple.v().newAssignStmt(bObjectLocal, bFromJsonExpr);
              units.insertBefore(bFromJsonStmt, returnStmt);
            } else {
              SootMethod fromJsonMethod =
                  gsonClass.getMethodUnsafe(
                      "java.lang.Object fromJson(java.lang.String,java.lang.Class)");
              VirtualInvokeExpr bFromJsonExpr =
                  Jimple.v()
                      .newVirtualInvokeExpr(
                          gsonLocal, fromJsonMethod.makeRef(), stringLocal, classConstant);
              bFromJsonStmt = Jimple.v().newAssignStmt(bObjectLocal, bFromJsonExpr);
              units.insertBefore(bFromJsonStmt, returnStmt);
            }
            // cast and assign
            Local bCast = generateNewLocal(body, RefType.v(pointsToClass));
            AssignStmt bCastAssign =
                Jimple.v()
                    .newAssignStmt(
                        bCast, Jimple.v().newCastExpr(bObjectLocal, RefType.v(pointsToClass)));
            units.insertBefore(bCastAssign, returnStmt);

            FieldRef bElsFieldRef =
                Jimple.v().newInstanceFieldRef(thisLocal, pointsToField.makeRef());
            AssignStmt bElsAssignment = Jimple.v().newAssignStmt(bElsFieldRef, bCast);
            units.insertBefore(bElsAssignment, returnStmt);

            GotoStmt gotoElseNop = Jimple.v().newGotoStmt(newNopStmt);
            units.insertBefore(gotoElseNop, returnStmt);

            units.insertBefore(newNopStmt, returnStmt);
          }
        }
      }
    }

    body.getUnits()
        .forEach(
            unit -> {
              System.out.println(unit);
            });
  }

  private static boolean ignoreToSaveRestore(SootClass aClass) {
    if (aClass.getName().startsWith("android.os")) {
      return true;
    }
    if (aClass.getName().startsWith("android.app")) return true;
    if (aClass.getName().startsWith("android.content")) return true;
    if (aClass.getName().startsWith("android.database")) return true;
    if (aClass.getName().startsWith("android.widget")) return true;
    if (aClass.getName().startsWith("android.webkit")) return true;
    return false;
  }

  public static Local generateNewLocal(Body body, Type type) {
    LocalGenerator lg = new LocalGenerator(body);
    return lg.generateLocal(type);
  }

  public static void setSigntureTag(SootField sootField) {
    if (sootField.getName().equalsIgnoreCase("stringList")) {
      System.out.println(sootField.getName() + ":" + sootField.getTags().size());
      sootField
          .getTags()
          .forEach(
              tag -> {
                if (tag instanceof SignatureTag) {
                  System.out.println(tag.getName() + ":" + tag.toString());
                  if (sootField.getTag(SignatureTag.NAME) == null) {}
                }
              });
    }
  }

  public static String getBundleKey(FieldOnlyAccessPath fieldOnlyAccessPath) {
    StringBuilder stringBuilder = new StringBuilder();
    for (Field field : fieldOnlyAccessPath.getFields()) {
      stringBuilder.append(field.getSootField().getName() + "#");
    }
    return stringBuilder.toString();
  }

  public static String getViewKey(ViewResult viewResult, String property) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(viewResult.getControl().getID() + "#" + property);
    return stringBuilder.toString();
  }

  public static String getBundleKey(
      FieldOnlyAccessPath fieldOnlyAccessPath, FieldOnlyAccessPath mayPoints) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(getBundleKey(fieldOnlyAccessPath) + "==");
    stringBuilder.append(getBundleKey(mayPoints));
    return stringBuilder.toString();
  }

  public static Set<SootClass> performPreChecking(ActivityAnalysisResult activityAnalysisResult) {
    Set<SootClass> requiredAdapters = new HashSet<>();
    Set<FieldOnlyAccessPath> criticalData =
        Sets.intersection(
                activityAnalysisResult.getMayModifyForEventListener(),
                activityAnalysisResult.getMayUseForEventListener())
            .immutableCopy();
    for (FieldOnlyAccessPath fieldOnlyAccessPath : criticalData) {
      SootField field =
          fieldOnlyAccessPath
              .getFields()
              .get(fieldOnlyAccessPath.getFields().size() - 1)
              .getSootField();
      Optional<SootClass> canProcess = checkTypePreProcessing(field.getType());
      if (canProcess.isPresent()) {
        requiredAdapters.add(canProcess.get());
      }
      if (activityAnalysisResult
          .getMayPointsToForEventListener()
          .containsKey(fieldOnlyAccessPath)) {
        Set<FieldOnlyAccessPath> mayPointsTo =
            activityAnalysisResult.getMayPointsToForEventListener().get(fieldOnlyAccessPath);
        for (FieldOnlyAccessPath mayPointsToAccessPath : mayPointsTo) {
          SootField mayPointsToField =
              mayPointsToAccessPath
                  .getFields()
                  .get(mayPointsToAccessPath.getFields().size() - 1)
                  .getSootField();
          Optional<SootClass> canProcessMayPointsTo =
              checkTypePreProcessing(mayPointsToField.getType());
          if (canProcessMayPointsTo.isPresent()) {
            requiredAdapters.add(canProcessMayPointsTo.get());
          }
        }
      }
    }
    for (ViewResult viewResult : activityAnalysisResult.getUiResults()) {
      for (ImmutablePair<SetterMethod, GetterMethod> pair : viewResult.getProperties()) {
        Type returnType = pair.getRight().getSootMethod().getReturnType();
        Optional<SootClass> canProcess = checkTypePreProcessing(returnType);
        if (canProcess.isPresent()) {
          requiredAdapters.add(canProcess.get());
        }
      }
    }
    return requiredAdapters;
  }

  private static SootMethod getSuperOnSave(SootClass activity) {
    SootMethod parentOnSaveMethod = null;
    SootClass superClass = activity.getSuperclass();
    while (parentOnSaveMethod == null && !superClass.getName().equals("java.lang.Object")) {
      parentOnSaveMethod =
          superClass.getMethodUnsafe("void onSaveInstanceState(android.os.Bundle)");
      superClass = superClass.getSuperclass();
    }
    return parentOnSaveMethod;
  }

  private static SootMethod getSuperOnRestore(SootClass activity) {
    SootMethod parentOnRestoreMethod = null;
    SootClass superClass = activity.getSuperclass();
    while (parentOnRestoreMethod == null && !superClass.getName().equals("java.lang.Object")) {
      parentOnRestoreMethod =
          superClass.getMethodUnsafe("void onRestoreInstanceState(android.os.Bundle)");
      superClass = superClass.getSuperclass();
    }
    return parentOnRestoreMethod;
  }

  public static boolean isPrimitiveType(Type type) {
    if (type instanceof PrimType) return true;
    Type stringType = RefType.v("java.lang.String").getSootClass().getType();
    if (type.equals(stringType)
        || type instanceof ByteType
        || type instanceof CharType
        || type instanceof DoubleType
        || type instanceof FloatType
        || type instanceof IntType
        || type instanceof LongType
        || type instanceof ShortType
        || type instanceof BooleanType) {
      return true;
    }
    return false;
  }

  public static boolean canPutTypeIntoBundle(Type type) {
    if (type instanceof PrimType) return true;
    Type stringType = RefType.v("java.lang.String").getSootClass().getType();
    Type charSequenceType = RefType.v("java.lang.CharSequence").getSootClass().getType();
    if (type.equals(stringType)
        || type.equals(charSequenceType)
        || type instanceof ByteType
        || type instanceof CharType
        || type instanceof DoubleType
        || type instanceof FloatType
        || type instanceof IntType
        || type instanceof LongType
        || type instanceof ShortType
        || type instanceof BooleanType) {
      return true;
    }
    return false;
  }

  public static SootMethod getAPItoPutInBundle(Type type) {
    Type stringType = RefType.v("java.lang.String").getSootClass().getType();
    Type charSequenceType = RefType.v("java.lang.CharSequence").getSootClass().getType();
    if (type.equals(stringType)) {
      return Scene.v().getMethod(PUT_STRING);
    } else if (type.equals(charSequenceType)) {
      return Scene.v().getMethod(PUT_CHAR_SEQUENCE);
    } else if (type instanceof ByteType) {
      return Scene.v().getMethod(PUT_BYTE);
    } else if (type instanceof CharType) {
      return Scene.v().getMethod(PUT_CHAR);
    } else if (type instanceof DoubleType) {
      return Scene.v().getMethod(PUT_DOUBLE);
    } else if (type instanceof FloatType) {
      return Scene.v().getMethod(PUT_FLOAT);
    } else if (type instanceof IntType) {
      return Scene.v().getMethod(PUT_INT);
    } else if (type instanceof LongType) {
      return Scene.v().getMethod(PUT_LONG);
    } else if (type instanceof ShortType) {
      return Scene.v().getMethod(PUT_SHORT);
    } else if (type instanceof BooleanType) {
      return Scene.v().getMethod(PUT_BOOLEAN);
    }
    return null;
  }

  public static SootMethod getAPItoGetFromBundle(Type type) {
    Type stringType = RefType.v("java.lang.String").getSootClass().getType();
    Type charSequenceType = RefType.v("java.lang.CharSequence").getSootClass().getType();
    if (type.equals(stringType)) {
      return Scene.v().getMethod(GET_STRING);
    } else if (type.equals(charSequenceType)) {
      return Scene.v().getMethod(GET_CHAR_SEQUENCE);
    } else if (type instanceof ByteType) {
      return Scene.v().getMethod(GET_BYTE);
    } else if (type instanceof CharType) {
      return Scene.v().getMethod(GET_CHAR);
    } else if (type instanceof DoubleType) {
      return Scene.v().getMethod(GET_DOUBLE);
    } else if (type instanceof FloatType) {
      return Scene.v().getMethod(GET_FLOAT);
    } else if (type instanceof IntType) {
      return Scene.v().getMethod(GET_INT);
    } else if (type instanceof LongType) {
      return Scene.v().getMethod(GET_LONG);
    } else if (type instanceof ShortType) {
      return Scene.v().getMethod(GET_SHORT);
    } else if (type instanceof BooleanType) {
      return Scene.v().getMethod(GET_BOOLEAN_NO_DEFAULT);
    }
    return null;
  }

  public static Optional<SootClass> checkTypePreProcessing(Type type) {
    if (!isPrimitiveType(type)) {
      SootClass sootClass = Scene.v().getSootClassUnsafe(type.toQuotedString());
      boolean isInterface = sootClass.isInterface();
      boolean hasEmptyConstructor = false;
      if (!isInterface) {
        for (SootMethod sootMethod : sootClass.getMethods()) {
          // empty constructor
          if (sootMethod.isConstructor() && sootMethod.getParameterCount() == 0) {
            hasEmptyConstructor = true;
          }
        }
      }
      if (isInterface || !hasEmptyConstructor) {
        return Optional.of(sootClass);
      }
    }
    return Optional.empty();
  }
}
