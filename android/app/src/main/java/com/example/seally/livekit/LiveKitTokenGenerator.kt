package com.example.seally.livekit

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LiveKitTokenGenerator {
    private const val ALGORITHM = "HmacSHA256"
    private const val JWT_ALGORITHM = "HS256"
    private const val TOKEN_EXPIRY_SECONDS = 7200L // 2 hours

    data class TokenOptions(
        val room: String,
        val identity: String,
        val canPublish: Boolean,
        val canSubscribe: Boolean,
    )

    fun generatePublisherToken(roomCode: String, apiKey: String, apiSecret: String): String {
        return generateToken(
            options = TokenOptions(
                room = roomCode,
                identity = "mobile-publisher",
                canPublish = true,
                canSubscribe = true,
            ),
            apiKey = apiKey,
            apiSecret = apiSecret,
        )
    }

    fun generateToken(options: TokenOptions, apiKey: String, apiSecret: String): String {
        val header = mapOf(
            "alg" to JWT_ALGORITHM,
            "typ" to "JWT",
        )

        val exp = System.currentTimeMillis() / 1000 + TOKEN_EXPIRY_SECONDS

        val videoClaim = mapOf(
            "roomJoin" to true,
            "room" to options.room,
            "canPublish" to options.canPublish,
            "canSubscribe" to options.canSubscribe,
        )

        val payload = mapOf(
            "video" to videoClaim,
            "iss" to apiKey,
            "exp" to exp,
            "nbf" to 0,
            "sub" to options.identity,
        )

        val headerJson = toJson(header)
        val payloadJson = toJson(payload)

        val headerPart = base64UrlEncode(headerJson.toByteArray())
        val payloadPart = base64UrlEncode(payloadJson.toByteArray())
        val message = "$headerPart.$payloadPart"

        val signature = signHmacSHA256(message, apiSecret)
        val signaturePart = base64UrlEncode(signature)

        return "$message.$signaturePart"
    }

    private fun toJson(map: Map<*, *>): String {
        return map.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
        ) { (key, value) ->
            val keyString = "\"$key\""
            val valueString = when (value) {
                is Boolean -> value.toString()
                is Number -> value.toString()
                is String -> "\"${value.escapeJson()}\""
                is Map<*, *> -> toJson(value)
                else -> "\"$value\""
            }
            "$keyString:$valueString"
        }
    }

    private fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun base64UrlEncode(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP or Base64.NO_PADDING)
            .replace('+', '-')
            .replace('/', '_')
    }

    private fun signHmacSHA256(data: String, secret: String): ByteArray {
        val keySpec = SecretKeySpec(secret.toByteArray(), ALGORITHM)
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(keySpec)
        return mac.doFinal(data.toByteArray())
    }

    fun generateRoomCode(length: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}
