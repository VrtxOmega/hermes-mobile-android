# Backend Setup

The Android app expects a reachable Hermes dashboard/gateway over a private network, VPN, or trusted LAN.

## Network

Example private-network URL:

```text
http://100.64.0.1:9119
```

Replace it with your actual host and port.

For production or broader device use, prefer HTTPS.

## Dashboard Auth

Use dashboard username/password auth and ticketed WebSockets. Do not ship static session tokens inside the APK.

Example environment shape:

```bash
HERMES_DASHBOARD_BASIC_AUTH_USERNAME=hermes-user
HERMES_DASHBOARD_BASIC_AUTH_PASSWORD_HASH=<password-hash>
HERMES_DASHBOARD_BASIC_AUTH_SECRET=<stable-random-secret>
```

See [examples/hermes-dashboard.env.example](../examples/hermes-dashboard.env.example).

## Expected Flow

1. `GET /api/status`
2. `GET /api/auth/providers`
3. `POST /auth/password-login`
4. `GET /api/auth/me`
5. `POST /api/auth/ws-ticket`
6. `WS /api/ws?ticket=...`

## Smoke Checks

From a desktop shell:

```bash
curl http://100.64.0.1:9119/api/status
```

From Android with ADB:

```bash
adb shell toybox nc -z -w 2 100.64.0.1 9119
```

Expected `/api/status` behavior for a protected backend:

```json
{
  "auth_required": true,
  "gateway_running": true,
  "gateway_state": "running"
}
```

