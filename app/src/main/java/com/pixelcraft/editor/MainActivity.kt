package com.pixelcraft.editor

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.pixelcraft.editor.databinding.ActivityMainBinding
import com.pixelcraft.editor.databinding.DialogExportBinding
import com.pixelcraft.editor.io.ImageExporter
import com.pixelcraft.editor.ui.EditorPagerAdapter
import kotlin.math.roundToInt

/**
 * The single screen of PixelCraft.
 *
 * The Activity owns no editing state at all — everything lives in
 * [EditorViewModel], which survives rotation. This file only:
 * 1. hosts the toolbar, empty state, preview card and tool tabs,
 * 2. forwards user actions to the ViewModel,
 * 3. renders ViewModel state changes.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EditorViewModel by viewModels()

    /** True while the ORIGINAL photo is shown instead of the edited one. */
    private var comparing = false

    // System Photo Picker: no runtime permission needed on any API level.
    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.importPhoto(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.emptyImportButton.setOnClickListener { launchPhotoPicker() }
        binding.cropApplyButton.setOnClickListener { onCropApplyClicked() }
        binding.cropCancelButton.setOnClickListener { viewModel.cancelCropMode() }

        setupToolTabs()
        observeViewModel()
        setupBackNavigation()
    }

    // ---------- Toolbar menu ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_import -> {
            launchPhotoPicker()
            true
        }
        R.id.action_compare -> {
            onCompareClicked()
            true
        }
        R.id.action_reset -> {
            onResetClicked()
            true
        }
        R.id.action_save -> {
            onSaveClicked()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---------- Setup ----------

    private fun launchPhotoPicker() {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun setupToolTabs() {
        val titles = resources.getStringArray(R.array.editor_tabs)
        binding.viewPager.adapter = EditorPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            when {
                viewModel.cropMode.value == true -> viewModel.cancelCropMode()
                comparing -> stopCompare()
                viewModel.hasPhoto && viewModel.hasEdits -> confirmDiscard()
                else -> finish()
            }
        }
    }

    private fun confirmDiscard() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discard_dialog_title)
            .setMessage(R.string.discard_dialog_message)
            .setPositiveButton(R.string.discard_dialog_positive) { _, _ -> finish() }
            .setNegativeButton(R.string.discard_dialog_negative, null)
            .show()
    }

    // ---------- ViewModel rendering ----------

    private fun observeViewModel() {
        viewModel.status.observe(this) { status ->
            when (status) {
                EditorStatus.EMPTY -> {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.editorArea.visibility = View.GONE
                    binding.busyOverlay.visibility = View.GONE
                    stopCompare()
                }
                EditorStatus.BUSY -> binding.busyOverlay.visibility = View.VISIBLE
                EditorStatus.READY -> {
                    binding.emptyState.visibility = View.GONE
                    binding.editorArea.visibility = View.VISIBLE
                    binding.busyOverlay.visibility = View.GONE
                }
            }
        }

        viewModel.busyMessage.observe(this) { messageRes ->
            binding.busyText.setText(messageRes)
        }

        viewModel.preview.observe(this) { bitmap ->
            if (bitmap != null && !comparing) {
                binding.imagePreview.setImageBitmap(bitmap)
            }
        }

        viewModel.error.observe(this) { message ->
            message?.let {
                showSnackbar(it)
                viewModel.consumeError()
            }
        }

        viewModel.exportResult.observe(this) { result ->
            result?.let {
                showSnackbar(getString(R.string.export_saved_to, it.displayName))
                viewModel.consumeExportResult()
            }
        }

        viewModel.cropMode.observe(this) { active ->
            binding.cropModeBar.visibility = if (active) View.VISIBLE else View.GONE
            binding.cropOverlay.visibility = if (active) View.VISIBLE else View.GONE
            if (active) {
                stopCompare()
                viewModel.currentImage?.let { bitmap ->
                    binding.cropOverlay.setBitmapSize(bitmap.width, bitmap.height)
                }
            }
        }
    }

    // ---------- Toolbar actions ----------

    private fun onCompareClicked() {
        when {
            !viewModel.hasPhoto -> showSnackbar(getString(R.string.snackbar_import_first))
            viewModel.cropMode.value == true ->
                showSnackbar(getString(R.string.snackbar_finish_crop))
            comparing -> stopCompare()
            else -> startCompare()
        }
    }

    private fun startCompare() {
        val original = viewModel.originalForCompare() ?: return
        comparing = true
        binding.compareBadge.visibility = View.VISIBLE
        binding.imagePreview.setImageBitmap(original)
    }

    private fun stopCompare() {
        if (!comparing) return
        comparing = false
        binding.compareBadge.visibility = View.GONE
        viewModel.preview.value?.let { binding.imagePreview.setImageBitmap(it) }
    }

    private fun onResetClicked() {
        if (!viewModel.hasPhoto) {
            showSnackbar(getString(R.string.snackbar_import_first))
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_dialog_title)
            .setMessage(R.string.reset_dialog_message)
            .setPositiveButton(R.string.reset_dialog_positive) { _, _ ->
                stopCompare()
                viewModel.resetAll()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onSaveClicked() {
        when {
            !viewModel.hasPhoto -> showSnackbar(getString(R.string.snackbar_import_first))
            viewModel.cropMode.value == true ->
                showSnackbar(getString(R.string.snackbar_finish_crop))
            else -> showExportDialog()
        }
    }

    // ---------- Export dialog ----------

    private fun showExportDialog() {
        val dialogBinding = DialogExportBinding.inflate(layoutInflater)

        dialogBinding.exportQualitySlider.value = 92f
        dialogBinding.exportQualityValue.text =
            getString(R.string.export_quality_percent, 92)
        dialogBinding.exportQualitySlider.addOnChangeListener { _, value, _ ->
            dialogBinding.exportQualityValue.text =
                getString(R.string.export_quality_percent, value.roundToInt())
        }

        // Quality applies to JPEG only; visually disable it when PNG is picked.
        dialogBinding.exportFormatGroup.setOnCheckedChangeListener { _, checkedId ->
            val jpegSelected = checkedId == R.id.jpegRadio
            dialogBinding.exportQualitySlider.isEnabled = jpegSelected
            dialogBinding.exportQualitySlider.alpha = if (jpegSelected) 1f else 0.4f
            dialogBinding.exportQualityLabel.alpha = if (jpegSelected) 1f else 0.4f
            dialogBinding.exportQualityValue.alpha = if (jpegSelected) 1f else 0.4f
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.export_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.export_save) { _, _ ->
                val format =
                    if (dialogBinding.pngRadio.isChecked) ImageExporter.Format.PNG
                    else ImageExporter.Format.JPEG
                viewModel.exportImage(format, dialogBinding.exportQualitySlider.value.roundToInt())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---------- Crop ----------

    private fun onCropApplyClicked() {
        val rect = binding.cropOverlay.getCropRectBitmapCoords()
        if (rect == null) {
            showSnackbar(getString(R.string.crop_empty_selection))
        } else {
            viewModel.applyCrop(rect)
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
