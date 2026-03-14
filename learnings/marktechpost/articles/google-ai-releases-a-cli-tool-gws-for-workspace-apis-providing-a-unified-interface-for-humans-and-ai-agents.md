---
title: "Google AI Releases a CLI Tool (gws) for Workspace APIs: Providing a Unified Interface for Humans and AI Agents"
date: "2026-03-05T14:50:19"
modified: "2026-03-05T14:50:29"
url: "https://www.marktechpost.com/2026/03/05/google-ai-releases-a-cli-tool-gws-for-workspace-apis-providing-a-unified-interface-for-humans-and-ai-agents/"
slug: "google-ai-releases-a-cli-tool-gws-for-workspace-apis-providing-a-unified-interface-for-humans-and-ai-agents"
---

![Google AI Releases a CLI Tool (gws) for Workspace APIs: Providing a Unified Interface for Humans and AI Agents](../images/120e55c41d38d749.png)

# Google AI Releases a CLI Tool (gws) for Workspace APIs: Providing a Unified Interface for Humans and AI Agents

> Integrating Google Workspace APIs—such as Drive, Gmail, Calendar, and Sheets—into applications and data pipelines typically requires writing boilerplate code to handle REST endpoints, pagination, and OAuth 2.0 flows. Google AI team just released a CLI Tool (gws) for Google Workspace. The open-source googleworkspace/cli (invoked via the gws command) provides a unified, dynamic command-line interface to […]

Integrating Google Workspace APIs—such as Drive, Gmail, Calendar, and Sheets—into applications and data pipelines typically requires writing boilerplate code to handle REST endpoints, pagination, and OAuth 2.0 flows. Google AI team just released a CLI Tool (gws) for Google Workspace. The open-source `googleworkspace/cli` (invoked via the `gws` command) provides a unified, dynamic command-line interface to manage these services.

Designed for both human developers and AI agents, `gws` eliminates the need for custom wrapper scripts by providing structured JSON outputs, native Model Context Protocol (MCP) support, and automated authentication workflows.

### Dynamic API Discovery Architecture

Unlike traditional CLI tools that compile a static list of commands, `gws` builds its command surface dynamically at runtime.

When executed, `gws` uses a two-phase parsing strategy:

- It reads the first argument to identify the target service (e.g., `drive`).

- It fetches that service’s Google Discovery Document (cached for 24 hours).

- It builds a command tree from the document’s resources and methods.

- It parses the remaining arguments, authenticates, and executes the HTTP request.

Because of this architecture, `gws` automatically supports new Google Workspace API endpoints the moment they are added to the Discovery Service.

### Core Features for Software Engineers and Data Scientists

The CLI can be installed via npm (`npm install -g @googleworkspace/cli`) or built from source (`cargo install --path .`). Once installed, it offers several built-in utilities for data extraction and automation:

- **Introspection and Preview:** Every resource includes `--help` documentation generated from the Discovery API. You can view the schema of any method (e.g., `gws schema drive.files.list`) or use the `--dry-run` flag to preview the exact HTTP request before execution.

- **Structured Data Extraction:** By default, every response—including errors and metadata—is returned as structured JSON.

- **Auto-Pagination:** For devs pulling large datasets, the `--page-all` flag automatically handles API cursors. It streams paginated results as NDJSON (Newline Delimited JSON), which can be piped directly into command-line JSON processors:Bash`gws drive files list --params '{"pageSize": 100}' --page-all | jq -r '.files[].name'`

### Integration with AI Agents and MCP

A primary use case for `gws` is serving as a tool-calling backend for Large Language Models (LLMs).

- **Model Context Protocol (MCP) Server:** By running `gws mcp -s drive,gmail,calendar`, the CLI starts an MCP server over `stdio`. This exposes Workspace APIs as structured tools that any MCP-compatible client (like Claude Desktop or VS Code) can natively call.

- **Pre-built Agent Skills:** The repository includes over 100 Agent Skills covering all supported APIs and common workflows. AI Engineers can install these directly into agent environments using `npx skills add github:googleworkspace/cli`.

- **Gemini CLI Extension:** Developers using the Gemini CLI can install the `gws` extension (`gemini extensions install https://github.com/googleworkspace/cli`), allowing the local Gemini agent to inherit `gws` credentials and manage Workspace resources natively.

- **Model Armor (Response Sanitization):** To mitigate prompt injection risks when feeding API data to an LLM, `gws` supports Google Cloud Model Armor. Passing the `--sanitize` flag scans API responses for malicious payloads before the data reaches your agent.

### Authentication Workflows

The CLI handles authentication securely across different environments, replacing the need for manual token management in custom scripts. Precedence is given to explicit tokens, followed by credentials files, and finally local keyring storage.

- **Local Desktop:** Running `gws auth setup` initiates an interactive flow to configure a Google Cloud project, enable necessary APIs, and handle OAuth login. Credentials are encrypted at rest using AES-256-GCM and stored in the OS keyring.

- **Headless / CI/CD:** For server environments, developers can complete the interactive auth locally and export the plaintext credentials:Bash`gws auth export --unmasked > credentials.json `On the headless machine, point the CLI to this file using an environment variable: `export GOOGLE_WORKSPACE_CLI_CREDENTIALS_FILE=/path/to/credentials.json`.

- **Service Accounts:** `gws` natively supports server-to-server Service Account key files and Domain-Wide Delegation via the `GOOGLE_WORKSPACE_CLI_IMPERSONATED_USER` variable.

---

Check out the **[Repo here](https://github.com/googleworkspace/cli). **Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[120k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**
