package com.blackbaud.git

import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.PullRequestMarker
import org.eclipse.egit.github.core.Repository
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec

import java.nio.charset.StandardCharsets
import java.nio.file.Path

class GitRepository {

    private Repository repository
    private CredentialsProvider credentialsProvider
    private GitRepositoryManager repositoryManager
    private List<String> patchesApplied = []
    private Git git
    private File rootDir

    GitRepository(Repository repository, CredentialsProvider credentialsProvider, GitRepositoryManager repositoryManager) {
        this.repository = repository
        this.credentialsProvider = credentialsProvider
        this.repositoryManager = repositoryManager
    }

    GitRepository(File rootDir) {
        this.rootDir = rootDir
        this.git = Git.open(rootDir)
    }

    List<String> getPatchesApplied() {
        patchesApplied
    }

    File getRootDir() {
        rootDir
    }

    Path getRootPath() {
        File absoluteFile = new File(rootDir.absolutePath)
        absoluteFile.toPath()
    }

    String getName() {
        repository.name
    }

    String getCloneUrl() {
        repository.cloneUrl
    }

    String getSshUrl() {
        repository.sshUrl
    }

    void clone(File targetDir) {
        if (targetDir.exists()) {
            throw new RuntimeException("Target directory must not exist, path=${targetDir.absolutePath}")
        }
        targetDir.parentFile.mkdirs()
        this.rootDir = targetDir
        this.git = Git.cloneRepository()
                .setDirectory(targetDir)
                .setURI(repository.cloneUrl)
                .setCredentialsProvider(credentialsProvider)
                .call()
    }

    void applyPatch(String patch) {
        patchesApplied << patch
        InputStream patchSream = new ByteArrayInputStream(patch.getBytes(StandardCharsets.UTF_8))
        git.apply()
                .setPatch(patchSream)
                .call()
    }

    void commit(String message) {
        git.add()
                .addFilepattern(".")
                .call()
        // this is either a bug with jgit or a misconfiguration but the file pattern '.' picks up platform
        // whitespace changes to gradlew.bat even through no specific changes were made, so exclude
        git.reset()
                .addPath("gradlew.bat")
                .call()
        git.commit()
                .setMessage(message)
                .call()
    }

    void pushAsBranchAndCreatePullRequest(String branch, String prDescription) {
        git.branchCreate()
                .setName(branch)
                .call()
        git.push()
                .setRemote("origin")
                .setCredentialsProvider(credentialsProvider)
                .setRefSpecs(new RefSpec("${branch}:${branch}"))
                .call()

        PullRequest pr = new PullRequest()
        pr.title = branch
        pr.body = getPullRequestBody(branch, prDescription)
        pr.head = new PullRequestMarker().setLabel(branch)
        pr.base = new PullRequestMarker().setLabel("master")
        repositoryManager.createPullRequest(repository, pr)
    }

    private String getPullRequestBody(String branch, String prDescription) {
        String ownerTag = repositoryManager.getTeamOwnerTag(name)
        ownerTag != null ? "${ownerTag} ${branch} ${prDescription}" : "${branch} ${prDescription}"
    }

}
