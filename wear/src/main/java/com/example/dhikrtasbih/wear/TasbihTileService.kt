package com.example.dhikrtasbih.wear

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.glance.action.actionStartActivity
import androidx.glance.wear.tiles.GlanceTileService
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback

class TasbihTileService : GlanceTileService() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("wear_tasbih_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("count", 0)

        // Basic Tile showing the count and tapping it opens the Wear app
        androidx.glance.wear.tiles.provideCurrentContent {
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
                        fontSize = androidx.compose.ui.unit.sp(32),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
