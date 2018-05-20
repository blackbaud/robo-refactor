package com.blackbaud.git.github

import com.blackbaud.git.GitRepository
import com.blackbaud.git.GitRepositoryManager
import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.PullRequestService
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

class GithubRepositoryManager implements GitRepositoryManager {

    private RepositoryService repositoryService
    private UsernamePasswordCredentialsProvider githubCredentialsProvider
    private GitHubClient gitHubClient

    GithubRepositoryManager(String githubUsername, String githubToken) {
        githubCredentialsProvider = new UsernamePasswordCredentialsProvider(githubUsername, githubToken)
        gitHubClient = new GitHubClient()
        gitHubClient.setCredentials(githubUsername, githubToken)
        repositoryService = new RepositoryService(gitHubClient)
    }

    List<GitRepository> getRepositories() {
        repositoryService.getOrgRepositories("Blackbaud").collect { repo ->
            new GitRepository(repo, githubCredentialsProvider, this)
        }
    }

    GitRepository getRepository(String name) {
        Repository repo = repositoryService.getRepository("Blackbaud", name)
        new GitRepository(repo, githubCredentialsProvider, this)
    }

    void createPullRequest(IRepositoryIdProvider repositoryIdProvider, PullRequest pullRequest) {
        PullRequestService prService = new PullRequestService(gitHubClient)
        prService.createPullRequest(repositoryIdProvider, pullRequest)
    }

}
