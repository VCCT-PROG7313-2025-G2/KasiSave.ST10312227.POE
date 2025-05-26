package com.example.kasisave

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.databinding.ItemCategoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CategoriesAdapter(
    private var items: MutableList<String>,
    var startDate: Long?,
    var endDate: Long?,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.VH>() {

    // Store totals per category, made public for access
    val categoryTotals = mutableMapOf<String, Double>()

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val category = items[position]
        holder.binding.tvCategoryName.text = category

        // Load total amount for this category from Firestore
        loadCategoryTotal(category, holder)

        holder.binding.root.setOnClickListener {
            onCategoryClick(category)
        }
    }

    fun update(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        categoryTotals.clear()
        notifyDataSetChanged()
    }

    fun clearTotals() {
        categoryTotals.clear()
    }

    fun getTotalForCategory(category: String): Double {
        return categoryTotals[category] ?: 0.0
    }

    private fun loadCategoryTotal(category: String, holder: VH) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("expenses")
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { snapshot ->
                var total = 0.0

                for (doc in snapshot) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val timestamp = doc.getTimestamp("date")
                    val dateMillis = timestamp?.toDate()?.time

                    Log.d("CategoriesAdapter", "Expense doc ${doc.id}: amount=$amount dateMillis=$dateMillis")



                    if (dateMillis != null) {
                        if (startDate != null && endDate != null) {
                            if (dateMillis in startDate!!..endDate!!) {
                                total += amount
                            }
                        } else {
                            total += amount
                        }
                    }

                    total += amount
                }

                Log.d("CategoriesAdapter", "Total for category '$category' is $total")
                categoryTotals[category] = total
                notifyItemChanged(items.indexOf(category))
            }
            .addOnFailureListener {
                Toast.makeText(
                    holder.binding.root.context,
                    "Failed to load total for $category",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("CategoriesAdapter", "Error loading totals for $category", it)
            }
    }
}
