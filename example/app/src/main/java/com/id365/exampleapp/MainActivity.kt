package com.id365.exampleapp

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Card
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.id365.exampleapp.ui.theme.CustomSdkTheme
import com.id365.exampleapp.ui.theme.ExampleAppTheme
import com.id365.exampleapp.ui.theme.animations.DifferentExampleLoadingSpinner
import com.id365.exampleapp.ui.theme.animations.ExampleLoadingSpinner
import com.id365.idverification.*
import com.id365.idverification.errors.IdVerificationException
import com.id365.idverification.views.ScannerSdkView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), IdVerificationEventHandler {

    private val token = mutableStateOf("")
    private val result = mutableStateOf("No result generated yet")
    private val tokenLoading = mutableStateOf(false)
    private val tokenIsValid = mutableStateOf(false)
    private var documentType: IdVerification.DocumentType = IdVerification.DocumentType.DOCUMENT

    // To get a valid client secret key, please contact 365id support @ support@365id.com
    private val clientSecret = "<Insert your client secret key here>"

    // To get a valid client Id key, please contact 365id support @ support@365id.com
    private val clientId = "<Insert your client Id key here>"

    private lateinit var navController: NavHostController
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
                        CustomSdkTheme {
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "Received intent: $intent")
        IdVerification.sendIntent(intent)
    }

    private fun setViewContent() {
        setContent {
            CustomSdkTheme {
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
        navController = rememberNavController()
        NavHost(navController = navController, startDestination = "Home") {
            composable("Home") {
                Home()
            }
            composable("Sdk") {
                IdVerification.ScannerSdkView()
            }
        }
    }



    @Composable
    fun Home() {
        val documentTypeDialogVisible = remember { mutableStateOf(false) }
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
            if (!tokenIsValid.value) Text(
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
            if (tokenLoading.value){
                CircularProgressIndicator()
            }
            Button(
                enabled = !tokenLoading.value,
                onClick = { requestToken() }
            ) {
                Text(text = "Request Token")
            }
            Button(
                onClick = { startTransaction(documentType) },
                enabled = tokenIsValid.value
            ) {
                Text(text = "Start Transaction")
            }
            Button(
                enabled = tokenIsValid.value,
                onClick = { documentTypeDialogVisible.value = true}
            ) {
                Text(text = "Choose Document Type")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        if (documentTypeDialogVisible.value){
            Dialog(onDismissRequest = {documentTypeDialogVisible.value = false}) {
                Card(
                    shape = androidx.compose.material3.MaterialTheme.shapes.small
                ) {
                    DocumentTypeView()
                }
            }
        }
    }

    /**
     * Each token generated has a lifespan of a few minutes, after which a new one will have to be
     * generated to begin a new transaction.
     */
    private fun requestToken() {
        tokenLoading.value = true
        TokenHandler.getToken(
            this,
            clientSecret,
            clientId
        ) { newToken: String? ->
            tokenLoading.value = false
            if (newToken != null) {
                tokenIsValid.value = true
                token.value = newToken
            }
            else {
                tokenIsValid.value = false
                result.value = "Make sure the clientSecret/clientId-key is correct"
            }
        }
    }

    /**
     * We start the SDK by calling the [start] function. Once we have confirmation the app is
     * started, we switch to the SDK main view in [IdVerificationView]. We pop the backstack in the callback
     * to return to the home screen.
     * The result in [IdVerificationResult] contains a TransactionId that you can later double check in
     * your backend to verify the result of the transaction.
     */
    private fun startTransaction(documentType: IdVerification.DocumentType) {

        /**
         * An example of how you can pick and choose what animations you want to use in the SDK.
         * This is done by overriding the default animations set in the SDK.
         */
        val theme = IdVerification.IdVerificationTheme(
            animations = IdVerification.Animations(
                prepareId3 = { ExampleLoadingSpinner() },
                loadingGeneric = { DifferentExampleLoadingSpinner() },
                loadingImageCapture = { DifferentExampleLoadingSpinner() }
            )
        )
        IdVerification.setCustomTheme(theme)

        val request = IdVerificationRequest(token.value)

        IdVerification.start(this.applicationContext, request, eventHandler = this, documentType = documentType)
    }

    /**
     * This view demonstrates the different document types you can choose from.
     * Choosing a document type will impact the ViewFinder size in the SDK, as well as the information displayed during the id verification process.
     * Starting the SDK without choosing a document type will set document type to its default value [IdVerification.DocumentType.DOCUMENT].
     */
    @Composable
    fun DocumentTypeView() {
        Box(modifier = Modifier
            .background(color = Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            )
            {
                Text(
                    style = MaterialTheme.typography.h5,
                    text = "Choose which type of document you want to scan.",
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                for(t in IdVerification.DocumentType.entries) {
                    Button(onClick = {
                        startTransaction(t)
                    }) {
                        Text(text = "$t")
                    }
                }
            }
        }

    }

    override fun onClosed() {
        Log.d("MainActivity", "SDK closed")
        CoroutineScope(Dispatchers.Main).launch {
            navController.navigate("Home")
        }
    }

    override fun onCompleted(result: IdVerificationResult) {
        //val r = result.asJson()
        if (result.error != null) {
            Log.e("MainActivity", "Sdk reported error: ${result.error}")
        }
        Log.d("MainActivity", "SDK completed: ${result.transactionId}")
        this@MainActivity.result.value = "Transaction complete: ${result.transactionId}"
        IdVerification.stop()
    }

    override fun onException(exception: IdVerificationException) {
        Log.e("MainActivity", "Exception ")
        result.value = "Failed to complete transaction: $exception"
        IdVerification.stop()
    }

    override fun onStarted() {
        Log.d("MainActivity", "SDK started")
        CoroutineScope(Dispatchers.Main).launch {
            navController.navigate("Sdk")
        }
    }

    override fun onUserDismissed() {
        Log.d("MainActivity", "SDK was dismissed by user")
        result.value = "SDK was closed by user"
        IdVerification.stop()
    }

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