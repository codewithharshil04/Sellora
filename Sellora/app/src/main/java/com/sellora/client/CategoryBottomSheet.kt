package com.sellora.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sellora.client.databinding.FragmentCategoryBottomSheetBinding

class CategoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentCategoryBottomSheetBinding? = null
    private val binding get() = _binding!!

    var onCategorySelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Category options
        binding.optAll.setOnClickListener { select("All") }
        binding.optDevelopment.setOnClickListener { select("Development") }
        binding.optDesign.setOnClickListener { select("Design") }
        binding.optVideo.setOnClickListener { select("Video") }
        binding.optWriting.setOnClickListener { select("Writing") }
        binding.optArt.setOnClickListener { select("Art") }
        binding.optOther.setOnClickListener { select("Other") }
    }

    private fun select(category: String) {
        onCategorySelected?.invoke(category)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnDismissListener(action: () -> Unit) {
        dialog?.setOnDismissListener { action() }
    }
}