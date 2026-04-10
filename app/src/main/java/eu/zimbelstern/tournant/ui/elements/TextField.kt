package eu.zimbelstern.tournant.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max


@Composable
fun TournantUnderlinedTextField(
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	placeholder: String,
	textMeasurer: TextMeasurer,
	density: Density = LocalDensity.current,
	focusManager: FocusManager = LocalFocusManager.current,
) {
	val textStyle = MaterialTheme.typography.body1
	val width = remember(value) {
		mutableStateOf(
			with (density) {
				max(
					textMeasurer.measure(value, textStyle).size.width,
					textMeasurer.measure(placeholder, textStyle).size.width
				).toDp()
			}
		)
	}
	var isFocused by remember { mutableStateOf(false) }
	val focusedColor = MaterialTheme.colors.primary
	val unfocusedColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
	BasicTextField(
		value = value,
		onValueChange = onValueChange,
		modifier = modifier
			.padding(start = 8.dp, end = 4.dp)
			.width(width.value)
			.onFocusChanged { isFocused = it.isFocused }
			.drawBehind {
				drawLine(
					color = if (isFocused) focusedColor else unfocusedColor,
					start = Offset(0f, size.height + 2.sp.toPx()),
					end = Offset(size.width, size.height + 2.sp.toPx()),
					strokeWidth = if (isFocused) 2.sp.toPx() else 1.5.sp.toPx()
				)
			},
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
		keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
		textStyle = textStyle,
		cursorBrush = SolidColor(focusedColor),
		decorationBox = { textField ->
			Box {
				if (value.isEmpty()) {
					Text(
						text = placeholder,
						style = textStyle,
						color = unfocusedColor
					)
				}
				textField()
			}
		}
	)
}