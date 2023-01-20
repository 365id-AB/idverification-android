package com.id365.exampleapp

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Objects.isNull
import java.util.concurrent.TimeUnit

// Handle Token requests
class TokenHandler {

    //region Properties
    @SerializedName("access_token")
    private var accessToken: String? = null
    @SerializedName("expires_in")
    private var expiresIn: Int? = null
    @SerializedName("refresh_token")
    private var refreshToken: String? = null
    @SerializedName("expireTimeTxt")
    private var expireTimeTxt: String? = null
    @SerializedName("clientSecret")
    private var clientSecret: String? = null
    @SerializedName("clientId")
    private var clientId: String? = null
    //endregion

    /** Set the expire time based on the expiredIn from the response */
    private fun setExpireTimeTxt() {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.SECOND, this.expiresIn ?: 0)
        this.expireTimeTxt = getDateFormatter().format(calendar.time)
    }

    /** Get seconds left of active token */
    private fun getTokenSecondsLeft(): Int {
        val dateTxt = this.expireTimeTxt ?: return 0
        val futureDate: Date?
        try {
            futureDate = getDateFormatter().parse(dateTxt)
        }
        catch (ignored: ParseException) {
            return 0
        }
        val dateNow = Date()
        val milliSecondsLeft: Long = futureDate.time - dateNow.time
        var secondsLeft: Int = TimeUnit.MILLISECONDS.toSeconds(milliSecondsLeft).toInt()
        if (secondsLeft < 0) {
            secondsLeft = 0
        }
        return secondsLeft
    }

    /** Check if the token is valid based on the expire date */
    private fun isValidToken(): Boolean {
        val minimumSecondsToUseToken = 25
        return getTokenSecondsLeft() >= minimumSecondsToUseToken
    }

    /** Get a UTC date formatter */
    private fun getDateFormatter(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    companion object {

        private val baseUrl: String
            get() {
                return "https://eu.customer.365id.com/api/v1/"
            }

        private const val STORED_TOKEN_DATA_KEY: String = "TokenData365ID"
        private const val TAG: String = "getToken()"

        /** Get sharedPreferences to handle device data */
        private fun getSharedPreferences(context: Context): SharedPreferences = context.getSharedPreferences(STORED_TOKEN_DATA_KEY, MODE_PRIVATE)

        /** Create TokenData object from json */
        private fun jsonToTokenData(json: String): TokenHandler? {
            return try {
                Gson().fromJson(json, TokenHandler::class.java)
            }
            catch (ignored: Exception) {
                null
            }
        }

        /** Create json from TokenData object */
        private fun TokenHandler.tokenDataToJson(): String? {
            return try {
                Gson().toJson(this)
            } catch (ignored: Exception) {
                null
            }
        }

        /** Save the TokenData on the device */
        private fun save(context: Context, clientSecret: String, clientId: String, responseBody: String?): Boolean {
            if (responseBody == null) return false
            val tokenData = jsonToTokenData(responseBody) ?: return false
            tokenData.setExpireTimeTxt()
            tokenData.clientId = clientId
            tokenData.clientSecret = clientSecret
            val updatedJson = tokenData.tokenDataToJson() ?: return false
            getSharedPreferences(context).edit().putString(STORED_TOKEN_DATA_KEY, updatedJson).apply()
            return true
        }

        /** Load the TokenData from the device */
        private fun load(context: Context): TokenHandler? {
            val json: String = getSharedPreferences(context).getString(STORED_TOKEN_DATA_KEY, null) ?: return null
            val tokenData = jsonToTokenData(json) ?: run {
                delete(context)
                return null
            }
            return tokenData
        }

        /** Delete the TokenData from the device */
        private fun delete(context: Context) {
            getSharedPreferences(context).edit().remove(STORED_TOKEN_DATA_KEY).apply()
        }

        /** Delete TokenData if Credentials has been changed */
        private fun deleteIfCredentialsChanged(context: Context, clientSecret: String, clientId: String) {
            val tokenData = load(context) ?: return
            val storedClientId = tokenData.clientId
            val storedClientSecret = tokenData.clientSecret
            if (!storedClientId.equals(clientId) || !storedClientSecret.equals(clientSecret)) {
                delete(context)
            }
        }

        /** Get a token for starting the SDK */
        fun getToken(context: Context, clientSecret: String, clientId: String, completionHandler: (String?) -> Unit) {
            deleteIfCredentialsChanged(context, clientSecret, clientId)
            val tokenData: TokenHandler? = load(context)

            if (tokenData != null) {
                Log.d(TAG, tokenData.getTokenSecondsLeft().toString() + " seconds left of token.")
                if (tokenData.isValidToken()) {
                    Log.d(TAG, "Using stored token.")
                    completionHandler(tokenData.accessToken)
                    return
                }
            }

            val action: String
            val bodyParams = JSONObject()
            val bearer: String?
            if (tokenData != null) {
                action = "refresh_token"
                bodyParams.put("refresh_token", tokenData.refreshToken)
                bearer = tokenData.accessToken
                delete(context)
            }
            else {
                action = "access_token"
                bodyParams.put("client_id", clientId)
                bodyParams.put("client_secret", clientSecret)
                bearer = null
            }

            Thread {
                var responseBody: String? = null
                var exception: java.lang.Exception? = null
                var conn: HttpURLConnection? = null
                try {
                    val url = URL(baseUrl + action)
                    val urlConnection: URLConnection = url.openConnection()
                    conn = urlConnection as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.requestMethod = "POST"
                    conn.doOutput = false
                    conn.doInput = true
                    conn.useCaches = false
                    conn.setRequestProperty("Content-Type", "application/json")
                    if (!bearer.isNullOrEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer $bearer")
                    }
                    val outputStream: OutputStream = conn.outputStream
                    outputStream.write(bodyParams.toString().toByteArray())
                    outputStream.close()
                    if (conn.responseCode == 200) {
                        val inputStream: InputStream = conn.inputStream
                        responseBody = inputStream.bufferedReader().use(BufferedReader::readText)
                        inputStream.close()
                    }
                } catch (newException: java.lang.Exception) {
                    exception = newException
                } finally {
                    conn?.disconnect()
                    if (
                        !isNull(exception) ||
                        responseBody.isNullOrEmpty() ||
                        !save(context, clientSecret, clientId, responseBody)
                    ) {
                        Log.d(TAG, "Error:")
                        Log.d(TAG, exception?.message ?: "N/A")
                        Handler(Looper.getMainLooper()).post {
                            completionHandler(null)
                        }
                    }
                    else {
                        Log.d(TAG, "Success!")
                        Handler(Looper.getMainLooper()).post {
                            completionHandler(load(context)?.accessToken)
                        }
                    }
                }
            }.start()
        }
    }
}