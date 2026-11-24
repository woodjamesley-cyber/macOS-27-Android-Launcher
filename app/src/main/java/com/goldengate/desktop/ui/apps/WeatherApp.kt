package com.goldengate.desktop.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.data.WeatherStore
import com.goldengate.desktop.data.weatherDescription
import com.goldengate.desktop.data.weatherGlyph
import com.goldengate.desktop.ui.components.DividerLine
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.components.SegmentedControl
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import kotlinx.coroutines.launch

@Composable
fun WeatherApp(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf(prefs.weatherCity) }
    val data = WeatherStore.data
    val loading = WeatherStore.loading
    val error = WeatherStore.error

    LaunchedEffect(city) {
        WeatherStore.refresh(city)
    }

    LaunchedEffect(state.command) {
        val command = state.command ?: return@LaunchedEffect
        if (command.app == BuiltinApp.WEATHER && command.action == "refresh") {
            WeatherStore.refresh(city, force = true)
        }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(style.field)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = style.tertiary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                BasicTextField(
                    value = city,
                    onValueChange = { city = it },
                    singleLine = true,
                    textStyle = TextStyle(color = style.text, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (city.isEmpty()) Text("City name", color = style.tertiary, fontSize = 14.sp)
                        inner()
                    }
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(style.field)
                    .clickable {
                        prefs.setWeatherCity(city)
                        scope.launch { WeatherStore.refresh(city, force = true) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Refresh forecast",
                    tint = style.secondary,
                    modifier = Modifier.size(17.dp)
                )
            }
            SegmentedControl(
                options = listOf("°C", "°F"),
                selected = if (prefs.weatherUnit == "f") 1 else 0,
                onSelect = { prefs.setWeatherUnit(if (it == 1) "f" else "c") }
            )
        }

        Spacer(Modifier.height(18.dp))

        if (data == null && loading) {
            Text("Contacting Open-Meteo…", color = style.secondary, style = MaterialTheme.typography.bodyMedium)
        } else if (data == null && error != null) {
            Column {
                Text("Weather unavailable", style = MaterialTheme.typography.titleMedium, color = style.text)
                Spacer(Modifier.height(4.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = style.secondary)
                Spacer(Modifier.height(10.dp))
                PillButton("Try again", prominent = true) {
                    scope.launch { WeatherStore.refresh(city, force = true) }
                }
            }
        } else if (data != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(data.city, style = MaterialTheme.typography.headlineSmall, color = style.text)
                    Text(
                        weatherDescription(data.code),
                        style = MaterialTheme.typography.bodyMedium,
                        color = style.secondary
                    )
                }
                Text(
                    weatherGlyph(data.code),
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
                    color = Color(0xFFFFD166)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    data.displayTemp(prefs.weatherUnit),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                    fontWeight = FontWeight.Light,
                    color = style.text
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Feels like ${if (prefs.weatherUnit == "f") "${(data.feelsC * 9 / 5 + 32).toInt()}°" else "${data.feelsC.toInt()}°"}" +
                    "   •   Humidity ${data.humidity}%   •   Wind ${data.windKph.toInt()} km/h",
                style = MaterialTheme.typography.labelMedium,
                color = style.tertiary
            )
            Spacer(Modifier.height(16.dp))
            DividerLine()
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                data.daily.forEach { day ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(style.field)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(day.dayLabel, style = MaterialTheme.typography.labelSmall, color = style.secondary)
                        Spacer(Modifier.height(4.dp))
                        Text(weatherGlyph(day.code), style = MaterialTheme.typography.bodyLarge, color = style.text)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            data.displayRange(day.minC, day.maxC, prefs.weatherUnit),
                            style = MaterialTheme.typography.labelSmall,
                            color = style.text
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Live data from Open-Meteo. Only the city name is sent; this launcher never asks for your device location.",
                style = MaterialTheme.typography.labelSmall,
                color = style.tertiary
            )
            Text(
                "Last updated: " + java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .format(java.time.Instant.ofEpochMilli(data.fetchedAt).atZone(java.time.ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelSmall,
                color = style.tertiary
            )
            Spacer(Modifier.height(8.dp))
            PillButton("Refresh") {
                scope.launch { WeatherStore.refresh(city, force = true) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
