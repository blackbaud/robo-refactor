package com.blackbaud.git

import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.eclipse.egit.github.core.PullRequest

interface GitRepositoryManager {

    GitRepository getRepository(String name)

    List<GitRepository> getRepositories()

    void createPullRequest(IRepositoryIdProvider repositoryIdProvider, PullRequest pullRequest)

    String getTeamOwnerTag(String repositoryName)

}
