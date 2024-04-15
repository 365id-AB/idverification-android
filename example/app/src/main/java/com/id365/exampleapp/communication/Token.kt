package com.id365.exampleapp.communication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

@Serializable
class Token(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("scope") val scope: String = "",
) {
    fun toJson(): String = json.encodeToString(this)

    @Transient
    val expiryDate: Date = Date(Date().time + expiresIn*1000L)

    @Transient
    val isExpired: Boolean = expiryDate.before(Date(Date().time - 10000L))
    companion object {
        private val json: Json by lazy {
            Json {
                ignoreUnknownKeys = true
            }
        }
        fun fromJson(value: String): Token = json.decodeFromString(value)
    }
}