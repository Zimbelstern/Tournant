package eu.zimbelstern.tournant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.data.ColorfulString
import eu.zimbelstern.tournant.databinding.RecyclerItemChipBinding
import eu.zimbelstern.tournant.ui.MainActivity

class ChipGroupAdapter(private val mainActivity: MainActivity) : RecyclerView.Adapter<ChipGroupAdapter.ChipGroupViewHolder>() {

	private var chips = listOf<ColorfulString>()

	class ChipDiffCallback(
		private val oldChips: List<ColorfulString>,
		private val newChips: List<ColorfulString>
	) : DiffUtil.Callback() {

		override fun getOldListSize() = oldChips.size
		override fun getNewListSize() = newChips.size

		override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			return oldChips[oldItemPosition].string == newChips[newItemPosition].string
		}

		override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			return oldChips[oldItemPosition] == newChips[newItemPosition]
		}

	}

	class ChipGroupViewHolder(val binding: RecyclerItemChipBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipGroupViewHolder {
		return ChipGroupViewHolder(
			RecyclerItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
	}

	override fun onBindViewHolder(holder: ChipGroupViewHolder, position: Int) {
		val chip = chips[position]
		holder.binding.root.apply {
			text = chip.string
			chipBackgroundColor = chip.color
			rippleColor = chip.rippleColor
			setOnClickListener {
				mainActivity.searchForSomething(chip.string)
			}
		}
	}

	override fun getItemCount() = chips.size

	fun updateChips(newChips: List<ColorfulString>) {
		val diff = DiffUtil.calculateDiff(ChipDiffCallback(chips, newChips))
		chips = newChips
		diff.dispatchUpdatesTo(this)
	}

}