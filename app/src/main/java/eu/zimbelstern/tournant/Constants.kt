package eu.zimbelstern.tournant

class Constants {

	companion object {
		const val MODE_STANDALONE = 1
		const val MODE_SYNCED = 2
		const val PREF_VERSION = "VERSION"
		const val PREF_MODE = "FILE_MODE"
		const val PREF_FILE = "SYNCED_FILE_URI"
		const val PREF_MARKDOWN = "MARKDOWN"
		const val PREF_SCREEN_ON = "SCREEN_ON"
		const val PREF_COLOR_THEME = "COLOR_THEME"
		const val PREF_DECIMAL_SEPARATOR_COMMA = "DECIMAL_SEPARATOR_COMMA"
		const val PREF_FILE_LAST_MODIFIED = "FILE_LAST_MODIFIED"
		const val PREF_SORT = "SORT"
		const val SORTED_BY_TITLE = 0
		const val SORTED_BY_RATING = 1
		const val SORTED_BY_PREPTIME = 2
		const val SORTED_BY_COOKTIME = 3
		const val SORTED_BY_TOTALTIME = 4
		const val SORTED_BY_CREATED = 5
		const val SORTED_BY_MODIFIED = 6
		const val SORTED_BY_INSTRUCTIONS_LENGTH = 7
		const val SORTED_BY_INGREDIENTS_COUNT = 8
		const val SORTED_BY_PREPARATIONS_COUNT = 9
		const val SORTED_BY_PREPARED = 10
		const val SORTED_BY_SEASON = 11
	}
	
}