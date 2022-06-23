package com.id365.exampleapp

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.id365.exampleapp.ui.theme.ExampleAppTheme
import com.id365.idverification._365iDRequest
import com.id365.idverification._365iDResult
import com.id365.idverification.startSdk
import com.id365.idverification.stopSdk
import com.id365.idverification.ui.theme.Id365ScannerSdkTheme
import com.id365.idverification.views.ScannerSdkView
import io.grpc.StatusException
import java.util.*

class MainActivity : ComponentActivity() {

    private val token = mutableStateOf("")
    private val result = mutableStateOf("No result generated yet")

    // URL to the 365id frontend for SDK applications
    private val url = "https://frontend-device-ag.int.365id.com:5001"

    // To get a valid license key, please contact 365id support @ support@365id.com
    private val license = "<Insert your license key here>"

    // Name of location (Optional)
    private val locationName = "<The name of location>"

    // Location Id, provided by 365id
    private val locationId = 0

    // A unique identifier for this device, generated randomly here for each session, but
    // you'll want to create your own unique identifier for each device.
    private val deviceId = UUID.randomUUID().toString()

    // This requests a token from the 365id backend.
    private val tokenRequester: TokenRequester by lazy { TokenRequester(url, license, deviceId) }

    /**
     * 365id Id Verification Android SDK requires permission to use the camera and access the NFC reader.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (permissionsGranted) {
            setViewContent()
        } else {
            val request = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                val deniedPermissions = it.filter { perm -> !perm.value }
                if (deniedPermissions.isNotEmpty()) {
                    deniedPermissions.keys.forEach { perm ->
                        Log.e("MainActivity", "Missing permission: $perm")
                    }
                    setContent {
                        Id365ScannerSdkTheme {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = MaterialTheme.colors.background),
                            ) {
                                Text(
                                    text = "The 365id Id Verification SDK Example app requires permission to use the camera and NFC to function.",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                } else {
                    setViewContent()
                }
            }
            request.launch(REQUIRED_PERMISSIONS)
        }
    }

    fun setViewContent() {
        setContent {
            Id365ScannerSdkTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainContent()
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "Home") {
            composable("Home") {
                Home(navController)
            }
            composable("SDK") {
                ScannerSdkView()
            }
        }
    }

    @Composable
    fun Home(
        navController: NavController
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 10.dp)
        ) {
            Text(
                style = MaterialTheme.typography.h5,
                text = "365id Scanner SDK Example App",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (token.value.isEmpty()) Text(
                text = "Token is not ready", textAlign = TextAlign.Center,
                modifier = Modifier.background(color = MaterialTheme.colors.error),
                color = MaterialTheme.colors.onError
            )
            else Text(
                text = "Token is ready, press 'Start Transaction' to start a scan",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(text = "Result: ${result.value}", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { requestToken() }
            ) {
                Text(text = "Request Token")
            }
            Button(
                onClick = { startTransaction(navController) },
                enabled = token.value.isNotEmpty()
            ) {
                Text(text = "Start Transaction")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    /**
     * Each token generated has a lifespan of a few minutes, after which a new one will have to be
     * generated to begin a new transaction.
     */
    fun requestToken() {
        try {
        token.value = tokenRequester.refresh()
        } catch (e: StatusException) {
            result.value = "${e.message.toString()} \n\nMake sure the license key is correct"
        } catch (e: Exception) {
            result.value = e.message.toString()
        }
    }

    /**
     * We start the SDK by calling the [startSdk] function. Once we have confirmation the app is
     * started, we switch to the SDK main view in [ScannerSdkView]. We pop the backstack in the callback
     * to return to the home screen.
     * The result in [_365iDResult] contains a TransactionId that you can later double check in
     * your backend to verify the result of the transaction.
     */
    fun startTransaction(navController: NavController) {
        val request = _365iDRequest(token.value, locationName, locationId)

        if (startSdk(this.applicationContext, request) {
                /**
                 * Callback
                 */

                val transactionId = it.transactionId
                val status = it.status

                when (status) {

                    _365iDResult.StatusType.OK -> {
                        // This is returned when a transaction completes successfully 
                        // Note: This does not mean the user identity or supplied document is verified, only that the transaction process itself did not end prematurely.
                        // The assessment shows a summary 
                        val assessment = it.assessment
                        print("Successful result")
                    }

                    _365iDResult.StatusType.Dismissed -> {
                        // This is returned if the user dismisses the SDK view prematurely.
                        print("User dismissed SDK")
                    }

                    _365iDResult.StatusType.ClientException -> {
                        // This is returned if the SDK encountered an internal error. Report such issues to 365id as bugs!
                        // We may get a unique message if a client exception happens containing the specific issue. Include it in a bug report.
                        val usermessage = it.userMessage
                        print("Client has thrown an exception")
                    }

                    _365iDResult.StatusType.ServerException -> {
                        // This is returned if there was an issue talking to 365id Cloud services. Could be a connectivity issue.
                        val usermessage = it.userMessage
                        // We may get a unique message from the 365id cloud services when this happens, containing a textual description of the backend issue. It may be a temporary server connection issue, or a bug in our backend.
                        print("Server has thrown an exception")
                    }

                    else ->
                        // This should not occur
                        print("Not supported status type was returned")

                }

                // Retrieves the result as a json
                result.value = it.asJson()

                // Stops the SDK and de-allocates the resources
                stopSdk()

            // Navigates back to Home view
            navController.navigate("Home")
        }) {
            // Navigates to the SDK view
            navController.navigate("SDK")
        }
    }


//    val callback = (it: _365iDResult) {
//        result.value = it.asJson()
//        stopSdk()
//        navController.popBackStack()
//    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        ExampleAppTheme(darkTheme = false) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background,
            ) {
                MainContent()
            }
        }
    }

    private val permissionsGranted: Boolean
        get() = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            permission.CAMERA,
            permission.NFC
        )
    }
}
