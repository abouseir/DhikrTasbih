package com.example.dhikrtasbih.wear

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.wear.tiles.GlanceTileService

class TasbihTileService : GlanceTileService() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("wear_tasbih_prefs", Context.MODE_PRIVATE)
        val dhikrIndex = prefs.getInt("dhikr_index", 0)
        
        // Failsafe in case index is out of bounds
        val safeIndex = if (dhikrIndex in tasbihItems.indices) dhikrIndex else 0
        val currentDhikr = tasbihItems[safeIndex]
        val count = prefs.getInt("count_${currentDhikr.id}", 0)
        val targetTxt = if (currentDhikr.target > 0) " / ${currentDhikr.target}" else " / ∞"

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\u200F${currentDhikr.textAr}\u200F",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "$count$targetTxt",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFD4AF37)),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(12.dp))
                Text(
                    text = "سَبِّحْ (افتح التطبيق)",
                    style = TextStyle(
                        color = ColorProvider(Color.Gray),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
