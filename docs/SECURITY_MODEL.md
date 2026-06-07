# Security Model

Hermes Mobile is designed for private-network first use.

## Stored On Android

- backend URL
- selected theme
- workbench default path
- encrypted dashboard cookies
- transient UI state

## Not Stored On Android

- provider API keys
- dashboard password after login
- raw cookies outside encrypted preferences
- Hermes memory database
- desktop project files
- desktop-host secrets
- raw gateway frames
- prompt bodies in diagnostic breadcrumbs

## Dangerous Actions

These actions should remain locked unless separately designed, confirmed, and verified:

- environment reveal
- credential edits
- provider secret edits
- service restart
- destructive file operations
- imports/deletes/updates
- hooks and automation changes
- privileged command approval without explicit confirmation

## Logging Rules

Allowed:

- `ws.open`
- `ws.closed`
- `ws.failure`
- `session.create.request`
- `session.resume.ok`
- `prompt.submit.failed`

Not allowed:

- prompt bodies
- passwords
- secret values
- cookies
- tokens
- raw WebSocket frames
- local machine usernames or absolute private paths

## Public Repo Hygiene

Do not commit:

- `local.properties`
- `device-verification/`
- screenshots
- logcat output
- build outputs
- encrypted app state
- real dashboard URLs
- real tailnet hostnames or IPs

