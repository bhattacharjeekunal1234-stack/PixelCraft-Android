package com.pixelcraft.editor.ui.tabs

import android.view.MotionEvent
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider
import com.pixelcraft.editor.EditorViewModel
import kotlin.math.roundToInt

/**
 * Shared behavior for the four tool fragments.
 *
 * All tabs talk to one EditorViewModel owned by the Activity, so an edit
 * made on the Light tab is instantly visible on the Color tab and in the
 * preview above.
 */
abstract class BaseTabFragment : Fragment() {

    protected val viewModel: EditorViewModel by activityViewModels()

    /**
     * Wires one slider: keeps its value label formatted and forwards user
     * changes to [apply]. Programmatic updates (e.g. after Reset) pass
     * fromUser = false, so they never loop back into the ViewModel.
     */
    protected fun bindSlider(
        slider: Slider,
        valueLabel: TextView,
        apply: (Int) -> Unit
    ) {
        slider.addOnChangeListener { _, value, fromUser ->
            valueLabel.text = formatSigned(value.roundToInt())
            if (fromUser) apply(value.roundToInt())
        }
        // A horizontal drag on the slider must not page the ViewPager below.
        slider.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    /** Moves a slider and its label to [value] without notifying the model. */
    protected fun syncSlider(slider: Slider, valueLabel: TextView, value: Int) {
        val clamped = value.coerceIn(slider.valueFrom.toInt(), slider.valueTo.toInt())
        slider.value = clamped.toFloat()
        valueLabel.text = formatSigned(clamped)
    }

    /** "-12", "0", "+35" — the signed format photo editors use. */
    protected fun formatSigned(value: Int): String =
        if (value > 0) "+$value" else value.toString()
}
