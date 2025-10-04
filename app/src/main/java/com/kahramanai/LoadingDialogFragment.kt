package com.kahramanai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.kahramanai.R // Make sure to import your project's R file


class LoadingDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This makes the dialog's own background transparent and removes the title,
        // allowing it to fill the screen.
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar)
    }

    override fun onStart() {
        super.onStart()
        // This is the crucial part for controlling the background dim
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent) // Make dialog background transparent

            // Set the dim amount here
            val params = attributes
            params.dimAmount = 0.7f // 70% dim. A value of 1.0f is fully black, 0.0f is no dim.
            attributes = params

            // Ensure the dim effect is enabled
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_loading_dialog, container, false)

        val cancelButton: Button = view.findViewById(R.id.cancelLoadingButton)
        cancelButton.setOnClickListener {
            dismiss()
        }

        return view
    }
}