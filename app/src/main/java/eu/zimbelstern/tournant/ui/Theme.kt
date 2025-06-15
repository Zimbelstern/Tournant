package eu.zimbelstern.tournant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val lightColorScheme = lightColors(
	primary = Color(0xff003c8f),
	onPrimary = Color.White,
	secondary = Color(0xff1565c0),
	onSecondary = Color.White,
	surface = Color(0xfffff5dc),
	onSurface = Color(0x8a000000)
)

val darkColorScheme = darkColors(
	primary = Color(0xffff9d0a),
	onPrimary = Color.Black,
	secondary = Color(0xffffb300),
	onSecondary = Color.White,
	surface = Color(0xff25231f),
	onSurface = Color(0xb3ffffff),
)

@Composable
fun TournantTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colors = if (isSystemInDarkTheme()) darkColorScheme else lightColorScheme,
		content = content
	)
}