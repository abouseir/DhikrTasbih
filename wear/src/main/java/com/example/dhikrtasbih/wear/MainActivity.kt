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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh

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
    var count by remember { mutableStateOf(prefs.getInt("count", 0)) }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable {
                    count++
                    prefs.edit().putInt("count", count).apply()
                    vibrateWearDevice(context, 30L)
                },
            contentAlignment = Alignment.Center
        ) {
            // Main counter
            Text(
                text = count.toString(),
                color = Color(0xFFD4AF37),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            )

            // Reset button at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(40.dp)
                    .clickable {
                        count = 0
                        prefs.edit().putInt("count", 0).apply()
                        vibrateWearDevice(context, 50L)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(24.dp)
                )
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
