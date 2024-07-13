package eu.zimbelstern.tournant.utils

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import eu.zimbelstern.tournant.R

class CheckableTextView : androidx.appcompat.widget.AppCompatTextView {

	constructor(context: Context) : super(context)
	constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
	constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

	var isChecked = false
		set(value) {
			setTextColor(
				ContextCompat.getColor(
					context,
					if (value)
						R.color.checked_text_color
					else
						R.color.normal_text_color
				)
			)
			compoundDrawables.filterNotNull().forEach {
				if (value)
					it.setTintList(null)
				else
					it.setTint(Color.TRANSPARENT)
			}
			field = value
		}

	init {
		setTextColor(ContextCompat.getColor(context, R.color.normal_text_color))
		highlightColor = Color.TRANSPARENT
	}

}