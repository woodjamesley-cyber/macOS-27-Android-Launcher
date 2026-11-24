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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.ui.theme.LocalGlassStyle

/** Minimal, dependency-free arithmetic parser (supports + - × ÷ % parentheses and decimals). */
object CalcEngine {
    fun evaluate(raw: String): Double? = runCatching {
        val cleaned = raw
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(",", "")
        val parser = Parser(cleaned)
        val value = parser.expression()
        if (parser.position < parser.text.length) null else value
    }.getOrNull()

    private class Parser(val text: String) {
        var position = 0

        fun expression(): Double {
            var value = term()
            while (position < text.length) {
                when (text[position]) {
                    '+' -> { position++; value += term() }
                    '-' -> { position++; value -= term() }
                    else -> return value
                }
            }
            return value
        }

        private fun term(): Double {
            var value = factor()
            while (position < text.length) {
                when (text[position]) {
                    '*' -> { position++; value *= factor() }
                    '/' -> {
                        position++
                        val divisor = factor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        value /= divisor
                    }
                    '%' -> {
                        position++
                        value %= factor()
                    }
                    else -> return value
                }
            }
            return value
        }

        private fun factor(): Double {
            if (position < text.length && text[position] == '(') {
                position++
                val value = expression()
                if (position < text.length && text[position] == ')') position++
                return value
            }
            if (position < text.length && (text[position] == '-' || text[position] == '+')) {
                val sign = if (text[position] == '-') -1.0 else 1.0
                position++
                return sign * factor()
            }
            val start = position
            while (position < text.length && (text[position].isDigit() || text[position] == '.')) position++
            if (start == position) throw IllegalArgumentException("Unexpected character")
            return text.substring(start, position).toDouble()
        }
    }
}

private fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    val rounded = if (kotlin.math.abs(value) >= 1e12) value else Math.round(value * 1e10) / 1e10
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else rounded.toString().trimEnd('0').trimEnd('.')
}

@Composable
fun CalculatorApp(state: LauncherState, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var history by remember { mutableStateOf<String?>(null) }

    fun press(token: String) {
        when (token) {
            "AC" -> { expression = ""; result = "0"; history = null }
            "⌫" -> { expression = expression.dropLast(1); result = CalcEngine.evaluate(expression)?.let(::formatNumber) ?: "0" }
            "=" -> {
                val value = CalcEngine.evaluate(expression)
                if (value != null) {
                    history = expression
                    result = formatNumber(value)
                    expression = result
                } else {
                    result = "Error"
                }
            }
            "+/−" -> {
                expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
            }
            else -> {
                expression += token
                CalcEngine.evaluate(expression)?.let { result = formatNumber(it) }
            }
        }
    }

    LaunchedEffect(state.command) {
        val command = state.command ?: return@LaunchedEffect
        if (command.app == BuiltinApp.CALCULATOR && command.action == "clear") {
            expression = ""
            result = "0"
            history = null
        }
    }

    Column(modifier.padding(16.dp)) {
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.End) {
            Spacer(Modifier.weight(1f))
            history?.let {
                Text(
                    "$it =",
                    style = MaterialTheme.typography.bodyMedium,
                    color = style.tertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                expression.ifBlank { "0" },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = style.secondary,
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                result,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
                color = style.text,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        val rows = listOf(
            listOf("AC", "+/−", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { label ->
                    val isOperator = label in setOf("÷", "×", "−", "+", "=")
                    val isFunction = label in setOf("AC", "+/−", "%", "⌫")
                    val bg = when {
                        label == "=" -> MaterialTheme.colorScheme.primary
                        isOperator -> Color(0x33FF9F0A)
                        isFunction -> style.hairline
                        else -> style.field
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(bg)
                            .clickable { press(label) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (label == "=") Color.White else style.text
                        )
                    }
                }
            }
        }
    }
}
