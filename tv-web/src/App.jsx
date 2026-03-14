import { useEffect, useRef, useState } from 'react'
import { Room, RoomEvent } from 'livekit-client'
import { generateViewerToken, generateRoomCode } from './lib/livekitToken'
import './App.css'

const LANDMARK_URL = import.meta.env.VITE_LIVEKIT_URL || ''
const LANDMARK_TOPIC = import.meta.env.VITE_LIVEKIT_LANDMARK_TOPIC || 'pose.binary.v2'
const FORCE_RELAY = (import.meta.env.VITE_LIVEKIT_FORCE_RELAY || 'true') !== 'false'
const LANDMARK_PACKET_VERSION = 2
const LANDMARK_PACKET_HEADER_BYTES = 16
const LANDMARK_PACKET_BYTES_PER_LANDMARK = 6
const LANDMARK_SCALE = 10000
const FRONT_CAMERA_FLAG = 0x01
const PROBLEMATIC_JOINTS_FLAG = 0x02
const PROBLEMATIC_JOINTS_BYTES = 4
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

const LIVEKIT_URL = import.meta.env.VITE_LIVEKIT_URL || ''

function App() {
  const [mRoomCode, setMRoomCode] = useState('')
  const [mToken, setMToken] = useState('')
  const [mStatus, setMStatus] = useState('Disconnected')
  const [mError, setMError] = useState('')
  const [mDebug, setMDebug] = useState('No remote participants.')
  const [mSkeletonFrame, setMSkeletonFrame] = useState(null)
  const [mRoom, setMRoom] = useState(null)
  const [mRotation, setMRotation] = useState(0)
  const [mIsFullscreen, setIsFullscreen] = useState(false)
  const mCanvasRef = useRef(null)
  const mRxWindowStartMsRef = useRef(0)
  const mRxWindowPacketCountRef = useRef(0)
  const mVideoStageRef = useRef(null)

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
    const flags = view.getUint8(1)
    const isFrontCamera = (flags & FRONT_CAMERA_FLAG) !== 0
    const hasProblematicJoints = (flags & PROBLEMATIC_JOINTS_FLAG) !== 0

    const expectedExtraBytes = hasProblematicJoints ? PROBLEMATIC_JOINTS_BYTES : 0
    const expectedBytes = LANDMARK_PACKET_HEADER_BYTES + (landmarkCount * LANDMARK_PACKET_BYTES_PER_LANDMARK) + expectedExtraBytes
    if (view.byteLength !== expectedBytes) {
      throw new Error(`invalid packet size ${view.byteLength}, expected ${expectedBytes}`)
    }

    const sequence = view.getUint32(4, true)
    const frameWidth = view.getUint16(12, true)
    const frameHeight = view.getUint16(14, true)
    const landmarks = []
    let offset = LANDMARK_PACKET_HEADER_BYTES
    for (let index = 0; index < landmarkCount; index += 1) {
      const x = view.getInt16(offset, true) / LANDMARK_SCALE
      const y = view.getInt16(offset + 2, true) / LANDMARK_SCALE
      const z = view.getInt16(offset + 4, true) / LANDMARK_SCALE
      landmarks.push({ x, y, z })
      offset += LANDMARK_PACKET_BYTES_PER_LANDMARK
    }

    let problematicBitmask = 0
    if (hasProblematicJoints) {
      problematicBitmask = view.getUint32(offset, true)
      console.log('Problematic joints bitmask:', problematicBitmask.toString(2), 'joints:', getProblematicJointIndices(problematicBitmask).map(i => jointNames[i] || i))
    }

    return { sequence, landmarkCount, frameWidth, frameHeight, isFrontCamera, landmarks, problematicBitmask }
  }

  const jointNames = {
    0: 'nose',
    1: 'left_eye_inner', 2: 'left_eye', 3: 'left_eye_outer',
    4: 'right_eye_inner', 5: 'right_eye', 6: 'right_eye_outer',
    7: 'left_ear', 8: 'right_ear',
    9: 'mouth_left', 10: 'mouth_right',
    11: 'left_shoulder', 12: 'right_shoulder',
    13: 'left_elbow', 14: 'right_elbow',
    15: 'left_wrist', 16: 'right_wrist',
    17: 'left_pinky', 18: 'right_pinky',
    19: 'left_index', 20: 'right_index',
    21: 'left_thumb', 22: 'right_thumb',
    23: 'left_hip', 24: 'right_hip',
    25: 'left_knee', 26: 'right_knee',
    27: 'left_ankle', 28: 'right_ankle',
    29: 'left_heel', 30: 'right_heel',
    31: 'left_foot_index', 32: 'right_foot_index',
  }

  const getProblematicJointIndices = (bitmask) => {
    const indices = []
    for (let i = 0; i < 33; i += 1) {
      if ((bitmask & (1 << i)) !== 0) {
        indices.push(i)
      }
    }
    return indices
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

    // Apply user-selected rotation
    const isQuarterTurn = mRotation === 90 || mRotation === 270
    const sourceWidth = isQuarterTurn ? frame.frameHeight : frame.frameWidth
    const sourceHeight = isQuarterTurn ? frame.frameWidth : frame.frameHeight

    const padding = 0.9
    const scale = Math.min(renderWidth / sourceWidth, renderHeight / sourceHeight) * padding
    const scaledFrameWidth = sourceWidth * scale
    const scaledFrameHeight = sourceHeight * scale
    const offsetX = (renderWidth - scaledFrameWidth) / 2
    const offsetY = (renderHeight - scaledFrameHeight) / 2

    const mapPoint = (landmark) => {
      if (!landmark) return null
      
      let x = landmark.x
      let y = landmark.y
      
      // Apply rotation
      if (mRotation === 90) {
        // 90° clockwise: (x, y) -> (y, 1-x)
        const temp = x
        x = y
        y = 1 - temp
      } else if (mRotation === 180) {
        // 180°: (x, y) -> (1-x, 1-y)
        x = 1 - x
        y = 1 - y
      } else if (mRotation === 270) {
        // 270° clockwise: (x, y) -> (1-y, x)
        const temp = x
        x = 1 - y
        y = temp
      }
      
      // Mirror for front camera
      const mirroredX = frame.isFrontCamera ? 1 - x : x
      
      return {
        x: (mirroredX * scaledFrameWidth) + offsetX,
        y: (y * scaledFrameHeight) + offsetY
      }
    }

    const isProblematicJoint = (index) => {
      if (!frame.problematicBitmask) return false
      return (frame.problematicBitmask & (1 << index)) !== 0
    }

    // Draw connections - use red only for connections between two problematic joints
    ctx.lineWidth = 3
    for (const [startIndex, endIndex] of POSE_CONNECTIONS) {
      const start = mapPoint(frame.landmarks[startIndex])
      const end = mapPoint(frame.landmarks[endIndex])
      if (!start || !end) continue

      const isProblematic = isProblematicJoint(startIndex) && isProblematicJoint(endIndex)
      ctx.strokeStyle = isProblematic ? '#ff4444' : '#00ffff'
      ctx.beginPath()
      ctx.moveTo(start.x, start.y)
      ctx.lineTo(end.x, end.y)
      ctx.stroke()
    }

    // Draw joints - red for problematic, yellow for normal
    for (let i = 0; i < frame.landmarks.length; i += 1) {
      const landmark = frame.landmarks[i]
      const point = mapPoint(landmark)
      if (!point) continue
      ctx.fillStyle = isProblematicJoint(i) ? '#ff4444' : '#ffe300'
      ctx.beginPath()
      ctx.arc(point.x, point.y, 5, 0, Math.PI * 2)
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

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange)
    }
  }, [])

  const inspectParticipants = (room) => {
    const lines = []
    for (const participant of room.remoteParticipants.values()) {
      lines.push(participant.identity)
    }
    setMDebug(lines.length ? `Participants: ${lines.join(', ')}` : 'No remote participants.')
  }

  const handleConnect = async (token) => {
    const tokenToUse = token || mToken
    if (!tokenToUse.trim()) {
      setMError('Missing token.')
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
          mRxWindowStartMsRef.current = nowMs
          mRxWindowPacketCountRef.current = 0
        }

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
      mRxWindowStartMsRef.current = 0
      mRxWindowPacketCountRef.current = 0
      setMSkeletonFrame(null)
    })

    try {
      await room.connect(LIVEKIT_URL, tokenToUse.trim())
      setMRoom(room)
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

  const handleGenerateRoomCode = async () => {
    try {
      const code = generateRoomCode()
      const token = await generateViewerToken(code)
      setMRoomCode(code)
      setMToken(token)
      setMError('')
      // Auto-connect after generating token
      await handleConnect(token)
    } catch (error) {
      setMError(`Failed to generate room code: ${error.message}`)
    }
  }

  const handleDisconnect = () => {
    mRoom?.disconnect()
    setMRoom(null)
    setMStatus('Disconnected')
    setMDebug('No remote participants.')
    mRxWindowStartMsRef.current = 0
    mRxWindowPacketCountRef.current = 0
    setMSkeletonFrame(null)
  }

  const handleRotateLeft = () => {
    setMRotation((prev) => (prev - 90 + 360) % 360)
  }

  const handleRotateRight = () => {
    setMRotation((prev) => (prev + 90) % 360)
  }

  const handleResetRotation = () => {
    setMRotation(0)
  }

  const handleToggleFullscreen = () => {
    if (!document.fullscreenElement) {
      mVideoStageRef.current?.requestFullscreen()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen()
      setIsFullscreen(false)
    }
  }

  return (
    <main className="page">
      <header className="hero">
        <h1>Seally Ice Harbor</h1>
        <p className="hero-subtitle">
          Monitor live seal-motion landmarks from mobile in an ocean-inspired control room.
        </p>
      </header>

      <section className="panel">
        <div className="actions">
          <button disabled={!!mRoom} onClick={handleGenerateRoomCode}>
            Generate Seal Room Code
          </button>
        </div>
        {mRoomCode && (
          <div className="room-code-display">
            <p><strong>Room Code:</strong> <span className="code">{mRoomCode}</span></p>
            <p className="hint">Share this code with the mobile seal tracker to dock the session</p>
          </div>
        )}
      </section>

      <section className="status">
        <p><strong>Harbor Status:</strong> {mStatus}</p>
        <p className="debug">{mDebug}</p>
        {mError && <p className="error">{mError}</p>}
      </section>

      <section ref={mVideoStageRef} className={`video-stage${mIsFullscreen ? ' fullscreen' : ''}`}>
        <canvas ref={mCanvasRef} className="skeleton-canvas" />
        <button className="fullscreen-toggle" onClick={handleToggleFullscreen} title={mIsFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}>
          {mIsFullscreen ? '❐' : '❐'}
        </button>
        <div className="rotation-controls">
          <button onClick={handleRotateLeft} title="Rotate left">↺</button>
          <button onClick={handleResetRotation} title="Reset rotation">⟲</button>
          <button onClick={handleRotateRight} title="Rotate right">↻</button>
        </div>
      </section>
    </main>
  )
}

export default App
