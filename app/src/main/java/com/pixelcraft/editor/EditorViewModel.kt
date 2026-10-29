package com.pixelcraft.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pixelcraft.editor.image.Adjustments
import com.pixelcraft.editor.image.FilterType
import com.pixelcraft.editor.image.ImageProcessor
import com.pixelcraft.editor.io.ImageExporter
import com.pixelcraft.editor.io.ImageImporter
import com.pixelcraft.editor.io.ImageImportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Rect
import android.os.Handler
import android.os.Looper

/** Which way a 90° rotation goes. */
enum class RotateDirection { LEFT, RIGHT }

/** High-level app screen state. */
enum class EditorStatus { EMPTY, BUSY, READY }

/**
 * Single source of truth for the whole editor.
 *
 * Bitmap model (three layers):
 * - [sourceBitmap]: the photo exactly as imported (used by Compare).
 * - [baseBitmap]: source after geometric edits (crop / rotation).
 * - edited preview: baseBitmap after color adjustments, recomputed on a
 *   background thread whenever the user moves a slider (debounced).
 *
 * All color math runs on Dispatchers.Default, so the UI never freezes.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    // ---------- Observable state ----------

    private val _status = MutableLiveData(EditorStatus.EMPTY)
    val status: LiveData<EditorStatus> = _status

    private val _busyMessage = MutableLiveData(R.string.busy_importing)
    val busyMessage: LiveData<Int> = _busyMessage

    private val _preview = MutableLiveData<Bitmap?>(null)
    val preview: LiveData<Bitmap?> = _preview

    private val _adjustments = MutableLiveData(Adjustments.DEFAULTS)
    val adjustments: LiveData<Adjustments> = _adjustments

    /** One-shot error messages for snackbars; UI clears it after showing. */
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _cropMode = MutableLiveData(false)
    val cropMode: LiveData<Boolean> = _cropMode

    private val _exportResult = MutableLiveData<ImageExporter.ExportResult?>(null)
    val exportResult: LiveData<ImageExporter.ExportResult?> = _exportResult

    // ---------- Bitmaps ----------

    var sourceBitmap: Bitmap? = null
        private set

    private var baseBitmap: Bitmap? = null

    /** baseBitmap after color adjustments; null until a render happens. */
    private var editedBitmap: Bitmap? = null

    val hasPhoto: Boolean
        get() = sourceBitmap != null

    /** True when there is anything to reset or to warn about on exit. */
    val hasEdits: Boolean
        get() = _adjustments.value != Adjustments.DEFAULTS || baseBitmap !== sourceBitmap

    /**
     * The bitmap to hand to export / crop math. Color adjustments never
     * change pixel dimensions, so crop rectangles taken from the edited
     * preview map 1:1 onto baseBitmap.
     */
    val currentImage: Bitmap?
        get() = editedBitmap ?: baseBitmap

    // ---------- Debounce machinery for slider processing ----------

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingProcess: Runnable? = null

    // ---------- Import ----------

    fun importPhoto(uri: Uri) {
        if (_status.value == EditorStatus.BUSY) return
        _status.value = EditorStatus.BUSY
        _busyMessage.value = R.string.busy_importing

        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    ImageImporter.decodeFromUri(getApplication(), uri)
                }
                sourceBitmap = bitmap
                baseBitmap = bitmap
                editedBitmap = null
                _adjustments.value = Adjustments.DEFAULTS
                _preview.value = bitmap
                _status.value = EditorStatus.READY
            } catch (error: ImageImportException) {
                _status.value = if (hasPhoto) EditorStatus.READY else EditorStatus.EMPTY
                _error.value = error.message
            }
        }
    }

    // ---------- Adjustments ----------

    fun setBrightness(value: Int) = updateAdjustments { it.withBrightness(value) }
    fun setContrast(value: Int) = updateAdjustments { it.withContrast(value) }
    fun setSaturation(value: Int) = updateAdjustments { it.withSaturation(value) }
    fun setExposure(value: Int) = updateAdjustments { it.withExposure(value) }
    fun setWhiteBalance(value: Int) = updateAdjustments { it.withWhiteBalance(value) }

    fun setFilter(filter: FilterType) = updateAdjustments { it.withFilter(filter) }

    /**
     * Applies one change to the adjustment set and schedules a re-render.
     * Slider drags fire dozens of events; the 60 ms debounce collapses them
     * into one background render per gesture pause.
     */
    private fun updateAdjustments(transform: (Adjustments) -> Adjustments) {
        if (!hasPhoto) return
        pendingProcess?.let { mainHandler.removeCallbacks(it) }
        _adjustments.value = transform(_adjustments.value ?: Adjustments.DEFAULTS)

        val run = Runnable { renderAdjustedBitmap() }
        pendingProcess = run
        mainHandler.postDelayed(run, 60)
    }

    private fun renderAdjustedBitmap() {
        val base = baseBitmap ?: return
        val snapshot = _adjustments.value ?: return

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                if (snapshot.isDefault) {
                    base
                } else {
                    ImageProcessor.apply(base, snapshot)
                }
            }
            // Render only if the user has not moved a slider since we started.
            if (_adjustments.value == snapshot) {
                editedBitmap = result
                _preview.value = result
            }
        }
    }

    // ---------- Reset / compare ----------

    fun resetAll() {
        if (!hasPhoto) return
        if (!hasEdits) {
            _error.value = getString(R.string.snackbar_nothing_to_reset)
            return
        }
        pendingProcess?.let { mainHandler.removeCallbacks(it) }
        _adjustments.value = Adjustments.DEFAULTS
        baseBitmap = sourceBitmap
        editedBitmap = sourceBitmap
        _preview.value = sourceBitmap
    }

    fun originalForCompare(): Bitmap? = sourceBitmap

    // ---------- Rotation ----------

    fun rotate(direction: RotateDirection) {
        val base = baseBitmap ?: return
        _status.value = EditorStatus.BUSY
        _busyMessage.value = R.string.busy_rotating

        viewModelScope.launch {
            baseBitmap = withContext(Dispatchers.Default) {
                ImageProcessor.rotate90(base, direction == RotateDirection.RIGHT)
            }
            _status.value = EditorStatus.READY
            _preview.value = baseBitmap
            renderAdjustedBitmap() // re-apply color settings to the new shape
        }
    }

    // ---------- Cropping ----------

    fun startCropMode() {
        if (!hasPhoto || _cropMode.value == true) return
        _cropMode.value = true
    }

    fun cancelCropMode() {
        _cropMode.value = false
    }

    /**
     * Receives the crop rectangle in bitmap coordinates (already converted
     * by CropOverlayView), cuts [baseBitmap] and re-renders.
     */
    fun applyCrop(rect: Rect) {
        val base = baseBitmap ?: return
        val left = rect.left.coerceIn(0, base.width - 1)
        val top = rect.top.coerceIn(0, base.height - 1)
        val width = rect.width().coerceIn(1, base.width - left)
        val height = rect.height().coerceIn(1, base.height - top)

        if (width < 16 || height < 16) {
            _error.value = getString(R.string.crop_too_small)
            return
        }

        _status.value = EditorStatus.BUSY
        _busyMessage.value = R.string.busy_rotating

        viewModelScope.launch {
            baseBitmap = withContext(Dispatchers.Default) {
                Bitmap.createBitmap(base, left, top, width, height)
            }
            _cropMode.value = false
            _status.value = EditorStatus.READY
            _preview.value = baseBitmap
            renderAdjustedBitmap()
        }
    }

    // ---------- Export ----------

    fun exportImage(format: ImageExporter.Format, jpegQuality: Int) {
        val bitmap = currentImage ?: return
        if (_status.value == EditorStatus.BUSY) return

        _status.value = EditorStatus.BUSY
        _busyMessage.value = R.string.busy_saving

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ImageExporter.export(getApplication(), bitmap, format, jpegQuality)
                }
                _status.value = EditorStatus.READY
                _exportResult.value = result
            } catch (error: Exception) {
                _status.value = EditorStatus.READY
                _error.value = error.message ?: getString(R.string.error_generic, "unknown")
            }
        }
    }

    fun consumeError() {
        _error.value = null
    }

    fun consumeExportResult() {
        _exportResult.value = null
    }

    private fun getString(resId: Int, vararg formatArgs: Any?): String =
        getApplication<Application>().getString(resId, *formatArgs)
}
