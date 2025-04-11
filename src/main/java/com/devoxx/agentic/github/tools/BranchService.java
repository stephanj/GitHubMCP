package com.devoxx.agentic.github.tools;

import org.kohsuke.github.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for GitHub branch-related operations
 */
@Service
public class BranchService extends AbstractToolService {

    @Tool(description = """
    List branches in a repository.
    Returns a list of branches with their details.
    """)
    public String listBranches(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Filter branches by prefix", required = false) String filter,
            @ToolParam(description = "Maximum number of branches to return", required = false) Integer limit
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
            
            List<Map<String, Object>> branchList = new ArrayList<>();
            int count = 0;
            
            for (GHBranch branch : repo.getBranches().values()) {
                // Apply filter if provided
                if (filter != null && !filter.isEmpty() && !branch.getName().startsWith(filter)) {
                    continue;
                }
                
                if (limit != null && count >= limit) {
                    break;
                }
                
                Map<String, Object> branchData = new HashMap<>();
                branchData.put("name", branch.getName());
                branchData.put("sha", branch.getSHA1());
                
                // Get the latest commit for this branch
                GHCommit commit = repo.getCommit(branch.getSHA1());
                if (commit != null) {
                    Map<String, Object> commitData = new HashMap<>();
                    commitData.put("message", commit.getCommitShortInfo().getMessage());
                    commitData.put("author", commit.getCommitShortInfo().getAuthor().getName());
                    commitData.put("date", commit.getCommitShortInfo().getCommitDate().toString());
                    branchData.put("latest_commit", commitData);
                }
                
                // Check if this is the default branch
                branchData.put("is_default", branch.getName().equals(repo.getDefaultBranch()));
                
                // Get protection status (requires separate API call)
                try {
                    GHBranchProtection protection = repo.getBranch(branch.getName()).getProtection();
                    branchData.put("protected", true);
                    // Add protection details if needed
                } catch (GHFileNotFoundException ex) {
                    // Branch is not protected
                    branchData.put("protected", false);
                } catch (Exception ex) {
                    // Ignore other exceptions for protection status
                    branchData.put("protected", false);
                }
                
                branchList.add(branchData);
                count++;
            }
            
            result.put("branches", branchList);
            result.put("total_count", branchList.size());
            
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
    Create a new branch in a repository.
    Creates a branch from a specified SHA or reference (defaults to the default branch if not specified).
    """)
    public String createBranch(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "New branch name") String branchName,
            @ToolParam(description = "SHA or reference to create branch from (defaults to default branch)", required = false) String fromRef
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (branchName == null || branchName.isEmpty()) {
                return errorMessage("Branch name is required");
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
            
            // Check if branch already exists
            try {
                GHBranch existingBranch = repo.getBranch(branchName);
                if (existingBranch != null) {
                    return errorMessage("Branch '" + branchName + "' already exists");
                }
            } catch (GHFileNotFoundException ex) {
                // Branch doesn't exist, which is what we want
            }
            
            // Determine the SHA to create from
            String sha;
            if (fromRef != null && !fromRef.isEmpty()) {
                try {
                    // Try to get the SHA from the reference
                    GHRef ref = repo.getRef("heads/" + fromRef);
                    sha = ref.getObject().getSha();
                } catch (GHFileNotFoundException ex) {
                    try {
                        // Try to resolve as a direct SHA
                        GHCommit commit = repo.getCommit(fromRef);
                        sha = commit.getSHA1();
                    } catch (Exception e) {
                        return errorMessage("Invalid reference: " + fromRef);
                    }
                }
            } else {
                // Use default branch
                String defaultBranch = repo.getDefaultBranch();
                GHRef ref = repo.getRef("heads/" + defaultBranch);
                sha = ref.getObject().getSha();
            }
            
            // Create the new branch
            GHRef newBranch = repo.createRef("refs/heads/" + branchName, sha);
            
            Map<String, Object> branchData = new HashMap<>();
            branchData.put("name", branchName);
            branchData.put("sha", sha);
            branchData.put("url", newBranch.getUrl().toString());
            
            result.put("branch", branchData);
            
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
