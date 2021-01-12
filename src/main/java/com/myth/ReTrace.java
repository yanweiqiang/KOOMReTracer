package com.myth;

import proguard.obfuscate.MappingProcessor;
import proguard.obfuscate.MappingReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReTrace implements MappingProcessor {
    private static final String REGEX_OPTION = "-regex";
    private static final String VERBOSE_OPTION = "-verbose";

    public static final String STACK_TRACE_EXPRESSION = "(?:.*?\\bat\\s+%c\\.%m\\s*\\(.*?(?::%l)?\\)\\s*)|(?:(?:.*?[:\"]\\s+)?%c(?::.*)?)";

    private static final String REGEX_CLASS = "\\b(?:[A-Za-z0-9_$]+\\.)*[A-Za-z0-9_$]+\\b";
    private static final String REGEX_CLASS_SLASH = "\\b(?:[A-Za-z0-9_$]+/)*[A-Za-z0-9_$]+\\b";
    private static final String REGEX_LINE_NUMBER = "\\b[0-9]+\\b";
    private static final String REGEX_TYPE = REGEX_CLASS + "(?:\\[\\])*";
    private static final String REGEX_MEMBER = "<?\\b[A-Za-z0-9_$]+\\b>?";
    private static final String REGEX_ARGUMENTS = "(?:" + REGEX_TYPE + "(?:\\s*,\\s*" + REGEX_TYPE + ")*)?";

    private Map classMap = new HashMap();
    private Map classFieldMap = new HashMap();
    private Map classMethodMap = new HashMap();

    public void pump(String mappingFilePath) {
        MappingReader reader = new MappingReader(new File(mappingFilePath));
        try {
            reader.pump(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(classMap.toString());
        //System.out.println(classFieldMap.toString());
    }

    @Override
    public boolean processClassMapping(String className, String newClassName) {
        // Obfuscated class name -> original class name.
        classMap.put(newClassName, className);
        return true;
    }

    @Override
    public void processFieldMapping(String className, String fieldType, String fieldName, String newFieldName) {
        // Original class name -> obfuscated field names.
        Map fieldMap = (Map) classFieldMap.get(className);
        if (fieldMap == null) {
            fieldMap = new HashMap();
            classFieldMap.put(className, fieldMap);
        }

        // Obfuscated field name -> fields.
        Set fieldSet = (Set) fieldMap.get(newFieldName);
        if (fieldSet == null) {
            fieldSet = new LinkedHashSet();
            fieldMap.put(newFieldName, fieldSet);
        }

        // Add the field information.
        fieldSet.add(new FieldInfo(fieldType,
                fieldName));
    }

    @Override
    public void processMethodMapping(String className, int firstLineNumber, int lastLineNumber, String methodReturnType, String methodName, String methodArguments, String newMethodName) {
        // Original class name -> obfuscated method names.
        Map methodMap = (Map) classMethodMap.get(className);
        if (methodMap == null) {
            methodMap = new HashMap();
            classMethodMap.put(className, methodMap);
        }

        // Obfuscated method name -> methods.
        Set methodSet = (Set) methodMap.get(newMethodName);
        if (methodSet == null) {
            methodSet = new LinkedHashSet();
            methodMap.put(newMethodName, methodSet);
        }

        // Add the method information.
        methodSet.add(new MethodInfo(firstLineNumber,
                lastLineNumber,
                methodReturnType,
                methodArguments,
                methodName));
    }

    public String getClassName(String pClassName) {
        return (String) classMap.get(pClassName);
    }

    public String getFieldName(String className, String pFieldName) {
        Map fieldMap = (Map) classFieldMap.get(className);

        if (fieldMap == null || fieldMap.isEmpty()) {
            return null;
        }

        Set fieldSet = (Set) fieldMap.get(pFieldName);

        if (fieldSet == null || fieldSet.isEmpty()) {
            return null;
        }

        Object[] fieldArr = new Object[fieldSet.size()];
        fieldSet.toArray(fieldArr);
        FieldInfo fieldInfo = (FieldInfo) fieldArr[0];
        return fieldInfo.originalName;
    }

    /**
     * A field record.
     */
    private static class FieldInfo {
        private String type;
        private String originalName;


        private FieldInfo(String type, String originalName) {
            this.type = type;
            this.originalName = originalName;
        }


        private boolean matches(String type) {
            return
                    type == null || type.equals(this.type);
        }
    }

    /**
     * A method record.
     */
    private static class MethodInfo {
        private int firstLineNumber;
        private int lastLineNumber;
        private String type;
        private String arguments;
        private String originalName;


        private MethodInfo(int firstLineNumber, int lastLineNumber, String type, String arguments, String originalName) {
            this.firstLineNumber = firstLineNumber;
            this.lastLineNumber = lastLineNumber;
            this.type = type;
            this.arguments = arguments;
            this.originalName = originalName;
        }


        private boolean matches(int lineNumber, String type, String arguments) {
            return
                    (lineNumber == 0 || (firstLineNumber <= lineNumber && lineNumber <= lastLineNumber) || lastLineNumber == 0) &&
                            (type == null || type.equals(this.type)) &&
                            (arguments == null || arguments.equals(this.arguments));
        }
    }
}
