package com.ether4o4.morsvitaest

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Current weather for the Widgets panel. */
data class WeatherInfo(
    val temperatureF: Int,
    val description: String,
    val emoji: String,
    val place: String,
)

private val weatherJson = Json { ignoreUnknownKeys = true }

/**
 * Fetches current weather with no API key: IP geolocation (ipwho.is) for an
 * approximate location, then Open-Meteo for the current conditions. Returns
 * null on any failure (offline, blocked, parse error) so the widget can fall
 * back gracefully.
 */
suspend fun weatherNow(): WeatherInfo? = try {
    val client = httpClient()
    try {
        val geo = weatherJson.parseToJsonElement(client.get("https://ipwho.is/").bodyAsText()).jsonObject
        val lat = geo["latitude"]!!.jsonPrimitive.double
        val lon = geo["longitude"]!!.jsonPrimitive.double
        val city = geo["city"]?.jsonPrimitive?.contentOrNull ?: ""
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current_weather=true&temperature_unit=fahrenheit"
        val cw = weatherJson.parseToJsonElement(client.get(url).bodyAsText())
            .jsonObject["current_weather"]!!.jsonObject
        val temp = cw["temperature"]!!.jsonPrimitive.double
        val code = cw["weathercode"]!!.jsonPrimitive.int
        val (desc, emoji) = describeWeather(code)
        WeatherInfo(temp.roundToInt(), desc, emoji, city)
    } finally {
        client.close()
    }
} catch (_: Exception) {
    null
}

private fun describeWeather(code: Int): Pair<String, String> = when (code) {
    0 -> "Clear" to "☀️"
    1, 2 -> "Partly Cloudy" to "⛅"
    3 -> "Overcast" to "☁️"
    45, 48 -> "Fog" to "🌫️"
    in 51..57 -> "Drizzle" to "🌦️"
    in 61..67 -> "Rain" to "🌧️"
    in 71..77 -> "Snow" to "❄️"
    in 80..82 -> "Showers" to "🌦️"
    in 85..86 -> "Snow Showers" to "🌨️"
    in 95..99 -> "Thunderstorm" to "⛈️"
    else -> "—" to "⛅"
}
