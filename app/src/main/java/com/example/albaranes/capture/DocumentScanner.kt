package com.example.albaranes.capture

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Thin wrapper around the ML Kit Document Scanner.
 *
 * The scanner UI runs inside Google Play services: it handles the camera, edge detection and
 * perspective correction, so the app needs no camera permission. We ask for a single page as JPEG.
 */
class DocumentScanner {

    private val client = GmsDocumentScanning.getClient(
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()
    )

    /** Requests the [IntentSender] that launches the scanner UI. */
    fun start(
        activity: Activity,
        onReady: (IntentSender) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        client.getStartScanIntent(activity)
            .addOnSuccessListener(onReady)
            .addOnFailureListener(onError)
    }

    companion object {
        /** Extracts the URI of the first scanned page (already deskewed), or null if there is none. */
        fun pageUri(data: Intent?): Uri? =
            GmsDocumentScanningResult.fromActivityResultIntent(data)
                ?.pages
                ?.firstOrNull()
                ?.imageUri
    }
}
