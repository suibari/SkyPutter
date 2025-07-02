package com.suibari.skyputter.ui.sentiment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.suibari.skyputter.ui.common.CommonTopBar
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun SentimentCalendarScreen(
    viewModel: SentimentCalendarViewModel,
    onBack: () -> Unit,
) {
    val dailySentiments = viewModel.dailySentiments.collectAsState()
    val today = remember { LocalDate.now() }
    val start = today.minusMonths(6).withDayOfMonth(1)
    val end = today.plusMonths(6).withDayOfMonth(1)
    val state = rememberCalendarState(
        startMonth = YearMonth.from(start),
        endMonth = YearMonth.from(end),
        firstVisibleMonth = YearMonth.from(today),
        firstDayOfWeek = DayOfWeek.SUNDAY
    )

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "きぶんカレンダー",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            VerticalCalendar(
                state = state,
                monthHeader = { month ->
                    Text(
                        text = month.yearMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                },
                dayContent = { day ->
                    val sentiment = dailySentiments.value[day.date]
                    val bg = sentiment?.let { getSentimentColor(it) } ?: Color.Transparent
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .background(bg, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (sentiment != null) Color.White else Color.Black
                        )
                    }
                }
            )
        }
    }
}

private val NegativeColor = Color(0xFFE57373) // 赤
private val NeutralColor = Color(0xFFFFF176)  // 黄
private val PositiveColor = Color(0xFF81C784) // 緑
private const val SENTIMENT_SCALE = 0.1f

private fun getSentimentColor(value: Float): Color {
    val scaled = (value / SENTIMENT_SCALE).coerceIn(-1f, 1f)
    return if (scaled < 0f) {
        val t = (scaled + 1f) / 1f // -1 → 0, 0 → 1
        lerp(NegativeColor, NeutralColor, t)
    } else {
        val t = scaled / 1f // 0 → 0, +1 → 1
        lerp(NeutralColor, PositiveColor, t)
    }
}