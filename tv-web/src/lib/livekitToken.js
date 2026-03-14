import { SignJWT } from 'jose'

const TOKEN_EXPIRY_SECONDS = 7200 // 2 hours

export function generateViewerToken(roomCode) {
  return generateToken({
    room: roomCode,
    identity: 'web-viewer',
    canPublish: false,
    canSubscribe: true,
  })
}

export async function generateToken(options) {
  const { room, identity, canPublish, canSubscribe } = options

  const apiKey = import.meta.env.VITE_LIVEKIT_API_KEY
  const apiSecret = import.meta.env.VITE_LIVEKIT_API_SECRET

  if (!apiKey || !apiSecret) {
    throw new Error('LiveKit API credentials not configured')
  }

  const key = new TextEncoder().encode(apiSecret)

  const jwt = await new SignJWT({
    video: {
      roomJoin: true,
      room,
      canPublish,
      canSubscribe,
    },
  })
    .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
    .setIssuedAt()
    .setIssuer(apiKey)
    .setSubject(identity)
    .setExpirationTime(Math.floor(Date.now() / 1000) + TOKEN_EXPIRY_SECONDS)
    .setNotBefore(0)
    .sign(key)

  return jwt
}

export function generateRoomCode(length = 6) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let result = ''
  for (let i = 0; i < length; i += 1) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}
