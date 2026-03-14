# Seally TV Web Receiver

This web app connects to a LiveKit room, subscribes to remote video, and reads normalized landmark packets from topic `pose.normalized.v1` (configurable).

## Environment

Create `tv-web/.env`:

```bash
VITE_LIVEKIT_URL=wss://your-project.livekit.cloud
VITE_LIVEKIT_TOKEN=your-web-viewer-token
VITE_LIVEKIT_LANDMARK_TOPIC=pose.normalized.v1
VITE_LIVEKIT_FORCE_RELAY=true
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
