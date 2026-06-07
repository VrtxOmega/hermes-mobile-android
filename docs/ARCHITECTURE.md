# Architecture

Hermes Mobile is intentionally thin. Android owns the mobile experience; the desktop host owns Hermes Agent state and execution.

## Layers

```text
Compose UI
  HermesMobileApp
  HermesMobileViewModel

Runtime helpers
  HermesUrl
  RouteDiagnostics
  HermesInteractions
  HermesSendSafety
  WorkbenchEvidence
  WorkbenchHandoff

Data clients
  HermesRestClient
  HermesGatewayWsClient
  HermesAuthStore
  HermesCookieJar

Hermes dashboard/gateway backend
  REST endpoints
  ticketed WebSocket
  desktop-host sessions, memory, skills, profiles, models, and tools
```

## REST

`HermesRestClient` wraps the dashboard HTTP API with OkHttp and Kotlin serialization. It stores no provider credentials.

## Auth

`HermesAuthStore` uses Android Keystore-backed AES-GCM encryption for saved cookies and local app preferences. `HermesCookieJar` restores dashboard cookies for the configured backend URL.

## WebSocket

`HermesGatewayWsClient` connects through `/api/auth/ws-ticket` and then `WS /api/ws?ticket=...`.

Supported RPCs include:

- `session.create`
- `session.resume`
- `prompt.submit`
- `approval.respond`
- `clarify.respond`
- `sudo.respond`
- `secret.respond`

Gateway diagnostics are intentionally method/status based. They do not log raw frame bodies, prompt bodies, passwords, cookies, tokens, or secret values.

## Send Safety

`PendingHermesSend` keeps a prompt recoverable until the backend acknowledges `prompt.submit`.

Failure stages:

- `ensureGateway`
- `session.resume` / `session.create`
- `prompt.submit`

On failure the app marks the gateway as error, closes the stale socket, keeps the prompt in retry state, and offers retry/clear from Chat.

## Workbench

Workbench prompts describe the requested task and route it to the desktop host. Android may open preview URLs in the embedded browser, but file and command evidence remain desktop-host handoffs.

