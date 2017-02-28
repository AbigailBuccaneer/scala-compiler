package io.github.abigailbuccaneer;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.FilterHelper;

import java.io.IOException;
import java.text.ParseException;

class ScalaIvyResolver {
    static ArtifactDownloadReport[] resolve() {
        Ivy ivy = Ivy.newInstance();
        try {
            ivy.configureDefault();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return resolve(ivy);
    }

    static ArtifactDownloadReport[] resolve(Ivy ivy) {
        String scalaVersion = System.getProperty("scala.version");
        if (scalaVersion == null) {
            String defaultScalaVersion = "2.10.6";
            Message.warn("scala.version not specified - defaulting to " + defaultScalaVersion);
            scalaVersion = defaultScalaVersion;
        }

        ResolveOptions resolveOptions = new ResolveOptions()
                .setConfs(new String[]{"default"})
                .setArtifactFilter(FilterHelper.getArtifactTypeFilter(new String[]{"jar"}))
                .setOutputReport(false);
        ModuleRevisionId scalaCompiler = new ModuleRevisionId(new ModuleId("org.scala-lang", "scala-compiler"), scalaVersion);
        try {
            return ivy.resolve(scalaCompiler, resolveOptions, false).getAllArtifactsReports();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
