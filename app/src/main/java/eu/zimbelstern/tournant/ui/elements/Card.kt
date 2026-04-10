package eu.zimbelstern.tournant.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.zimbelstern.tournant.ui.TournantTheme

@Composable
fun TournantCard(
	modifier: Modifier = Modifier,
	marginTop: Dp = 0.dp,
	marginBottom: Dp = 0.dp,
	marginStart: Dp = 0.dp,
	marginEnd: Dp = 0.dp,
	content: @Composable () -> Unit
) =
	Box(
		Modifier.padding(top = marginTop, bottom = marginBottom, start = marginStart, end = marginEnd)
	) {
		Card(
			modifier = modifier,
			elevation = 0.dp,
			shape = RoundedCornerShape(8.dp)
		) {
			Surface(
				Modifier.padding(20.dp)
			) {
				content()
			}
		}
	}

@Preview(showBackground = true)
@Composable
fun TournantCardPreview() {
	TournantTheme {
		TournantCard {
			Text("TournantCard")
		}
	}
}
