package com.example.dhikrtasbih

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.datastore.preferences.core.intPreferencesKey

object TasbihWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val trigger = currentState(key = intPreferencesKey("trigger")) ?: 0
            val prefs = PreferencesManager(context)
            val tasbihList = prefs.getTasbihList() ?: emptyList()
            val index = prefs.getWidgetIndex().coerceIn(0, maxOf(0, tasbihList.size - 1))
            val selectedDhikr = tasbihList.getOrNull(index) ?: DhikrItem(0, "تسبيح", 0, 0)
            
            GlanceTheme {
                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(Color(0xFF000000))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ── Reset button row at the top
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier.size(36.dp).clickable(actionRunCallback<ResetAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↺",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFF888888)),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // ── Dhikr text with prev/next navigation
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = GlanceModifier.size(48.dp).clickable(actionRunCallback<PrevAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⟨",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFFD4AF37)),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            val fixedText = selectedDhikr.textAr.map { if (it == '﴿') '﴾' else if (it == '﴾') '﴿' else it }.joinToString("")
                            Text(
                                text = "\u200F$fixedText\u200F",
                                modifier = GlanceModifier.defaultWeight().padding(horizontal = 4.dp),
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFF5F5F5)),
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )

                            )

                            Box(
                                modifier = GlanceModifier.size(48.dp).clickable(actionRunCallback<NextAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⟩",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFFD4AF37)),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // ── Counter
                        val targetTxt = if (selectedDhikr.target > 0) " / ${selectedDhikr.target}" else " / ∞"
                        Text(
                            text = "${selectedDhikr.count}$targetTxt",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFFD4AF37)),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(12.dp))

                        // ── Increment button at the bottom
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(Color(0xFFD4AF37))
                                .cornerRadius(16.dp)
                                .padding(vertical = 16.dp)
                                .clickable(actionRunCallback<IncrementAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "سَبِّحْ",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color.Black),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))
                    }
                }
            }
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = PreferencesManager(context)
        val size = prefs.getTasbihList()?.size ?: 1
        if (size > 0) {
            val next = (prefs.getWidgetIndex() + 1) % size
            prefs.saveWidgetIndex(next)
            updateAppWidgetState(context, glanceId) { state ->
                val key = intPreferencesKey("trigger")
                state[key] = (state[key] ?: 0) + 1
            }
            TasbihWidget.update(context, glanceId)
        }
    }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = PreferencesManager(context)
        val size = prefs.getTasbihList()?.size ?: 1
        if (size > 0) {
            val prev = if (prefs.getWidgetIndex() - 1 < 0) size - 1 else prefs.getWidgetIndex() - 1
            prefs.saveWidgetIndex(prev)
            updateAppWidgetState(context, glanceId) { state ->
                val key = intPreferencesKey("trigger")
                state[key] = (state[key] ?: 0) + 1
            }
            TasbihWidget.update(context, glanceId)
        }
    }
}

class IncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = PreferencesManager(context)
        val tasbihList = prefs.getTasbihList()?.toMutableList() ?: mutableListOf()
        val index = prefs.getWidgetIndex().coerceIn(0, maxOf(0, tasbihList.size - 1))
        
        if (tasbihList.isNotEmpty()) {
            val dhikr = tasbihList[index]
            tasbihList[index] = dhikr.copy(count = dhikr.count + 1)
            prefs.saveTasbihList(tasbihList)
            updateAppWidgetState(context, glanceId) { state ->
                val key = intPreferencesKey("trigger")
                state[key] = (state[key] ?: 0) + 1
            }
            TasbihWidget.update(context, glanceId)
        }
    }
}

class ResetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = PreferencesManager(context)
        val tasbihList = prefs.getTasbihList()?.toMutableList() ?: mutableListOf()
        val index = prefs.getWidgetIndex().coerceIn(0, maxOf(0, tasbihList.size - 1))
        
        if (tasbihList.isNotEmpty()) {
            val dhikr = tasbihList[index]
            tasbihList[index] = dhikr.copy(count = 0)
            prefs.saveTasbihList(tasbihList)
            updateAppWidgetState(context, glanceId) { state ->
                val key = intPreferencesKey("trigger")
                state[key] = (state[key] ?: 0) + 1
            }
            TasbihWidget.update(context, glanceId)
        }
    }
}

class TasbihWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasbihWidget
}
