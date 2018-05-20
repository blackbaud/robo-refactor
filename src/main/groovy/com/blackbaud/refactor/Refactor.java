package com.blackbaud.refactor;

import com.blackbaud.git.GitRepository;

public interface Refactor {

    String getStoryId();

    String getPullRequestDescription();

    void apply(GradleProject factory, GitRepository repository);

}
