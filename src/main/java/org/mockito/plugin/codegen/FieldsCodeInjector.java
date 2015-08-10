package org.mockito.plugin.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Inserts code with declaration of fields that can be auto-generated in a Mockito test:
 * - mocked fields
 * - subject of the test
 *
 * Mocked fields are inserted for each single non-primitive object defined in the tested class. So far the fields
 * declared in parent of the tested class are ignored. Example of the code generated for an object of type ClassName:
 * <code>
 *     @Mock
 *     private ClassName className;
 * </code>
 *
 * Field for a subject of the test has format:
 * <code>
 *     @InjectMocks
 *     private SubjectClassName underTest;
 * </code>
 *
 * Created by przemek on 8/9/15.
 */
public class FieldsCodeInjector implements CodeInjector {

    public static final String TEST_CLASS_NAME_SUFFIX = "Test";
    public static final String INJECT_MOCKS_CLASS_NAME = "InjectMocks";
    public static final String INJECT_MOCKS_ANNOTATION_QUALIFIED_NAME = "org.mockito.InjectMocks";
    public static final String MOCK_ANNOTATION_QUALIFIED_NAME = "org.mockito.Mock";
    public static final String MOCK_ANNOTATION_SHORT_NAME = "Mock";
    public static final String UNDER_TEST_FIELD_NAME = "underTest";

    private final PsiJavaFile psiJavaFile;
    private final Project project;
    private final JavaPsiFacade javaPsiFacade;
    private final ImportOrganizer importOrganizer;

    public FieldsCodeInjector(PsiJavaFile psiJavaFile, ImportOrganizer importOrganizer) {
        this.psiJavaFile = psiJavaFile;
        this.project = psiJavaFile.getProject();
        this.javaPsiFacade = JavaPsiFacade.getInstance(project);
        this.importOrganizer = importOrganizer;
    }

    @Override
    public void inject() {
        PsiClass psiClass = MockitoPluginUtils.getUnitTestClass(psiJavaFile);
        Set<String> existingFieldTypeNames = getFieldTypeNames(psiClass);

        String underTestQualifiedClassName = getUnderTestQualifiedClassName(psiClass);
        if (underTestQualifiedClassName == null) {
            return;
        }

        insertMockedFields(underTestQualifiedClassName, psiClass, existingFieldTypeNames);

        insertUnderTestField(psiClass, existingFieldTypeNames, underTestQualifiedClassName);
    }

    private void insertUnderTestField(PsiClass psiClass, Set<String> existingFieldTypeNames,
                                      String underTestQualifiedClassName) {
        if (!existingFieldTypeNames.contains(underTestQualifiedClassName)) {
            insertUnderTestField(psiClass, underTestQualifiedClassName);
            importOrganizer.addImport(psiJavaFile, INJECT_MOCKS_ANNOTATION_QUALIFIED_NAME);
        }
    }

    private void insertMockedFields(String underTestQualifiedClassName, PsiClass psiClass,
                                    Set<String> existingFieldTypeNames) {
        PsiClass underTestPsiClass = javaPsiFacade.findClass(
                underTestQualifiedClassName, GlobalSearchScope.allScope(project));
        if (underTestPsiClass == null) {
            return;
        }
        boolean addedMocks = false;
        for (PsiField psiField : underTestPsiClass.getFields()) {
            if (isNotPrimitive(psiField) &&
                    !existingFieldTypeNames.contains(psiField.getName())) {
                insertMockedField(psiClass, psiField.getType().getCanonicalText());
                addedMocks = true;
            }
        }
        if (addedMocks) {
            importOrganizer.addImport(psiJavaFile, MOCK_ANNOTATION_QUALIFIED_NAME);
        }
    }

    private boolean isNotPrimitive(PsiField psiField) {
        return !(psiField.getType() instanceof PsiPrimitiveType);
    }

    /**
     * Returns fully qualified names of the fields declared in the class, ignores fields inherited from parent classes.
     */
    @NotNull
    private Set<String> getFieldTypeNames(PsiClass psiClass) {
        Set<String> existingFieldTypeNames = new HashSet<>();
        for (PsiField psiField : psiClass.getFields()) {
            existingFieldTypeNames.add(psiField.getType().getCanonicalText());
        }
        return existingFieldTypeNames;
    }

    private void insertUnderTestField(PsiClass psiClass, String fullyQualifiedTypeName) {
        insertNewField(psiClass, fullyQualifiedTypeName, UNDER_TEST_FIELD_NAME, INJECT_MOCKS_CLASS_NAME);
    }

    private void insertMockedField(PsiClass psiClass, String fullyQualifiedTypeName) {
        String newFieldName = toShortClassName(fullyQualifiedTypeName);
        newFieldName = Character.toLowerCase(newFieldName.charAt(0)) +
                newFieldName.substring(1, newFieldName.length());

        insertNewField(psiClass, fullyQualifiedTypeName, newFieldName, MOCK_ANNOTATION_SHORT_NAME);
    }

    private void insertNewField(PsiClass psiClass, String newFieldTypeName, String newFieldName,
                                String annotationClassName) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClassType newFieldType = PsiType.getTypeByName(newFieldTypeName,
                project, GlobalSearchScope.projectScope(project));
        PsiField underTestField = javaPsiFacade.getElementFactory().createField(
                newFieldName, newFieldType);
        underTestField.getModifierList().addAnnotation(annotationClassName);
        psiClass.add(underTestField);
    }

    @NotNull
    private String toShortClassName(String qualifiedClassName) {
        return qualifiedClassName.substring(qualifiedClassName.lastIndexOf('.') + 1, qualifiedClassName.length());
    }

    private String getUnderTestQualifiedClassName(PsiClass psiClass) {
        String testClassName = psiClass.getQualifiedName();
        if (testClassName.endsWith(TEST_CLASS_NAME_SUFFIX)) {
            return testClassName.substring(0, testClassName.length() - TEST_CLASS_NAME_SUFFIX.length());
        }
        return null;
    }
}