package com.pixelcraft.editor.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import com.pixelcraft.editor.databinding.FragmentFiltersBinding
import com.pixelcraft.editor.image.FilterType

/**
 * Filters tab: the four one-tap presets.
 *
 * The buttons are checkable; their checked state is driven purely by the
 * ViewModel, so the highlighted button always reflects the active filter
 * even after a Reset on another tab.
 */
class FiltersTabFragment : BaseTabFragment() {

    private var _binding: FragmentFiltersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filterButtons = mapOf(
            binding.filterOriginal to FilterType.ORIGINAL,
            binding.filterGrayscale to FilterType.GRAYSCALE,
            binding.filterWarm to FilterType.WARM,
            binding.filterCool to FilterType.COOL
        )

        filterButtons.forEach { (button, filter) ->
            button.setOnClickListener { viewModel.setFilter(filter) }
        }

        viewModel.adjustments.observe(viewLifecycleOwner) { adjustments ->
            filterButtons.forEach { (button, filter) ->
                setCheckedSilently(button, adjustments.filter == filter)
            }
        }
    }

    // Clicking already toggles the button; we only need to correct the
    // state when the change came from the model (preset → ORIGINAL, reset).
    private fun setCheckedSilently(button: MaterialButton, checked: Boolean) {
        if (button.isChecked != checked) {
            button.isChecked = checked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
