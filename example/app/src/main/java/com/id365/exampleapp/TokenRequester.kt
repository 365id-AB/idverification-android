package com.id365.exampleapp

import android.util.Log
import com.id365.idverification.contracts.grpc.Authentication.AuthenticationGrpcKt
import com.id365.idverification.contracts.grpc.Authentication.authenticateRequest
import com.id365.idverification.contracts.grpc.Authentication.refreshTokenRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import java.net.URL

class TokenRequester(
    private val url: String,
    private val license: String,
    deviceId: String,
) {
    private var token: String
    private var refreshToken: String

    init {
        try {
            val authenticationService = AuthenticationGrpcKt.AuthenticationCoroutineStub(channel())

            val request = authenticateRequest {
                licenseKey = license
                vendorId = deviceId
            }
            val response = runBlocking {
                authenticationService.authenticate(request)
            }
            token = response.token
            refreshToken = response.refreshToken

            val channel = authenticationService.channel as ManagedChannel
            channel.shutdown()
        } catch (e: StatusException) {
            Log.e("TokenRequester", "GrpcException happened during init: $e")
            throw e
        } catch (e: Exception) {
            Log.e("TokenRequester", "Unexpected exception during init: $e")
            throw e
        }
    }

    private val headers
        get() = Metadata().apply{ put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer $token")}


    private fun channel(): ManagedChannel {
        val url = URL(url)
        val port = if (url.port == -1) 5000 else url.port

        val builder = ManagedChannelBuilder.forAddress(url.host, port)
            .enableRetry()
            .maxRetryAttempts(1)
            .executor(Dispatchers.IO.asExecutor())
            .useTransportSecurity()

        return builder.build()
    }

    fun refresh(): String {
        return try {
            val authenticationService = AuthenticationGrpcKt.AuthenticationCoroutineStub(channel())

            val request = refreshTokenRequest {
                refreshToken = this@TokenRequester.refreshToken
            }
            val response = runBlocking {
                authenticationService.refreshToken(
                    request = request,
                    headers = headers
                )
            }
            token = response.token
            refreshToken = response.refreshToken

            val channel = authenticationService.channel as ManagedChannel
            channel.shutdown()
            token
        } catch (e: StatusException) {
            Log.e("TokenRequester", "GrpcException happened during refresh: $e")
            throw e
        } catch (e: Exception) {
            Log.e("TokenRequester", "Unexpected exception during refresh: $e")
            throw e
        }
    }
}