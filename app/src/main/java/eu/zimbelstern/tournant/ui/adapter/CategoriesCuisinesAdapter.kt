package eu.zimbelstern.tournant.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.ChipData
import eu.zimbelstern.tournant.databinding.CategoriesAndCuisinesBinding
import eu.zimbelstern.tournant.ui.MainActivity

class CategoriesCuisinesAdapter(mainActivity: MainActivity)
	: RecyclerView.Adapter<CategoriesCuisinesAdapter.CategoriesCuisinesViewHolder>() {

	private var chips = listOf(
		listOf<ChipData>(),
		listOf()
	)

	private var adapters = listOf(
		ChipGroupAdapter(mainActivity),
		ChipGroupAdapter(mainActivity)
	)

	class CategoriesCuisinesViewHolder(val binding: CategoriesAndCuisinesBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesCuisinesViewHolder {
		return CategoriesCuisinesViewHolder(
			CategoriesAndCuisinesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
	}

	override fun onBindViewHolder(holder: CategoriesCuisinesViewHolder, position: Int) {
		val isCategory = position == 0 && chips[0].isNotEmpty()

		holder.binding.ccTitle.apply {
			text = context.getString(if (isCategory) R.string.category else R.string.cuisine)
		}

		holder.binding.ccRecycler.apply {
			adapter = adapters[if (isCategory) 0 else 1]
			layoutManager = FlexboxLayoutManager(context)
		}
	}

	override fun getItemCount() = chips.filter { it.isNotEmpty() }.size

	@SuppressLint("NotifyDataSetChanged")
	fun updateChipAdapters(newChips: List<List<ChipData>>) {
		val diff = DiffUtil.calculateDiff(ChipListListDiffCallback(chips, newChips))
		chips = newChips
		adapters.forEachIndexed { i, adapter ->
			adapter.updateChips(chips[i])
		}
		diff.dispatchUpdatesTo(this)
	}

	class ChipListListDiffCallback(
		private val oldLists: List<List<ChipData>>,
		private val newLists: List<List<ChipData>>
	) : DiffUtil.Callback() {

		override fun getOldListSize() =
			oldLists.filter { it.isNotEmpty() }.size

		override fun getNewListSize() =
			newLists.filter { it.isNotEmpty() }.size

		override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
			(oldItemPosition == 0 && oldLists[0].isNotEmpty()) ==
				(newItemPosition == 0 && newLists[0].isNotEmpty())


		override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
			oldLists[if (oldItemPosition == 0 && oldLists[0].isNotEmpty()) 0 else 1].isEmpty() ==
				newLists[if (newItemPosition == 0 && newLists[0].isNotEmpty()) 0 else 1].isEmpty()

	}

}