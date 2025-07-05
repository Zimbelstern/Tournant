package eu.zimbelstern.tournant.utils

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

@Suppress("unused")
object DataBindingAdapters {

	@BindingAdapter("suffixText")
	@JvmStatic
	fun markAsOptional(view: TextInputLayout, optional: Boolean) {
		view.suffixText = if (optional) "*" else ""
	}

}