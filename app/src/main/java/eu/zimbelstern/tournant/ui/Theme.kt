package eu.zimbelstern.tournant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eu.zimbelstern.tournant.R

private val lightColorScheme = lightColors(
	primary = Color(0xff003c8f),
	onPrimary = Color.White,
	secondary = Color(0xff1565c0),
	onSecondary = Color.White,
	surface = Color(0xfffff5dc),
	onSurface = Color(0x8a000000)
)

private val darkColorScheme = darkColors(
	primary = Color(0xffff9d0a),
	onPrimary = Color.Black,
	secondary = Color(0xffffb300),
	onSecondary = Color.Black,
	surface = Color(0xff25231f),
	onSurface = Color(0xb3ffffff),
)

private fun buildTypography(
	colorScheme: Colors,
	headingColor: Color
) = Typography(
	body1 = TextStyle(
		fontSize = 17.sp,
		color = colorScheme.onSurface
	),
	caption = TextStyle(
		fontSize = 17.sp,
		color = colorScheme.onSurface,
		fontWeight = FontWeight.Bold
	),
	h2 = TextStyle(
		fontSize = 21.sp,
		color = headingColor,
		fontFamily = FontFamily(Font(R.font.quicksand_bold))
	)
)

private val lightTypography = buildTypography(lightColorScheme, Color(0xff2C5BA2))
private val darkTypography = buildTypography(darkColorScheme, Color(0xff5E92F3))

@Composable
fun TournantTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colors = if (isSystemInDarkTheme()) darkColorScheme else lightColorScheme,
		content = content,
		typography = if (isSystemInDarkTheme()) darkTypography else lightTypography
	)
}