package com.devoxx.agentic.github.tools;

import org.kohsuke.github.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for GitHub repository content management operations
 */
@Service
public class ContentService extends AbstractToolService {

    @Tool(description = """
        Get the contents of a file in a repository.
        Returns the file content and metadata such as size and sha.
    """)
    public String getFileContents(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Path to the file in the repository") String path,
            @ToolParam(description = "Branch or commit SHA (defaults to the default branch)", required = false) String ref
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (path == null || path.isEmpty()) {
                return errorMessage("File path is required");
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
            
            // Get contents, using ref if provided
            GHContent content;
            if (ref != null && !ref.isEmpty()) {
                content = repo.getFileContent(path, ref);
            } else {
                content = repo.getFileContent(path);
            }
            
            // Check if it's a file
            if (content.isDirectory()) {
                return errorMessage("Path points to a directory, not a file");
            }

            var contentData = getContentDetails(content);
            contentData.put("type", content.getType());
            contentData.put("url", content.getHtmlUrl());
            contentData.put("download_url", content.getDownloadUrl());
            
            // Get and decode content 
            String base64Content = content.getContent();
            if (base64Content != null) {
                // The content is base64 encoded
                String decodedContent = new String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8);
                contentData.put("content", decodedContent);
            } else {
                contentData.put("content", "");
            }
            
            result.put("file", contentData);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("File or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(description = """
    List contents of a directory in a repository.
    Returns a list of files and directories at the specified path.
    """)
    public String listDirectoryContents(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Path to the directory in the repository (use '/' for root)", required = false) String path,
            @ToolParam(description = "Branch or commit SHA (defaults to the default branch)", required = false) String ref
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
            
            // Set default path if not provided
            String dirPath = (path != null && !path.isEmpty()) ? path : "";
            
            // Get contents, using ref if provided
            List<GHContent> contents;
            if (ref != null && !ref.isEmpty()) {
                contents = repo.getDirectoryContent(dirPath, ref);
            } else {
                contents = repo.getDirectoryContent(dirPath);
            }
            
            List<Map<String, Object>> contentsList = new ArrayList<>();
            
            for (GHContent content : contents) {
                Map<String, Object> contentData = getContentDetails(content);
                contentData.put("type", content.isDirectory() ? "directory" : "file");
                contentData.put("url", content.getHtmlUrl());
                if (!content.isDirectory()) {
                    contentData.put("download_url", content.getDownloadUrl());
                }
                
                contentsList.add(contentData);
            }
            
            result.put("contents", contentsList);
            result.put("path", dirPath);
            
            return successMessage(result);
            
        } catch (GHFileNotFoundException e) {
            return errorMessage("Directory or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }

    private Map<String, Object> getContentDetails(GHContent content) {
        Map<String, Object> contentData = new HashMap<>();
        contentData.put("name", content.getName());
        contentData.put("path", content.getPath());
        contentData.put("sha", content.getSha());
        contentData.put("size", content.getSize());
        return contentData;
    }

    @Tool(description = """
    Create or update a file in a repository.
    If the file doesn't exist, it will be created. If it exists, it will be updated.
    """)
    public String createOrUpdateFile(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false) String repository,
            @ToolParam(description = "Path to the file in the repository") String path,
            @ToolParam(description = "File content") String content,
            @ToolParam(description = "Commit message") String message,
            @ToolParam(description = "Branch name (defaults to the default branch)", required = false) String branch,
            @ToolParam(description = "Current file SHA (required for updates, not for new files)", required = false) String sha
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (path == null || path.isEmpty()) {
                return errorMessage("File path is required");
            }
            
            if (content == null) {
                return errorMessage("File content is required");
            }
            
            if (message == null || message.isEmpty()) {
                return errorMessage("Commit message is required");
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
            
            // Determine branch to use
            String branchToUse = (branch != null && !branch.isEmpty()) ? 
                                  branch : repo.getDefaultBranch();
            
            // Create GHContentBuilder
            GHContentBuilder contentBuilder = repo.createContent()
                    .content(content)
                    .message(message)
                    .path(path)
                    .branch(branchToUse);
            
            // Add SHA if updating an existing file
            if (sha != null && !sha.isEmpty()) {
                contentBuilder.sha(sha);
            }
            
            // Commit the changes
            GHContentUpdateResponse response = contentBuilder.commit();
            
            // Prepare response data
            Map<String, Object> contentData = new HashMap<>();
            contentData.put("path", path);
            
            // Get the commit info
            GitCommit commit = response.getCommit();
            Map<String, Object> commitData = new HashMap<>();
            commitData.put("sha", commit.getSHA1());
            commitData.put("url", commit.getHtmlUrl());
            commitData.put("message", commit.getMessage());
            contentData.put("commit", commitData);
            
            // Get the content info
            GHContent fileContent = response.getContent();
            contentData.put("sha", fileContent.getSha());
            contentData.put("name", fileContent.getName());
            contentData.put("url", fileContent.getHtmlUrl());
            
            result.put("operation", sha != null ? "update" : "create");
            result.put("file", contentData);
            
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
    Search for code within repositories.
    Searches GitHub for code matching the query.
    """)
    public String searchCode(
            @ToolParam(description = "Search query") String query,
            @ToolParam(description = "Repository name in format 'owner/repo' to limit search", required = false) String repository,
            @ToolParam(description = "Filter by file extension (e.g., 'java', 'py')", required = false) String extension,
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
            
            // Add extension filter if provided
            if (extension != null && !extension.isEmpty()) {
                queryBuilder.append(" extension:").append(extension);
            }
            
            int actualLimit = (limit != null && limit > 0) ? limit : 20; // Default to 20 results
            
            GHContentSearchBuilder searchBuilder = github.searchContent()
                    .q(queryBuilder.toString());
            
            List<Map<String, Object>> resultsList = new ArrayList<>();
            int count = 0;
            
            for (GHContent content : searchBuilder.list().withPageSize(actualLimit)) {
                if (count >= actualLimit) {
                    break;
                }
                
                Map<String, Object> contentData = new HashMap<>();
                contentData.put("name", content.getName());
                contentData.put("path", content.getPath());
                contentData.put("sha", content.getSha());
                contentData.put("repository", content.getOwner().getFullName());
                contentData.put("html_url", content.getHtmlUrl());
                
                // Try to get a snippet of content for context
                try {
                    // The content is base64 encoded
                    String base64Content = content.getContent();
                    if (base64Content != null) {
                        String decodedContent = new String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8);
                        
                        // Get a snippet (first 200 chars or less)
                        int snippetLength = Math.min(decodedContent.length(), 200);
                        String snippet = decodedContent.substring(0, snippetLength);
                        if (snippetLength < decodedContent.length()) {
                            snippet += "...";
                        }
                        
                        contentData.put("text_matches", snippet);
                    }
                } catch (Exception e) {
                    // Ignore content retrieval errors for search results
                    contentData.put("text_matches", "[Content unavailable]");
                }
                
                resultsList.add(contentData);
                count++;
            }
            
            result.put("items", resultsList);
            result.put("count", resultsList.size());
            result.put("query", queryBuilder.toString());
            
            return successMessage(result);
            
        } catch (IOException e) {
            return errorMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return errorMessage("Unexpected error: " + e.getMessage());
        }
    }
}
