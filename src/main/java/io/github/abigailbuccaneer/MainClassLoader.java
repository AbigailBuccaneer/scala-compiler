package io.github.abigailbuccaneer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;

class MainClassLoader extends URLClassLoader {
    MainClassLoader(URL[] urls) {
        super(urls);
    }

    void invokeMain(String mainClass, String[] args) throws InvocationTargetException, IllegalAccessException {
        Class<?> clazz = null;
        try {
            clazz = loadClass(mainClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class " + mainClass + " not found", e);
        }
        Method method = null;
        try {
            method = clazz.getMethod("main", args.getClass());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + mainClass + " has no main method", e);
        }
        if (method.getReturnType() != void.class || !Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("main method on class " + mainClass + " has incorrect signature");
        }

        StringBuilder command = new StringBuilder(mainClass);
        for (String argument : args) {
            command.append(' ').append(argument);
        }
        System.err.println(command);

        method.invoke(null, new Object[] { args });
    }
}
