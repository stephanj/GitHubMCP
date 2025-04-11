package com.devoxx.agentic.github;

import com.devoxx.agentic.github.tools.BranchService;
import com.devoxx.agentic.github.tools.CommitService;
import com.devoxx.agentic.github.tools.ContentService;
import com.devoxx.agentic.github.tools.IssueService;
import com.devoxx.agentic.github.tools.PullRequestService;
import com.devoxx.agentic.github.tools.RepositoryService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GitHubMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider mcpServices(IssueService issueService,
                                            PullRequestService pullRequestService,
                                            RepositoryService repositoryService,
                                            BranchService branchService,
                                            CommitService commitService,
                                            ContentService contentService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(issueService, pullRequestService, repositoryService, branchService, commitService, contentService)
                .build();
    }
}
