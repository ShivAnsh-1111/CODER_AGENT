# coder-agent

`coder-agent` is a local-first coding assistant built with Java 25, Spring Boot, and Ollama. The initial release provides model discovery and coding chat over a loopback-only REST API.

## Requirements

- Java 25
- Maven 3.6.3 or later
- Ollama running locally with a coding model pulled, for example `ollama pull qwen2.5-coder:7b`

## Configure and run

Set `CODER_AGENT_OLLAMA_MODEL` to an installed model if you do not use the default. The Ollama URL, timeouts, chat history and output bounds are also configurable with `CODER_AGENT_OLLAMA_BASE_URL`, `CODER_AGENT_OLLAMA_CONNECT_TIMEOUT`, `CODER_AGENT_OLLAMA_REQUEST_TIMEOUT`, `CODER_AGENT_MAX_HISTORY_MESSAGES`, `CODER_AGENT_MAX_CONTEXT_CHARS`, `CODER_AGENT_MAX_SESSIONS`, `CODER_AGENT_MAX_MESSAGE_CHARS`, `CODER_AGENT_MAX_OUTPUT_TOKENS`, and `CODER_AGENT_MAX_RESPONSE_CHARS`.

Build and run:

```powershell
mvn verify
mvn spring-boot:run
```

The HTTP server binds to `127.0.0.1:8080` by default. Ollama remains an external service; the application does not download or bundle models and does not fall back to a cloud provider.

## API

- `GET /api/v1/health` — application/Ollama connectivity and configured model status.
- `GET /api/v1/models` — installed Ollama models and configured active model.
- `POST /api/v1/chat` — coding conversation. Provide `sessionId` and `message` as JSON:
- `DELETE /api/v1/chat/{sessionId}` — clear that in-memory chat session.

```json
{
  "sessionId": "my-session",
  "message": "Explain how Java records work."
}
```

Sessions are held in memory and bounded by both the configured history and session limits; they reset when the application restarts.

## Security notes

The server binds to loopback by default. Chat requests and model output are size-bounded, but generated text is untrusted and must be reviewed. Later releases will introduce controlled repository tools and approval-gated file changes and command execution; those capabilities are not present in this initial phase.
