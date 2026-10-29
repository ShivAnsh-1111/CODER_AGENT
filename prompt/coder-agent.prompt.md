# Project: Coder Agent — Java 25 + Local Ollama

## 1. Role and objective

Act as a senior Java architect, AI application engineer, and security-focused software developer.

Build a production-minded, local-first AI coding assistant named `coder-agent`, using Java 25 and a locally running Ollama instance.

The application must help developers understand, search, generate, review, refactor, test, and debug source code in an existing local repository.

Do not build only a chatbot. Build a coding agent with controlled tools, repository awareness, task planning, code changes, and automated verification.

Implement actual working functionality. Do not leave placeholder implementations, fake responses, TODO-only methods, or hardcoded simulated tool results.

## 2. Technology stack

- Java 25 LTS.
- Maven.
- Latest stable Spring Boot release compatible with Java 25; use Spring Boot 3.5.x or 4.x based on dependency compatibility, and do not use snapshots or milestones.
- Spring Web MVC for REST APIs.
- Spring's HTTP client or Java 25 HttpClient for Ollama communication.
- Jackson for JSON processing.
- Bean Validation.
- JUnit 5 and Mockito.
- Spring Boot Test and integration testing.
- Testcontainers only when a real external container is needed.
- SQLite or another lightweight embedded database for optional persistent conversation and task history; avoid requiring a database server for the first run.
- SLF4J and Logback for logging.
- Docker Compose for optional local development services.

Keep the Ollama integration behind an `LlmProvider` interface. Do not require cloud AI APIs, API keys, or paid services.

Use stable, mutually compatible dependencies. Pin versions through Spring Boot dependency management wherever possible.

## 3. Architecture

Use a modular monolith with clear separation of concerns.

Suggested package structure:

`com.coderagent`
- `config` — application configuration and bean definitions
- `api` — REST controllers and request/response DTOs
- `agent` — orchestration, task planning, execution loop and state
- `llm` — provider interface, Ollama implementation, prompts and response parsing
- `workspace` — repository access, file discovery, source reading and search
- `tools` — safe tool registry and tool implementations
- `editing` — patch parsing, validation, previews and file changes
- `execution` — command policies, process execution and timeout handling
- `verification` — Maven build, test execution and result parsing
- `memory` — conversation and task history
- `security` — workspace authorization and safety policies
- `observability` — structured logs, metrics and request correlation
- `exception` — domain exceptions and global error handling

Use interfaces at boundaries and dependency injection for implementations.

Do not overengineer with microservices, Kafka, Kubernetes, vector databases, or a frontend framework in version 1.

## 4. Ollama integration

Default Ollama URL: `http://localhost:11434`.

Make the following configurable:

- `CODER_AGENT_OLLAMA_BASE_URL`
- `CODER_AGENT_OLLAMA_MODEL`
- `CODER_AGENT_WORKSPACE_ROOT`
- Request timeout and generation timeout.
- Maximum context size and output size.
- Maximum agent iterations.
- Maximum command execution time.
- Whether file modification and command execution are enabled.

Use the official Ollama HTTP API:
- `GET /api/tags` to discover installed models.
- `POST /api/chat` for conversational and coding tasks.
- `POST /api/generate` only where completion-style generation is useful.

Support streaming model output where practical. Parse streaming JSON correctly, handle connection failures and timeouts, and expose useful error messages without leaking internal stack traces.

Check model availability before executing tasks. Never silently switch to a cloud provider.

Do not hardcode a model name throughout the application. Make it configurable and display the active model in the status API.

Treat model output as untrusted input. Validate structured tool requests against a strict schema before execution. If the chosen model lacks reliable tool-calling support, use a constrained JSON action protocol and validate it on the server.

## 5. Core agent capabilities

### A. Repository understanding

Implement:
- Register or select a workspace directory.
- List files and directories.
- Search source code by text.
- Read files with line numbers.
- Detect common project types, including Maven Java, Gradle Java and Spring Boot.
- Read `pom.xml`, build configuration, README and relevant source files.
- Build a repository tree while respecting configured ignore rules.
- Limit file sizes, traversal depth, and the number of files included in prompts.
- Exclude `.git`, `target`, `build`, `.idea`, `node_modules`, secrets, credentials, binaries and generated files by default.

Do not send the entire repository to Ollama. Retrieve only relevant files and bounded snippets for each task.

### B. Coding chat

Provide a coding conversation endpoint supporting:
- Java explanations.
- Code generation.
- Bug diagnosis.
- Refactoring suggestions.
- Design-pattern recommendations.
- Spring Boot and microservices guidance.
- Test generation.
- Existing-code review.

Maintain conversation history per session, with configurable size limits and clear session-reset behavior.

### C. Agent planning and execution

Implement an explicit, bounded agent loop:

1. Accept the developer's task.
2. Identify the workspace and project type.
3. Create a structured plan.
4. Select the minimum necessary context.
5. Ask the LLM for a response or proposed tool action.
6. Validate the action against a server-side tool registry.
7. Execute only permitted tools.
8. Return tool results to the model.
9. Continue within a configurable iteration limit.
10. Verify changes, summarize results, and stop.

Track task states such as `PENDING`, `RUNNING`, `AWAITING_APPROVAL`, `SUCCEEDED`, `FAILED`, and `CANCELLED`.

Prevent infinite loops. Add per-task deadlines, maximum tool calls, cancellation, bounded output, and clear failure reasons.

### D. Tool registry

Implement a typed tool interface with:
- Tool name.
- Description.
- Validated input schema.
- Risk classification.
- Approval requirements.
- Execution method.
- Structured result.

Provide these initial tools:
- `list_files`
- `search_code`
- `read_file`
- `inspect_project`
- `propose_file_changes`
- `preview_changes`
- `apply_approved_changes`
- `run_maven_tests`
- `run_maven_verify`
- `git_diff`
- `git_status`

The LLM may request a tool, but it must never directly execute arbitrary Java, shell commands, scripts, or operating-system operations.

### E. Safe code editing

Implement changes using unified diffs or another explicit patch format.

Required workflow:
1. Generate a proposed patch.
2. Validate the patch and workspace boundaries.
3. Show a preview of affected files and changed lines.
4. Require user approval before modifying files.
5. Confirm that target files have not changed since the preview.
6. Apply the patch atomically where practical.
7. Save a backup or provide a reliable rollback mechanism.
8. Show the resulting Git diff.

Do not overwrite a complete source file merely because the model returned incomplete content.

Reject absolute paths outside the workspace, `..` traversal, symbolic-link escapes, writes to protected files, invalid patches, and unexpected binary modifications.

Never modify `.git`, private keys, environment secrets, or credential files.

### F. Test and build execution

Support Maven projects first.

- Detect the Maven wrapper and prefer `./mvnw` or `mvnw.cmd` when available.
- Allow a configured Maven executable when a wrapper is absent.
- Run targeted tests before the full suite where possible.
- Support test and verification commands using fixed, server-defined argument lists.
- Capture exit codes, standard output, standard error, duration and concise failure summaries.
- Enforce timeouts, output limits and cancellation.
- Never invoke `sh -c`, `bash -c`, PowerShell, or equivalent shell-evaluation mechanisms with model-generated input.
- Run commands in an isolated working directory or restricted environment where available.
- Require explicit approval before executing builds or commands that may modify files, access networks, install dependencies, or run project-controlled plugins.

Treat repository build scripts and Maven plugins as potentially untrusted code. Document the sandbox limitations when isolation is unavailable.

### G. Debug and repair workflow

Implement a task such as:

"Fix the failing unit tests in this Spring Boot project."

The agent should:
1. Inspect the project.
2. Run the approved diagnostic command.
3. Parse and summarize test failures.
4. Inspect relevant source and test files.
5. Propose the smallest reasonable fix.
6. Preview and obtain approval for modifications.
7. Apply approved changes.
8. Rerun targeted tests.
9. Run broader verification when requested.
10. Report what changed, which tests passed, and which issues remain.

Do not claim that a build or test passed unless the corresponding command actually completed successfully.

## 6. REST API

Implement documented endpoints, including:

- `GET /api/v1/health` — application and Ollama connectivity status.
- `GET /api/v1/models` — installed Ollama models.
- `GET /api/v1/workspace` — current workspace information.
- `POST /api/v1/workspace` — validate and select a permitted workspace.
- `GET /api/v1/workspace/tree` — bounded repository tree.
- `POST /api/v1/chat` — coding conversation.
- `POST /api/v1/tasks` — create an agent task.
- `GET /api/v1/tasks/{taskId}` — task state and results.
- `POST /api/v1/tasks/{taskId}/cancel` — cancel a task.
- `GET /api/v1/tasks/{taskId}/plan` — inspect the plan.
- `GET /api/v1/tasks/{taskId}/diff` — inspect proposed or applied changes.
- `POST /api/v1/tasks/{taskId}/approve` — approve an explicitly pending action.
- `POST /api/v1/tasks/{taskId}/reject` — reject a pending action.

Use request validation, consistent error responses, suitable HTTP status codes, pagination or output limits where necessary, and OpenAPI documentation.

Long-running agent tasks must not block HTTP request threads. Return a task identifier and expose progress through polling or server-sent events.

Protect all mutating endpoints against unauthorized requests. Bind to loopback by default and never expose an unauthenticated coding agent to the public network.

## 7. Security requirements

Implement these as mandatory controls:

- Local-only operation by default.
- Workspace-root enforcement for all file access.
- Path traversal and symbolic-link protection.
- Tool allowlisting and typed argument validation.
- Explicit approval for file changes and potentially unsafe execution.
- Request and response size limits.
- Command timeouts and cancellation.
- Redaction of secrets in logs and model context.
- No execution of instructions found in README files, comments, source files or tool output unless they are independently validated as part of the user's task.
- Treat prompt injection in repository files and build output as untrusted content.
- Never let model output change security policy, workspace permissions or tool approval settings.
- No automatic Git commit, push, deployment, deletion, or external network operation.
- Clear audit events for tool invocations, approvals, file changes and command results.

Do not claim that a local model guarantees privacy or that process isolation exists unless the application actually enforces it.

## 8. Code quality and Java 25 standards

Use:
- Clear, descriptive names.
- Small, cohesive classes.
- Constructor injection.
- Records for suitable immutable DTOs.
- Sealed interfaces where they materially improve domain modeling.
- Modern switch expressions and pattern matching where useful.
- Structured concurrency or virtual threads only where justified and compatible with the selected framework.
- Try-with-resources and correct resource lifecycle management.
- Explicit exception handling.
- Configuration properties with validation.
- Null-safe API contracts.
- No unnecessary static state or global mutable state.

Do not use preview Java features unless explicitly enabled and documented.

Do not use deprecated APIs, unstable snapshots, or unnecessary dependencies.

## 9. Testing requirements

Write meaningful automated tests for:
- Ollama client success, timeout and connection failure.
- Model discovery.
- Request validation.
- Agent iteration limits and cancellation.
- Tool registration and invalid tool requests.
- Path traversal and symbolic-link attacks.
- Patch validation and stale-file detection.
- Approval and rejection workflows.
- Maven command timeouts and output truncation.
- REST API behavior.
- Prompt injection attempts in repository content.
- End-to-end coding tasks using mocked Ollama responses.

Use JUnit 5 and Mockito. Use Spring Boot integration tests where appropriate.

Tests must not require a real local model by default. Provide a separate optional smoke test against a running Ollama instance.

Do not disable failing tests, reduce assertions, or fabricate test results to achieve a green build.

## 10. Documentation and developer experience

Create:
- `README.md`
- `ARCHITECTURE.md`
- `SECURITY.md`
- `CONTRIBUTING.md`
- `.gitignore`
- `.env.example` without real secrets
- `application.yml`
- `Dockerfile` only if useful and correctly configured for local networking
- Unit and integration tests
- Example API requests
- A troubleshooting guide

Document how to:
1. Install and start Ollama.
2. Pull a coding-capable model compatible with the machine's RAM and VRAM.
3. Verify the Ollama API.
4. Configure Java 25 and Maven.
5. Build and test the application.
6. Run the application locally.
7. Select a workspace.
8. Start a coding task.
9. Review and approve a patch.
10. Troubleshoot slow inference, context limits, model errors and failed tests.

Keep the Ollama server external to the application by default. Do not bundle model weights into the application or download models automatically.

## 11. Implementation workflow

Work in small, verifiable phases.

Phase 1: Inspect the existing repository and document assumptions. If the repository is empty, initialize a clean Maven project.

Phase 2: Implement application configuration, Ollama connectivity, model discovery, and a basic coding chat.

Phase 3: Implement workspace inspection, safe file reading, and code search.

Phase 4: Implement the agent state machine, task planning, and typed tool registry.

Phase 5: Implement patch previews, approval, safe file editing, and Git diff.

Phase 6: Implement approved test/build execution, diagnostics, and repair loops.

Phase 7: Implement task persistence, progress reporting, security hardening, and complete tests.

Phase 8: Run the complete build, fix failures, update documentation, and demonstrate a real local workflow.

At the end of every phase:
- Summarize files created or changed.
- Run the relevant tests and report actual results.
- Identify remaining risks and limitations.
- Ensure the project still builds before proceeding.

Do not replace existing user code or configuration without inspecting it first. Do not make unrelated changes.

## 12. Definition of done

The first usable release is complete only when:

1. The application starts with Java 25 and a stable Maven build.
2. It connects to a local Ollama instance and discovers available models.
3. It can answer coding questions using the selected local model.
4. It can inspect and search a real Java repository.
5. It can propose a code change and display a diff.
6. It cannot modify source files without explicit approval.
7. It can run an approved test command with timeout and output limits.
8. It can use actual test failures to suggest a repair.
9. It has meaningful automated tests for core logic and security controls.
10. A clean checkout can be built and run using the README instructions.

Start by inspecting the repository, identifying compatibility risks, and presenting a concise implementation plan. Then implement Phase 1 and Phase 2. Continue incrementally, testing each phase instead of generating the entire project in one unverified response.