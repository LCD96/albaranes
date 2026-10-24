package com.example.albaranes.ui.capture

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.albaranes.capture.ScannedImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CaptureState {
    data object Empty : CaptureState
    data object Loading : CaptureState
    data class Scanned(val uri: Uri, val bitmap: Bitmap) : CaptureState
    data class Error(val kind: ErrorKind, val detail: String?) : CaptureState
}

enum class ErrorKind { SCANNER, NO_IMAGE, LOAD }

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    var state: CaptureState by mutableStateOf(CaptureState.Empty)
        private set

    fun onScannerError(e: Exception) {
        Log.e(TAG, "Scanner failed to start", e)
        state = CaptureState.Error(ErrorKind.SCANNER, e.message)
    }

    fun onScanResult(uri: Uri?) {
        if (uri == null) {
            state = CaptureState.Error(ErrorKind.NO_IMAGE, null)
            return
        }
        state = CaptureState.Loading
        viewModelScope.launch {
            state = try {
                val bitmap = withContext(Dispatchers.IO) {
                    ScannedImageLoader.load(getApplication<Application>().contentResolver, uri)
                }
                Log.d(TAG, "Loaded ${bitmap.width}x${bitmap.height} from $uri")
                CaptureState.Scanned(uri, bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot load scanned image", e)
                CaptureState.Error(ErrorKind.LOAD, e.message)
            }
        }
    }

    private companion object {
        const val TAG = "Albaranes"
    }
}
