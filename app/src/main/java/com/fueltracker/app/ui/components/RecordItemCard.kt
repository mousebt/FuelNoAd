package com.fueltracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordItemCard(
    record: FuelRecord,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(record.timestamp)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Fuel Type Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = record.fuelType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Consumption or Status Badge
                    if (record.calculatedConsumption != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${record.calculatedConsumption} L/100km",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    } else if (!record.isFull) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "未加满·待统算",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD97706)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "基准记录",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "里程 ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${record.odometer.toLong()} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (record.distanceTraveled != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(+${record.distanceTraveled.toInt()}km)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF059669)
                        )
                    }
                }

                if (!record.stationName.isNullOrBlank()) {
                    Text(
                        text = "📍 ${record.stationName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Right side: Money & Volume
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "¥${String.format("%.2f", record.totalPrice)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.fuelAmount} L · ¥${record.unitPrice}/L",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
