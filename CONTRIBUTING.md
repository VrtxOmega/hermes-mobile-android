# Contributing

This project is early and mobile-first.

## Development Rules

- Keep Chat and the composer reachable on a real phone viewport.
- Keep provider secrets and desktop-host credentials out of Android.
- Keep dangerous/admin actions locked until they have explicit confirmation flows.
- Add focused tests for parser, URL, interaction, evidence, and send-safety logic.
- Do not commit screenshots, logs, local paths, device IDs, or private backend URLs.

## Test

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Pull Requests

Small, reviewable changes are preferred. Include:

- what changed
- why it changed
- test commands and result
- screenshots only if they are sanitized

