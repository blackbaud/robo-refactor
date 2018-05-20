package com.blackbaud.refactor

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.idea.IdeaContentRoot
import org.gradle.tooling.model.idea.IdeaDependency
import org.gradle.tooling.model.idea.IdeaModule
import org.gradle.tooling.model.idea.IdeaModuleDependency
import org.gradle.tooling.model.idea.IdeaModuleIdentifier
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency
import org.gradle.tooling.model.idea.IdeaSourceDirectory

import java.nio.file.Path

class GradleProjectInfo {

    private File projectDirectory
    private ProjectConnection projectConnection
    private IdeaProject ideaProject
    private IdeaModule rootModule
    private Map<IdeaModuleIdentifier, IdeaModule> moduleMap = [:]

    GradleProjectInfo(File projectDirectory) {
        this.projectDirectory = projectDirectory
        this.projectConnection = GradleConnector.newConnector()
                .forProjectDirectory(projectDirectory)
                .connect()
        this.ideaProject = projectConnection.getModel(IdeaProject.class)

        for (IdeaModule module : ideaProject.modules) {
            if (module.name == projectDirectory.name) {
                rootModule = module
            } else {
                moduleMap[module.identifier] = module
            }
        }

        if (rootModule == null) {
            throw new RuntimeException("Failed to resolve root module")
        }
    }

    List<Path> getRootSourceFiles() {
        List<Path> rootSourceFiles = []
        rootSourceDirs*.eachFileRecurse {
            if (it.isFile() && it.name.endsWith(".java")) {
                rootSourceFiles << it.toPath()
            }
        }
        rootSourceFiles
    }

    private List<File> getRootSourceDirs() {
        List<File> rootSourceDirs = []
        for (IdeaContentRoot contentRoot : rootModule.contentRoots) {
            for (IdeaSourceDirectory sourceDirectory : contentRoot.sourceDirectories) {
                if (sourceDirectory.directory.exists()) {
                    rootSourceDirs << sourceDirectory.directory
                }
            }
        }
        rootSourceDirs
    }

    List<Path> getRootSourceDependencies() {
        List<Path> rootSourceDependencies = []
        collectDependencies(rootSourceDependencies, rootModule)
        rootSourceDependencies
    }

    private void collectDependencies(List<Path> dependencies, IdeaModule module, List<String> alreadyCollected = []) {
        alreadyCollected << module.name
        for (IdeaDependency dependency : module.dependencies) {
            if (dependency instanceof IdeaModuleDependency) {
                IdeaModule dependantModule = moduleMap[dependency.target]
                if (alreadyCollected.contains(dependantModule.name) == false) {
                    collectDependencies(dependencies, dependantModule, alreadyCollected)
                }
            } else if (dependency instanceof IdeaSingleEntryLibraryDependency) {
                dependencies << dependency.file.toPath()
            }
        }
    }

    void close() {
        projectConnection.close()
    }

}
