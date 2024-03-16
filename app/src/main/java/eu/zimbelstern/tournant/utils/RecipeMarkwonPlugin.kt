package eu.zimbelstern.tournant.utils

import android.content.Context
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import eu.zimbelstern.tournant.R
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.MarkwonTheme
import org.commonmark.node.BlockQuote
import org.commonmark.node.Heading
import org.commonmark.parser.Parser
import kotlin.math.roundToInt


class RecipeMarkwonPlugin(private val context: Context) : AbstractMarkwonPlugin() {

	override fun configureParser(builder: Parser.Builder) {
		builder.enabledBlockTypes(setOf(BlockQuote::class.java, Heading::class.java))
	}

	override fun configureTheme(builder: MarkwonTheme.Builder) {
		builder.bulletWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 5f, context.resources.displayMetrics).roundToInt())
			.headingBreakHeight(0)
			.headingTextSizeMultipliers(floatArrayOf(1.15f, 1.1f, 1.05f, 1f, 1f, 1f))
			.headingTypeface(ResourcesCompat.getFont(context, R.font.quicksand_bold) ?: return)
	}

	override fun configureVisitor(builder: MarkwonVisitor.Builder) {
		// Removes empty lines below headings by overriding default behaviour
		builder.on(Heading::class.java) { visitor, heading ->
			visitor.ensureNewLine()
			val length = visitor.length()
			visitor.visitChildren(heading)
			CoreProps.HEADING_LEVEL.set(visitor.renderProps(), heading.level)
			visitor.setSpansForNodeOptional(heading, length)
			if (visitor.hasNext(heading)) {
				visitor.ensureNewLine()
			}
		}
	}

}