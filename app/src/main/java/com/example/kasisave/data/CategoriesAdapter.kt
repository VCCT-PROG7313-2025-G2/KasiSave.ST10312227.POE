package com.example.kasisave

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.databinding.ItemCategoryBinding

class CategoriesAdapter(
    private val items: MutableList<String>
) : RecyclerView.Adapter<CategoriesAdapter.VH>() {

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.tvCategoryName.text = items[position]
    }

    /** Completely replace the current list with [newItems] and refresh. */
    fun update(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /** Convenience: add one item to the end. */
    fun add(name: String) {
        items.add(name)
        notifyItemInserted(items.lastIndex)
    }
}
