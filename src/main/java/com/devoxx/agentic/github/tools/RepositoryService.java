package com.devoxx.agentic.github.tools;

import org.kohsuke.github.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for GitHub repository-related operations
 */
@Service
public class RepositoryService extends AbstractToolService {

    @Tool(description = """
        List repositories for the authenticated user.
        Returns a list of repositories the user has access to.
    """)
    public String listRepositories(
            @ToolParam(description = "Maximum number of repositories to return", required = false) Integer limit
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<GitHubEnv> env = GitHubClientFactory.getEnvironment();
            if (env.isEmpty()) {
                return errorMessage("GitHub is not configured correctly");
            }
            
            GitHubEnv githubEnv = env.get();
            GitHub github = GitHubClientFactory.createClient(githubEnv);
            
            GHMyself myself = github.getMyself();
            Map<String, GHRepository> repos = myself.getAllRepositories();
            
            List<Map<String, Object>> repoList = new ArrayList<>();
            int count = 0;
            
            for (GHRepository repo : repos.values()) {
                if (limit != null && count >= limit) {
                    break;
                }
                
                Map<String, Object> repoData = new HashMap<>();
                repoData.put("name", repo.getName());
                repoData.put("full_name", repo.getFullName());
                repoData.put("description", repo.getDescription());
                repoData.put("url", repo.getHtmlUrl().toString());
                repoData.put("stars", repo.getStargazersCount());
                repoData.put("forks", repo.listForks().toList().size());
                repoData.put("private", repo.isPrivate());
                
                repoList.add(repoData);
                count++;
            }
            
            result.put("repositories", repoList);
            result.put("total_count", repos.size());
            
            return successMessage(result);
            
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(description = """
        Get information about a specific repository.
        Returns details about the repository such as description, stars, forks, etc.
    """)
    public String getRepository(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<GitHubEnv> env = GitHubClientFactory.getEnvironment();
            if (env.isEmpty()) {
                return errorMessage("GitHub is not configured correctly");
            }
            
            GitHubEnv githubEnv = env.get();
            GitHub github = GitHubClientFactory.createClient(githubEnv);
            
            // Use provided repository or default from environment
            String repoName = (repository != null && !repository.isEmpty()) ? 
                                repository : githubEnv.getFullRepository();
                                
            if (repoName == null || repoName.isEmpty()) {
                return errorMessage("Repository name is required");
            }
            
            GHRepository repo = github.getRepository(repoName);
            
            Map<String, Object> repoData = new HashMap<>();
            repoData.put("name", repo.getName());
            repoData.put("full_name", repo.getFullName());
            repoData.put("description", repo.getDescription());
            repoData.put("url", repo.getHtmlUrl().toString());
            repoData.put("stars", repo.getStargazersCount());
            repoData.put("forks", repo.listForks().toList().size());
            repoData.put("open_issues", repo.getOpenIssueCount());
            repoData.put("watchers", repo.getWatchersCount());
            repoData.put("license", repo.getLicense() != null ? repo.getLicense().getName() : null);
            repoData.put("default_branch", repo.getDefaultBranch());
            repoData.put("created_at", repo.getCreatedAt().toString());
            repoData.put("updated_at", repo.getUpdatedAt().toString());
            repoData.put("private", repo.isPrivate());
            
            result.put("repository", repoData);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
    
    @Tool(description = """
        Search for repositories.
        Searches GitHub for repositories matching the query.
    """)
    public String searchRepositories(
            @ToolParam(description = "Search query") String query,
            @ToolParam(description = "Maximum number of results to return", required = false) Integer limit
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (query == null || query.isEmpty()) {
                return errorMessage("Search query is required");
            }
            
            Optional<GitHubEnv> env = GitHubClientFactory.getEnvironment();
            if (env.isEmpty()) {
                return errorMessage("GitHub is not configured correctly");
            }
            
            GitHubEnv githubEnv = env.get();
            GitHub github = GitHubClientFactory.createClient(githubEnv);
            
            int actualLimit = (limit != null && limit > 0) ? limit : 10;
            
            GHRepositorySearchBuilder searchBuilder = github.searchRepositories()
                    .q(query)
                    .order(GHDirection.DESC)
                    .sort(GHRepositorySearchBuilder.Sort.STARS);
            
            List<Map<String, Object>> repoList = new ArrayList<>();
            
            searchBuilder.list().withPageSize(actualLimit).iterator().forEachRemaining(repo -> {
                if (repoList.size() < actualLimit) {
                    Map<String, Object> repoData = new HashMap<>();
                    repoData.put("name", repo.getName());
                    repoData.put("full_name", repo.getFullName());
                    repoData.put("description", repo.getDescription());
                    repoData.put("url", repo.getHtmlUrl().toString());
                    repoData.put("stars", repo.getStargazersCount());
                    try {
                        repoData.put("forks", repo.listForks().toList().size());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    repoData.put("language", repo.getLanguage());
                    
                    repoList.add(repoData);
                }
            });
            
            result.put("repositories", repoList);
            result.put("query", query);
            
            return successMessage(result);
            
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
}
