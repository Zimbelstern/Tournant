package eu.zimbelstern.tournant.ui

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

val materialColors100 = listOf(
	Color(0xffffcdd2),
	Color(0xfff8bbd0),
	Color(0xffe1bee7),
	Color(0xffd1c4e9),
	Color(0xffc5cae9),
	Color(0xffbbdefb),
	Color(0xffb3e5fc),
	Color(0xffb2ebf2),
	Color(0xffb2dfdb),
	Color(0xffc8e6c9),
	Color(0xffdcedc8),
	Color(0xfff0f4c3),
	Color(0xfffff9c4),
	Color(0xffffecb3),
	Color(0xffffe0b2),
	Color(0xffffccbc)
)

val materialColors200 = listOf(
	Color(0xffef9a9a),
	Color(0xfff48fb1),
	Color(0xffce93d8),
	Color(0xffb39ddb),
	Color(0xff9fa8da),
	Color(0xff90caf9),
	Color(0xff81d4fa),
	Color(0xff80deea),
	Color(0xff80cbc4),
	Color(0xffa5d6a7),
	Color(0xffc5e1a5),
	Color(0xffe6ee9c),
	Color(0xfffff59d),
	Color(0xffffe082),
	Color(0xffffcc80),
	Color(0xffffab91)
)

val materialColors700 = listOf(
	Color(0xffd01716),
	Color(0xffc2185b),
	Color(0xff7b1fa2),
	Color(0xff512da8),
	Color(0xff303f9f),
	Color(0xff455ede),
	Color(0xff0288d1),
	Color(0xff0097a7),
	Color(0xff00796b),
	Color(0xff0a7e07),
	Color(0xff689f38),
	Color(0xffafb42b),
	Color(0xfffbc02d),
	Color(0xffffa000),
	Color(0xfff57c00),
	Color(0xffe64a19)
)

val materialColors900 = listOf(
	Color(0xffb0120a),
	Color(0xff880e4f),
	Color(0xff4a148c),
	Color(0xff311b92),
	Color(0xff1a237e),
	Color(0xff2a36b1),
	Color(0xff01579b),
	Color(0xff006064),
	Color(0xff004d40),
	Color(0xff0d5302),
	Color(0xff33691e),
	Color(0xff827717),
	Color(0xfff57f17),
	Color(0xffff6f00),
	Color(0xffe65100),
	Color(0xffbf360c)
)

fun List<Color>.getRandom(it: Any): Color =
	get(Random(it.hashCode()).nextInt(size))
