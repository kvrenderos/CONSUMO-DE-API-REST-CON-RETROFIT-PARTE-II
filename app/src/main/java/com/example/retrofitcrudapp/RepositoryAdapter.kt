package com.example.retrofitcrudapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.retrofitcrudapp.databinding.ItemRepositoryBinding

class RepositoryAdapter : RecyclerView.Adapter<RepositoryAdapter.RepositoryHolder>() {
    private val repositories = mutableListOf<GithubRepository>()

    fun clear() {
        val count = repositories.size
        repositories.clear()
        notifyItemRangeRemoved(0, count)
    }

    fun append(items: List<GithubRepository>) {
        val start = repositories.size
        repositories.addAll(items)
        notifyItemRangeInserted(start, items.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RepositoryHolder(
        ItemRepositoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: RepositoryHolder, position: Int) {
        holder.bind(repositories[position])
    }

    override fun getItemCount() = repositories.size

    class RepositoryHolder(private val binding: ItemRepositoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(repository: GithubRepository) {
            binding.repositoryName.text = repository.name
            binding.repositoryDescription.text = repository.description
                ?: itemView.context.getString(R.string.no_description)
            binding.repositoryDetails.text = itemView.context.getString(
                R.string.repository_details,
                repository.language ?: itemView.context.getString(R.string.no_language),
                repository.stars
            )
        }
    }
}
