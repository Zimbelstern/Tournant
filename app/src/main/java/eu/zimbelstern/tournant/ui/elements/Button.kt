package eu.zimbelstern.tournant.ui.elements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


@Composable
fun TournantRoundIconButton(
	modifier: Modifier = Modifier,
	isDark: Boolean = false,
	onClick: () -> Unit,
	icon: ImageVector,
	contentDescription: String?
) {
	Surface(
		modifier = modifier.size(36.dp),
		shape = CircleShape,
		color = if (isDark) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
		contentColor = if (isDark) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSecondary
	) {
		IconButton(
			modifier = Modifier.fillMaxSize(),
			onClick = onClick
		) {
			Icon(
				modifier = Modifier.size(24.dp),
				imageVector = icon,
				contentDescription = contentDescription
			)
		}
	}
}

@Composable
fun TournantRoundedIconButton(
	modifier: Modifier = Modifier,
	isDark: Boolean = false,
	onClick: () -> Unit,
	icon: ImageVector,
	contentDescription: String?
) {
	Surface(
		modifier = modifier.size(36.dp),
		shape = RoundedCornerShape(8.dp),
		color = if (isDark) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
		contentColor = if (isDark) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSecondary
	) {
		IconButton(
			modifier = Modifier.fillMaxSize(),
			onClick = onClick
		) {
			Icon(
				modifier = Modifier.size(24.dp),
				imageVector = icon,
				contentDescription = contentDescription
			)
		}
	}
}