package com.example.sampahclassifierapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sampahclassifierapp.data.LearnEntity
import com.example.sampahclassifierapp.databinding.ItemLearnAccordionBinding

class LearnAdapter : RecyclerView.Adapter<LearnAdapter.ViewHolder>() {

    private var items: List<LearnEntity> = emptyList()

    fun submitList(newList: List<LearnEntity>) {
        items = newList
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemLearnAccordionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LearnEntity, onToggle: () -> Unit) {
            binding.tvLearnTitle.text = item.title
            binding.tvLearnDescription.text = item.description
            binding.imgLearnIcon.setImageResource(item.iconResId)

            // Logic Expand/Collapse
            binding.tvLearnDescription.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.imgExpandArrow.rotation = if (item.isExpanded) 180f else 0f

            binding.root.setOnClickListener {
                onToggle()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLearnAccordionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item) {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION && currentPos in items.indices) {
                items[currentPos].isExpanded = !items[currentPos].isExpanded
                notifyItemChanged(currentPos)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
