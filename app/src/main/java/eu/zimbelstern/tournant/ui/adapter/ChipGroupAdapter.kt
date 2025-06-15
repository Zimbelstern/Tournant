package eu.zimbelstern.tournant.ui.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.ShapeAppearanceModel
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.ChipData
import eu.zimbelstern.tournant.databinding.RecyclerItemChipBinding
import eu.zimbelstern.tournant.ui.MainActivity
import kotlin.math.roundToInt

class ChipGroupAdapter(private val mainActivity: MainActivity, private val keywords: Boolean = false) : RecyclerView.Adapter<ChipGroupAdapter.ChipGroupViewHolder>() {

	private var chips = listOf<ChipData>()

	class ChipDiffCallback(
		private val oldChips: List<ChipData>,
		private val newChips: List<ChipData>
	) : DiffUtil.Callback() {

		override fun getOldListSize() = oldChips.size
		override fun getNewListSize() = newChips.size

		override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			return oldChips[oldItemPosition].string == newChips[newItemPosition].string
					&& oldChips[oldItemPosition].count == newChips[newItemPosition].count
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

	@SuppressLint("SetTextI18n")
	override fun onBindViewHolder(holder: ChipGroupViewHolder, position: Int) {
		val chip = chips[position]
		holder.binding.root.apply {
			if (keywords) {
				val dp = resources.displayMetrics.density
				chipMinHeight =  24 * dp
				setPadding(0, 0, 0, 0)
				setEnsureMinTouchTargetSize(false)
				layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
					(6 * dp).roundToInt().let { margin ->
						setMargins(margin, margin, margin, margin)
					}
				}
				chipStrokeColor = ColorStateList.valueOf(chip.rippleColor.toArgb())
				chipStrokeWidth = 2 * dp
				shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(4 * dp)
				setTextColor(ContextCompat.getColor(context, R.color.black))
			}
			text = "${chip.string} | ${chip.count}"
			chipBackgroundColor = ColorStateList.valueOf(chip.color.toArgb())
			rippleColor = ColorStateList.valueOf(chip.rippleColor.toArgb())
			setOnClickListener {
				mainActivity.searchForSomething(chip.string)
			}
		}
	}

	override fun getItemCount() = chips.size

	fun updateChips(newChips: List<ChipData>) {
		val diff = DiffUtil.calculateDiff(ChipDiffCallback(chips, newChips))
		chips = newChips
		diff.dispatchUpdatesTo(this)
	}

}