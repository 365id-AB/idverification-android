package com.id365.exampleapp

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.id365.exampleapp.ui.theme.animations.ExampleLoadingSpinner
import com.id365.exampleapp.viewmodel.ViewModel
import com.id365.idverification.*
import com.id365.idverification.errors.IdVerificationException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity(), IdVerificationEventHandler {

    private lateinit var navController: NavHostController
    private val viewModel: ViewModel by viewModels()

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
                                    .background(color = MaterialTheme.colorScheme.background),
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

    override fun onPostCreate(savedInstanceState: Bundle?) {
        viewModel.load(this)
        super.onPostCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "Received intent: $intent")
        IdVerification.sendIntent(intent)
    }

    private fun setViewContent() {
        setContent {
            CustomSdkTheme {
                // A surface container using the 'surface' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
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
                IdVerification.IdVerificationView()
            }
        }
    }


    @Composable
    fun TokenStatusMessage() {
        val message = if (!viewModel.tokenIsValid.value)
            "Token is not ready"
        else if (viewModel.tokenLoading.value)
            "Token is loading..."
        else
            "Token is ready, press 'Start Transaction' to start a scan"

        val colors = if (!viewModel.tokenIsValid.value) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            CardDefaults.outlinedCardColors()
        }

        Card(
            colors = colors
        ) {
            Text(text = message, textAlign = TextAlign.Center)
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
                style = MaterialTheme.typography.titleLarge,
                text = "365id Scanner SDK Example App",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            TokenStatusMessage()
            Spacer(modifier = Modifier.height(40.dp))
            if (viewModel.result.value.isNotEmpty()) {
                Text(text = "Result: ${viewModel.result.value}", textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (viewModel.tokenLoading.value){
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                enabled = !viewModel.tokenIsValid.value && !viewModel.tokenLoading.value,
                onClick = { viewModel.requestToken(this@MainActivity) }
            ) {
                Text(text = "Request Token")
            }
            Button(
                onClick = { startTransaction(viewModel.documentSizeType.value) },
                enabled = viewModel.tokenIsValid.value && !viewModel.tokenLoading.value
            ) {
                Text(text = "Start Transaction")
            }
            Button(
                enabled = viewModel.tokenIsValid.value && !viewModel.tokenLoading.value,
                onClick = { documentTypeDialogVisible.value = true}
            ) {
                Text(text = "Choose Document Type")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        if (documentTypeDialogVisible.value){
            Dialog(onDismissRequest = {documentTypeDialogVisible.value = false}) {
                Card(
                    shape = MaterialTheme.shapes.small
                ) {
                    DocumentTypeView()
                }
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
    private fun startTransaction(documentSizeType: IdVerification.DocumentSizeType) {

        /**
         * An example of how you can pick and choose what animations you want to use in the SDK.
         * This is done by overriding the default animations set in the SDK.
         */
        val theme = IdVerification.IdVerificationTheme(
            animations = IdVerification.Animations(
                prepareId3 = { ExampleLoadingSpinner() },
                loadingGeneric = { ExampleLoadingSpinner() },
                loadingImageCapture = { ExampleLoadingSpinner() }
            )
        )
        IdVerification.setCustomTheme(theme)

        val request = IdVerificationRequest(viewModel.token.value.accessToken)

        IdVerification.start(this.applicationContext, request, eventHandler = this, documentSizeType = documentSizeType)
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
                    style = MaterialTheme.typography.labelLarge,
                    text = "Choose which type of document you want to scan.",
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                for(t in IdVerification.DocumentSizeType.entries) {
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
        this@MainActivity.viewModel.result.value = "Transaction complete: ${result.transactionId}"
        IdVerification.stop()
    }

    override fun onException(exception: IdVerificationException) {
        Log.e("MainActivity", "Exception ")
        viewModel.result.value = "Failed to complete transaction: $exception"
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
        viewModel.result.value = "SDK was closed by user"
        IdVerification.stop()
    }

    override fun onDocumentFeedback(documentType: DocumentType, countryCode: String) {
        Log.d("MainActivity", "onDocumentFeedback")
    }

    override fun onNfcFeedback(nfcFeedback: NfcFeedback, expiryDate: String) {
        Log.d("MainActivity", "onNfcFeedback")
    }

    override fun onFaceMatchFeedback(faceMatchFeedback: FacematchFeedback) {
        Log.d("MainActivity", "onFaceMatchFeedback")
    }

    override fun onTransactionCreated(transactionId: String) {
        Log.d("MainActivity", "onTransactionCreated")
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        ExampleAppTheme(darkTheme = false) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
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