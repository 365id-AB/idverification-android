package com.id365.exampleapp.communication
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class Communicator(
    private val baseUrl: String = "https://global-customer-frontend.365id.com/api/v1/"
) {
    private val client = HttpClient(CIO) {
        install(HttpTimeout)
    }
    suspend fun authenticate(clientId: String, clientSecret: String): Token {
        Log.d(DebugTAG, "Authenticating with 365id backend")
        val response = client.post("$baseUrl/access_token") {
            contentType(ContentType.Application.Json)
            setBody(AccessTokenRequest(clientId, clientSecret).toJson())
            timeout { requestTimeoutMillis = 61000L }
        }
        return if (response.status == HttpStatusCode.OK) {
            Token.fromJson(response.bodyAsText())
        } else {
            throw Exception("${response.status}: ${response.bodyAsText()}")
        }
    }

    suspend fun refresh(token: Token): Token {
        Log.d(DebugTAG, "Refreshing the token")
        val response = client.post("$baseUrl/refresh_token") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(token.refreshToken).toJson())
            headers { set("Authorization", token.accessToken) }
            timeout { requestTimeoutMillis = 61000L }
        }
        return if (response.status == HttpStatusCode.OK) {
            Token.fromJson(response.bodyAsText())
        } else {
            throw Exception("${response.status}: ${response.bodyAsText()}")
        }
    }

    companion object {
        val DebugTAG = "ExampleAppCommunicator"
    }
}