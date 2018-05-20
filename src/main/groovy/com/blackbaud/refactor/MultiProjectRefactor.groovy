package com.blackbaud.refactor

import com.blackbaud.git.GitRepository
import com.blackbaud.git.GitRepositoryManager
import com.blackbaud.git.github.GithubRepositoryManager
import groovy.util.logging.Slf4j

@Slf4j
class MultiProjectRefactor {

    static MultiProjectRefactor createGithubRefactor(String githubUsername, String githubToken) {
        GithubRepositoryManager githubRepositoryManager = new GithubRepositoryManager(githubUsername, githubToken)
        new MultiProjectRefactor(githubRepositoryManager)
    }

    private GitRepositoryManager gitRepositoryManager
    private File buildDir
    private File stagingDir
    private boolean dryRun = false
    private boolean verifyPullRequest = true

    private MultiProjectRefactor(GitRepositoryManager gitRepositoryManager) {
        this.gitRepositoryManager = gitRepositoryManager
        this.buildDir = new File(new File("build").absolutePath)
        this.stagingDir = new File(buildDir, "staging")
        this.stagingDir.mkdirs()
    }

    void dryRun() {
        this.dryRun = true
    }

    void disableVerifyPullRequest() {
        this.verifyPullRequest = false
    }

    void apply(Refactor refactor, List<String> repositories) {
        if (repositories.isEmpty()) {
            throw new RuntimeException("Repository list empty")
        }

        File alreadyProcessedReposFile = new File(buildDir, refactor.getStoryId())
        List<String> alreadyProcessedRepos = alreadyProcessedReposFile.exists() ? alreadyProcessedReposFile.readLines() : []

        for (String repositoryName : repositories) {
            if (alreadyProcessedRepos.contains(repositoryName)) {
                continue
            }

            refactorProject(refactor, repositoryName)

            alreadyProcessedRepos.add(repositoryName)
            alreadyProcessedReposFile.text = alreadyProcessedRepos.join(System.getProperty("line.separator"))
        }
    }

    private refactorProject(Refactor refactor, String repositoryName) {
        GitRepository repository = gitRepositoryManager.getRepository(repositoryName)
        stagingDir.deleteDir()
        repository.clone(stagingDir)

        GradleProject project = new GradleProject(stagingDir)

        log.info("")
        log.info("************************************************************")
        log.info("************************************************************")
        log.info("Applying refactoring to repository ${repositoryName}")
        log.info("")

        refactor.apply(project, repository)

        if (repository.patchesApplied.size() > 0) {
            for (int i = 0; i < repository.patchesApplied.size(); i++) {
                if (i > 0) {
                    log.info("-----------------------------------------------------")
                    log.info("")
                }
                log.info(repository.patchesApplied[i])
            }

            // NOTE: it would be ideal to validate the project but for some reason the docker tasks are failing...
            // perhaps lack of docker-machine environment initialization?
//            println "Validating project..."
//            project.validate()

            if ((verifyPullRequest == false) || (dryRun == false && shouldCreatePr())) {
                log.info("Pushing branch and creating PR...")
                repository.pushAsBranchAndCreatePullRequest(refactor.storyId, refactor.pullRequestDescription)
            }
        } else {
            log.info("No changes detected, moving on to next project")
        }
    }

    private boolean shouldCreatePr() {
        println "Would you like to push the branch and create a PR (y/n)?"
        String response = System.in.newReader().readLine().toLowerCase()
        int failCount = 0
        while (response != "y" && response != "n") {
            if (failCount == 0) {
                println "In case you're unfamiliar with '(y/n)', it means press press 'y' OR 'n' followed by 'return'"
            } else if (failCount == 1) {
                println "You're trying my patience..."
            } else {
                println "I do not suffer fools"
                System.exit(1)
            }
            failCount++
        }
        response == "y"
    }

}
