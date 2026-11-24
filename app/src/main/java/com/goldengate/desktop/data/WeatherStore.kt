package com.goldengate.desktop.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Immutable
data class DailyForecast(
    val dayLabel: String,
    val minC: Double,
    val maxC: Double,
    val code: Int
)

@Immutable
data class WeatherData(
    val city: String,
    val tempC: Double,
    val feelsC: Double,
    val humidity: Int,
    val windKph: Double,
    val code: Int,
    val daily: List<DailyForecast>,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun displayTemp(unit: String): String {
        val value = if (unit == "f") tempC * 9 / 5 + 32 else tempC
        return "${value.toInt()}°"
    }

    fun displayRange(minC: Double, maxC: Double, unit: String): String {
        val lo = if (unit == "f") minC * 9 / 5 + 32 else minC
        val hi = if (unit == "f") maxC * 9 / 5 + 32 else maxC
        return "${lo.toInt()}° / ${hi.toInt()}°"
    }
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75 -> "Snow"
    77 -> "Snow grains"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Weather"
}

fun weatherGlyph(code: Int): String = when (code) {
    0, 1 -> "☀︎"
    2 -> "⛅︎"
    3 -> "☁︎"
    45, 48 -> "🌫"
    51, 53, 55, 56, 57 -> "🌦"
    61, 63, 65, 66, 67, 80, 81, 82 -> "🌧"
    71, 73, 75, 77, 85, 86 -> "❄︎"
    95, 96, 99 -> "⛈"
    else -> "☁︎"
}

/**
 * Live weather without an API key, using the free Open-Meteo endpoints. Nothing but the city name
 * is sent; no device location is requested at all.
 */
object WeatherStore {

    var data by mutableStateOf<WeatherData?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var lastCity by mutableStateOf("")

    suspend fun refresh(city: String, force: Boolean = false) {
        if (city.isBlank()) return
        if (!force && data != null && lastCity == city &&
            System.currentTimeMillis() - data!!.fetchedAt < 15 * 60_000
        ) return
        loading = true
        error = null
        val result = runCatching { fetch(city) }
        result.onSuccess {
            data = it
            lastCity = city
        }.onFailure {
            error = it.message ?: "Could not reach the weather service"
        }
        loading = false
    }

    private suspend fun fetch(city: String): WeatherData = withContext(Dispatchers.IO) {
        val geo = getJson(
            "https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&format=json&name=" +
                URLEncoder.encode(city, "UTF-8")
        )
        val place = geo.optJSONArray("results")?.optJSONObject(0)
            ?: throw IllegalStateException("City not found — try another spelling")
        val lat = place.getDouble("latitude")
        val lon = place.getDouble("longitude")
        val name = place.optString("name", city) +
            place.optString("country_code", "").let { if (it.isBlank()) "" else ", $it" }

        val forecast = getJson(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=6&timezone=auto"
        )
        val current = forecast.getJSONObject("current")
        val daily = forecast.getJSONObject("daily")
        val codes = daily.getJSONArray("weather_code")
        val maxes = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")
        val times = daily.getJSONArray("time")
        val days = (0 until minOf(codes.length(), 6)).map { i ->
            DailyForecast(
                dayLabel = runCatching {
                    java.time.LocalDate.parse(times.getString(i))
                        .format(java.time.format.DateTimeFormatter.ofPattern("EEE"))
                }.getOrDefault("Day ${i + 1}"),
                minC = mins.getDouble(i),
                maxC = maxes.getDouble(i),
                code = codes.getInt(i)
            )
        }
        WeatherData(
            city = name,
            tempC = current.getDouble("temperature_2m"),
            feelsC = current.optDouble("apparent_temperature", current.getDouble("temperature_2m")),
            humidity = current.optInt("relative_humidity_2m", 0),
            windKph = current.optDouble("wind_speed_10m", 0.0),
            code = current.optInt("weather_code", 0),
            daily = days
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
