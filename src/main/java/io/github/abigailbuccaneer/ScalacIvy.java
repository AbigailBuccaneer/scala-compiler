package io.github.abigailbuccaneer;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.FilterHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.*;

public class ScalacIvy {
    private static Map<String, File> resolveArtifacts(Ivy ivy, ModuleRevisionId mrid, ResolveOptions options) throws IOException, ParseException {
        ResolveReport report = ivy.resolve(mrid, options, false);
        if (report.hasError()) {
            throw new IOException("resolve error");
        }
        if (report.getConfigurationReport("default").getFailedArtifactsReports().length > 0) {
            throw new IOException("couldn't download an artifact");
        }
        Map<String, File> result = new HashMap<>();
        for (ArtifactDownloadReport artifactDownloadReport : report.getConfigurationReport("default").getAllArtifactsReports()) {
            result.put(artifactDownloadReport.getName(), artifactDownloadReport.getLocalFile());
        }
        return result;
    }

    private static void invokeClass(URL[] classpath, String mainClass, String[] args) throws Throwable {
        URLClassLoader loader = new URLClassLoader(classpath);
        Class<?> clazz = null;
        try {
            clazz = loader.loadClass(mainClass);
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
        try {
            method.invoke(null, new Object[] { args });
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public static void main(String[] args) throws Throwable {
        String scalacRevision = System.getProperty("scala.version", "2.9.3");
        IvySettings ivySettings = new IvySettings();
        Ivy ivy = Ivy.newInstance(ivySettings);
        ResolveOptions resolveOptions = new ResolveOptions()
                .setConfs(new String[]{"default"})
                .setArtifactFilter(FilterHelper.getArtifactTypeFilter(new String[]{"jar"}))
                .setOutputReport(false);
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_WARN));
        try {
            ivy.configureDefault();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ModuleRevisionId zinc = new ModuleRevisionId(new ModuleId("com.typesafe.zinc", "zinc"), "0.3.13");
        ModuleRevisionId scalac = new ModuleRevisionId(new ModuleId("org.scala-lang", "scala-compiler"), scalacRevision);

        Map<String, File> zincArtifacts = resolveArtifacts(ivy, zinc, resolveOptions);
        Map<String, File> scalacArtifacts = resolveArtifacts(ivy, scalac, resolveOptions);

        URL[] zincClasspath = new URL[zincArtifacts.size()];
        int i = 0;
        for (Map.Entry<String, File> artifact : zincArtifacts.entrySet()) {
            zincClasspath[i++] = artifact.getValue().toURI().toURL();
        }

        StringBuilder scalaPath = new StringBuilder();
        String delimeter = "";
        for (Map.Entry<String, File> artifact : scalacArtifacts.entrySet()) {
            scalaPath.append(delimeter).append(artifact.getValue().getCanonicalPath());
            delimeter = File.pathSeparator;
        }

        List<String> arguments = new ArrayList<>();
        arguments.add("-scala-path");
        arguments.add(scalaPath.toString());
        arguments.add("-sbt-interface");
        arguments.add(zincArtifacts.get("sbt-interface").getCanonicalPath());
        arguments.add("-compiler-interface");
        arguments.add(zincArtifacts.get("compiler-interface").getCanonicalPath());

        ListIterator<String> argIter = Arrays.asList(args).listIterator();
        while (argIter.hasNext()) {
            String arg = argIter.next();
            if (arg.startsWith("-Z")) {
                arguments.add(arg.substring(2));
            }
            else if (arg.equals("-classpath") || arg.equals("-cp") || arg.equals("-d")) {
                arguments.add(arg);
                arguments.add(argIter.next());
            }
            else if (arg.startsWith("-")) {
                arguments.add("-S" + arg);
            }
            else {
                arguments.add(arg);
            }
        }

        System.out.print("com.typesafe.zinc.Main");
        for (String argument : arguments) {
            System.out.print(' ');
            System.out.print(argument);
        }
        System.out.println();

        invokeClass(zincClasspath, "com.typesafe.zinc.Main", arguments.toArray(new String[]{}));
        System.exit(0);
    }
}
