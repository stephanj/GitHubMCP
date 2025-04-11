package com.devoxx.agentic.github.tools;

import org.kohsuke.github.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for GitHub pull request-related operations
 */
@Service
public class PullRequestService extends AbstractToolService {

    @Tool(description = """
    List pull requests for a repository.
    Returns pull requests with filtering options for state.
    """)
    public String listPullRequests(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "State of pull requests (open, closed, all)", required = false) String state,
            @ToolParam(description = "Maximum number of results to return", required = false) Integer limit
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
            
            GHIssueState prState = GHIssueState.OPEN;
            if (state != null) {
                prState = switch (state.toLowerCase()) {
                    case "closed" -> GHIssueState.CLOSED;
                    case "all" -> GHIssueState.ALL;
                    default -> prState;
                };
            }
            
            List<GHPullRequest> pullRequests = repo.getPullRequests(prState);
            List<Map<String, Object>> prList = new ArrayList<>();
            int count = 0;
            
            for (GHPullRequest pr : pullRequests) {
                if (limit != null && count >= limit) {
                    break;
                }
                
                Map<String, Object> prData = new HashMap<>();
                prData.put("number", pr.getNumber());
                prData.put("title", pr.getTitle());
                prData.put("state", pr.getState().name().toLowerCase());
                prData.put("html_url", pr.getHtmlUrl().toString());
                prData.put("created_at", pr.getCreatedAt().toString());
                prData.put("updated_at", pr.getUpdatedAt().toString());
                prData.put("closed_at", pr.getClosedAt() != null ? pr.getClosedAt().toString() : null);
                prData.put("merged_at", pr.getMergedAt() != null ? pr.getMergedAt().toString() : null);
                prData.put("is_merged", pr.isMerged());
                
                prData.put("user", pr.getUser().getLogin());
                
                prData.put("base_branch", pr.getBase().getRef());
                prData.put("head_branch", pr.getHead().getRef());
                
                prList.add(prData);
                count++;
            }
            
            result.put("pull_requests", prList);
            result.put("total_count", pullRequests.size());
            
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
    Get a specific pull request.
    Returns detailed information about the pull request.
    """)
    public String getPullRequest(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Pull request number") Integer prNumber
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (prNumber == null) {
                return errorMessage("Pull request number is required");
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
            GHPullRequest pr = repo.getPullRequest(prNumber);
            
            Map<String, Object> prData = new HashMap<>();
            prData.put("number", pr.getNumber());
            prData.put("title", pr.getTitle());
            prData.put("body", pr.getBody());
            prData.put("state", pr.getState().name().toLowerCase());
            prData.put("html_url", pr.getHtmlUrl().toString());
            prData.put("created_at", pr.getCreatedAt().toString());
            prData.put("updated_at", pr.getUpdatedAt().toString());
            prData.put("closed_at", pr.getClosedAt() != null ? pr.getClosedAt().toString() : null);
            prData.put("merged_at", pr.getMergedAt() != null ? pr.getMergedAt().toString() : null);
            prData.put("is_merged", pr.isMerged());
            
            prData.put("user", pr.getUser().getLogin());
            
            prData.put("base_branch", pr.getBase().getRef());
            prData.put("head_branch", pr.getHead().getRef());
            
            // Get comments
            List<Map<String, Object>> commentsList = new ArrayList<>();
            for (GHIssueComment comment : pr.getComments()) {
                Map<String, Object> commentData = new HashMap<>();
                commentData.put("id", comment.getId());
                commentData.put("user", comment.getUser().getLogin());
                commentData.put("body", comment.getBody());
                commentData.put("created_at", comment.getCreatedAt().toString());
                commentData.put("updated_at", comment.getUpdatedAt().toString());
                
                commentsList.add(commentData);
            }
            
            prData.put("comments", commentsList);
            
            // Get files
            List<Map<String, Object>> filesList = new ArrayList<>();
            for (GHPullRequestFileDetail file : pr.listFiles()) {
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("filename", file.getFilename());
                fileData.put("status", file.getStatus());
                fileData.put("additions", file.getAdditions());
                fileData.put("deletions", file.getDeletions());
                fileData.put("changes", file.getChanges());
                
                filesList.add(fileData);
            }
            
            prData.put("files", filesList);
            
            result.put("pull_request", prData);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Pull request or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
    
    @Tool(description = """
        Create a comment on a pull request.
        Posts a new comment on the specified pull request.
    """)
    public String createPullRequestComment(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Pull request number") Integer prNumber,
            @ToolParam(description = "Comment text") String body
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (prNumber == null) {
                return errorMessage("Pull request number is required");
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
            GHPullRequest pr = repo.getPullRequest(prNumber);
            
            GHIssueComment comment = pr.comment(body);
            
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("id", comment.getId());
            commentData.put("body", comment.getBody());
            commentData.put("html_url", comment.getHtmlUrl().toString());
            commentData.put("created_at", comment.getCreatedAt().toString());
            
            result.put("comment", commentData);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Pull request or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
    
    @Tool(description = """
        Merge a pull request.
        Merges the pull request with the specified merge method.
    """)
    public String mergePullRequest(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Pull request number") Integer prNumber,
            @ToolParam(description = "Commit message for the merge", required = false) String commitMessage,
            @ToolParam(description = "Merge method (merge, squash, rebase)", required = false) String mergeMethod
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (prNumber == null) {
                return errorMessage("Pull request number is required");
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
            GHPullRequest pr = repo.getPullRequest(prNumber);
            
            // Check if PR is already merged
            if (pr.isMerged()) {
                return errorMessage("Pull request is already merged");
            }
            
            // Set default merge method if not provided
            String method = (mergeMethod != null && !mergeMethod.isEmpty()) ? 
                             mergeMethod.toLowerCase() : "merge";
            
            boolean success = switch (method) {
                case "squash" -> {
                    pr.merge(commitMessage, null, GHPullRequest.MergeMethod.SQUASH);
                    yield true;
                }
                case "rebase" -> {
                    pr.merge(commitMessage, null, GHPullRequest.MergeMethod.REBASE);
                    yield true;
                }
                case "merge" -> {
                    pr.merge(commitMessage, null, GHPullRequest.MergeMethod.MERGE);
                    yield true;
                }
                default -> false;
            };

            if (success) {
                result.put("merged", true);
                result.put("method", method);
                result.put("pull_request_number", prNumber);
                result.put("repository", repoName);

                return successMessage(result);
            } else {
                return errorMessage("Failed to merge pull request");
            }

        } catch (GHFileNotFoundException e) {
            return errorMessage("Pull request or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
}
