package com.id365.exampleapp.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.id365.exampleapp.AppConfiguration
import com.id365.exampleapp.communication.Communicator
import com.id365.exampleapp.communication.Token
import com.id365.idverification.IdVerification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.concurrent.timer
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ViewModel @Inject constructor() :ViewModel() {
    val token = mutableStateOf(Token())
    val result = mutableStateOf("No result generated yet")
    val tokenLoading = mutableStateOf(false)
    val tokenIsValid = mutableStateOf(false)
    val documentSizeType = mutableStateOf(IdVerification.DocumentSizeType.DOCUMENT)
    private val communicator = Communicator()
    private val _onClosedEvent = MutableSharedFlow<Unit>()
    val closedEvent = _onClosedEvent.asSharedFlow()

    init {
        timer(initialDelay=10000L, period=10000L) {
            CoroutineScope(Dispatchers.Main).launch {
                tokenIsValid.value = !token.value.isExpired
            }
        }
    }


    /**
     * Each token generated has a lifespan of a few minutes, after which a new one will have to be
     * generated to begin a new transaction.
     */
    fun requestToken(context: Context) {
        tokenLoading.value = true
        CoroutineScope(Dispatchers.Main).launch {
            result.value = "Waiting for new token..."
        }
        CoroutineScope(Dispatchers.IO).launch {
            token.value = try {
                if (token.value.refreshToken.isNotEmpty()) {
                    communicator.refresh(token.value)
                } else {
                    communicator.authenticate(
                        AppConfiguration.clientId,
                        AppConfiguration.clientSecret
                    )
                }
            } catch(e: Exception) {
                result.value = "Make sure the clientId and clientSecret are correct"
                Token()
            }
        }.invokeOnCompletion { error ->
            CoroutineScope(Dispatchers.Main).launch {
                if (error != null && error !is CancellationException) {
                    context.getSharedPreferences(
                        AppConfiguration.STORED_TOKEN_DATA_KEY,
                        ComponentActivity.MODE_PRIVATE
                    )
                        .edit().remove(AppConfiguration.STORED_TOKEN_DATA_KEY).apply()
                    token.value = Token()
                    result.value = "Make sure the clientId and clientSecret are correct"
                } else {
                    context.getSharedPreferences(
                        AppConfiguration.STORED_TOKEN_DATA_KEY,
                        ComponentActivity.MODE_PRIVATE
                    )
                        .edit().putString(AppConfiguration.STORED_TOKEN_DATA_KEY, token.value.toJson()).apply()
                    result.value = ""
                }
                tokenLoading.value = false
                tokenIsValid.value = !token.value.isExpired
            }
        }
    }

    fun load(context: Context) {
        // Read token from preferences if available
        val storedToken = context.getSharedPreferences(AppConfiguration.STORED_TOKEN_DATA_KEY,
            ComponentActivity.MODE_PRIVATE
        )
            .getString(AppConfiguration.STORED_TOKEN_DATA_KEY, "") ?: ""
        if (storedToken.isNotEmpty()) token.value = Token.fromJson(storedToken)

    }

    fun onClosed() {
        viewModelScope.launch { _onClosedEvent.emit(Unit) }
    }
}
