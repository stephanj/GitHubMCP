package com.devoxx.agentic.github.tools;

import org.kohsuke.github.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for GitHub issue-related operations
 */
@Service
public class IssueService extends AbstractToolService {

    @Tool(description = """
        List issues for a repository.
        Returns issues with filtering options for state, labels, and more.
    """)
    public String listIssues(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "State of issues to return (open, closed, all)", required = false) String state,
            @ToolParam(description = "Maximum number of issues to return", required = false) Integer limit
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
            
            GHIssueState issueState = GHIssueState.OPEN;
            if (state != null) {
                issueState = switch (state.toLowerCase()) {
                    case "closed" -> GHIssueState.CLOSED;
                    case "all" -> GHIssueState.ALL;
                    default -> issueState;
                };
            }
            
            List<GHIssue> issues;
            
//            if (labels != null && !labels.isEmpty()) {
//                String[] labelArray = labels.split(",");
//                issues = repo.getIssues(issueState, labelArray);
//            } else {
                issues = repo.getIssues(issueState);
//            }
            
            List<Map<String, Object>> issueList = new ArrayList<>();
            int count = 0;
            
            for (GHIssue issue : issues) {
                if (limit != null && count >= limit) {
                    break;
                }
                
                Map<String, Object> issueData = new HashMap<>();
                issueData.put("number", issue.getNumber());
                issueData.put("title", issue.getTitle());
                issueData.put("state", issue.getState().name().toLowerCase());
                issueData.put("html_url", issue.getHtmlUrl().toString());
                
                List<String> issueLabels = new ArrayList<>();
                for (GHLabel label : issue.getLabels()) {
                    issueLabels.add(label.getName());
                }
                issueData.put("labels", issueLabels);
                
                issueData.put("created_at", issue.getCreatedAt().toString());
                issueData.put("updated_at", issue.getUpdatedAt().toString());
                issueData.put("closed_at", issue.getClosedAt() != null ? issue.getClosedAt().toString() : null);
                
                if (issue.getAssignee() != null) {
                    issueData.put("assignee", issue.getAssignee().getLogin());
                }
                
                issueList.add(issueData);
                count++;
            }
            
            result.put("issues", issueList);
            result.put("total_count", issues.size());
            
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
        Get a specific issue in a repository.
        Returns detailed information about the issue.
    """)
    public String getIssue(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Issue number") Integer issueNumber
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (issueNumber == null) {
                return errorMessage("Issue number is required");
            }
            
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
            GHIssue issue = repo.getIssue(issueNumber);
            
            Map<String, Object> issueData = new HashMap<>();
            issueData.put("number", issue.getNumber());
            issueData.put("title", issue.getTitle());
            issueData.put("body", issue.getBody());
            issueData.put("state", issue.getState().name().toLowerCase());
            issueData.put("html_url", issue.getHtmlUrl().toString());
            
            List<String> labels = new ArrayList<>();
            for (GHLabel label : issue.getLabels()) {
                labels.add(label.getName());
            }
            issueData.put("labels", labels);
            
            issueData.put("created_at", issue.getCreatedAt().toString());
            issueData.put("updated_at", issue.getUpdatedAt().toString());
            issueData.put("closed_at", issue.getClosedAt() != null ? issue.getClosedAt().toString() : null);
            
            if (issue.getAssignee() != null) {
                issueData.put("assignee", issue.getAssignee().getLogin());
            }
            
            // Get comments
            List<Map<String, Object>> commentsList = new ArrayList<>();
            for (GHIssueComment comment : issue.getComments()) {
                Map<String, Object> commentData = new HashMap<>();
                commentData.put("id", comment.getId());
                commentData.put("user", comment.getUser().getLogin());
                commentData.put("body", comment.getBody());
                commentData.put("created_at", comment.getCreatedAt().toString());
                commentData.put("updated_at", comment.getUpdatedAt().toString());
                
                commentsList.add(commentData);
            }
            
            issueData.put("comments", commentsList);
            issueData.put("comments_count", commentsList.size());
            
            result.put("issue", issueData);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Issue or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(description = """
    Create a new issue in a repository.
    Creates an issue with the specified title, body, and optional labels.
    """)
    public String createIssue(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Issue title") String title,
            @ToolParam(description = "Issue body/description", required = false) String body,
            @ToolParam(description = "Comma-separated list of labels", required = false) String labels
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (title == null || title.isEmpty()) {
                return errorMessage("Issue title is required");
            }
            
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
            
            GHIssueBuilder issueBuilder = repo.createIssue(title);
            
            if (body != null && !body.isEmpty()) {
                issueBuilder.body(body);
            }
            
            if (labels != null && !labels.isEmpty()) {
                String[] labelArray = labels.split(",");
                for (String label : labelArray) {
                    issueBuilder.label(label.trim());
                }
            }
            
            GHIssue issue = issueBuilder.create();
            
            Map<String, Object> issueData = new HashMap<>();
            issueData.put("number", issue.getNumber());
            issueData.put("title", issue.getTitle());
            issueData.put("body", issue.getBody());
            issueData.put("html_url", issue.getHtmlUrl().toString());
            
            result.put("issue", issueData);
            
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
    Add a comment to an issue.
    Posts a new comment on the specified issue.
    """)
    public String addIssueComment(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Issue number") Integer issueNumber,
            @ToolParam(description = "Comment text") String body
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (issueNumber == null) {
                return errorMessage("Issue number is required");
            }
            
            if (body == null || body.isEmpty()) {
                return errorMessage("Comment body is required");
            }
            
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
            GHIssue issue = repo.getIssue(issueNumber);
            
            GHIssueComment comment = issue.comment(body);
            
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("id", comment.getId());
            commentData.put("body", comment.getBody());
            commentData.put("html_url", comment.getHtmlUrl().toString());
            commentData.put("created_at", comment.getCreatedAt().toString());
            
            result.put("comment", commentData);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Issue or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
    
    @Tool(description = """
    Search for issues.
    Searches for issues matching the query across GitHub or in a specific repository.
    """)
    public String searchIssues(
            @ToolParam(description = "Search query") String query,
            @ToolParam(description = "Repository name in format 'owner/repo' to limit search", required = false) String repository,
            @ToolParam(description = "State of issues to search for (open, closed)", required = false) String state,
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
            
            // Build search query
            StringBuilder queryBuilder = new StringBuilder(query);
            
            // Add repository filter if provided
            if (repository != null && !repository.isEmpty()) {
                queryBuilder.append(" repo:").append(repository);
            }
            
            // Add state filter if provided
            if (state != null && !state.isEmpty()) {
                queryBuilder.append(" is:").append(state);
            }
            
            int actualLimit = (limit != null && limit > 0) ? limit : 10;
            
            GHIssueSearchBuilder searchBuilder = github.searchIssues()
                    .q(queryBuilder.toString())
                    .order(GHDirection.DESC)
                    .sort(GHIssueSearchBuilder.Sort.CREATED);
            
            List<Map<String, Object>> issueList = new ArrayList<>();
            
            searchBuilder.list().withPageSize(actualLimit).iterator().forEachRemaining(issue -> {
                if (issueList.size() < actualLimit) {
                    Map<String, Object> issueData = new HashMap<>();
                    issueData.put("number", issue.getNumber());
                    issueData.put("title", issue.getTitle());
                    issueData.put("state", issue.getState().name().toLowerCase());
                    issueData.put("repository", issue.getRepository().getFullName());
                    issueData.put("html_url", issue.getHtmlUrl().toString());
                    try {
                        issueData.put("created_at", issue.getCreatedAt().toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    issueList.add(issueData);
                }
            });
            
            result.put("issues", issueList);
            result.put("query", queryBuilder.toString());
            
            return successMessage(result);
            
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
}
