package com.example.dhikrtasbih.wear

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.MaterialTheme

data class Dhikr(val id: Int, val textAr: String, val target: Int)

val defaultDhikrs = listOf(
    Dhikr(1, "سُبْحَانَ اللَّهِ", 33),
    Dhikr(2, "الْحَمْدُ لِلَّهِ", 33),
    Dhikr(3, "اللَّهُ أَكْبَرُ", 33),
    Dhikr(4, "لَا إِلَهَ إِلَّا اللَّهُ", 33),
    Dhikr(5, "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", 0)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("wear_tasbih_prefs", Context.MODE_PRIVATE)
    
    var dhikrIndex by remember { mutableStateOf(prefs.getInt("dhikr_index", 0)) }
    val currentDhikr = defaultDhikrs[dhikrIndex]
    
    var count by remember { mutableStateOf(prefs.getInt("count_${currentDhikr.id}", 0)) }

    // Update count when index changes
    LaunchedEffect(dhikrIndex) {
        count = prefs.getInt("count_${defaultDhikrs[dhikrIndex].id}", 0)
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⟨",
                        color = Color(0xFFD4AF37),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            dhikrIndex = if (dhikrIndex - 1 < 0) defaultDhikrs.size - 1 else dhikrIndex - 1
                            prefs.edit().putInt("dhikr_index", dhikrIndex).apply()
                            vibrateWearDevice(context, 20L)
                        }.padding(8.dp)
                    )
                    
                    Text(
                        text = "🔄",
                        fontSize = 16.sp,
                        modifier = Modifier.clickable {
                            count = 0
                            prefs.edit().putInt("count_${currentDhikr.id}", 0).apply()
                            vibrateWearDevice(context, 40L)
                        }.padding(8.dp)
                    )

                    Text(
                        text = "⟩",
                        color = Color(0xFFD4AF37),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            dhikrIndex = (dhikrIndex + 1) % defaultDhikrs.size
                            prefs.edit().putInt("dhikr_index", dhikrIndex).apply()
                            vibrateWearDevice(context, 20L)
                        }.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Dhikr Text
                Text(
                    text = "\u200F${currentDhikr.textAr}\u200F",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Counter and Target
                val targetStr = if (currentDhikr.target > 0) " / ${currentDhikr.target}" else " / ∞"
                Text(
                    text = "$count$targetStr",
                    color = Color(0xFFD4AF37),
                    style = MaterialTheme.typography.display3.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Large Increment Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFFD4AF37), RoundedCornerShape(24.dp))
                        .clickable {
                            if (currentDhikr.target == 0 || count < currentDhikr.target) {
                                count++
                                prefs.edit().putInt("count_${currentDhikr.id}", count).apply()
                                vibrateWearDevice(context, 30L)
                            } else {
                                vibrateWearDevice(context, 100L) // Long vibrate if max reached
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "سَبِّحْ",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun vibrateWearDevice(context: Context, durationMs: Long = 30L) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            ?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            ?.vibrate(durationMs)
    }
}
