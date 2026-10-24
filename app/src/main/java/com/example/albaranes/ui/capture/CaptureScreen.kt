package com.example.albaranes.ui.capture

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.albaranes.R
import com.example.albaranes.capture.DocumentScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(viewModel: CaptureViewModel = viewModel()) {
    val activity = LocalContext.current.findActivity()
    val scanner = remember { DocumentScanner() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // A cancelled scan leaves the current state untouched.
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onScanResult(DocumentScanner.pageUri(result.data))
        }
    }
    val startScan = {
        scanner.start(
            activity,
            onReady = { launcher.launch(IntentSenderRequest.Builder(it).build()) },
            onError = viewModel::onScannerError,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.capture_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CaptureContent(viewModel.state)
            }
            Button(onClick = startScan, modifier = Modifier.fillMaxWidth()) {
                val label = if (viewModel.state is CaptureState.Scanned) {
                    R.string.capture_button_rescan
                } else {
                    R.string.capture_button_scan
                }
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun CaptureContent(state: CaptureState) {
    when (state) {
        CaptureState.Empty -> Text(
            text = stringResource(R.string.capture_empty_hint),
            textAlign = TextAlign.Center,
        )

        CaptureState.Loading -> CircularProgressIndicator()

        is CaptureState.Scanned -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val imageBitmap = remember(state.bitmap) { state.bitmap.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = stringResource(R.string.capture_image_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.capture_image_size, state.bitmap.width, state.bitmap.height),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        is CaptureState.Error -> Text(
            text = when (state.kind) {
                ErrorKind.SCANNER -> stringResource(R.string.capture_error_scanner, state.detail.orEmpty())
                ErrorKind.NO_IMAGE -> stringResource(R.string.capture_error_no_image)
                ErrorKind.LOAD -> stringResource(R.string.capture_error_load, state.detail.orEmpty())
            },
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No Activity in context")
}
