package com.blackbaud.refactor.robo

import com.blackbaud.git.GitRepository
import com.blackbaud.git.github.GithubRepositoryManager
import com.blackbaud.refactor.GradleProject
import com.blackbaud.refactor.Refactor
import com.netflix.rewrite.ast.Tr
import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.PullRequestMarker
import org.eclipse.jgit.api.Git

class UseBlackbaudSpringApplication implements Refactor {

    @Override
    String getStoryId() {
        "LUM-27973"
    }

    @Override
    String getPullRequestDescription() {
        "use BlackbaudSpringApplication"
    }

    @Override
    void apply(GradleProject project, GitRepository repository) {
        List<Tr.CompilationUnit> compilationUnits = project.getRootProjectCompilationUnits()
        for (Tr.CompilationUnit compilationUnit : compilationUnits) {
            List<Tr.MethodInvocation> invocations = compilationUnit.findMethodCalls("org.springframework.boot.SpringApplication run(Object, String[])")
            if (invocations.isEmpty()) {
                invocations = compilationUnit.findMethodCalls("org.springframework.boot.SpringApplication run(Object[], String[])")
            }
            if (invocations.size() > 0) {
                String diff = compilationUnit.refactor()
                        .changeMethodTargetToStatic(invocations, "com.blackbaud.boot.BlackbaudSpringApplication")
                        .diff(repository.rootPath)
                repository.applyPatch(diff)
                repository.commit("${storyId} use BlackbaudSpringApplication")
            }
        }
    }

    static void mainTest(String[] args) {
        File projectDir = new File("staging")
        GradleProject projectInfo = new GradleProject(projectDir)
        GitRepository repository = new GitRepository(projectDir)
        new UseBlackbaudSpringApplication().apply(projectInfo, repository)
    }

}
