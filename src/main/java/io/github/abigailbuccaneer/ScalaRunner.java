package io.github.abigailbuccaneer;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

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
        try {
            new MainClassLoader(urls).invokeMain("scala.tools.nsc.MainGenericRunner", args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
