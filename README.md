# GitHub MCP Server

This project implements a Model Context Protocol (MCP) server for GitHub API access. It provides a set of tools that allow LLM agents to interact with GitHub repositories, issues, pull requests, and other GitHub resources.

## Features

The server provides the following GitHub operations:

- **Repository Management**
  - List repositories for the authenticated user
  - Get repository details
  - Search for repositories

- **Issue Management**
  - List issues with filtering options
  - Get detailed information about issues
  - Create new issues
  - Add comments to issues
  - Search for issues

- **Pull Request Management**
  - List pull requests with filtering options
  - Get detailed information about pull requests
  - Create comments on pull requests
  - Merge pull requests

These operations are exposed as tools for Large Language Models using the Model Context Protocol (MCP), allowing AI systems to safely interact with GitHub through its API.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Spring Boot 3.3.6
- Spring AI MCP Server components
- A GitHub account and personal access token

### Building the Project

Build the project using Maven:

```bash
mvn clean package
```

### Running the Server

Run the server using the following command:

```bash
java -jar target/GitHubMCP-1.0-SNAPSHOT.jar
```

The server can use STDIO for communication with MCP clients or can be run as a web server.

## Environment Variables

The GitHub MCP server supports the following environment variables for authentication:

- `GITHUB_TOKEN` or `GITHUB_PERSONAL_ACCESS_TOKEN`: Your GitHub personal access token
- `GITHUB_HOST`: The base URL of your GitHub instance (e.g., `github.com` or `github.mycompany.com` for GitHub Enterprise)
- `GITHUB_REPOSITORY`: Default repository to use if not specified in API calls (e.g., `owner/repo`)

You can set these environment variables when launching the MCP server, and the GitHub services will use them as default values. This allows you to avoid having to provide authentication details with every API call.

## Usage with MCP Clients

### Using with Claude Desktop

Edit your claude_desktop_config.json file with the following:

```json
{
  "mcpServers": {
    "github": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
        "/path/to/GitHubMCP/target/GitHubMCP-1.0-SNAPSHOT.jar"
      ],
      "env": {
        "GITHUB_TOKEN": "your-github-token-here",
        "GITHUB_HOST": "github.com",
        "GITHUB_REPOSITORY": "your-username/your-repository"
      }
    }  
  }
}
```

### Using with DevoxxGenie or similar MCP clients

1. In your MCP client, access the MCP Server configuration screen
2. Configure the server with the following settings:
   - **Name**: `GitHub` (or any descriptive name)
   - **Transport Type**: `STDIO`
   - **Command**: Full path to your Java executable
   - **Arguments**:
     ```
     -Dspring.ai.mcp.server.stdio=true
     -Dspring.main.web-application-type=none
     -Dlogging.pattern.console=
     -jar
     /Users/stephan/IdeaProjects/GitHubMCP/target/GitHubMCP-1.0-SNAPSHOT.jar
     ```
   - **Environment Variables**:
     ```
     GITHUB_TOKEN=your-github-token-here
     GITHUB_HOST=github.com
     GITHUB_REPOSITORY=your-username/your-repository
     ```

## Security Considerations

When using this server, be aware that:
- You need to provide a GitHub personal access token for authentication
- The LLM agent will have access to create, read, and modify GitHub resources
- Consider running the server with appropriate permissions and in a controlled environment
- Ensure your token has only the minimum required permissions for your use case

## Example Usage

### With Environment Variables

If you've configured the MCP server with environment variables, you can simply ask:

```
Can you get a list of open issues in my GitHub repository?
```

Claude will use the GitHub services with the pre-configured authentication details to fetch the open issues from your GitHub repository and summarize them.

### With Explicit Repository

You can override the default repository by specifying it in your request:

```
Can you list the pull requests for the anthropics/claude-playground repository?
```

The GitHub service will use your authentication token but query the specified repository instead of the default one.

## Available Services

The GitHub MCP server is organized into several service classes, each providing different functionality:

1. **RepositoryService**
   - List, get, and search for repositories

2. **IssueService**
   - List, get, create, and search for issues
   - Add comments to issues

3. **PullRequestService**
   - List and get pull requests
   - Add comments to pull requests
   - Merge pull requests
