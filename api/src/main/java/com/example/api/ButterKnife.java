package com.example.api;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * ButterKnife工具类
 * Created by DavidChen on 2017/7/26.
 */

public class ButterKnife {
    public static void bind(Activity target) {
        View sourceView = target.getWindow().getDecorView();
        createBinding(target, sourceView);
    }

    private static void createBinding(Activity target, View source) {
        Class<?> targetClass = target.getClass();
        String className = targetClass.getName();
        try {
            Class<?> bindingClass = targetClass.getClassLoader().loadClass(className + "_ViewBinding");
            Constructor constructor = bindingClass.getConstructor(targetClass, View.class);
            constructor.newInstance(target, source);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
