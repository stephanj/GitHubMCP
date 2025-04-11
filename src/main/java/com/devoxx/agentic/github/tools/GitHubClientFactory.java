package com.devoxx.agentic.github.tools;

import org.kohsuke.github.GitHub;
import java.io.IOException;
import java.util.Optional;

/**
 * Factory for creating GitHub client instances with proper configuration
 */
public class GitHubClientFactory {
    
    /**
     * Creates a GitHub client based on the provided environment configuration
     * 
     * @param env The GitHub environment configuration
     * @return A configured GitHub client
     * @throws IOException if there's an error connecting to GitHub
     */
    public static GitHub createClient(GitHubEnv env) throws IOException {
        if (env.isEnterprise()) {
            return GitHub.connectToEnterprise(env.githubHost(), env.githubToken());
        } else {
            return GitHub.connectUsingOAuth(env.githubToken());
        }
    }

    /**
     * Retrieves environment variables needed for GitHub API access
     * 
     * @return Optional containing GitHub environment, or empty if configuration is missing
     */
    public static Optional<GitHubEnv> getEnvironment() {
        String token = getEnvValue("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            // Try personal access token alternative env var
            token = getEnvValue("GITHUB_PERSONAL_ACCESS_TOKEN");
            if (token == null || token.isEmpty()) {
                System.err.println("WARNING: GitHub token not found in environment variables");
                return Optional.empty();
            }
        }
        
        String host = getEnvValue("GITHUB_HOST");
        if (host == null || host.isEmpty()) {
            // Try GH_HOST alternative
            host = getEnvValue("GH_HOST");
            // Default to github.com if not set
            if (host == null || host.isEmpty()) {
                host = "github.com";
            }
        }
        
        String repository = getEnvValue("GITHUB_REPOSITORY");
        
        // Log information without exposing token
        String tokenPreview = (token.length() > 8) ? 
            token.substring(0, 4) + "..." + token.substring(token.length() - 4) : "****";
        System.out.println("GitHub configuration: Host=" + host + 
                           ", Token=" + tokenPreview + 
                           ", Default repository=" + repository);
        
        return Optional.of(new GitHubEnv(token, host, repository));
    }
    
    /**
     * Helper method to retrieve environment variables from various sources
     */
    private static String getEnvValue(String name) {
        // First check system properties (useful for tests)
        String value = System.getProperty(name);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Then check environment variables
        return System.getenv(name);
    }
}
