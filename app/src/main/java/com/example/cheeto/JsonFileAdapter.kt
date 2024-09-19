package com.example.cheeto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cheeto.databinding.ItemJsonFileBinding
import java.io.File

class JsonFileAdapter(
    private val files: List<File>,
    private val onFileSelected: (File) -> Unit
) : RecyclerView.Adapter<JsonFileAdapter.JsonFileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JsonFileViewHolder {
        val binding = ItemJsonFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JsonFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JsonFileViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file, onFileSelected)
    }

    override fun getItemCount() = files.size

    class JsonFileViewHolder(private val binding: ItemJsonFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File, onFileSelected: (File) -> Unit) {
            binding.fileName.text = file.name
            binding.root.setOnClickListener { onFileSelected(file) }
        }
    }
}