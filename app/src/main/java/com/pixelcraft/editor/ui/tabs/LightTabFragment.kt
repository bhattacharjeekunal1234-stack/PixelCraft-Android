package com.pixelcraft.editor.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pixelcraft.editor.databinding.FragmentLightBinding

/** Light tab: brightness, contrast and exposure sliders. */
class LightTabFragment : BaseTabFragment() {

    private var _binding: FragmentLightBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindSlider(binding.brightnessSlider, binding.brightnessValue) {
            viewModel.setBrightness(it)
        }
        bindSlider(binding.contrastSlider, binding.contrastValue) {
            viewModel.setContrast(it)
        }
        bindSlider(binding.exposureSlider, binding.exposureValue) {
            viewModel.setExposure(it)
        }

        // Keep sliders in step when a filter preset or Reset changes values.
        viewModel.adjustments.observe(viewLifecycleOwner) { adjustments ->
            syncSlider(binding.brightnessSlider, binding.brightnessValue, adjustments.brightness)
            syncSlider(binding.contrastSlider, binding.contrastValue, adjustments.contrast)
            syncSlider(binding.exposureSlider, binding.exposureValue, adjustments.exposure)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
