package com.pixelcraft.editor.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pixelcraft.editor.RotateDirection
import com.pixelcraft.editor.databinding.FragmentTransformBinding

/** Transform tab: 90° rotation buttons and the crop entry point. */
class TransformTabFragment : BaseTabFragment() {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rotateLeftButton.setOnClickListener {
            viewModel.rotate(RotateDirection.LEFT)
        }
        binding.rotateRightButton.setOnClickListener {
            viewModel.rotate(RotateDirection.RIGHT)
        }
        binding.cropButton.setOnClickListener {
            viewModel.startCropMode()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
