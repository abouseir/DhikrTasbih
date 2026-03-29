package com.example.dhikrtasbih.wear

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.tiles.GlanceTileService

class TasbihTileService : GlanceTileService() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("wear_tasbih_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("count", 0)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = TextStyle(
                    color = ColorProvider(Color(0xFFD4AF37)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
