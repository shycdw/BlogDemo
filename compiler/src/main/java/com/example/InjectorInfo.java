package com.example;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import static javax.lang.model.element.ElementKind.PACKAGE;

/**
 * 要生成的注入类相关信息
 * Created by DavidChen on 2017/7/26.
 */

public class InjectorInfo {
    private static final String SUFFIX = "_ViewBinding";
    private static final String VIEW_TYPE = "android.view.View";

    private static final ClassName ONCLICK = ClassName.get("android.view.View", "OnClickListener");
    private static final ClassName VIEW = ClassName.get("android.view", "View");

    private String mPackageName;
    private String mInjectorName;
    private TypeElement mTypeElement;
    private ClassName mInjectorClassName;
    private ClassName mOriginClassName;

    Map<Integer, VariableElement> variableElementMap = new HashMap<>();
    Map<Integer[], ExecutableElement> executableElementMap = new HashMap<>();

    InjectorInfo(TypeElement typeElement) {
        this.mTypeElement = typeElement;
        this.mPackageName = getPackage(mTypeElement).getQualifiedName().toString();
        this.mInjectorName = generateClassName(mPackageName, mTypeElement);
        mInjectorClassName = ClassName.get(mPackageName, typeElement.getSimpleName() + SUFFIX);
        mOriginClassName = ClassName.get(mPackageName, typeElement.getSimpleName().toString());
    }

    TypeElement getTypeElement() {
        return mTypeElement;
    }

    String generateCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code from Butter Knife. Do not modify!\n");
        builder.append("package ").append(mPackageName).append(";\n\n");
        builder.append("import ").append(VIEW_TYPE).append(";\n");
        builder.append("\n");
        builder.append("public class ").append(mInjectorName).append(" implements").append(" View.OnClickListener").append(" {\n");
        builder.append("private ").append(mTypeElement.getQualifiedName()).append(" target;\n");
        generateMethods(builder);
        builder.append("\n");
        implementsEvents(builder);
        builder.append("\n");
        builder.append(" }\n");
        return builder.toString();
    }

    private void implementsEvents(StringBuilder builder) {
        builder.append("public void onClick(View v) {\n");
        builder.append("switch(v.getId()) {\n");

        for (Integer[] ids : executableElementMap.keySet()) {
            ExecutableElement executableElement = executableElementMap.get(ids);
            for (int id : ids) {
                builder.append("case ").append(id).append(":\n");
            }
            builder.append("target.").append(executableElement.getSimpleName()).append("(v);\n");
            builder.append("break;\n");
        }

        builder.append("}\n");
        builder.append("}\n");
    }


    private void generateMethods(StringBuilder builder) {
        builder.append("public ").append(mInjectorName).append("(")
                .append(mTypeElement.getQualifiedName()).append(" target, ");
        builder.append("View source) {\n");

        builder.append("this.target = target;\n");

        for (int id : variableElementMap.keySet()) {
            VariableElement variableElement = variableElementMap.get(id);
            TypeMirror typeMirror = variableElement.asType();
            String type = typeMirror.toString();
            String name = variableElement.getSimpleName().toString();
            builder.append("target.").append(name).append(" = ");
            builder.append("(").append(type).append(")");
            builder.append("source.findViewById(");
            builder.append(id).append(");\n");
        }

        for (Integer[] ids : executableElementMap.keySet()) {
            for (int id : ids) {
                builder.append("source.findViewById(").append(id).append(").setOnClickListener(this);\n");
            }
        }

        builder.append(" }\n");
    }

    /**
     * 生成注入类文件名
     *
     * @param packageName 包名
     * @param typeElement 类元素
     * @return 注入类类名，如，MainActivity.java 生成类名为MainActivity_ViewBinding.java
     */
    private static String generateClassName(String packageName, TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString().substring(
                packageName.length() + 1).replace('.', '$');
        return className + SUFFIX;
    }

    /**
     * 获取PackageElement
     *
     * @throws NullPointerException 如果element为null
     */
    private static PackageElement getPackage(Element element) {
        while (element.getKind() != PACKAGE) {
            element = element.getEnclosingElement();
        }
        return (PackageElement) element;
    }

    JavaFile brewJava() {
        return JavaFile.builder(mPackageName, createType())
                .addFileComment("Generated code from Butter Knife. Do not modify!")
                .build();
    }

    private TypeSpec createType() {
        // 类
        TypeSpec.Builder builder = TypeSpec.classBuilder(mInjectorClassName.simpleName())
                .addModifiers(Modifier.PUBLIC);
        // 接口
        builder.addSuperinterface(ONCLICK);
        builder.addField(generateTarget());
        builder.addMethod(generateConstructor());
        builder.addMethod(generateEvent());
        //

        return builder.build();
    }

    private MethodSpec generateEvent() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("onClick")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(VIEW, "v")
                .returns(void.class);
        builder.beginControlFlow("switch(v.getId())");
        for (Integer[] ints : executableElementMap.keySet()) {
            ExecutableElement executableElement = executableElementMap.get(ints);
            CodeBlock.Builder code = CodeBlock.builder();
            for (int id : ints) {
                code.add("case $L:\n", id);
            }
            code.add("target.$L(v)", executableElement.getSimpleName());
            builder.addStatement("$L", code.build());
            builder.addStatement("break");
        }
        builder.endControlFlow();
        return builder.build();
    }

    private FieldSpec generateTarget() {
        FieldSpec.Builder builder = FieldSpec.builder(mOriginClassName, "target", Modifier.PRIVATE);
        return builder.build();
    }

    private MethodSpec generateConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mOriginClassName, "target")
                .addParameter(VIEW, "source");
        builder.addStatement("this.target = target");
        for (int id : variableElementMap.keySet()) {
            CodeBlock.Builder code = CodeBlock.builder();
            VariableElement variableElement = variableElementMap.get(id);
            ClassName className = getClassName(variableElement);
            code.add("target.$L = ", variableElement.getSimpleName());
            code.add("($T)source.findViewById($L)", className, id);
            builder.addStatement("$L", code.build());
        }
        for (Integer[] ints : executableElementMap.keySet()) {
            for (int id : ints) {
                builder.addStatement("source.findViewById($L).setOnClickListener(this)", id);
            }
        }
        return builder.build();
    }

    private ClassName getClassName(Element element) {
        TypeMirror elementType = element.asType();
        if (elementType.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = (TypeVariable) elementType;
            elementType = typeVariable.getUpperBound();
        }
        TypeName type = TypeName.get(elementType);
        if (type instanceof ParameterizedTypeName) {
            return ((ParameterizedTypeName) type).rawType;
        }
        return (ClassName) type;
    }
}
