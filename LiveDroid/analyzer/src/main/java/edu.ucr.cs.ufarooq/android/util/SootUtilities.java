/* Utilities to use with Soot

 Copyright (c) 2001-2010 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY
 */
package edu.ucr.cs.ufarooq.android.util;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.jimple.toolkits.invoke.SynchronizerManager;
import soot.jimple.toolkits.scalar.Evaluator;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

///////////////////////////////////////////////////////////////////
//// SootUtilities

/**
 This class consists of static utility methods for use with Soot

 @author Stephen Neuendorffer, Christopher Hylands
 @version $Id: SootUtilities.java 57040 2010-01-27 20:52:32Z cxh $
 @since Ptolemy II 2.0
 @Pt.ProposedRating Red (cxh)
 @Pt.AcceptedRating Red (cxh)
 */
public class SootUtilities {
    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Make the given field final.  Anywhere where the the given
     *  field is used in the given class, inline the reference with
     *  the given value.  Anywhere where the given field is illegally
     *  defined in the given class, inline the definition to throw an
     *  exception.  This happens unless the given class is the
     *  defining class for the given field and the definition occurs
     *  within an initializer (for instance fields) or a static
     *  initializer (for static fields).

     public static void assertFinalField(SootClass theClass,
     SootField theField) {
     // First make the field final.
     theField.setModifiers(theField.getModifiers() | Modifier.FINAL);

     // Find any assignment to the field in the class and convert
     // them to Exceptions, unless they are in constructors,
     // in which case ignore them.
     for (Iterator methods = theClass.getMethods().iterator();
     methods.hasNext();) {
     SootMethod method = (SootMethod)methods.next();
     if (method.getName().equals("<init>")) {
     continue;
     }

     JimpleBody body = (JimpleBody)method.retrieveActiveBody();
     Chain units = body.getUnits();

     for (Iterator stmts = units.snapshotIterator();
     stmts.hasNext();) {
     Stmt stmt = (Stmt)stmts.next();
     // Remove all the definitions.
     for (Iterator boxes = stmt.getDefBoxes().iterator();
     boxes.hasNext();) {
     ValueBox box = (ValueBox)boxes.next();
     Value value = box.getValue();
     if (value instanceof FieldRef) {
     FieldRef ref = (FieldRef)value;
     if (ref.getField() == theField) {
     System.out.println("removing stmt = " + stmt);
     units.remove(stmt);
     }
     }
     }
     // Inline all the uses.
     if (Evaluator.isValueConstantValued(newValue)) {
     for (Iterator boxes = stmt.getUseBoxes().iterator();
     boxes.hasNext();) {
     ValueBox box = (ValueBox)boxes.next();
     Value value = box.getValue();
     if (value instanceof FieldRef) {
     FieldRef ref = (FieldRef)value;
     if (ref.getField() == theField) {
     System.out.println("inlining stmt = " + stmt);

     box.setValue(Evaluator
     .getConstantValueOf(newValue));
     }
     }

     }
     }
     }
     }
     }
     */
    /** Make the given field final.  Anywhere where the the given
     *  field is used in the given class, inline the reference with
     *  the given value.  Anywhere where the given field is illegally
     *  defined in the given class, inline the definition to throw an
     *  exception.  This happens unless the given class is the
     *  defining class for the given field and the definition occurs
     *  within an initializer (for instance fields) or a static
     *  initializer (for static fields).  Note that this is rather
     *  limited, since it is only really useful for constant values.
     *  In would be nice to specify a more complex expression to
     *  inline, but I'm not sure how to do it.
     */
    public static void assertFinalField(SootClass theClass, SootField theField,
            Value newValue) {
        // First make the field final.
        theField.setModifiers(theField.getModifiers() | Modifier.FINAL);

        // Find any assignment to the field in the class and convert
        // them to Exceptions, unless they are in constructors,
        // in which case remove them.
        for (Iterator methods = theClass.getMethods().iterator(); methods
                .hasNext();) {
            SootMethod method = (SootMethod) methods.next();
            JimpleBody body = (JimpleBody) method.retrieveActiveBody();
            Chain units = body.getUnits();

            for (Iterator stmts = units.snapshotIterator(); stmts.hasNext();) {
                Stmt stmt = (Stmt) stmts.next();

                // Remove all the definitions.
                for (Iterator boxes = stmt.getDefBoxes().iterator(); boxes
                        .hasNext();) {
                    ValueBox box = (ValueBox) boxes.next();
                    Value value = box.getValue();

                    if (value instanceof FieldRef) {
                        FieldRef ref = (FieldRef) value;

                        if (ref.getField() == theField) {
                            System.out.println("removing stmt = " + stmt);
                            units.remove(stmt);
                        }
                    }
                }

                // Inline all the uses.
                if (Evaluator.isValueConstantValued(newValue)) {
                    for (Iterator boxes = stmt.getUseBoxes().iterator(); boxes
                            .hasNext();) {
                        ValueBox box = (ValueBox) boxes.next();
                        Value value = box.getValue();

                        if (value instanceof FieldRef) {
                            FieldRef ref = (FieldRef) value;

                            if (ref.getField() == theField) {
                                System.out.println("inlining stmt = " + stmt);

                                box.setValue(Evaluator
                                        .getConstantValueOf(newValue));
                            }
                        }
                    }
                }
            }
        }

        if (Modifier.isStatic(theField.getModifiers())) {
            SootMethod method;

            // create a class initializer if one does not already exist.
            if (theClass.declaresMethodByName("<clinit>")) {
                method = theClass.getMethodByName("<clinit>");
            } else {
                method = new SootMethod("<clinit>", new LinkedList(), NullType
                        .v(), Modifier.PUBLIC);
                theClass.addMethod(method);
            }

            JimpleBody body = (JimpleBody) method.retrieveActiveBody();
            Chain units = body.getUnits();
            Stmt insertPoint = (Stmt) units.getLast();
            Local local = Jimple.v().newLocal("_CGTemp" + theField.getName(),
                    theField.getType());
            body.getLocals().add(local);
            units.insertBefore(Jimple.v().newAssignStmt(local, newValue),
                    insertPoint);

            FieldRef fieldRef = Jimple.v()
                    .newStaticFieldRef(theField.makeRef());
            units.insertBefore(Jimple.v().newAssignStmt(fieldRef, local),
                    insertPoint);
        } else {
            for (Iterator methods = theClass.getMethods().iterator(); methods
                    .hasNext();) {
                SootMethod method = (SootMethod) methods.next();

                // ignore things that aren't initializers.
                if (!method.getName().equals("<init>")) {
                    continue;
                }

                JimpleBody body = (JimpleBody) method.retrieveActiveBody();
                Chain units = body.getUnits();
                Stmt insertPoint = (Stmt) units.getLast();
                Local local = Jimple.v().newLocal(
                        "_CGTemp" + theField.getName(), theField.getType());
                body.getLocals().add(local);
                units.insertBefore(Jimple.v().newAssignStmt(local, newValue),
                        insertPoint);

                FieldRef fieldRef = Jimple.v().newInstanceFieldRef(
                        body.getThisLocal(), theField.makeRef());
                units.insertBefore(Jimple.v().newAssignStmt(fieldRef, local),
                        insertPoint);
            }
        }
    }

    /** Copy a class */
    public static SootClass copyClass(SootClass oldClass, String newClassName) {
        //System.out.println("SootClass.copyClass(" + oldClass + ", "
        //                   + newClassName + ")");
        // Create the new Class
        SootClass newClass = new SootClass(newClassName, oldClass
                .getModifiers());

        try {
            Scene.v().addClass(newClass);
        } catch (RuntimeException runtime) {
            throw new RuntimeException("Perhaps you are calling the same "
                    + "transform twice?: " + runtime);
        }

        // Set the Superclass.
        newClass.setSuperclass(oldClass.getSuperclass());

        // Copy the interface declarations
        newClass.getInterfaces().addAll(oldClass.getInterfaces());

        // Copy the fields.
        _copyFields(newClass, oldClass);

        // Copy the methods.
        Iterator methods = oldClass.getMethods().iterator();

        while (methods.hasNext()) {
            SootMethod oldMethod = (SootMethod) methods.next();

            SootMethod newMethod = new SootMethod(oldMethod.getName(),
                    oldMethod.getParameterTypes(), oldMethod.getReturnType(),
                    oldMethod.getModifiers(), oldMethod.getExceptions());
            newClass.addMethod(newMethod);

            JimpleBody body = Jimple.v().newBody(newMethod);
            body.importBodyContentsFrom(oldMethod.retrieveActiveBody());
            newMethod.setActiveBody(body);
        }

        changeTypesOfFields(newClass, oldClass, newClass);
        changeTypesInMethods(newClass, oldClass, newClass);
        return newClass;
    }

    /** Search through all the fields in the given class and if the
     *  field is of class oldClass, then change it to newClass.
     *  @param theClass The class containing fields to modify.
     *  @param oldClass The class to replace.
     *  @param newClass The new class.
     */
    public static void changeTypesOfFields(SootClass theClass,
            SootClass oldClass, SootClass newClass) {
        Iterator fields = theClass.getFields().snapshotIterator();

        while (fields.hasNext()) {
            SootField oldField = (SootField) fields.next();
            Type type = oldField.getType();

            //  System.out.println("field with type " + type);
            if (type instanceof RefType) {
                SootClass refClass = ((RefType) type).getSootClass();

                if (refClass == oldClass) {
                    oldField.setType(RefType.v(newClass));

                    // we have to do this seemingly useless
                    // thing, since the scene caches a pointer
                    // to the field based on it's parameter types.
                    theClass.removeField(oldField);
                    theClass.addField(oldField);
                } else if (refClass.getName().startsWith(oldClass.getName())) {
                    SootClass changeClass = _getInnerClassCopy(oldClass,
                            refClass, newClass);
                    oldField.setType(RefType.v(changeClass));

                    // we have to do this seemingly useless
                    // thing, since the scene caches a pointer
                    // to the field based on it's parameter types.
                    theClass.removeField(oldField);
                    theClass.addField(oldField);
                }
            }
        }
    }

    /** Search through all the methods in the given class and change
     *  all references to the old class to references to the new class.
     *  This includes field references, type casts, this references,
     *  new object instantiations and method invocations.
     *  @param theClass The class containing methods to modify.
     *  @param oldClass The class to replace.
     *  @param newClass The new class.
     */
    public static void changeTypesInMethods(SootClass theClass,
            SootClass oldClass, SootClass newClass) {
        System.out.println("fixing references on " + theClass);
        System.out.println("replacing " + oldClass + " with " + newClass);
        ArrayList methodList = new ArrayList(theClass.getMethods());

        for (Iterator methods = methodList.iterator(); methods.hasNext();) {
            SootMethod newMethod = (SootMethod) methods.next();

            // System.out.println("newMethod = " + newMethod.getSignature());
            Type returnType = newMethod.getReturnType();

            if (returnType instanceof RefType
                    && (((RefType) returnType).getSootClass() == oldClass)) {
                newMethod.setReturnType(RefType.v(newClass));
            }

            List paramTypes = new LinkedList();

            for (Iterator oldParamTypes = newMethod.getParameterTypes()
                    .iterator(); oldParamTypes.hasNext();) {
                Type type = (Type) oldParamTypes.next();

                if (type instanceof RefType
                        && (((RefType) type).getSootClass() == oldClass)) {
                    paramTypes.add(RefType.v(newClass));
                } else if (type instanceof RefType
                        && (((RefType) type).getSootClass().getName()
                                .startsWith(oldClass.getName()))) {
                    SootClass changeClass = _getInnerClassCopy(oldClass,
                            ((RefType) type).getSootClass(), newClass);
                    paramTypes.add(RefType.v(changeClass));
                } else {
                    paramTypes.add(type);
                }
            }

            newMethod.setParameterTypes(paramTypes);

            // we have to do this seemingly useless
            // thing, since the scene caches a pointer
            // to the method based on it's parameter types.
            theClass.removeMethod(newMethod);
            theClass.addMethod(newMethod);

            Body newBody = newMethod.retrieveActiveBody();

            for (Iterator locals = newBody.getLocals().iterator(); locals
                    .hasNext();) {
                Local local = (Local) locals.next();
                Type type = local.getType();

                if (type instanceof RefType
                        && (((RefType) type).getSootClass() == oldClass)) {
                    local.setType(RefType.v(newClass));
                }
            }

            Iterator j = newBody.getUnits().iterator();

            while (j.hasNext()) {
                Unit unit = (Unit) j.next();
                Iterator boxes = unit.getUseAndDefBoxes().iterator();
                //  System.out.println("unit = " + unit);

                while (boxes.hasNext()) {
                    ValueBox box = (ValueBox) boxes.next();
                    Value value = box.getValue();

                    if (value instanceof FieldRef) {
                        // Fix references to fields
                        FieldRef r = (FieldRef) value;
                        SootFieldRef fieldRef = r.getFieldRef();
                        if (fieldRef.type() instanceof RefType) {
                            RefType fieldType = (RefType) fieldRef.type();
                            SootClass fieldClass = fieldType.getSootClass();
                            if (fieldClass == oldClass) {
                                r.setFieldRef(Scene.v().makeFieldRef(
                                        fieldRef.declaringClass(),
                                        fieldRef.name(), RefType.v(newClass),
                                        fieldRef.isStatic()));
                            }
                            fieldRef = r.getFieldRef();
                        }

                        if (fieldRef.declaringClass() == oldClass) {
                            // We might also have a reference to a field
                            // which is not actually declared in the
                            // oldclass, in which case, we just fix up
                            // the ref to point to the new super class
                            r.setFieldRef(Scene.v().makeFieldRef(newClass,
                                    fieldRef.name(), fieldRef.type(),
                                    fieldRef.isStatic()));
                        } else if (fieldRef.declaringClass().getName()
                                .startsWith(oldClass.getName())) {
                            SootClass changeClass = _getInnerClassCopy(
                                    oldClass, r.getField().getDeclaringClass(),
                                    newClass);
                            r.setFieldRef(changeClass.getFieldByName(
                                    r.getField().getName()).makeRef());
                        }//  else if (r.getField().getDeclaringClass() == oldClass) {
                        //                             r.setFieldRef(
                        //                                     newClass.getFieldByName(
                        //                                             r.getField().getName()).makeRef());

                        //                             //   System.out.println("fieldRef = " +
                        //                             //              box.getValue());
                        //                         }

                    } else if (value instanceof CastExpr) {
                        // Fix casts
                        CastExpr r = (CastExpr) value;
                        Type type = r.getType();

                        if (type instanceof RefType) {
                            SootClass refClass = ((RefType) type)
                                    .getSootClass();

                            if (refClass == oldClass) {
                                r.setCastType(RefType.v(newClass));

                                // System.out.println("newValue = " +
                                //        box.getValue());
                            } else if (refClass.getName().startsWith(
                                    oldClass.getName())) {
                                SootClass changeClass = _getInnerClassCopy(
                                        oldClass, refClass, newClass);
                                r.setCastType(RefType.v(changeClass));
                            }
                        }
                    } else if (value instanceof ThisRef) {
                        // Fix references to 'this'
                        ThisRef r = (ThisRef) value;
                        Type type = r.getType();

                        if (type instanceof RefType
                                && (((RefType) type).getSootClass() == oldClass)) {
                            box.setValue(Jimple.v().newThisRef(
                                    RefType.v(newClass)));
                        }
                    } else if (value instanceof ParameterRef) {
                        // Fix references to a parameter
                        ParameterRef r = (ParameterRef) value;
                        Type type = r.getType();

                        if (type instanceof RefType
                                && (((RefType) type).getSootClass() == oldClass)) {
                            box.setValue(Jimple.v().newParameterRef(
                                    RefType.v(newClass), r.getIndex()));
                        }
                    } else if (value instanceof InvokeExpr) {
                        // Fix up the method invokes.
                        InvokeExpr r = (InvokeExpr) value;
                        SootMethodRef methodRef = r.getMethodRef();
                        System.out.println("invoke = " + r);

                        List newParameterTypes = new LinkedList();
                        for (Iterator i = methodRef.parameterTypes().iterator(); i
                                .hasNext();) {
                            Type type = (Type) i.next();
                            if (type instanceof RefType
                                    && (((RefType) type).getSootClass() == oldClass)) {
                                System.out.println("matchedParameter = "
                                        + newClass);
                                newParameterTypes.add(RefType.v(newClass));
                            } else if (type instanceof RefType
                                    && (((RefType) type).getSootClass()
                                            .getName().startsWith(oldClass
                                            .getName()))) {
                                System.out.println("matchedParameter = "
                                        + newClass);
                                SootClass changeClass = _getInnerClassCopy(
                                        oldClass, ((RefType) type)
                                                .getSootClass(), newClass);
                                newParameterTypes.add(RefType.v(changeClass));
                            } else {
                                newParameterTypes.add(type);
                            }

                        }

                        Type newReturnType = methodRef.returnType();
                        if (newReturnType instanceof RefType
                                && (((RefType) newReturnType).getSootClass() == oldClass)) {
                            newReturnType = RefType.v(newClass);
                        }

                        // Update the parameter types and the return type.
                        methodRef = Scene.v().makeMethodRef(
                                methodRef.declaringClass(), methodRef.name(),
                                newParameterTypes, newReturnType,
                                methodRef.isStatic());
                        r.setMethodRef(methodRef);

                        if (methodRef.declaringClass() == oldClass) {
                            r.setMethodRef(Scene.v().makeMethodRef(newClass,
                                    methodRef.name(),
                                    methodRef.parameterTypes(),
                                    methodRef.returnType(),
                                    methodRef.isStatic()));
                            // System.out.println("newValue = " +
                            // box.getValue());
                        } else if (methodRef.declaringClass().getName()
                                .startsWith(oldClass.getName())) {
                            SootClass changeClass = _getInnerClassCopy(
                                    oldClass, methodRef.declaringClass(),
                                    newClass);
                            r.setMethodRef(Scene.v().makeMethodRef(changeClass,
                                    methodRef.name(),
                                    methodRef.parameterTypes(),
                                    methodRef.returnType(),
                                    methodRef.isStatic()));
                        }
                    } else if (value instanceof NewExpr) {
                        // Fix up the object creations.
                        NewExpr r = (NewExpr) value;

                        if (r.getBaseType().getSootClass() == oldClass) {
                            r.setBaseType(RefType.v(newClass));

                            //   System.out.println("newValue = " +
                            //           box.getValue());
                        } else if (r.getBaseType().getSootClass().getName()
                                .startsWith(oldClass.getName())) {
                            SootClass changeClass = _getInnerClassCopy(
                                    oldClass, r.getBaseType().getSootClass(),
                                    newClass);
                            r.setBaseType(RefType.v(changeClass));
                        }
                    }

                    //    System.out.println("value = " + value);
                    //   System.out.println("class = " +
                    //            value.getClass().getName());
                }
            }
        }
    }

    /** Return an object that represents the same numeric value as the
     *  given value.
     *  @param value A constant value.
     */
    public static Object convertConstantValueToArgument(Value value) {
        if (value instanceof NullConstant) {
            return null;
        } else if (value instanceof IntConstant) {
            return Integer.valueOf(((IntConstant) value).value);
        } else if (value instanceof LongConstant) {
            return Long.valueOf(((LongConstant) value).value);
        } else if (value instanceof StringConstant) {
            return ((StringConstant) value).value;
        } else if (value instanceof DoubleConstant) {
            return Double.valueOf(((DoubleConstant) value).value);
        } else {
            throw new RuntimeException("unrecognized constant value = " + value);
        }
    }



    /** Create a new instance field with the given name
     *  and type and add it to the
     *  given class.  Add statements to the given body to initialize the
     *  field from the given local.
     */
    public static SootField createAndSetFieldFromLocal(JimpleBody body,
            Local local, SootClass theClass, Type type, String name) {
        return createAndSetFieldFromLocal(body, local, theClass, type, name,
                (Unit) body.getUnits().getLast());
    }

    /** Create a new instance field with the given name and type and
     *  add it to the given class.  Add statements to the given body
     *  after the given insertion point to initialize the field from
     *  the given local.
     */
    public static SootField createAndSetFieldFromLocal(JimpleBody body,
            Local local, SootClass theClass, Type type, String name,
            Unit insertPoint) {
        Chain units = body.getUnits();
        Local thisLocal = body.getThisLocal();

        Local castLocal;

        if (local.getType().equals(type)) {
            castLocal = local;
        } else {
            castLocal = Jimple.v().newLocal("local_" + name, type);
            body.getLocals().add(castLocal);

            // Cast the local to the type of the field.
            units.insertAfter(Jimple.v().newAssignStmt(castLocal,
                    Jimple.v().newCastExpr(local, type)), insertPoint);
            insertPoint = (Unit) body.getUnits().getSuccOf(insertPoint);
        }

        // Create the new field if necessary
        SootField field;

        if (theClass.declaresFieldByName(name)) {
            field = theClass.getFieldByName(name);
        } else {
            field = new SootField(name, type, Modifier.PUBLIC);
            theClass.addField(field);
        }

        // Set the field.
        units.insertAfter(Jimple.v().newAssignStmt(
                Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef()),
                castLocal), insertPoint);
        return field;
    }

    /** Create statements that correspond to a for loop and return
     *  them.  The returned list will incorporate the statements in
     *  the given list of initializer and body statements, and execute
     *  while the given conditional expression is true.
     */
    public static List createForLoopBefore(Body body, Unit insertPoint,
            List initializerList, List bodyList, Expr conditionalExpr) {
        List list = new LinkedList();
        Stmt bodyStart = (Stmt) bodyList.get(0);
        Stmt conditionalStmt = Jimple.v().newIfStmt(conditionalExpr, bodyStart);
        body.getUnits().insertBefore(initializerList, insertPoint);
        body.getUnits().insertBefore(Jimple.v().newGotoStmt(conditionalStmt),
                insertPoint);
        body.getUnits().insertBefore(bodyList, insertPoint);
        body.getUnits().insertBefore(conditionalStmt, insertPoint);
        return list;
    }

    /** Create a type with the same shape as the given shape type,
     *  containing elements of the type given by the given element
     *  type.  That is, if <i>shapeType</i> is a base type (a
     *  reference Type, or a native type), then return
     *  <i>elementType</i>.  If <i>shapeType</i> is an ArrayType, and
     *  <i>elementType</i> is a simple type, then return a new array
     *  type with the same number of dimensions as <i>shapeType</i>,
     *  and element type <i>elementType</i>.  If both are array types,
     *  then return a new array type with the sum of the number of
     *  dimensions, and the element type <i>elementType</i>.
     */
    public static Type createIsomorphicType(Type shapeType, Type elementType) {
        if (shapeType instanceof RefType || shapeType instanceof PrimType) {
            return elementType;
        } else if (shapeType instanceof ArrayType) {
            ArrayType arrayShapeType = (ArrayType) shapeType;

            if (elementType instanceof RefType
                    || elementType instanceof PrimType) {
                return ArrayType.v(elementType, arrayShapeType.numDimensions);
            } else if (elementType instanceof ArrayType) {
                ArrayType arrayElementType = (ArrayType) elementType;
                return ArrayType.v(arrayElementType.baseType,
                        arrayElementType.numDimensions
                                + arrayShapeType.numDimensions);
            }
        }

        throw new RuntimeException("Types for shape = " + shapeType
                + " and element = " + elementType
                + " must be arrays or base types.");
    }

    /** Create a new local variable in the given body, initialized
     *  before the given unit that refers to a Runtime exception with
     *  the given string message.
     */
    public static Local createRuntimeException(Body body, Unit unit,
            String string) {
        SootClass exceptionClass = Scene.v().getSootClass(
                "java.lang.RuntimeException");
        RefType exceptionType = RefType.v(exceptionClass);
        SootMethod initMethod = exceptionClass
                .getMethod("void <init>(java.lang.String)");

        Local local = Jimple.v().newLocal("exceptionLocal", exceptionType);
        body.getLocals().add(local);
        body.getUnits().insertBefore(
                Jimple.v().newAssignStmt(local,
                        Jimple.v().newNewExpr(exceptionType)), unit);
        body.getUnits().insertBefore(
                Jimple.v()
                        .newInvokeStmt(
                                Jimple.v().newSpecialInvokeExpr(local,
                                        initMethod.makeRef(),
                                        StringConstant.v(string))), unit);
        return local;
    }

    /** Create a new static class that will behave identically to the
     *  given instance.  Replace all references to the given instance with
     *  references to the static class.
     *  @param theClass The context in which method calls on the given
     *  instance will be replaced with method calls to the new static
     *  class.
     *  @param containerBody The body that contains the definition
     *  statement.
     *  @param newStmt The statement where the instance is created.
     *  The right hand side of the definition is assumed to be an
     *  instance of NewExpr.
     *  @param constructorStmt The statement where the initializer
     *  for the instance is called.
     *  @param className The name of the class that will be created.
     */
    public static SootClass createStaticClassForInstance(SootClass theClass,
            Body containerBody, DefinitionStmt newStmt,
            InvokeStmt constructorStmt, String className) {
        // FIXME: We need to do something if the object is passed to a
        // method.  Perhaps we can copy the method and inline the static
        // methods inside.  Even worse, what if we pass to an
        // object constructor?  We really need to create a static instance
        // of that class as well, and recurse.
        // First copy the class.
        NewExpr newExpr = (NewExpr) newStmt.getRightOp();
        SootClass instanceClass = newExpr.getBaseType().getSootClass();
        SootClass staticClass = copyClass(instanceClass, className);
        staticClass.setApplicationClass();

        // some reason when writing out BAF.
        // fold the class up to object.
        SootClass objectClass = Scene.v().loadClassAndSupport("java.lang.Object");
        SootClass superClass = staticClass.getSuperclass();

        while (superClass != objectClass) {
            superClass.setLibraryClass();
            SootUtilities.foldClass(staticClass);
            superClass = staticClass.getSuperclass();
        }

        // Push the constructor code into the <clinit> method.
        SootMethod constructorMethod = (constructorStmt.getInvokeExpr())
                .getMethod();
        SootMethod staticConstructorMethod = staticClass
                .getMethod(constructorMethod.getSubSignature());

        SootMethod clinitMethod;

        // create a class initializer if one does not already exist.
        if (staticClass.declaresMethodByName("<clinit>")) {
            clinitMethod = staticClass.getMethodByName("<clinit>");
        } else {
            clinitMethod = new SootMethod("<clinit>", new LinkedList(),
                    NullType.v(), Modifier.STATIC);
            staticClass.addMethod(clinitMethod);
        }

        System.out.println("constructor = " + constructorStmt);

        constructorMethod.retrieveActiveBody();

        JimpleBody clinitBody = (JimpleBody) clinitMethod.retrieveActiveBody();
        Chain clinitUnits = clinitBody.getUnits();
        Stmt insertPoint = (Stmt) clinitUnits.getLast();

        // insert a (static) call to the (non static)
        // constructor.
        // Later we will come back and inline this after we make all the
        // method static.
        InvokeExpr constructorExpr = constructorStmt.getInvokeExpr();
        Stmt insertStmt = Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(
                        staticConstructorMethod.makeRef(),
                        constructorExpr.getArgs()));
        clinitUnits.insertBefore(insertStmt, insertPoint);

        // Loop through the class and make all the non-static method static.
        // Make all reference to this into static references.
        ArrayList methodList = new ArrayList(staticClass.getMethods());

        for (Iterator methods = methodList.iterator(); methods.hasNext();) {
            SootMethod method = (SootMethod) methods.next();

            // ignore static methods.
            if (method.isStatic()) {
                continue;
            }

            System.out.println("method = " + method);

            JimpleBody body = (JimpleBody) method.retrieveActiveBody();

            Local thisLocal = body.getThisLocal();

            // If we have an init method, then remove specialinvoke calls.
            // These are calls to the superclass,
            // and no longer make sense.
            if (method.getName().equals("<init>")) {
                Iterator units = body.getUnits().snapshotIterator();

                while (units.hasNext()) {
                    Stmt s = (Stmt) units.next();

                    if (s instanceof InvokeStmt
                            && ((InvokeStmt) s).getInvokeExpr() instanceof SpecialInvokeExpr) {
                        body.getUnits().remove(s);
                        break;
                    }
                }

                // and rename the method to something that is not reserved
                // so we can call it manually.
                method.setName("_init");
            }

            // FIXME: checks for equality with thisLocal.  What if
            // thisLocal is aliased in another local?  Maybe we should
            // run the CopyPropagator somewhere here?
            // change method calls to static invocation
            for (Iterator useBoxes = body.getUseAndDefBoxes().iterator(); useBoxes
                    .hasNext();) {
                ValueBox box = (ValueBox) useBoxes.next();

                if (box.getValue() instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr expr = (InstanceInvokeExpr) box
                            .getValue();
                    Local local = (Local) expr.getBase();

                    if (local == thisLocal) {
                        System.out.println("fixing invoke = " + expr);
                        box.setValue(Jimple.v().newStaticInvokeExpr(
                                expr.getMethod().makeRef(), expr.getArgs()));
                    }
                } else if (box.getValue() instanceof InstanceFieldRef) {
                    InstanceFieldRef expr = (InstanceFieldRef) box.getValue();
                    Local local = (Local) expr.getBase();

                    if (local == thisLocal) {
                        System.out.println("fixing field = " + expr);
                        box.setValue(Jimple.v().newStaticFieldRef(
                                expr.getField().makeRef()));
                    }
                }
            }

            // Fix synchronization locks.  Anything synchronized on this
            // should instead be synchronized on the class.
            for (Iterator stmts = body.getUnits().snapshotIterator(); stmts
                    .hasNext();) {
                Stmt stmt = (Stmt) stmts.next();

                if (stmt instanceof MonitorStmt) {
                    MonitorStmt monitorStmt = (MonitorStmt) stmt;
                    Local lock = (Local) monitorStmt.getOp();

                    if (lock == thisLocal) {
                        Local classLocal = SynchronizerManager.v()
                                .addStmtsToFetchClassBefore(body, stmt);
                        monitorStmt.setOp(classLocal);
                    }
                }
            }

            // remove the this identity statement.
            Iterator units = body.getUnits().snapshotIterator();

            while (units.hasNext()) {
                Stmt s = (Stmt) units.next();

                if (s instanceof IdentityStmt
                        && ((IdentityStmt) s).getRightOp() instanceof ThisRef) {
                    body.getUnits().remove(s);
                }
            }

            // make the method static.
            method.setModifiers(method.getModifiers() | Modifier.STATIC);
        }

        // Loop through the class and make all the non-static fields static.
        // Make all reference to this into static references.
        for (Iterator fields = staticClass.getFields().iterator(); fields
                .hasNext();) {
            SootField field = (SootField) fields.next();

            // make the fieldd static.
            field.setModifiers(field.getModifiers() | Modifier.STATIC);
        }

        System.out.println("inlining = " + constructorMethod);
        System.out.println("inlineCall = " + insertStmt);
        System.out.println("container = " + clinitMethod);
        SiteInliner.inlineSite(staticConstructorMethod, insertStmt,
                clinitMethod);

        // Now loop through all the reachable uses of the new definition
        // and replace references to the local with references to the
        // static class.
        // FIXME this only traces through locals.  What if we set the
        // value to a field?
        CompleteUnitGraph unitGraph = new CompleteUnitGraph(containerBody);

        // this will help us figure out where locals are defined.
        SimpleLocalDefs localDefs = new SimpleLocalDefs(unitGraph);
        SimpleLocalUses localUses = new SimpleLocalUses(unitGraph, localDefs);
        List useList = localUses.getUsesOf(newStmt);

        for (Iterator pairs = useList.iterator(); pairs.hasNext();) {
            UnitValueBoxPair pair = (UnitValueBoxPair) pairs.next();
            Stmt useStmt = (Stmt) pair.getUnit();
            /*ValueBox useBox = (ValueBox)*/pair.getValueBox();
            System.out.println("used at = " + useStmt);
        }

        return staticClass;
    }

    /** Return true if the given class derives from the given base class.
     */
    public static boolean derivesFrom(SootClass theClass, SootClass baseClass) {
        SootClass objectClass = Scene.v().getSootClassUnsafe("java.lang.Object");
        while (theClass != objectClass) {
            if ((baseClass == theClass)
                   /* || theClass.implementsInterface(baseClass.getName())*/) {
                return true;
            }

            theClass = theClass.getSuperclass();
        }

        return false;
    }

    /** Return true if type2 is a subtype of type1.
     */
    public static boolean isSubtypeOf(Type type1, Type type2) {
        if (type1 instanceof RefType && type2 instanceof RefType) {
            SootClass class1 = ((RefType) type1).getSootClass();
            SootClass class2 = ((RefType) type2).getSootClass();
            return derivesFrom(class1, class2);
        } else if (/* type1 instanceof Type && */
        type2 instanceof RefType) {
            Type elementType1 = ((ArrayType) type1).baseType;

            // FIXME: FindBugs: Impossible cast from soot.RefType to
            // soot.ArrayType
            Type elementType2 = ((ArrayType) type2).baseType;
            return isSubtypeOf(elementType1, elementType2);
        } else {
            return type1.equals(type2); // numeric types.
        }
    }

    /** Merge the given class with its super class.  All of the
     * methods and fields of the super class will be added to the
     * given class, if they do not already exist.  Methods existing in
     * both the given class and the super class will be merged by
     * inlining the super class method.
     */
    public static void foldClass(SootClass theClass) {
        SootClass superClass = theClass.getSuperclass();
        System.out.println("folding " + theClass + " into " + superClass);
        Scene.v().setActiveHierarchy(new Hierarchy());

        // First resolve all the FieldRefs and MethodRefs, so we know what
        // they are currently pointing to.
        //         for (Iterator methods = superClass.getMethods().iterator(); methods
        //                 .hasNext();) {
        //             SootMethod oldMethod = (SootMethod) methods.next();
        //             oldMethod.retrieveActiveBody();
        //             Body newBody = newMethod.retrieveActiveBody();

        //             // use a snapshotIterator since we are going to be manipulating
        //             // the statements.
        //             Iterator j = newBody.getUnits().snapshotIterator();

        //             while (j.hasNext()) {
        //                 Stmt stmt = (Stmt) j.next();
        //                 Iterator boxes = stmt.getUseAndDefBoxes().iterator();

        //                 while (boxes.hasNext()) {
        //                     ValueBox box = (ValueBox) boxes.next();
        //                     Value value = box.getValue();

        //                     if (value instanceof FieldRef) {

        // Copy the interface declarations.
        theClass.getInterfaces().addAll(superClass.getInterfaces());

        // Rename fields in the given class
        // whose name is the same between
        // the given class and its superclass.
        for (Iterator fields = theClass.getFields().snapshotIterator(); fields
                .hasNext();) {
            SootField field = (SootField) fields.next();

            if (superClass.declaresFieldByName(field.getName())) {
                // SootField superField = superClass.getFieldByName(field.getName());
                String newName = StringUtilities.sanitizeName(superClass
                        .getName())
                        + field.getName();
                System.out.println("Renaming field " + field + " to " + newName
                        + " to avoid collision with superClass field "
                        + superClass.getFieldByName(field.getName()));

                field.setName(newName);

                // We have to do this seemingly useless
                // thing, since the scene caches a pointer
                // to the field based on it's name.
                theClass.removeField(field);
                theClass.addField(field);
            }
        }

        // Copy the field declarations.
        /*List collidedFieldList =*/_copyFields(theClass, superClass);

        // Now create new methods in the given class for methods that
        // exist in the super class, but not in the given class.
        // Invoke the super class.
        for (Iterator methods = superClass.getMethods().iterator(); methods
                .hasNext();) {
            SootMethod oldMethod = (SootMethod) methods.next();

            if (theClass.declaresMethod(oldMethod.getSubSignature())) {
                continue;
            }

            oldMethod.retrieveActiveBody();

            SootMethod newMethod = new SootMethod(oldMethod.getName(),
                    oldMethod.getParameterTypes(), oldMethod.getReturnType(),
                    oldMethod.getModifiers(), oldMethod.getExceptions());
            theClass.addMethod(newMethod);

            JimpleBody newBody = Jimple.v().newBody(newMethod);
            newMethod.setActiveBody(newBody);
            newBody.insertIdentityStmts();

            //System.out.println("method = " + newMethod);
            //System.out.println("oldMethod = " + oldMethod);
            // Call the super method
            Chain units = newBody.getUnits();

            // get a list of the locals that reference
            // the parameters of the
            // constructor.
            List parameterList = new ArrayList();
            parameterList.addAll(newBody.getLocals());

            Stmt invokeStmt = null;

            // Invoke the method...
            // handling static and void methods differently
            if (oldMethod.getReturnType() == VoidType.v()) {
                InvokeExpr invokeExpr;

                if (newMethod.isStatic()) {
                    invokeExpr = Jimple.v().newStaticInvokeExpr(
                            oldMethod.makeRef(), parameterList);
                } else {
                    Local thisLocal = newBody.getThisLocal();
                    parameterList.remove(thisLocal);
                    invokeExpr = Jimple.v().newVirtualInvokeExpr(thisLocal,
                            oldMethod.makeRef(), parameterList);
                }

                invokeStmt = Jimple.v().newInvokeStmt(invokeExpr);
                units.add(invokeStmt);

                // return void
                units.add(Jimple.v().newReturnVoidStmt());
            } else {
                InvokeExpr invokeExpr;

                // Create a new local for the return value.
                Local returnValueLocal = Jimple.v().newLocal("returnValue",
                        oldMethod.getReturnType());
                newBody.getLocals().add(returnValueLocal);

                if (newMethod.isStatic()) {
                    invokeExpr = Jimple.v().newStaticInvokeExpr(
                            oldMethod.makeRef(), parameterList);
                } else {
                    Local thisLocal = newBody.getThisLocal();
                    parameterList.remove(thisLocal);
                    invokeExpr = Jimple.v().newVirtualInvokeExpr(thisLocal,
                            oldMethod.makeRef(), parameterList);
                }

                invokeStmt = Jimple.v().newAssignStmt(returnValueLocal,
                        invokeExpr);
                units.add(invokeStmt);

                // return the value
                units.add(Jimple.v().newReturnStmt(returnValueLocal));
            }
        }

        // Loop through all the methods again, this time looking for
        // method invocations on the old superClass...  Inline these calls.
        // This code is similar to inlineCallsToMethod, but avoids iterating
        // over all the methods in the class twice, which could get
        // very expensive.
        for (Iterator methods = theClass.getMethods().iterator(); methods
                .hasNext();) {
            SootMethod newMethod = (SootMethod) methods.next();
            Body newBody = newMethod.retrieveActiveBody();

            // use a snapshotIterator since we are going to be manipulating
            // the statements.
            Iterator j = newBody.getUnits().snapshotIterator();

            while (j.hasNext()) {
                Stmt stmt = (Stmt) j.next();

                System.out.println("stmt = " + stmt);
                Iterator boxes = stmt.getUseAndDefBoxes().iterator();

                while (boxes.hasNext()) {
                    ValueBox box = (ValueBox) boxes.next();
                    Value value = box.getValue();

                    if (value instanceof FieldRef) {
                        // Fix references to fields
                        FieldRef r = (FieldRef) value;
                        SootFieldRef fieldRef = r.getFieldRef();

                        if (r.getField().getDeclaringClass() != superClass
                                && fieldRef.declaringClass() == superClass) {
                            // We might also have a reference to a method
                            // which is not actually declared in the
                            // superclass, in which case, we just fix up
                            // the ref to point to the new super class
                            r.setFieldRef(Scene.v().makeFieldRef(
                                    superClass.getSuperclass(),
                                    fieldRef.name(), fieldRef.type(),
                                    fieldRef.isStatic()));
                        }
                    }
                }
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invoke = stmt.getInvokeExpr();
                    SootMethodRef invokeMethodRef = invoke.getMethodRef();
                    SootMethod invokeMethod = invoke.getMethod();

                    if (invokeMethod.getDeclaringClass() == superClass) {

                        // Force the body of the thing we are inlining to be
                        // loaded
                        try {
                            if (invokeMethod.isConcrete()) {
                                invokeMethod.retrieveActiveBody();
                            } else {
                                System.out.println("SootUtilities."
                                        + "foldClass() " + invokeMethod
                                        + " is not concrete!");

                                // javac -target 1.2 and greater
                                // ends up causing problems here when
                                // calling super on a method, but the
                                // direct parent does not have a
                                // method by that name.
                                //
                                // If I have 3 classes A, B and C,
                                // where C extends B which extends A
                                // and A and C define a method foo and
                                // C calls super.foo, then under javac
                                // -target 1.2 the constant pool ends
                                // up with a reference to
                                // [2] methodref=soot/coffi/B.foo
                                // and under javac -target 1.1, we end up with
                                // [2] methodref=soot/coffi/A.foo
                                // So, we look for the method in the superclass
                                SootClass scratchClass = invokeMethod
                                        .getDeclaringClass();

                                while (scratchClass.hasSuperclass()) {
                                    SootClass superC = scratchClass
                                            .getSuperclass();

                                    if (superC.declaresMethod(invokeMethod
                                            .getSubSignature())) {
                                        invokeMethod = superC
                                                .getMethod(invokeMethod
                                                        .getSubSignature());
                                        System.out.println("SootUtilties."
                                                + "foldClass() " + "found "
                                                + superC + " " + invokeMethod);
                                        invokeMethod.retrieveActiveBody();
                                        break;
                                    }

                                    scratchClass = superC;
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
//                            throw new Exception(ex,
//                                    "foldClass: Problem with "
//                                            + "retrieveActiveBody()");
                        }

                        SiteInliner.inlineSite(invokeMethod, stmt, newMethod);
                    } else if (invokeMethodRef.declaringClass() == superClass) {
                        // We might also have a reference to a method
                        // which is not actually declared in the
                        // superclass, in which case, we just fix up
                        // the ref to point to the new super class
                        invoke.setMethodRef(Scene.v().makeMethodRef(
                                superClass.getSuperclass(),
                                invokeMethodRef.name(),
                                invokeMethodRef.parameterTypes(),
                                invokeMethodRef.returnType(),
                                invokeMethodRef.isStatic()));
                    }
                }
            }
        }

        // And now replace any remaining references to the super class.
        changeTypesOfFields(theClass, superClass, theClass);
        changeTypesInMethods(theClass, superClass, theClass);

        theClass.setSuperclass(superClass.getSuperclass());
        System.out.println("done folding");
    }

    /** Given a Type object, return the java.lang.Class object that the
     *  type represents.
     */
    public static Class getClassForType(Type type)
            throws ClassNotFoundException {
        if (type instanceof RefType) {
            return Class.forName(((RefType) type).getSootClass().getName());
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            StringBuffer identifier = new StringBuffer();

            for (int i = 0; i < arrayType.numDimensions; i++) {
                identifier.append("[");
            }

            identifier.append(getClassForType(arrayType.baseType).getName());
            return Class.forName(identifier.toString());
        } else if (type instanceof ByteType) {
            return Byte.TYPE;
        } else if (type instanceof CharType) {
            return Character.TYPE;
        } else if (type instanceof DoubleType) {
            return Double.TYPE;
        } else if (type instanceof FloatType) {
            return Float.TYPE;
        } else if (type instanceof IntType) {
            return Integer.TYPE;
        } else if (type instanceof LongType) {
            return Long.TYPE;
        } else if (type instanceof ShortType) {
            return Short.TYPE;
        } else if (type instanceof BooleanType) {
            return Boolean.TYPE;
        } else {
            throw new RuntimeException("unknown type = " + type);
        }
    }

    /** Get the method in the given class that has the given name and will
     *  accept the given argument list.
     */
    public static SootMethod getMatchingMethod(SootClass theClass, String name,
            List args) {
        boolean found = false;
        SootMethod foundMethod = null;

        Iterator methods = theClass.getMethods().iterator();

        while (methods.hasNext()) {
            SootMethod method = (SootMethod) methods.next();

            //  System.out.println("checking method " + method);
            if (method.getName().equals(name)
                    && (args.size() == method.getParameterCount())) {
                Iterator parameterTypes = method.getParameterTypes().iterator();
                Iterator arguments = args.iterator();
                boolean isEqual = true;

                while (parameterTypes.hasNext()) {
                    Type parameterType = (Type) parameterTypes.next();
                    Value argument = (Value) arguments.next();
                    Type argumentType = argument.getType();

                    if (argumentType != parameterType) {
                        // This is inefficient.  Full type merging is
                        // expensive and unnecessary.
                        isEqual = (parameterType == argumentType.merge(
                                parameterType, Scene.v()));
                    }

                    if (!isEqual) {
                        break;
                    }
                }

                if (isEqual && found) {
                    throw new RuntimeException("ambiguous method");
                } else {
                    found = true;
                    foundMethod = method;
                    break;
                }
            }
        }

        return foundMethod;
    }

    /**
     */
    public static SootMethod getSootMethodForMethod(Method method) {
        StringBuffer buffer = new StringBuffer();
        String className = Scene.v().quotedNameOf(
                method.getDeclaringClass().getName());
        Scene.v().loadClassAndSupport(className);
        buffer.append("<" + className + ": ");

        String returnType = method.getReturnType().getName();

        buffer.append(returnType + " "
                + Scene.v().quotedNameOf(method.getName()) + "(");

        Class[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length > 0) {
            buffer.append(parameterTypes[0].getName());

            for (int i = 1; i < parameterTypes.length; i++) {
                buffer.append(",");
                buffer.append(parameterTypes[i].getName());
            }
        }

        buffer.append(")");
        buffer.append(">");

        //         SootClass math = Scene.v().getSootClass("java.lang.Math");
        //         Iterator methods = math.getMethods().iterator();
        //         while (methods.hasNext()) {
        //             System.out.println("method = " + ((SootMethod)methods.next()).getSignature());
        //         }
        System.out.println("looking for method " + buffer);
        return Scene.v().getMethod(buffer.toString());
    }

    public static SootMethod resolveSpecialInvokationForInlining(
            SpecialInvokeExpr expr, SootMethod callingMethod) {
        SootMethod inlinee = Scene.v().getActiveHierarchy()
                .resolveSpecialDispatch(expr, callingMethod);

        // Make sure we can access the body of the method that is
        // being inlined.
        if (!inlinee.getDeclaringClass().isApplicationClass()) {
            inlinee.getDeclaringClass().setLibraryClass();
        }

        inlinee.retrieveActiveBody();
        return inlinee;
    }

    public static SootMethod resolveVirtualInvokationForInlining(
            SootClass baseClass, SootMethod targetMethod) {
        SootMethod inlinee = null;

        // Now inline the resulting call.
        List methodList = Scene.v().getActiveHierarchy()
                .resolveAbstractDispatch(baseClass, targetMethod);

        if (methodList.size() == 1) {
            // inline the method.
            inlinee = (SootMethod) methodList.get(0);
        } else {
            StringBuffer message = new StringBuffer("Can't resolve "
                    + targetMethod + " on baseClass " + baseClass + "\n");

            for (int i = 0; i < methodList.size(); i++) {
                message.append("target = " + methodList.get(i) + "\n");
            }

            throw new RuntimeException(message.toString());
        }

        // Make sure we can access the body of the method that is
        // being inlined.
        if (!inlinee.getDeclaringClass().isApplicationClass()) {
            inlinee.getDeclaringClass().setLibraryClass();
        }

        inlinee.retrieveActiveBody();
        return inlinee;
    }

    /** Inline all calls to the given method that occur within the given class.
     *  Note that this alone will really only increase the size of the
     *  affected code, but it turns cross-method optimizations into local
     *  optimizations which often allows us to do interesting things
     *  afterwards, since we know the values of any parameters to the method.
     */
    public static void inlineCallsToMethod(SootMethod inlineMethod,
            SootClass theClass) {
        // Force the body of the thing we are inlining to be loaded
        // This is required by the inliner.
        inlineMethod.retrieveActiveBody();

        for (Iterator methods = theClass.getMethods().iterator(); methods
                .hasNext();) {
            SootMethod method = (SootMethod) methods.next();

            // System.out.println("method = " + method.getSignature());
            Body body = method.retrieveActiveBody();

            // use a snapshotIterator since we are going to be manipulating
            // the statements.
            Iterator j = body.getUnits().snapshotIterator();

            while (j.hasNext()) {
                Stmt stmt = (Stmt) j.next();

                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invoke = stmt.getInvokeExpr();

                    if (invoke.getMethod() == inlineMethod) {
                        //System.out.println("inlining " + invoke.getMethod());
                        SiteInliner.inlineSite(inlineMethod, stmt, method);
                    }
                }
            }
        }
    }

    /** Inline all the method calls whose base is 'this'
     *  in the given method.
     *  @return true if any changes were made.
     */
    public static boolean inlineCallsOnThisInMethod(SootMethod method) {
        SootClass theClass = method.getDeclaringClass();
        JimpleBody body = (JimpleBody) method.retrieveActiveBody();

        // use a snapshotIterator since we are going to be manipulating
        // the statements.
        boolean inlinedAnything = false;
        Iterator j = body.getUnits().snapshotIterator();

        while (j.hasNext()) {
            Stmt stmt = (Stmt) j.next();

            if (stmt.containsInvokeExpr()) {
                InvokeExpr invoke = stmt.getInvokeExpr();

                if (method instanceof StaticInvokeExpr) {
                    // simply inline static methods.
                    SootMethod inlineMethod = invoke.getMethod();

                    // Don't inline a recursive method call.
                    if (inlineMethod.equals(method)) {
                        continue;
                    }

                    SiteInliner.inlineSite(inlineMethod, stmt, method);
                    inlinedAnything = true;
                } else if (!method.isStatic()
                        && invoke instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr instanceInvoke = (InstanceInvokeExpr) invoke;

                    if (instanceInvoke.getBase().equals(body.getThisLocal())) {
                        SootMethod inlineMethod;

                        if (instanceInvoke instanceof VirtualInvokeExpr) {
                            inlineMethod = searchForMethodByName(theClass,
                                    method.getName());
                        } else {
                            // super. method call
                            // don't inline super constructors.
                            if (method.getName().equals("<init>")) {
                                continue;
                            }

                            inlineMethod = searchForMethodByName(theClass
                                    .getSuperclass(), method.getName());
                        }

                        // Don't inline a recursive method call.
                        if (inlineMethod.equals(method)) {
                            continue;
                        }

                        //System.out.println("inlining " + invoke.getMethod());
                        SiteInliner.inlineSite(inlineMethod, stmt, method);
                        inlinedAnything = true;
                    }
                }
            }
        }

        return inlinedAnything;
    }

    /** Return true if the given value represents something that can
     *  be aliased in Java by something else.  For instance, a local
     *  variable can point to the same object as another local variable.
     */
    public static boolean isAliasableValue(Value value) {
        boolean isAliasableObject = value instanceof Local
                || value instanceof FieldRef || value instanceof CastExpr
                || value instanceof ArrayRef || value instanceof NewArrayExpr;
        boolean isAliasableType = value.getType() instanceof ArrayType
                || value.getType() instanceof RefType;
        return isAliasableObject && isAliasableType;
    }

    /** Make the given field a static field.
     *  Loop through all the methods of the given class and replace
     *  instance references to the given field with static references.
     *  Note that in general, this is not a safe thing to do unless there is
     *  guaranteed to be exactly one instance of the class that defines the
     *  given field.
     */
    public static void makeFieldStatic(SootClass theClass, SootField field) {
        field.setModifiers(field.getModifiers() | Modifier.STATIC);

        ArrayList methodList = new ArrayList(theClass.getMethods());

        for (Iterator methods = methodList.iterator(); methods.hasNext();) {
            SootMethod method = (SootMethod) methods.next();

            // ignore static methods?
            if (method.isStatic()) {
                continue;
            }

            JimpleBody body = (JimpleBody) method.retrieveActiveBody();

            for (Iterator useBoxes = body.getUseAndDefBoxes().iterator(); useBoxes
                    .hasNext();) {
                ValueBox box = (ValueBox) useBoxes.next();

                if (box.getValue() instanceof InstanceFieldRef) {
                    InstanceFieldRef expr = (InstanceFieldRef) box.getValue();

                    if (expr.getField() == field) {
                        System.out.println("fixing field = " + expr);
                        box.setValue(Jimple.v().newStaticFieldRef(
                                expr.getField().makeRef()));
                    }
                }
            }
        }
    }

    /** Reflect the given method on the class of the given object.
     * Invoke the method and return the returned value
     */
    public static Object reflectAndInvokeMethod(Object object,
            SootMethod sootMethod, Value[] argValues) {
        Class objectClass = object.getClass();
        Class[] parameterClasses = new Class[sootMethod.getParameterCount()];
        int i = 0;
        Object[] args = new Object[argValues.length];

        for (Iterator parameterTypes = sootMethod.getParameterTypes()
                .iterator(); parameterTypes.hasNext();) {
            Type parameterType = (Type) parameterTypes.next();

            try {
                args[i] = convertConstantValueToArgument(argValues[i]);
                parameterClasses[i] = getClassForType(parameterType);
                i++;
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Class not found = "
                        + parameterType.toString());
            }
        }

        Object returned;

        try {
            Method method = objectClass.getMethod(sootMethod.getName(),
                    parameterClasses);

            returned = method.invoke(object, args);
        } catch (Exception ex) {
            throw new RuntimeException("Method not found = "
                    + sootMethod.getName());
        }

        return returned;
    }

    /** Reflect the given constructor method with the given arguments
     *  on the class of the given object.  Invoke the method and return
     *  the returned value.
     *  @param sootMethod The soot initializer method that corresponds to the
     *  correct Java constructor.
     *  @param argValues The arguments, which must be constant valued.
     */
    public static Object reflectAndInvokeConstructor(SootMethod sootMethod,
            Value[] argValues) {
        SootClass sootClass = sootMethod.getDeclaringClass();
        Class objectClass;

        try {
            objectClass = Class.forName(sootClass.getName());
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex.toString());
        }

        Class[] parameterClasses = new Class[sootMethod.getParameterCount()];
        int i = 0;
        Object[] args = new Object[argValues.length];

        for (Iterator parameterTypes = sootMethod.getParameterTypes()
                .iterator(); parameterTypes.hasNext();) {
            Type parameterType = (Type) parameterTypes.next();

            try {
                args[i] = convertConstantValueToArgument(argValues[i]);
                parameterClasses[i] = getClassForType(parameterType);
                i++;
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Class not found = "
                        + parameterType.toString());
            }
        }

        Object returned;

        try {
            Constructor constructor = objectClass
                    .getConstructor(parameterClasses);

            returned = constructor.newInstance(args);
        } catch (Exception ex) {
            throw new RuntimeException("Method not found = "
                    + sootMethod.getName());
        }

        return returned;
    }

    /** Replace the invoke expression in the given statement in the
     * given body with the given value.  If the statement is an invoke
     * statement (without a return value) and the value is not an
     * invoke expression, then blindly replacing the invoke expression
     * is incorrect.  This method deals with this corner case by
     * removing the statement from the given body.
     */
    public static void replaceInvokeExpr(JimpleBody body, Stmt stmt, Value value) {
        if (stmt instanceof InvokeStmt && !(value instanceof InvokeExpr)) {
            body.getUnits().remove(stmt);
        } else {
            stmt.getInvokeExprBox().setValue(value);
        }
    }

    /** Get the method with the given name in the given class
     *  (or one of its super classes).
     *  If the method name is ambiguous or there is some other problem,
     *  then a RuntimeException is thrown that includes the names of
     *  all possible methods for that class at that level.
     */
    public static SootMethod searchForMethodByName(SootClass theClass,
            String name) {
        while (theClass != null) {
            if (theClass.declaresMethodByName(name)) {
                //System.out.println("found method " + name + " in " + theClass);
                try {
                    return theClass.getMethodByName(name);
                } catch (RuntimeException ex) {
                    // Print out what the possible methods are.
                    StringBuffer results = new StringBuffer("Methods are: ");
                    boolean commaNeeded = false;
                    Iterator methods = theClass.getMethods().iterator();
                    while (methods.hasNext()) {
                        SootMethod method = (SootMethod) methods.next();
                        if (commaNeeded) {
                            // Add a comma after the first method
                            results.append(", ");
                        } else {
                            commaNeeded = true;
                        }
                        results.append(method.toString());
                    }
                    throw new RuntimeException("Failed to search \"" + theClass
                            + "\" for \"" + name + "\" possible " + "methods: "
                            + results.toString(), ex);
                }
            }

            theClass = theClass.getSuperclass();
            theClass.setLibraryClass();
        }

        throw new RuntimeException("Method " + name + " not found in class "
                + theClass);
    }

    /** Anywhere where the iterator of the given field is referenced
     *  in the given class, unroll the iterator as if it contained the
     *  objects referenced by the given fields.
     */
    public static void unrollIteratorInstances(SootClass theClass,
            SootField field, List fieldList) {
        // FIXME: This is currently written using a lot of manually searching
        // of the blocks.  Unfortunately, finding them all is hard.
        // This should really be done using Dava (when it is finished)
        SootClass iteratorClass = Scene.v().getSootClass("java.util.Iterator");
        SootMethod iteratorNextMethod = iteratorClass
                .getMethod("java.lang.Object next()");
        SootMethod iteratorHasNextMethod = iteratorClass
                .getMethod("boolean hasNext()");

        for (Iterator methods = theClass.getMethods().iterator(); methods
                .hasNext();) {
            SootMethod method = (SootMethod) methods.next();
            JimpleBody body = (JimpleBody) method.retrieveActiveBody();

            BlockGraph graph = new CompleteBlockGraph(body);

            for (Iterator blocks = graph.iterator(); blocks.hasNext();) {
                Block block = (Block) blocks.next();

                // System.out.println("body = " + block);
                // filter out anything that doesn't look like a loop body.
                if ((block.getPreds().size() != 1)
                        || (block.getSuccs().size() != 1)) {
                    continue;
                }

                // filter out anything that isn't attached to something
                // that looks like a conditional jump.
                Block whileCond = (Block) block.getSuccs().get(0);

                //  System.out.println("cond = " + whileCond);
                if ((whileCond != block.getPreds().get(0))
                        || (whileCond.getPreds().size() != 2)
                        || (whileCond.getSuccs().size() != 2)) {
                    continue;
                }

                // filter out anything that doesn't start with a call
                // to hasNext().
                if (!(whileCond.getHead() instanceof DefinitionStmt)) {
                    continue;
                }

                DefinitionStmt stmt = (DefinitionStmt) whileCond.getHead();

                if (!(stmt.getRightOp() instanceof InterfaceInvokeExpr)) {
                    continue;
                }

                InterfaceInvokeExpr expr = (InterfaceInvokeExpr) stmt
                        .getRightOp();

                if (expr.getMethod() != iteratorHasNextMethod) {
                    continue;
                }

                // At this point we know we have a while (hasNext()) loop.
                // Now go check for iterator is defined...  it should be just
                // above
                Local iteratorLocal = (Local) expr.getBase();
                Block whilePredecessor = (Block) whileCond.getPreds().get(0);

                if (whilePredecessor == block) {
                    whilePredecessor = (Block) whileCond.getPreds().get(1);
                }

                // System.out.println("whilePredecessor = " + whilePredecessor);
                Unit unit = whilePredecessor.getTail();
                boolean found = false;

                // walk backwards until we find a definition of the iterator.
                while ((unit != whilePredecessor.getHead()) && !found) {
                    if (unit instanceof DefinitionStmt
                            && ((DefinitionStmt) unit).getLeftOp().equals(
                                    iteratorLocal)) {
                        found = true;
                    } else {
                        unit = whilePredecessor.getPredOf(unit);
                    }
                }

                //  System.out.println("iterator def = " + unit);
                DefinitionStmt iteratorDefinition = ((DefinitionStmt) unit);

                if (!(iteratorDefinition.getRightOp() instanceof InterfaceInvokeExpr)
                        || !((InterfaceInvokeExpr) iteratorDefinition
                                .getRightOp()).getMethod().getName().equals(
                                "iterator")) {
                    continue;
                }

                Local collectionLocal = (Local) ((InterfaceInvokeExpr) iteratorDefinition
                        .getRightOp()).getBase();

                //  System.out.println("collection Local = " + collectionLocal);
                found = false;

                // Walk backward again until we reach the definition
                // of the collection.
                while ((unit != whilePredecessor.getHead()) && !found) {
                    if (unit instanceof DefinitionStmt
                            && ((DefinitionStmt) unit).getLeftOp().equals(
                                    collectionLocal)) {
                        found = true;
                    } else {
                        unit = whilePredecessor.getPredOf(unit);
                    }
                }

                //  System.out.println("collection def = " + unit);
                // System.out.println("field = " + field);
                DefinitionStmt collectionDefinition = ((DefinitionStmt) unit);

                if (!(collectionDefinition.getRightOp() instanceof FieldRef)
                        || (((FieldRef) collectionDefinition.getRightOp())
                                .getField() != field)) {
                    continue;
                }

                // FINALLY we know we've found something we can unroll... :)
                // System.out.println("is unrollable...");
                // There should be a jump from the predecessor to the
                // condition.  Redirect this jump to the body.
                whileCond.getHead().redirectJumpsToThisTo(block.getHead());

                Local thisLocal = body.getThisLocal();
                Chain units = body.getUnits();
                List blockStmtList = new LinkedList();

                // pull the statements that we are inlining out of the block
                // so that we can copy them.  Note that this also removes
                // them from the method body.
                Unit insertPoint = (Unit) units.getSuccOf(block.getTail());

                for (Iterator blockStmts = block.iterator(); blockStmts
                        .hasNext();) {
                    Stmt original = (Stmt) blockStmts.next();
                    blockStmtList.add(original);
                    blockStmts.remove();
                }

                // Remove the jump that should be the final statement.
                blockStmtList.remove(blockStmtList
                        .get(blockStmtList.size() - 1));

                // Loop through and unroll the loop body once for
                // every element of the field list.
                for (Iterator fields = fieldList.iterator(); fields.hasNext();) {
                    SootField insertField = (SootField) fields.next();

                    for (Iterator blockStmts = blockStmtList.iterator(); blockStmts
                            .hasNext();) {
                        // clone each statement
                        Stmt original = (Stmt) blockStmts.next();
                        Stmt clone = (Stmt) original.clone();

                        // If the statement is a call to the next() method,
                        // then inline it with the next value of the iterator.
                        for (Iterator boxes = clone.getUseBoxes().iterator(); boxes
                                .hasNext();) {
                            ValueBox box = (ValueBox) boxes.next();
                            Value value = box.getValue();

                            if (value instanceof InvokeExpr) {
                                InvokeExpr r = (InvokeExpr) value;

                                if (r.getMethod() == iteratorNextMethod) {
                                    box.setValue(Jimple.v()
                                            .newInstanceFieldRef(thisLocal,
                                                    insertField.makeRef()));
                                }
                            }
                        }

                        units.insertBefore(clone, insertPoint);
                    }
                }

                // remove the conditional
                for (Iterator blockStmts = whileCond.iterator(); blockStmts
                        .hasNext();) {
                    /*Stmt original = (Stmt)*/blockStmts.next();
                    blockStmts.remove();
                }

                body.getUnits().remove(iteratorDefinition);

                // Find while loops.
                // This code modified from WhileMatcher.

                /*
                 List successorList = block.getSuccs();

                 if (successorList.size() == 2) {
                 Block whileBody, whileSucc;
                 boolean found = false;

                 whileBody = whileSucc = block;
                 whileBody = (Block) successorList.get(0);
                 whileSucc = (Block) successorList.get(1);

                 if ((whileBody.getPreds().size() == 1) &&
                 (whileBody.getSuccs().size() == 1) &&
                 (whileBody.getSuccs().get(0) == block))
                 found = true;
                 if (!found) {
                 Block bt;
                 bt = whileSucc;
                 whileSucc = whileBody;
                 whileBody = bt;

                 if ((whileBody.getPreds().size() == 1) &&
                 (whileBody.getSuccs().size() == 1) &&
                 (whileBody.getSuccs().get(0) == block))
                 found = true;
                 }

                 if (found) {
                 if (con
                 System.out.println("found while Loop:");
                 System.out.println("body = " + whileBody);
                 System.out.println("cond = " + block);
                 System.out.println("successor = " + whileSucc);
                 }
                 }      */
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Copy all the fields into the given class from the given old class.
     *  @return A list of fields in the new class that were created whose
     *  names collide with fields already there.
     */
    private static List _copyFields(SootClass newClass, SootClass oldClass) {
        List list = new LinkedList();
        Iterator fields = oldClass.getFields().iterator();

        while (fields.hasNext()) {
            SootField oldField = (SootField) fields.next();

            if (newClass.declaresFieldByName(oldField.getName())) {
                // FIXME
                throw new RuntimeException("Field " + oldField
                        + " cannot be folded into " + newClass
                        + " because its name is the same as "
                        + newClass.getFieldByName(oldField.getName()));
            }

            SootField newField = new SootField(oldField.getName(), oldField
                    .getType(), oldField.getModifiers());
            newClass.addField(newField);
        }

        return list;
    }

    private static SootClass _getInnerClassCopy(SootClass oldOuterClass,
            SootClass oldInnerClass, SootClass newOuterClass) {
        String oldInnerClassName = oldInnerClass.getName();
        String oldInnerClassSpecifier = oldInnerClassName
                .substring(oldOuterClass.getName().length());

        //System.out.println("oldInnerClassSpecifier = " +
        //        oldInnerClassSpecifier);
        String newInnerClassName = newOuterClass.getName()
                + oldInnerClassSpecifier;
        SootClass newInnerClass;

        if (Scene.v().containsClass(newInnerClassName)) {
            newInnerClass = Scene.v().getSootClass(newInnerClassName);
        } else {
            oldInnerClass.setLibraryClass();

            //   System.out.println("copying "+ oldInnerClass +
            //           " to " + newInnerClassName);
            newInnerClass = copyClass(oldInnerClass, newInnerClassName);
            newInnerClass.setApplicationClass();
        }

        changeTypesOfFields(newInnerClass, oldOuterClass, newOuterClass);
        changeTypesInMethods(newInnerClass, oldOuterClass, newOuterClass);
        return newInnerClass;
    }
}