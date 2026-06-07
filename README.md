# Hermes Mobile Android

Native Kotlin/Jetpack Compose Android client for using a Hermes Agent desktop dashboard or gateway from a phone over a private network.


## What This Is

Hermes Mobile is an Android peer for a Hermes Agent desktop or dashboard host. The phone is a mobile control surface; the desktop host remains the source of truth for:

- chat sessions and message history
- profiles and model selection
- skills, memory, cron/jobs, logs, toolsets, and platform state
- agent workbench execution
- provider credentials and local project files

The Android app stores only mobile preferences, the configured backend URL, encrypted authentication cookies, and transient UI state.

## Current Features

- Password-login flow against the Hermes dashboard backend.
- Ticketed WebSocket connection to the Hermes gateway.
- Chat session create/resume and streaming assistant output.
- Session list and stored-session message history.
- Profile and model picker surfaces.
- Tool, approval, clarify, sudo, and secret-request rendering.
- Send safety layer: prompts remain recoverable until `prompt.submit` is acknowledged.
- Retry/clear UI for stale gateway or failed-send states.
- Workbench modes for Android app work, web preview, repo coding, build/test, and browser QA.
- Embedded Android browser for safe preview URLs and dashboard pages.
- Artifact/evidence parsing for preview URLs, changed files, and command evidence.
- Management tabs for profiles, models, skills, cron/jobs, memory, logs, MCP, messaging, toolsets, artifacts, and agents where the backend exposes them.
- Hermes-inspired theme presets: Nous, Nous Light, Midnight, Ember, Mono, Cyberpunk, and Slate.

## Security Model

The phone should not hold provider API keys or desktop-host secrets.

Locked by design:

- environment reveal
- credential edits
- provider secret edits
- gateway restart
- hook/import/delete/update actions
- privileged command approval without explicit user confirmation
- local Android execution of desktop-host build commands

See [docs/SECURITY_MODEL.md](docs/SECURITY_MODEL.md).

## Backend Contract

The app expects a Hermes dashboard/gateway compatible with these endpoints:

- `GET /api/status`
- `GET /api/auth/providers`
- `POST /auth/password-login`
- `GET /api/auth/me`
- `POST /api/auth/ws-ticket`
- `GET /api/sessions`
- `GET /api/sessions/{id}/messages`
- `GET /api/profiles`
- `GET /api/model/info`
- `GET /api/model/options`
- `POST /api/model/set`
- `GET /api/skills`
- `GET /api/cron/jobs`
- `GET /api/memory`
- `GET /api/logs`
- `GET /api/mcp/servers`
- `GET /api/messaging/platforms`
- `GET /api/tools/toolsets`
- `GET /api/artifacts`
- `GET /api/agents`
- `WS /api/ws?ticket=...`

See [docs/BACKEND_SETUP.md](docs/BACKEND_SETUP.md).

## Build

Requirements:

- Android Studio or Android SDK command-line tools
- JDK 17
- Android SDK 35

```bash
./gradlew testDebugUnitTest assembleDebug
```

Install on a connected Android device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Configure

The default backend URL is a placeholder:

```text
http://100.64.0.1:9119
```

Open the app, enter the real HTTPS or private-network HTTP URL for your Hermes dashboard, and log in using the dashboard password provider. Do not put backend secrets in the APK.

## Repository Layout

```text
app/src/main/java/org/hermesmobile/client/
  data/       REST, cookies, encrypted auth storage, WebSocket gateway
  model/      typed Hermes API and gateway models
  runtime/    URL, route, interaction, send-safety, evidence, and handoff logic
  ui/         Compose screens, theme presets, ViewModel
app/src/test/java/org/hermesmobile/client/
  runtime/    focused unit tests for parsing, URLs, interactions, and send safety
docs/         architecture, backend, parity, security, and release notes
examples/     sanitized backend environment example
```

## Development Notes

- Keep mobile UI compact; the chat composer must remain reachable on large and small phones.
- Never log prompt bodies, cookies, passwords, tokens, raw gateway frames, or secret values.
- Add tests for pure runtime logic before wiring new UI.
- Treat destructive/admin actions as locked until separately designed and verified.

## Status

This is an early mobile client aimed at private-network use first. Release signing, public distribution, biometric app lock, notification hooks, broader device QA, and upstream packaging are future work.

