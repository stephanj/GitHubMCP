package com.devoxx.agentic.github.tools;

public record GitHubEnv(String githubToken, String githubHost, String repository) {
    /**
     * Returns whether the environment is set up for GitHub Enterprise
     */
    public boolean isEnterprise() {
        return githubHost != null && !githubHost.isEmpty() && 
               !githubHost.equals("github.com") && !githubHost.equals("https://github.com");
    }
    
    /**
     * Gets the full repository name in owner/repo format
     */
    public String getFullRepository() {
        return repository;
    }
}
