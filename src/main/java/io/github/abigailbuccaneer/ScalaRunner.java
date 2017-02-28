package io.github.abigailbuccaneer;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScalaRunner {
    public static void main(String[] args) {
        Message.setDefaultLogger(new DefaultMessageLogger(Message.MSG_WARN));
        ArtifactDownloadReport[] scalacArtifacts = ScalaIvyResolver.resolve();
        if (scalacArtifacts == null) {
            System.exit(1);
        }
        URL[] urls = new URL[scalacArtifacts.length];
        for (int i = 0; i < scalacArtifacts.length; ++i) {
            try {
                urls[i] = scalacArtifacts[i].getLocalFile().toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        List<String> actualArgs = new ArrayList<>(args.length + 2);
        actualArgs.add("-bootclasspath");
        StringBuilder bootClasspath = new StringBuilder();
        String delimeter = "";
        for (URL url : urls) {
            try {
                bootClasspath.append(delimeter).append(new File(url.toURI()).getCanonicalPath());
                delimeter = File.pathSeparator;
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
        actualArgs.add(bootClasspath.toString());

        actualArgs.addAll(Arrays.asList(args));

        try {
            new MainClassLoader(urls).invokeMain("scala.tools.nsc.MainGenericRunner", actualArgs.toArray(new String[]{}));
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
