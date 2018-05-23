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

    private MultiProjectRefactor(GitRepositoryManager gitRepositoryManager) {
        this.gitRepositoryManager = gitRepositoryManager
        this.buildDir = new File(new File("build").absolutePath)
        this.stagingDir = new File(buildDir, "staging")
        this.stagingDir.deleteDir()
        this.stagingDir.mkdirs()
    }

    void dryRun() {
        this.dryRun = true
    }

    void apply(Refactor refactor, List<String> repositoryNames) {
        if (repositoryNames.isEmpty()) {
            throw new RuntimeException("Repository list empty")
        }

        AlreadyProcessedRepositoryManager alreadyProcessedRepositoryManager = new AlreadyProcessedRepositoryManager(buildDir, refactor.getStoryId())
        List<GitRepository> repositories = []

        try {
            for (String repositoryName : repositoryNames) {
                if (alreadyProcessedRepositoryManager.isAlreadyProcessed(repositoryName)) {
                    continue
                }

                GitRepository repository = refactorProject(refactor, repositoryName)
                if (repository != null) {
                    repositories << repository
                }
                alreadyProcessedRepositoryManager.addProcessedRepo(repositoryName)
            }
        } catch (Exception ex) {
            println "Failure while applying refactoring, ex=${ex.message}"
            ex.printStackTrace()
        } finally {
            if ((repositories.size() > 0) && (dryRun == false) && shouldCreatePr()) {
                pushBranchesAndCreatePRs(repositories, refactor)
            }
        }
    }

    private void pushBranchesAndCreatePRs(List<GitRepository> repositories, Refactor refactor) {
        for (GitRepository repository : repositories) {
            log.info("Pushing branch and creating PR for ${repository.name}")
            repository.pushAsBranchAndCreatePullRequest(refactor.storyId, refactor.pullRequestDescription)
        }
    }

    private GitRepository refactorProject(Refactor refactor, String repositoryName) {
        GitRepository repository = gitRepositoryManager.getRepository(repositoryName)
        File repoDir = new File(stagingDir, repositoryName)
        repository.clone(repoDir)

        GradleProject project = new GradleProject(repoDir)

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
            repository
        } else {
            log.info("No changes detected, moving on to next project")
            null
        }
    }

    private boolean shouldCreatePr() {
        println "Would you like to push the branchs and create PRs (y/n)?"
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

    private static class AlreadyProcessedRepositoryManager {

        File alreadyProcessedReposFile
        List<String> alreadyProcessedRepos

        AlreadyProcessedRepositoryManager(File buildDir, String storyId) {
            this.alreadyProcessedReposFile = new File(buildDir, storyId)
            this.alreadyProcessedRepos = alreadyProcessedReposFile.exists() ? alreadyProcessedReposFile.readLines() : []
        }

        boolean isAlreadyProcessed(String repositoryName) {
            alreadyProcessedRepos.contains(repositoryName)
        }

        void addProcessedRepo(String repositoryName) {
            alreadyProcessedRepos.add(repositoryName)
            alreadyProcessedReposFile.text = alreadyProcessedRepos.join(System.getProperty("line.separator"))
        }

    }

}
