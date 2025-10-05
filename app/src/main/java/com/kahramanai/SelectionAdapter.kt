package com.kahramanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kahramanai.data.SelectableItem

class SelectionAdapter(
    private val items: List<SelectableItem>,
    private val onItemClick: (SelectableItem) -> Unit
) : RecyclerView.Adapter<SelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemButton: MaterialButton = view.findViewById(R.id.itemButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_selectable, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        // Set the text of the button
        holder.itemButton.text = item.name
        // Set the click listener on the button itself
        holder.itemButton.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}