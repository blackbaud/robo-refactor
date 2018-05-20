package com.blackbaud.refactor

import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.internal.consumer.BlockingResultHandler
import org.gradle.tooling.model.GradleTask

import java.nio.file.Path

class GradleProject {

    private File projectDir

    GradleProject(File projectDir) {
        this.projectDir = projectDir
    }

    List<Tr.CompilationUnit> getRootProjectCompilationUnits() {
        GradleProjectInfo projectInfo = new GradleProjectInfo(projectDir)

        try {
            List<Path> sourcePaths = projectInfo.getRootSourceFiles()
            List<Path> binaryDependencies = projectInfo.getRootSourceDependencies()
            Parser parser = new OracleJdkParser(binaryDependencies)
            return parser.parse(sourcePaths)
        } finally {
            projectInfo.close()
        }
    }

    void validate() {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()

        try {
            org.gradle.tooling.model.GradleProject project = connection.getModel(org.gradle.tooling.model.GradleProject.class)
            GradleTask clean
            GradleTask check
            for (GradleTask task : project.tasks) {
                if (task.name == "clean") {
                    clean = task
                } else if (task.name == "check") {
                    check = task
                }
            }

            BlockingResultHandler resultHandler = new BlockingResultHandler<>(Object.class)
            connection.newBuild()
                    .forTasks(clean, check)
                    .setStandardOutput(System.out)
                    .run(resultHandler)
            resultHandler.getResult()
        } finally {
            connection.close()
        }
    }

}
