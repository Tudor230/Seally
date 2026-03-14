import { useEffect, useRef, useState } from 'react'
import { Room, RoomEvent } from 'livekit-client'
import './App.css'

const LANDMARK_TOPIC = import.meta.env.VITE_LIVEKIT_LANDMARK_TOPIC || 'pose.binary.v2'
const FORCE_RELAY = (import.meta.env.VITE_LIVEKIT_FORCE_RELAY || 'true') !== 'false'
const LANDMARK_PACKET_VERSION = 2
const LANDMARK_PACKET_HEADER_BYTES = 16
const LANDMARK_PACKET_BYTES_PER_LANDMARK = 6
const LANDMARK_SCALE = 10000
const FRONT_CAMERA_FLAG = 0x01
const POSE_CONNECTIONS = [
  [0, 1], [0, 4], [1, 2], [2, 3], [3, 7], [4, 5], [5, 6], [6, 8],
  [9, 10], [11, 12],
  [11, 13], [13, 15], [15, 17], [15, 19], [15, 21],
  [17, 19],
  [12, 14], [14, 16], [16, 18], [16, 20], [16, 22],
  [18, 20],
  [11, 23], [12, 24], [23, 24],
  [23, 25], [24, 26], [25, 27], [26, 28],
  [27, 29], [29, 31],
  [28, 30], [30, 32],
]

function App() {
  const [mUrl, setMUrl] = useState(import.meta.env.VITE_LIVEKIT_URL || '')
  const [mToken, setMToken] = useState(import.meta.env.VITE_LIVEKIT_TOKEN || '')
  const [mStatus, setMStatus] = useState('Disconnected')
  const [mError, setMError] = useState('')
  const [mDebug, setMDebug] = useState('No remote participants.')
  const [mLandmarkSeq, setMLandmarkSeq] = useState(null)
  const [mLandmarkCount, setMLandmarkCount] = useState(0)
  const [mReceivedPps, setMReceivedPps] = useState(0)
  const [mSkeletonFrame, setMSkeletonFrame] = useState(null)
  const [mRoom, setMRoom] = useState(null)
  const mCanvasRef = useRef(null)
  const mRxWindowStartMsRef = useRef(0)
  const mRxWindowPacketCountRef = useRef(0)

  const decodeLandmarkPacket = (payload) => {
    const view = new DataView(payload.buffer, payload.byteOffset, payload.byteLength)
    if (view.byteLength < LANDMARK_PACKET_HEADER_BYTES) {
      throw new Error('packet too small')
    }

    const version = view.getUint8(0)
    if (version !== LANDMARK_PACKET_VERSION) {
      throw new Error(`unsupported packet version ${version}`)
    }

    const landmarkCount = view.getUint16(2, true)
    const expectedBytes = LANDMARK_PACKET_HEADER_BYTES + (landmarkCount * LANDMARK_PACKET_BYTES_PER_LANDMARK)
    if (view.byteLength !== expectedBytes) {
      throw new Error(`invalid packet size ${view.byteLength}, expected ${expectedBytes}`)
    }

    const sequence = view.getUint32(4, true)
    const frameWidth = view.getUint16(12, true)
    const frameHeight = view.getUint16(14, true)
    const flags = view.getUint8(1)
    const isFrontCamera = (flags & FRONT_CAMERA_FLAG) !== 0
    const landmarks = []
    let offset = LANDMARK_PACKET_HEADER_BYTES
    for (let index = 0; index < landmarkCount; index += 1) {
      const x = view.getInt16(offset, true) / LANDMARK_SCALE
      const y = view.getInt16(offset + 2, true) / LANDMARK_SCALE
      const z = view.getInt16(offset + 4, true) / LANDMARK_SCALE
      landmarks.push({ x, y, z })
      offset += LANDMARK_PACKET_BYTES_PER_LANDMARK
    }
    return { sequence, landmarkCount, frameWidth, frameHeight, isFrontCamera, landmarks }
  }

  const drawSkeleton = (frame) => {
    const canvas = mCanvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const renderWidth = Math.max(canvas.clientWidth, 1)
    const renderHeight = Math.max(canvas.clientHeight, 1)
    if (canvas.width !== renderWidth || canvas.height !== renderHeight) {
      canvas.width = renderWidth
      canvas.height = renderHeight
    }

    ctx.clearRect(0, 0, renderWidth, renderHeight)
    ctx.fillStyle = '#000'
    ctx.fillRect(0, 0, renderWidth, renderHeight)

    if (!frame || frame.landmarks.length === 0 || frame.frameWidth <= 0 || frame.frameHeight <= 0) {
      return
    }

    const scale = Math.max(renderWidth / frame.frameWidth, renderHeight / frame.frameHeight)
    const scaledFrameWidth = frame.frameWidth * scale
    const scaledFrameHeight = frame.frameHeight * scale
    const offsetX = (renderWidth - scaledFrameWidth) / 2
    const offsetY = (renderHeight - scaledFrameHeight) / 2

    const mapX = (normalizedX) => {
      const mirroredX = frame.isFrontCamera ? 1 - normalizedX : normalizedX
      return (mirroredX * scaledFrameWidth) + offsetX
    }
    const mapY = (normalizedY) => (normalizedY * scaledFrameHeight) + offsetY

    ctx.strokeStyle = '#00ffff'
    ctx.lineWidth = 3
    for (const [startIndex, endIndex] of POSE_CONNECTIONS) {
      const start = frame.landmarks[startIndex]
      const end = frame.landmarks[endIndex]
      if (!start || !end) continue
      ctx.beginPath()
      ctx.moveTo(mapX(start.x), mapY(start.y))
      ctx.lineTo(mapX(end.x), mapY(end.y))
      ctx.stroke()
    }

    ctx.fillStyle = '#ffe300'
    for (const landmark of frame.landmarks) {
      ctx.beginPath()
      ctx.arc(mapX(landmark.x), mapY(landmark.y), 4, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  useEffect(() => {
    return () => {
      mRoom?.disconnect()
    }
  }, [mRoom])

  useEffect(() => {
    drawSkeleton(mSkeletonFrame)
  }, [mSkeletonFrame])

  const inspectParticipants = (room) => {
    const lines = []
    for (const participant of room.remoteParticipants.values()) {
      lines.push(participant.identity)
    }
    setMDebug(lines.length ? `Participants: ${lines.join(', ')}` : 'No remote participants.')
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

    room.on(RoomEvent.ParticipantConnected, () => {
      inspectParticipants(room)
    })

    room.on(RoomEvent.DataReceived, (payload, participant, _kind, topic) => {
      if (topic !== LANDMARK_TOPIC) return
      try {
        const data = decodeLandmarkPacket(payload)
        const nowMs = Date.now()
        if (mRxWindowStartMsRef.current === 0) {
          mRxWindowStartMsRef.current = nowMs
        }
        mRxWindowPacketCountRef.current += 1
        const elapsedMs = nowMs - mRxWindowStartMsRef.current
        if (elapsedMs >= 1000) {
          const packetsPerSecond = Math.round((mRxWindowPacketCountRef.current * 1000) / elapsedMs)
          setMReceivedPps(packetsPerSecond)
          mRxWindowStartMsRef.current = nowMs
          mRxWindowPacketCountRef.current = 0
        }

        setMLandmarkSeq(data.sequence)
        setMLandmarkCount(data.landmarkCount)
        setMSkeletonFrame(data)
        setMError('')
        setMStatus(`Connected (landmarks from ${participant?.identity || 'unknown'})`)
      } catch (error) {
        setMError(`Invalid landmark payload: ${error.message}`)
      }
    })

    room.on(RoomEvent.Disconnected, () => {
      setMStatus('Disconnected')
      setMDebug('No remote participants.')
      setMReceivedPps(0)
      mRxWindowStartMsRef.current = 0
      mRxWindowPacketCountRef.current = 0
      setMSkeletonFrame(null)
    })

    try {
      await room.connect(mUrl.trim(), mToken.trim())
      setMRoom(room)
      setMReceivedPps(0)
      mRxWindowStartMsRef.current = 0
      mRxWindowPacketCountRef.current = 0
      setMStatus('Connected (waiting for landmarks)')
      inspectParticipants(room)
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
    setMDebug('No remote participants.')
    setMReceivedPps(0)
    mRxWindowStartMsRef.current = 0
    mRxWindowPacketCountRef.current = 0
    setMSkeletonFrame(null)
  }

  return (
    <main className="page">
      <h1>Seally LiveKit Skeleton Receiver</h1>

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
        <p><strong>Landmarks:</strong> seq={mLandmarkSeq ?? '-'} count={mLandmarkCount} rxPps={mReceivedPps}</p>
        <p className="debug">{mDebug}</p>
        {mError && <p className="error">{mError}</p>}
      </section>

      <section className="video-stage">
        <canvas ref={mCanvasRef} className="skeleton-canvas" />
      </section>
    </main>
  )
}

export default App
