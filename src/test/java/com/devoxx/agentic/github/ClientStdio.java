package com.devoxx.agentic.github;

import io.github.cdimascio.dotenv.Dotenv;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

public class ClientStdio {

    public static void main(String[] args) {
        var stdioParams = ServerParameters.builder("java")
                .args("-Dspring.ai.mcp.server.stdio=true", 
                      "-Dspring.main.web-application-type=none",
                      "-Dlogging.pattern.console=", 
                      "-jar",
                      "/Users/stephan/IdeaProjects/GitHubMCP/target/GitHubMCP-1.0-SNAPSHOT.jar")
                .addEnvVar("GITHUB_TOKEN", Dotenv.load().get("GITHUB_TOKEN"))
                .addEnvVar("GITHUB_HOST", "github.com")
                .addEnvVar("GITHUB_REPOSITORY", "stephan-dowding/mcp-examples")
                .build();

        var transport = new StdioClientTransport(stdioParams);
        var client = McpClient.sync(transport).build();

        client.initialize();

        // List and demonstrate tools
        ListToolsResult toolsList = client.listTools();
        System.out.println("Available GitHub MCP Tools = " + toolsList);

        client.closeGracefully();
    }
}
