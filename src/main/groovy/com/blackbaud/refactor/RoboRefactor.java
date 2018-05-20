package com.blackbaud.refactor;

import com.blackbaud.refactor.robo.UseBlackbaudSpringApplication;
import com.google.common.io.Files;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class RoboRefactor implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RoboRefactor.class);
        app.setWebEnvironment(false);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        refactorBlackbaudSpringApplication(args);
    }

    private void refactorBlackbaudSpringApplication(String[] args) throws Exception {
        MultiProjectRefactor refactor = createGithubRefactor(args);
        List<String> repositories = getRepositories();
        refactor.dryRun();
        refactor.apply(new UseBlackbaudSpringApplication(), repositories);
    }

    private List<String> getRepositories() throws IOException {
        File repoListFile = new File("repos.list");
        List<String> repositories = new ArrayList<>();
        for (String repository : Files.readLines(repoListFile, StandardCharsets.UTF_8)) {
            repository = repository.trim();
            if (repository.isEmpty() == false) {
                repositories.add(repository);
            }
        }
        return repositories;
    }

    private MultiProjectRefactor createGithubRefactor(String[] args) {
        String githubUsername = args[0];
        String githubToken = args[1];
        return MultiProjectRefactor.createGithubRefactor(githubUsername, githubToken);
    }

}
