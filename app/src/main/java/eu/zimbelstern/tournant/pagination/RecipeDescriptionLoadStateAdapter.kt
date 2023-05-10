package eu.zimbelstern.tournant.pagination

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerLoadingBinding

class RecipeDescriptionLoadStateAdapter : LoadStateAdapter<RecipeDescriptionLoadStateAdapter.LoadStateViewHolder>() {

	inner class LoadStateViewHolder(val binding: RecyclerLoadingBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
		return LoadStateViewHolder(
			RecyclerLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
	}

	override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
		holder.binding.loading.isVisible = loadState is LoadState.Loading
	}

}