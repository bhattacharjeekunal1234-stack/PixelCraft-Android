package com.pixelcraft.editor.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pixelcraft.editor.databinding.FragmentColorBinding

/** Color tab: saturation and white balance sliders. */
class ColorTabFragment : BaseTabFragment() {

    private var _binding: FragmentColorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindSlider(binding.saturationSlider, binding.saturationValue) {
            viewModel.setSaturation(it)
        }
        bindSlider(binding.whiteBalanceSlider, binding.whiteBalanceValue) {
            viewModel.setWhiteBalance(it)
        }

        viewModel.adjustments.observe(viewLifecycleOwner) { adjustments ->
            syncSlider(binding.saturationSlider, binding.saturationValue, adjustments.saturation)
            syncSlider(binding.whiteBalanceSlider, binding.whiteBalanceValue, adjustments.whiteBalance)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
