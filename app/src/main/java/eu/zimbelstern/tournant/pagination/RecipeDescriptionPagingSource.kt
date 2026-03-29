package eu.zimbelstern.tournant.pagination

import androidx.paging.PagingSource
import androidx.paging.PagingState
import eu.zimbelstern.tournant.data.RecipeDescription
import eu.zimbelstern.tournant.data.room.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class RecipeDescriptionPagingSource(
	private val recipeRepository: RecipeRepository,
	private val query: String?,
	private val orderedBy: Int
) : PagingSource<Int, RecipeDescription>() {

	override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecipeDescription> {
		val result = withContext(Dispatchers.IO) {
			recipeRepository.getRecipeDescriptions(
				query = query ?: "",
				orderedBy = orderedBy,
				offset = params.key?.plus(2)?.times(params.loadSize) ?: 0,
				limit = params.loadSize,
				Calendar.getInstance().get(Calendar.MONTH)
			).onEach {
				it.keywords = LinkedHashSet(recipeRepository.getKeywords(it.id))
			}
		}

		return LoadResult.Page(
			data = result,
			prevKey = params.key?.minus(1),
			nextKey = if (result.isEmpty()) null else params.key?.plus(1) ?: 1
		)
	}

	override fun getRefreshKey(state: PagingState<Int, RecipeDescription>): Int? {
		return state.anchorPosition?.let {
			state.closestPageToPosition(it)?.prevKey?.plus(1) ?: 0
		}
	}

}
