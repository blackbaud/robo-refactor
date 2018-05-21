package com.blackbaud.git.github

import com.blackbaud.git.GitRepository
import com.blackbaud.git.GitRepositoryManager
import com.blackbaud.jenkins.jobs.config.ProjectToChannelMapping
import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.PullRequestService
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import static com.blackbaud.jenkins.jobs.config.ProjectToChannelMapping.TeamSlackChannel.*

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

    @Override
    String getTeamOwnerTag(String repositoryName) {
        String slackChannelName = ProjectToChannelMapping.projectSlackChannelMap[repositoryName]

        switch (slackChannelName) {
            case MICRO_CERVEZAS.channelName:
                return "@blackbaud/micro-cervezas"
            case VOLTRON.channelName:
                return "@blackbaud/voltron"
            case CEREBRO.channelName:
                return "@blackbaud/team-cerebro"
            case HIGHLANDER.channelName:
                return "@blackbaud/highlander"
            case BRADY_BUNCH.channelName:
                return "@blackbaud/brady-bunch"
            default:
                return null
        }
    }

}
