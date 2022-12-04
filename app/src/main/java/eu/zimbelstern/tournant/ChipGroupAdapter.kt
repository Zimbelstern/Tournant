package eu.zimbelstern.tournant

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemChipBinding
import kotlin.random.Random

class ChipGroupAdapter(private val mainActivity: MainActivity, private val allChips: List<String>) : RecyclerView.Adapter<ChipGroupAdapter.ChipGroupViewHolder>() {

	private var filteredChips = allChips

	private val colors = mainActivity.resources.obtainTypedArray(R.array.material_colors_700)
	private val colorsRipple = mainActivity.resources.obtainTypedArray(R.array.material_colors_900)
	private val ccPseudoRandomInt = allChips.associateWith {
		Random(it.hashCode()).nextInt(mainActivity.resources.getStringArray(R.array.material_colors_700).size)
	}
	private val ccColors = ccPseudoRandomInt.mapValues { colors.getColorStateList(it.value) }
	private val ccRippleColors = ccPseudoRandomInt.mapValues { colorsRipple.getColorStateList(it.value) }

	class ChipGroupViewHolder(val binding: RecyclerItemChipBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipGroupViewHolder {
		val binding = RecyclerItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return ChipGroupViewHolder(binding)
	}

	override fun onBindViewHolder(holder: ChipGroupViewHolder, position: Int) {
		holder.binding.root.apply {
			text = filteredChips[position]
			chipBackgroundColor = ccColors[text]
			rippleColor = ccRippleColors[text]
			setOnClickListener {
				mainActivity.searchForSomething(text)
			}
		}
	}

	override fun getItemCount(): Int {
		return filteredChips.size
	}

	@SuppressLint("NotifyDataSetChanged")
	fun filterChips(query: CharSequence?) {
		filteredChips = if (query != null) allChips.filter {
			it.contains(query, true)
		} else allChips
		notifyDataSetChanged()
	}

}