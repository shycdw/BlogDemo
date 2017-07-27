package com.example;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class ButterKnifeProcessor extends AbstractProcessor {

    private Filer mFiler;   // 用于生成java文件

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotationTypes = new LinkedHashSet<>();
        annotationTypes.add(BindView.class.getCanonicalName());
        return annotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    // 用于存放类与该类包含的注解集合
    private Map<String, InjectorInfo> injectorInfoMap = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindView.class);
        // 遍历所有的BindView注解。
        for (Element element : elements) {
            parseBindView(element);
        }

        Set<? extends Element> eventElements = roundEnv.getElementsAnnotatedWith(OnClick.class);
        // 遍历获取所有OnClick注解
        for (Element element : eventElements) {
            parseOnClick(element);
        }

        // 生成类文件

        for (String qualifiedName : injectorInfoMap.keySet()) {
            InjectorInfo injectorInfo = injectorInfoMap.get(qualifiedName);
            /*try {
                JavaFileObject sourceFile = mFiler.createSourceFile(qualifiedName + "_ViewBinding", injectorInfo.getTypeElement());
                Writer writer = sourceFile.openWriter();
                writer.write(injectorInfo.generateCode());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            JavaFile javaFile = injectorInfo.brewJava();
            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void parseOnClick(Element element) {
        ExecutableElement executableElement = (ExecutableElement) element;
        TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
        String qualifiedName = typeElement.getQualifiedName().toString();
        InjectorInfo injectorInfo = injectorInfoMap.get(qualifiedName);
        if (injectorInfo == null) {
            injectorInfo = new InjectorInfo(typeElement);
            injectorInfoMap.put(qualifiedName, injectorInfo);
        }
        OnClick onClick = executableElement.getAnnotation(OnClick.class);
        if (onClick != null) {
            int[] idInt = onClick.value();
            Integer ids[] = new Integer[idInt.length];
            for (int i = 0; i < idInt.length; i++) {
                ids[i] = idInt[i];
            }
            injectorInfo.executableElementMap.put(ids, executableElement);
        }
    }

    private void parseBindView(Element element) {
        VariableElement variableElement = (VariableElement) element;
        TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
        String qualifiedName = typeElement.getQualifiedName().toString();
        InjectorInfo injectorInfo = injectorInfoMap.get(qualifiedName);
        if (injectorInfo == null) {
            injectorInfo = new InjectorInfo(typeElement);
            injectorInfoMap.put(qualifiedName, injectorInfo);
        }
        BindView bindView = variableElement.getAnnotation(BindView.class);
        if (bindView != null) {
            int id = bindView.value();
            injectorInfo.variableElementMap.put(id, variableElement);
        }
    }
}
