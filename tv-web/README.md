# Seally TV Web Receiver

This web app connects to a LiveKit room and renders a body skeleton on canvas from compact binary landmark packets on topic `pose.binary.v2` (configurable). Remote video subscription is disabled in this mode.

## Environment

Create `tv-web/.env`:

```bash
VITE_LIVEKIT_URL=wss://your-project.livekit.cloud
VITE_LIVEKIT_TOKEN=your-web-viewer-token
VITE_LIVEKIT_LANDMARK_TOPIC=pose.binary.v2
VITE_LIVEKIT_FORCE_RELAY=false
```

## Run

```bash
npm install
npm run dev
```

## Generate one token per client

From repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate-livekit-tokens.ps1 `
  -ApiKey "<LIVEKIT_API_KEY>" `
  -ApiSecret "<LIVEKIT_API_SECRET>" `
  -Room "seally-room"
```

The script prints:
- `MOBILE_TOKEN` (identity: `mobile-publisher`, publish + subscribe)
- `WEB_TOKEN` (identity: `web-viewer`, subscribe only)
