package com.devoxx.agentic.github;

import com.devoxx.agentic.github.tools.GitHubClientFactory;
import com.devoxx.agentic.github.tools.GitHubEnv;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHMyself;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "GITHUB_TOKEN", matches = ".+")
class GitHubServiceTest {

    @Test
    void testGitHubConnection() throws IOException {
        String githubToken = Dotenv.load().get("GITHUB_TOKEN");
        String githubHost = Dotenv.load().get("GITHUB_HOST");

        System.setProperty("GITHUB_TOKEN", githubToken);
        System.setProperty("GITHUB_HOST", githubHost);

        // Test environment setup
        Optional<GitHubEnv> env = GitHubClientFactory.getEnvironment();
        assertTrue(env.isPresent(), "GitHub environment configuration should be present");
        
        GitHubEnv githubEnv = env.get();
        assertNotNull(githubEnv.githubToken(), "GitHub token should not be null");
        assertNotNull(githubEnv.githubHost(), "GitHub host should not be null");
        
        // Test connection to GitHub API
        GitHub github = GitHubClientFactory.createClient(githubEnv);
        assertNotNull(github, "GitHub client should not be null");
        
        // Test that we can get authenticated user
        GHMyself myself = github.getMyself();
        assertNotNull(myself, "GitHub user should not be null");
        assertNotNull(myself.getLogin(), "GitHub username should not be null");
        
        System.out.println("Successfully connected to GitHub as: " + myself.getLogin());
    }
}
