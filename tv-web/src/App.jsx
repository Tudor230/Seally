import { useEffect, useMemo, useRef, useState } from 'react'
import { Room, RoomEvent, Track } from 'livekit-client'
import './App.css'

const LANDMARK_TOPIC = import.meta.env.VITE_LIVEKIT_LANDMARK_TOPIC || 'pose.normalized.v1'
const FORCE_RELAY = (import.meta.env.VITE_LIVEKIT_FORCE_RELAY || 'true') !== 'false'

function App() {
  const [mUrl, setMUrl] = useState(import.meta.env.VITE_LIVEKIT_URL || '')
  const [mToken, setMToken] = useState(import.meta.env.VITE_LIVEKIT_TOKEN || '')
  const [mStatus, setMStatus] = useState('Disconnected')
  const [mError, setMError] = useState('')
  const [mDebug, setMDebug] = useState('No remote publications.')
  const [mLandmarkSeq, setMLandmarkSeq] = useState(null)
  const [mLandmarkCount, setMLandmarkCount] = useState(0)
  const [mRoom, setMRoom] = useState(null)
  const mVideoContainerRef = useRef(null)
  const mDecoder = useMemo(() => new TextDecoder(), [])

  useEffect(() => {
    return () => {
      mRoom?.disconnect()
    }
  }, [mRoom])

  const attachVideo = (track) => {
    const element = track.attach()
    element.className = 'remote-video'
    element.autoplay = true
    element.playsInline = true
    if (mVideoContainerRef.current) {
      mVideoContainerRef.current.replaceChildren(element)
    }
    element.play?.().catch(() => {})
    setMStatus('Connected (video subscribed)')
  }

  const inspectPublications = (room) => {
    const lines = []
    for (const participant of room.remoteParticipants.values()) {
      for (const publication of participant.trackPublications.values()) {
        lines.push(
          `${participant.identity} ${publication.kind} ${publication.isSubscribed ? 'subscribed' : 'pending'}`,
        )
        if (publication.kind === Track.Kind.Video) {
          publication.setSubscribed(true)
          if (publication.videoTrack) {
            attachVideo(publication.videoTrack)
          }
        }
      }
    }
    setMDebug(lines.length ? lines.join(' | ') : 'No remote publications.')
  }

  const handleConnect = async () => {
    if (!mUrl.trim() || !mToken.trim()) {
      setMError('Missing URL/token.')
      return
    }

    setMError('')
    setMStatus('Connecting...')

    const room = new Room({
      adaptiveStream: true,
      dynacast: true,
      rtcConfig: FORCE_RELAY ? { iceTransportPolicy: 'relay' } : undefined,
    })

    room.on(RoomEvent.TrackSubscribed, (track) => {
      if (track.kind === Track.Kind.Video) {
        attachVideo(track)
      }
    })

    room.on(RoomEvent.TrackPublished, (publication) => {
      if (publication.kind === Track.Kind.Video) {
        publication.setSubscribed(true)
      }
    })

    room.on(RoomEvent.ParticipantConnected, () => {
      inspectPublications(room)
    })

    room.on(RoomEvent.DataReceived, (payload, participant, _kind, topic) => {
      if (topic !== LANDMARK_TOPIC) return
      try {
        const data = JSON.parse(mDecoder.decode(payload))
        setMLandmarkSeq(data.seq ?? null)
        setMLandmarkCount(Array.isArray(data.landmarks) ? data.landmarks.length : 0)
        setMStatus(`Connected (landmarks from ${participant?.identity || 'unknown'})`)
      } catch (error) {
        setMError(`Invalid landmark payload: ${error.message}`)
      }
    })

    room.on(RoomEvent.Disconnected, () => {
      setMStatus('Disconnected')
      setMDebug('No remote publications.')
    })

    try {
      await room.connect(mUrl.trim(), mToken.trim())
      setMRoom(room)
      setMStatus('Connected (waiting for video/data)')
      inspectPublications(room)
    } catch (error) {
      setMError(`Connect failed: ${error.message}`)
      setMStatus('Disconnected')
      room.disconnect()
    }
  }

  const handleDisconnect = () => {
    mRoom?.disconnect()
    setMRoom(null)
    setMStatus('Disconnected')
    setMDebug('No remote publications.')
  }

  return (
    <main className="page">
      <h1>Seally LiveKit Receiver</h1>

      <section className="panel">
        <label>
          LiveKit URL
          <input value={mUrl} onChange={(event) => setMUrl(event.target.value)} />
        </label>
        <label>
          Viewer token
          <textarea value={mToken} onChange={(event) => setMToken(event.target.value)} />
        </label>
        <div className="actions">
          <button disabled={!!mRoom} onClick={handleConnect}>Connect</button>
          <button disabled={!mRoom} onClick={handleDisconnect}>Disconnect</button>
        </div>
      </section>

      <section className="status">
        <p><strong>Status:</strong> {mStatus}</p>
        <p><strong>Topic:</strong> {LANDMARK_TOPIC}</p>
        <p><strong>Landmarks:</strong> seq={mLandmarkSeq ?? '-'} count={mLandmarkCount}</p>
        <p className="debug">{mDebug}</p>
        {mError && <p className="error">{mError}</p>}
      </section>

      <section className="video-stage">
        <div ref={mVideoContainerRef} className="video-container" />
      </section>
    </main>
  )
}

export default App
