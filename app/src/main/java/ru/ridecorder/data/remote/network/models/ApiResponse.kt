package ru.ridecorder.data.remote.network.models

class ApiResponse<T> private constructor(
    val success: Boolean,
    val errorMessage: String? = null,
    val data: T? = null
) {
    // Вторичный конструктор для успешного варианта
    constructor(data: T) : this(
        success = true,
        data = data
    )

    // Вторичный конструктор для неудачного варианта
    constructor(errorMessage: String) : this(
        success = false,
        errorMessage = errorMessage
    )
}