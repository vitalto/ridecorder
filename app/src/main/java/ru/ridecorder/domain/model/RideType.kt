package ru.ridecorder.domain.model

enum class RideType(val value: String, val displayName: String) {
    TRAINING("training", "Тренировка"),
    RACE("race", "Гонка"),
    REGULAR("regular", "Регулярный маршрут"),
    OTHER("other", "Другое");

    companion object {
        fun fromString(value: String?): RideType {
            return entries.find { it.value == value } ?: OTHER
        }

        fun getDisplayName(rideType: RideType): String {
            return rideType.displayName
        }
    }
}