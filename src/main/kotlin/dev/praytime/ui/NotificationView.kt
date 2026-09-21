package dev.praytime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.praytime.calculation.PrayerTime

private val dueAmber = Color(0xFFFFC24D)
private val prayedGreen = Color(0xFF3EDC81)
private val prayedGreenText = Color(0xFF0B2E1B)

@Composable
fun PrayerNotification(
    prayer: PrayerTime,
    onPrayed: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(272.dp)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(cardSurface)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(prayerColors.getValue(prayer.name)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    prayer.name,
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "DUE",
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black),
                    color = dueAmber,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    prayer.displayTime,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✕",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Time to pray ${prayer.name}.",
                style = TextStyle(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(prayedGreen)
                    .clickable(onClick = onPrayed)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "✓",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black),
                    color = prayedGreenText,
                )
                Text(
                    "Mark prayed",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = prayedGreenText,
                )
            }
        }
    }
}
