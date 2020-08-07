package edu.ucr.cs.ufarooq.liveDroid;

import com.android.builder.model.SourceProvider;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import edu.ucr.cs.ufarooq.liveDroid.staticResults.ApplicationAnalysisResult;
import edu.ucr.cs.ufarooq.liveDroid.staticResults.ViewResult;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;

public class Utils {

    public static final String xml = "xml";
    public static final String saveCallbackParameter = "outState";
    public static final String restoreCallbackParameter = "savedInstanceState";
    public static final String DUMMY_PACKAGE_NAME = "edu.cs.ucr.dummy";
    public static final String DUMMY_CLASS_NAME = "DummyConstructClass";
    public static final String DUMMY_CLASS_DEF =
            "package " + DUMMY_PACKAGE_NAME + ";\n" +
                    "public class " + DUMMY_CLASS_NAME + " {\n" +
                    "}";
    public static final String CLASS_DEF_LayoutTraverser = "package edu.cs.ucr.dummy;\n" +
            "\n" +
            "import android.os.Bundle;\n" +
            "import android.os.Parcel;\n" +
            "import android.view.View;\n" +
            "import android.view.ViewGroup;\n" +
            "\n" +
            "public class LayoutTraverser {\n" +
            "\n" +
            "    private final Processor processor;\n" +
            "    private final Bundle bundle;\n" +
            "\n" +
            "    public interface Processor {\n" +
            "        void process(View view, Bundle bundle);\n" +
            "    }\n" +
            "\n" +
            "    private LayoutTraverser(Processor processor, Bundle bundle) {\n" +
            "        this.processor = processor;\n" +
            "        this.bundle = bundle;\n" +
            "    }\n" +
            "\n" +
            "    public static LayoutTraverser build(Processor processor, Bundle bundle) {\n" +
            "        return new LayoutTraverser(processor, bundle);\n" +
            "    }\n" +
            "\n" +
            "    public View traverse(ViewGroup root) {\n" +
            "        final int childCount = root.getChildCount();\n" +
            "\n" +
            "        for (int i = 0; i < childCount; ++i) {\n" +
            "            final View child = root.getChildAt(i);\n" +
            "\n" +
            "            if (child instanceof ViewGroup) {\n" +
            "                traverse((ViewGroup) child);\n" +
            "            }else{\n" +
            "                processor.process(child,bundle);\n" +
            "            }\n" +
            "        }\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    public static int getBundleSizeInBytes(Bundle bundle) {\n" +
            "        Parcel parcel = Parcel.obtain();\n" +
            "        int size;\n" +
            "\n" +
            "        parcel.writeBundle(bundle);\n" +
            "        size = parcel.dataSize();\n" +
            "        parcel.recycle();\n" +
            "\n" +
            "        return size;\n" +
            "    }\n" +
            "}";
    public static final String SAVE_VIEWS = "android.view.ViewGroup root = (android.view.ViewGroup) findViewById(android.R.id.content);\n" +
            "        " + DUMMY_PACKAGE_NAME + ".LayoutTraverser.build(new " + DUMMY_PACKAGE_NAME + ".LayoutTraverser.Processor() {\n" +
            "            @Override\n" +
            "            public void process(View view, Bundle bundle) {\n" +
            "                try {\n" +
            "                    Object onSaveInstanceState = view.getClass().getMethod(\"onSaveInstanceState\").invoke(view);\n" +
            "                    android.os.Parcelable saveState = (android.os.Parcelable) onSaveInstanceState;\n" +
            "                    bundle.putParcelable(String.valueOf(view.getId()), saveState);\n" +
            "                } catch (IllegalAccessException e) {\n" +
            "                    e.printStackTrace();\n" +
            "                } catch (java.lang.reflect.InvocationTargetException e) {\n" +
            "                    e.printStackTrace();\n" +
            "                } catch (NoSuchMethodException e) {\n" +
            "                    e.printStackTrace();\n" +
            "                }\n" +
            "\n" +
            "            }\n" +
            "        }, " + saveCallbackParameter + ").traverse(root);";
    public static final String RESTORE_VIEW = "android.view.ViewGroup root = (android.view.ViewGroup) findViewById(android.R.id.content);\n" +
            "        " + DUMMY_PACKAGE_NAME + ".LayoutTraverser.build(new " + DUMMY_PACKAGE_NAME + ".LayoutTraverser.Processor() {\n" +
            "            @Override\n" +
            "            public void process(View view, Bundle bundle) {\n" +
            "                try {\n" +
            "                    android.os.Parcelable viewState = bundle.getParcelable(String.valueOf(view.getId()));\n" +
            "                    if(viewState!=null){\n" +
            "\n" +
            "                        view.getClass().getMethod(\"onRestoreInstanceState\", android.os.Parcelable.class).invoke(view, viewState);\n" +
            "                    }\n" +
            "\n" +
            "                } catch (IllegalAccessException e) {\n" +
            "                    e.printStackTrace();\n" +
            "                } catch (java.lang.reflect.InvocationTargetException e) {\n" +
            "                    e.printStackTrace();\n" +
            "                } catch (NoSuchMethodException e) {\n" +
            "                    e.printStackTrace();\n" +
            "                }\n" +
            "\n" +
            "            }\n" +
            "        }, "+restoreCallbackParameter+").traverse(root);";
    public static final boolean DEBUG = false;

    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static void showMessage(String message) {
        if (DEBUG)
            Messages.showMessageDialog(message, "Title", Messages.getErrorIcon());
    }

    public static boolean isActivity(PsiClass psiClass) {
        if (psiClass == null || psiClass.getSuperClass() == null)
            return false;
        if (psiClass.getSuperClass().getName().equalsIgnoreCase("Activity")) {
            return true;
        } else if (psiClass.getSuperClass().getName().equalsIgnoreCase("Object")) {
            return false;
        } else {
            return isActivity(psiClass.getSuperClass());
        }
    }

//    public static boolean isView(PsiClass androidView, PsiClass psiClass) {
//        if (psiClass == null || psiClass.getSuperClass() == null)
//            return false;
//        if (psiClass.getSuperClass().equals(androidView)) {
//            return true;
//        } else if (psiClass.getSuperClass().getName().equalsIgnoreCase("Object")) {
//            return false;
//        } else {
//            return isView(androidView, psiClass.getSuperClass());
//        }
//    }


    public static boolean isView(PsiClass androidView, PsiClass psiClass, PsiClass javaObject) {
        if (psiClass == null || psiClass.getSuperClass() == null)
            return false;
        if(psiClass.getQualifiedName().startsWith("android.view")){
            return true;
        }
        if (psiClass.getSuperClass().equals(androidView)) {
            return true;
        } else if (psiClass.getSuperClass().equals(javaObject)) {
            return false;
        } else {
            return isView(androidView, psiClass.getSuperClass(), javaObject);
        }
    }

    /**
     * @param e the action event that occurred
     * @return The PSIClass object based on which class your mouse cursor was in
     */
    public static PsiClass getPsiClassFromContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        if (psiFile == null || editor == null) {
            return null;
        }

        int offSet = editor.getCaretModel().getOffset();
        PsiElement elementAt = psiFile.findElementAt(offSet);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);

        return psiClass;
    }

    public static Optional<PsiClass> resolveFieldType(PsiField psiField) {
        if (psiField != null) {
            PsiType fieldType = psiField.getType();
            if (fieldType instanceof PsiClassType) {
                PsiClassType fieldClassType = ((PsiClassType) fieldType);
                if (fieldClassType.getParameterCount() == 1) {
                    return toPsiClass(fieldClassType.getParameters()[0]);
                } else if (fieldClassType.getParameterCount() == 2) {
                    return toPsiClass(fieldClassType.getParameters()[1]);
                } else {
                    return toPsiClass(fieldType);
                }
            } else if (fieldType instanceof PsiArrayType) {
                return toPsiClass(((PsiArrayType) fieldType).getComponentType());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private static Optional<PsiClass> toPsiClass(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) psiType).resolve();
            if (psiClass != null && !psiClass.isEnum()) {
                return Optional.of(psiClass);
            }
        }
        return Optional.empty();
    }

    public static File browseFile() {
        JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jfc.setDialogTitle("Select Static Analysis Result");
        jfc.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("XML static analysis results", "xml");
        jfc.addChoosableFileFilter(filter);

        int returnValue = jfc.showOpenDialog(null);
        // int returnValue = jfc.showSaveDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            return selectedFile;
            //System.out.println(selectedFile.getAbsolutePath());
        }
        return null;
    }

    public static PsiClass findClass(PsiManager manager, String className) {
        PsiClass objectClass = ClassUtil.findPsiClass(manager, className);
        return objectClass;
    }

    public static ApplicationAnalysisResult parseXMLResults(File xmlFile, PsiManager psiManager) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        Element appElement = doc.getDocumentElement();
        String packageName = appElement.getAttribute("packageName");
        String version = appElement.getAttribute("version");
        ApplicationAnalysisResult applicationAnalysisResult = new ApplicationAnalysisResult(packageName, version, psiManager);

        Node activitiesNode = appElement.getElementsByTagName("Activities").item(0);
        NodeList activityNodeList = activitiesNode.getChildNodes();
        applicationAnalysisResult.parseAndSetActivities(activityNodeList);
        return applicationAnalysisResult;
    }

    public static void importClass(Project project, PsiClass psiClass, PsiClass importClass) {

        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiFile psiFile = psiClass.getContainingFile();
            if (psiFile.isWritable()) {
                PsiJavaFile psiClassContainingFile = (PsiJavaFile) psiFile;
                if (psiClassContainingFile != null) {
                    psiClassContainingFile.importClass(importClass);
                }
            }
        });
    }

    public static String variableNameForView(ViewResult viewResult) {
        if (viewResult.getName().isEmpty()) {
            String simpleClassName = viewResult.getViewType().substring(viewResult.getViewType().lastIndexOf('.') + 1);
            return simpleClassName.toLowerCase() + viewResult.getId();
        }
        return viewResult.getName() + "" + viewResult.getId();
    }

    /**
     * @Override protected void onSaveInstanceState(Bundle outState) {
     * super.onSaveInstanceState(outState);
     * }
     * @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
     * super.onRestoreInstanceState(savedInstanceState);
     * }
     */
    public static void insertSaveRestoreMethods(Project project, PsiClass psiClass, String saveStatements, String restoreStatements) {
        final String bundleType = "android.os.Bundle";
        final String saveInstanceMethodName = "onSaveInstanceState";
        final String restoreInstanceMethodName = "onRestoreInstanceState";

        PsiMethod existingOnSave = getVoidSingleParamMethod(psiClass, saveInstanceMethodName, bundleType);
        PsiMethod existingOnRestore = getVoidSingleParamMethod(psiClass, restoreInstanceMethodName, bundleType);
        if (existingOnSave != null) {
            String methodBody = getCallbackStatements(existingOnSave);
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (psiClass.getContainingFile().isWritable()) {
                    existingOnSave.delete();
                    PsiMethod callback = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                            "protected void onSaveInstanceState(Bundle " + saveCallbackParameter + ") {\n" +
                                    methodBody + "\n" +
                                    saveStatements +
                                    "\n}", psiClass);
                    psiClass.addBefore(callback, psiClass.getRBrace());
                }
            });
        } else {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (psiClass.getContainingFile().isWritable()) {
                    PsiMethod callback = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                            "protected void onSaveInstanceState(Bundle " + saveCallbackParameter + ") {\n" +
                                    "        super.onSaveInstanceState(outState);\n" +
                                    saveStatements +
                                    "\n}", psiClass);
                    psiClass.addBefore(callback, psiClass.getRBrace());
                }
            });
        }

        if (existingOnRestore != null) {
            String methodBody = getCallbackStatements(existingOnRestore);
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (psiClass.getContainingFile().isWritable()) {
                    existingOnRestore.delete();
                    PsiMethod callback = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                            "protected void onRestoreInstanceState(Bundle " + restoreCallbackParameter + ") {\n" +
                                    methodBody + "\n" +
                                    restoreStatements +
                                    "\n}", psiClass);
                    psiClass.addBefore(callback, psiClass.getRBrace());
                }
            });
        } else {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (psiClass.getContainingFile().isWritable()) {
                    PsiMethod callback = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                            "protected void onRestoreInstanceState(Bundle " + restoreCallbackParameter + ") {\n" +
                                    "        super.onRestoreInstanceState(savedInstanceState);\n" +
                                    restoreStatements +
                                    "\n}", psiClass);
                    psiClass.addBefore(callback, psiClass.getRBrace());
                }
            });
        }
    }

    public static PsiMethod getSetter(PsiField currField) {
        PsiMethod existingSetter = getVoidSingleParamMethod(currField.getContainingClass(), getSetterName(currField), currField.getType().getCanonicalText());
        return existingSetter;
    }

    public static PsiMethod getGetter(PsiField currField) {
        PsiMethod existingGetter = getReturnedNoParamMethod(currField.getContainingClass(), getGetterName(currField), currField.getType().getCanonicalText());
        return existingGetter;
    }

    public static void generateGetterAndSetterIfNotExist(Project project, PsiField currField) {
        PsiMethod existingSetter = getSetter(currField);
        PsiMethod existingGetter = getGetter(currField);
        if (existingGetter == null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (currField.getContainingClass().getContainingFile().isWritable()) {
                    PsiMethod callback = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                            "public " + currField.getType().getCanonicalText() + " " + getGetterName(currField) + "() {\n" +
                                    "return this." + currField.getName() + ";" +
                                    "\n}", currField.getContainingClass());
                    currField.getContainingClass().addBefore(callback, currField.getContainingClass().getRBrace());
                }
            });
        }
        if (existingSetter == null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (currField.getContainingClass().getContainingFile().isWritable()) {
                    PsiMethod callback = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                            "public void " + getSetterName(currField) + "(" + currField.getType().getCanonicalText() + " " + currField.getName() + ") {\n" +
                                    "this." + currField.getName() + "=" + currField.getName() + ";" +
                                    "\n}", currField.getContainingClass());
                    currField.getContainingClass().addBefore(callback, currField.getContainingClass().getRBrace());
                }
            });
        }

    }


    public static PsiMethod getVoidSingleParamMethod(PsiClass psiClass, String methodName, String paramType) {
        PsiElement[] methods = PsiTreeUtil.collectElements(psiClass, new PsiElementFilter() {
            public boolean isAccepted(PsiElement e) {
                if (e instanceof PsiMethod) {
                    PsiMethod method = (PsiMethod) e;
                    if (method.getName().equals(methodName) && method.getReturnType().getCanonicalText().equals("void") && method.getParameterList().getParameters().length > 0) {
                        String type = method.getParameterList().getParameters()[0].getType().getCanonicalText();
                        if (type.equals(paramType)) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
        });
        if (methods != null && methods.length > 0) {
            PsiMethod method = (PsiMethod) methods[0];
            return method;
        } else
            return null;
    }

    public static void generateDummyConstructIfNotExist(Project project, PsiManager psiManager, SourceProvider provider, PsiClass psiClass) {

        PsiMethod dummyConstructor = getDummyConstructor(psiClass);
        if (dummyConstructor == null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (dummyConstructor == null) {
                    PsiClass dummyClass = Utils.findClass(psiManager, DUMMY_PACKAGE_NAME + "." + DUMMY_CLASS_NAME);
                    if (dummyClass == null) {
                        PsiDirectory dummyDirectory = createPackage(project, provider, DUMMY_PACKAGE_NAME);
                        PsiFile configElement = PsiFileFactory.getInstance(project).createFileFromText(DUMMY_CLASS_NAME + ".java", JavaFileType.INSTANCE, DUMMY_CLASS_DEF);
                        PsiElement createdDummyClass = dummyDirectory.add(configElement);
                    }
                }

                final String constructorBody = "public " + psiClass.getName() + "(" + DUMMY_PACKAGE_NAME + "." + DUMMY_CLASS_NAME + " dummy){\n" +
                        " \n" +
                        "}";
                final String defaultconstructorBody = "public " + psiClass.getName() + "(){\n" +
                        " \n" +
                        "}";
                if (psiClass.getConstructors().length == 0) {
                    //Add default constructor to avoid breaking code
                    PsiMethod defaultConstruct = JavaPsiFacade.getElementFactory(project).createMethodFromText(defaultconstructorBody, psiClass);
                    psiClass.addBefore(defaultConstruct, psiClass.getRBrace());
                }
                PsiMethod dummyConstruct = JavaPsiFacade.getElementFactory(project).createMethodFromText(constructorBody, psiClass);
                psiClass.addBefore(dummyConstruct, psiClass.getRBrace());

            });
        }
    }

    public static PsiMethod getReturnedNoParamMethod(PsiClass psiClass, String methodName, String returnType) {
        PsiElement[] methods = PsiTreeUtil.collectElements(psiClass, new PsiElementFilter() {
            public boolean isAccepted(PsiElement e) {
                if (e instanceof PsiMethod) {
                    PsiMethod method = (PsiMethod) e;
                    if (method.getName().equals(methodName) && method.getReturnType().getCanonicalText().equals(returnType) && method.getParameterList().getParameters().length == 0) {
                        return true;
                    }
                    return false;
                }
                return false;
            }
        });
        if (methods != null && methods.length > 0) {
            PsiMethod method = (PsiMethod) methods[0];
            return method;
        } else
            return null;
    }

    public static PsiMethod getDummyConstructor(PsiClass psiClass) {
        PsiMethod[] constructors = psiClass.getConstructors();

        for (int i = 0; i < constructors.length; i++) {
            PsiMethod constructor = constructors[i];
            if (constructor.getParameterList().getParametersCount() == 1) {
                PsiParameter firstParameter = constructor.getParameterList().getParameters()[0];
                if (firstParameter.getType().getCanonicalText().equals(DUMMY_PACKAGE_NAME + "." + DUMMY_CLASS_NAME))
                    return constructor;
            }
        }
        return null;

    }

    public static PsiDirectory createPackage(Project project, SourceProvider provider, String qualifiedPackage)
            throws IncorrectOperationException {
        PsiDirectory result = null;
        File packageDir = provider.getJavaDirectories().iterator().next();

        VirtualFile javaVFile =
                LocalFileSystem.getInstance().findFileByIoFile(packageDir);
        PsiDirectory parent = PsiManager.getInstance(project).findDirectory(javaVFile);

        StringTokenizer token = new StringTokenizer(qualifiedPackage, ".");
        while (token.hasMoreTokens()) {
            String dirName = token.nextToken();
            parent = createDirectory(parent, dirName);
        }
        return parent;
    }

    public static PsiDirectory createDirectory(PsiDirectory parent, String name)
            throws IncorrectOperationException {
        PsiDirectory result = null;

        for (PsiDirectory dir : parent.getSubdirectories()) {
            if (dir.getName().equalsIgnoreCase(name)) {
                result = dir;
                break;
            }
        }

        if (null == result) {
            result = parent.createSubdirectory(name);
        }

        return result;
    }

    private static String getCallbackStatements(PsiMethod method) {
        if (method != null) {
            String callbackBody = "";
            PsiStatement[] callbackStatements = method.getBody().getStatements();
            for (PsiStatement statement : callbackStatements) {
                callbackBody = callbackBody + statement.getText() + "\n";
            }
            return callbackBody;
        }
        return "";
    }

    public static String extractMethodName(String method) {
        String[] returnAndName = method.split("\\s");
        return returnAndName[1].split("\\(")[0];
    }

    public static String extractMethodReturn(String method) {
        String[] returnAndName = method.split("\\s");
        return returnAndName[0];
    }

    public static String getSetterName(PsiField field) {
        return PropertyUtil.suggestSetterName(field);
        //return "set" + capitalizeFirstLetter(field.getName());
    }

    public static String getGetterName(PsiField field) {
        return PropertyUtil.suggestGetterName(field);
        //return "get" + capitalizeFirstLetter(field.getName());
    }

    public static String capitalizeFirstLetter(String original) {
        return StringUtils.capitalize(original);
//        if (original == null || original.length() == 0) {
//            return original;
//        }
//        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }
}
