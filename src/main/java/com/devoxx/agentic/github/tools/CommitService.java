package com.devoxx.agentic.github.tools;

import org.kohsuke.github.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for GitHub commit-related operations
 */
@Service
public class CommitService extends AbstractToolService {

    @Tool(description = """
        Get detailed information about a specific commit.
        Returns commit data including author, committer, message, and file changes.
    """)
    public String getCommitDetails(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Commit SHA") String sha
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (sha == null || sha.isEmpty()) {
                return errorMessage("Commit SHA is required");
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
            GHCommit commit = repo.getCommit(sha);
            
            Map<String, Object> commitData = new HashMap<>();
            commitData.put("sha", commit.getSHA1());
            commitData.put("message", commit.getCommitShortInfo().getMessage());
            commitData.put("html_url", commit.getHtmlUrl().toString());
            
            // Author details
            GHCommit.ShortInfo info = commit.getCommitShortInfo();
            Map<String, Object> authorData = new HashMap<>();
            authorData.put("name", info.getAuthor().getName());
            authorData.put("email", info.getAuthor().getEmail());
            authorData.put("date", info.getAuthoredDate().toString());
            commitData.put("author", authorData);
            
            // Committer details (might be different from author)
            Map<String, Object> committerData = new HashMap<>();
            committerData.put("name", info.getCommitter().getName());
            committerData.put("email", info.getCommitter().getEmail());
            committerData.put("date", info.getCommitDate().toString());
            commitData.put("committer", committerData);
            
            // Parents
            List<Map<String, String>> parentsList = new ArrayList<>();
            for (GHCommit parent : commit.getParents()) {
                Map<String, String> parentData = new HashMap<>();
                parentData.put("sha", parent.getSHA1());
                parentData.put("url", parent.getHtmlUrl().toString());
                parentsList.add(parentData);
            }
            commitData.put("parents", parentsList);
            
            // File changes
            List<Map<String, Object>> filesList = new ArrayList<>();
            for (GHCommit.File file : commit.listFiles()) {
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("filename", file.getFileName());
                fileData.put("status", file.getStatus());
                fileData.put("additions", file.getLinesAdded());
                fileData.put("deletions", file.getLinesDeleted());
                fileData.put("changes", file.getLinesChanged());
                fileData.put("patch", file.getPatch());
                filesList.add(fileData);
            }
            commitData.put("files", filesList);
            
            // Stats
            Map<String, Integer> statsData = new HashMap<>();
            statsData.put("additions", commit.getLinesAdded());
            statsData.put("deletions", commit.getLinesDeleted());
            statsData.put("total", commit.getLinesChanged());
            commitData.put("stats", statsData);
            
            result.put("commit", commitData);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Commit or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(description = """
    List commits in a repository.
    Returns a list of commits with filtering options for branch and author.
    """)
    public String listCommits(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Branch or tag name to filter commits", required = false) String branch,
            @ToolParam(description = "Author name or email to filter commits", required = false) String author,
            @ToolParam(description = "Path to filter commits that touch the specified path", required = false) String path,
            @ToolParam(description = "Maximum number of commits to return", required = false) Integer limit
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
            
            // Setup commit query parameters
            GHCommitQueryBuilder queryBuilder = repo.queryCommits();
            
            if (branch != null && !branch.isEmpty()) {
                queryBuilder.from(branch);
            }
            
            if (author != null && !author.isEmpty()) {
                queryBuilder.author(author);
            }
            
            if (path != null && !path.isEmpty()) {
                queryBuilder.path(path);
            }
            
            int actualLimit = (limit != null && limit > 0) ? limit : 30; // Default to 30 commits
            
            List<Map<String, Object>> commitList = new ArrayList<>();
            int count = 0;
            
            for (GHCommit commit : queryBuilder.list().withPageSize(actualLimit)) {
                if (count >= actualLimit) {
                    break;
                }
                
                Map<String, Object> commitData = new HashMap<>();
                commitData.put("sha", commit.getSHA1());
                
                GHCommit.ShortInfo info = commit.getCommitShortInfo();
                commitData.put("message", info.getMessage());
                commitData.put("author", info.getAuthor().getName());
                commitData.put("author_email", info.getAuthor().getEmail());
                commitData.put("date", info.getAuthoredDate().toString());
                
                // Include stats
                Map<String, Integer> statsData = new HashMap<>();
                statsData.put("additions", commit.getLinesAdded());
                statsData.put("deletions", commit.getLinesDeleted());
                statsData.put("total", commit.getLinesChanged());
                commitData.put("stats", statsData);
                
                commitData.put("html_url", commit.getHtmlUrl().toString());
                
                commitList.add(commitData);
                count++;
            }
            
            result.put("commits", commitList);
            result.put("count", commitList.size());
            
            if (branch != null && !branch.isEmpty()) {
                result.put("branch", branch);
            }
            
            if (author != null && !author.isEmpty()) {
                result.put("author", author);
            }
            
            if (path != null && !path.isEmpty()) {
                result.put("path", path);
            }
            
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
    Search for a commit based on the provided text or keywords in the project history.
    Useful for finding specific change sets or code modifications by commit messages or diff content.
    Takes a query parameter and returns the matching commit information.
    Returns matched commit hashes as a JSON array.
    """)
    public String findCommitByMessage(
            @ToolParam(description = "Text to search for in commit messages") String text,
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Branch or tag name to search within", required = false) String branch,
            @ToolParam(description = "Maximum number of results to return", required = false) Integer limit
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (text == null || text.isEmpty()) {
                return errorMessage("Search text is required");
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
            
            // Setup query parameters
            GHCommitQueryBuilder queryBuilder = repo.queryCommits();
            
            if (branch != null && !branch.isEmpty()) {
                queryBuilder.from(branch);
            }
            
            // Normalize the search text to lowercase for case-insensitive matching
            String searchText = text.toLowerCase();
            int actualLimit = (limit != null && limit > 0) ? limit : 20; // Default to 20 results
            
            List<Map<String, Object>> matchedCommits = new ArrayList<>();
            int count = 0;
            
            for (GHCommit commit : queryBuilder.list()) {
                if (count >= actualLimit) {
                    break;
                }
                
                GHCommit.ShortInfo info = commit.getCommitShortInfo();
                String message = info.getMessage();
                
                // Check if the commit message contains the search text
                if (message != null && message.toLowerCase().contains(searchText)) {
                    Map<String, Object> commitData = new HashMap<>();
                    commitData.put("sha", commit.getSHA1());
                    commitData.put("message", message);
                    commitData.put("author", info.getAuthor().getName());
                    commitData.put("date", info.getAuthoredDate().toString());
                    commitData.put("html_url", commit.getHtmlUrl().toString());
                    
                    // Include stats
                    Map<String, Integer> statsData = new HashMap<>();
                    statsData.put("additions", commit.getLinesAdded());
                    statsData.put("deletions", commit.getLinesDeleted());
                    statsData.put("total", commit.getLinesChanged());
                    commitData.put("stats", statsData);
                    
                    matchedCommits.add(commitData);
                    count++;
                }
            }
            
            result.put("commits", matchedCommits);
            result.put("count", matchedCommits.size());
            result.put("search_text", text);
            
            if (branch != null && !branch.isEmpty()) {
                result.put("branch", branch);
            }
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
}
