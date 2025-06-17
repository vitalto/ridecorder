package ru.ridecorder.data.remote

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") // Ожидаемый формат без 'Z'

    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.atOffset(ZoneOffset.UTC)?.format(formatter)) // Преобразуем в UTC строку без 'Z'
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant {
        val dateTime = LocalDateTime.parse(json?.asString, formatter) // Парсим строку без 'Z'
        return dateTime.atOffset(ZoneOffset.UTC).toInstant() // Преобразуем в Instant, считая UTC
    }
}