package ru.ridecorder.domain.model

enum class Visibility(val value: String, val displayName: String) {
    ALL("all", "Все пользователи"),
    FOLLOWERS("followers", "Подписчики"),
    NOBODY("nobody", "Только вы");

    companion object {
        fun fromString(value: String?): Visibility {
            return entries.find { it.value == value } ?: NOBODY
        }

        fun getDisplayName(visibility: Visibility): String {
            return visibility.displayName
        }
    }
}
