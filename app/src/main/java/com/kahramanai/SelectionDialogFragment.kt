package com.kahramanai

// file: SelectionDialogFragment.kt
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kahramanai.data.SelectableItem

class SelectionDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_selection_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.selectionRecyclerView)
        val itemsJson = arguments?.getString(ARG_ITEMS_JSON) ?: "[]"

        // Parse the JSON string back into a list of objects
        val listType = object : TypeToken<List<SelectableItem>>() {}.type
        val items: List<SelectableItem> = Gson().fromJson(itemsJson, listType)

        // Setup the adapter
        val adapter = SelectionAdapter(items) { selectedItem ->

            Log.d("SelectionDialog", "Item clicked! ID: ${selectedItem.id}, Name: ${selectedItem.name}")

            // Use the Fragment Result API to send the selected item back
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to selectedItem.id))
            dismiss() // Close the dialog
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        // Get the dialog's window
        dialog?.window?.apply {
            // Set the width to match the parent and the height to wrap the content
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        const val TAG = "SelectionDialog"
        const val REQUEST_KEY = "selection_request"
        const val RESULT_KEY = "selected_item_id"
        private const val ARG_ITEMS_JSON = "items_json"

        fun newInstance(items: List<SelectableItem>): SelectionDialogFragment {
            val args = Bundle()
            // Convert the list to a JSON string to pass it safely to the dialog
            val itemsJson = Gson().toJson(items)
            args.putString(ARG_ITEMS_JSON, itemsJson)
            val fragment = SelectionDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}