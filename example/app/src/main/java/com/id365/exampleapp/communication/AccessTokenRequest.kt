package com.id365.exampleapp.communication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class AccessTokenRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
) {
    fun toJson() = json.encodeToString(this)
    companion object {
        private val json: Json by lazy {
            Json {
                ignoreUnknownKeys = true
            }
        }
    }
}
