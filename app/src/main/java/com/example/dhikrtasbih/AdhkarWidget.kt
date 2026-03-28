package com.example.dhikrtasbih

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.datastore.preferences.core.intPreferencesKey

// ── Category IDs from AdhkarData ──────────────────────────────────────────────
// 1 = أذكار الصباح  |  2 = أذكار المساء  |  3 = أذكار النوم
// 4 = أذكار الاستيقاظ  |  5 = أذكار بعد الصلاة
private val WIDGET_CAT_ID_KEY  = intPreferencesKey("adhkar_widget_cat_id")
private val WIDGET_ITEM_IDX_KEY = intPreferencesKey("adhkar_widget_item_idx")
private val TRIGGER_KEY        = intPreferencesKey("adhkar_widget_trigger")

private val CATEGORY_ORDER = listOf(1, 2, 5, 3, 4) // morning, evening, after-prayer, sleep, wake-up
private val CATEGORY_ICONS = mapOf(
    1 to "☀️", 2 to "🌙", 3 to "😴", 4 to "🌅", 5 to "🕌"
)

object AdhkarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Read state
            @Suppress("UNUSED_VARIABLE")
            val trigger  = currentState(key = TRIGGER_KEY) ?: 0
            val catId    = currentState(key = WIDGET_CAT_ID_KEY) ?: 1
            val itemIdx  = currentState(key = WIDGET_ITEM_IDX_KEY) ?: 0

            val prefs = PreferencesManager(context)
            val categories = AdhkarData.categories()
            val category   = categories.find { it.id == catId } ?: categories.first()

            // Inject saved counts into category items
            category.items.forEach { item -> item.count = prefs.getAdhkarCount(category.id, item.id) }

            val safeIdx  = itemIdx.coerceIn(0, maxOf(0, category.items.size - 1))
            val item     = category.items.getOrNull(safeIdx) ?: return@provideContent
            val progress = if (item.target > 0) item.count.toFloat() / item.target.toFloat() else 0f
            val isDone   = item.target > 0 && item.count >= item.target
            val catIcon  = CATEGORY_ICONS[catId] ?: "📿"

            GlanceTheme {
                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(Color(0xFF000000))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ── Top bar: Category label + cycle button
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(48.dp)
                                    .clickable(actionRunCallback<AdhkarCycleCategoryAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "⚙",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFF888888)),
                                        fontSize = 24.sp
                                    )
                                )
                            }
                            Spacer(GlanceModifier.defaultWeight())
                            Text(
                                text = "$catIcon ${category.titleAr}",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFD4AF37)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // ── Item Navigation row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = GlanceModifier.size(48.dp).clickable(actionRunCallback<AdhkarPrevItemAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⟨", style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(
                                        if (isDone) Color(0xFF444444) else Color(0xFFD4AF37)
                                    ),
                                    fontSize = 24.sp, fontWeight = FontWeight.Bold
                                ))
                            }

                            val textLength = item.textAr.length
                            val dynamicFontSize = when {
                                textLength > 400 -> 14.sp
                                textLength > 250 -> 16.sp
                                textLength > 150 -> 18.sp
                                textLength > 60 -> 24.sp
                                else -> 30.sp
                            }

                            val fixedText = item.textAr
                            Text(
                                text = "\u200F$fixedText\u200F",
                                modifier = GlanceModifier.defaultWeight().padding(horizontal = 4.dp),
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(
                                        if (isDone) Color(0xFF4CAF50) else Color(0xFFF5F5F5)
                                    ),
                                    fontSize = dynamicFontSize,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Box(
                                modifier = GlanceModifier.size(48.dp).clickable(actionRunCallback<AdhkarNextItemAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⟩", style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(
                                        if (isDone) Color(0xFF444444) else Color(0xFFD4AF37)
                                    ),
                                    fontSize = 24.sp, fontWeight = FontWeight.Bold
                                ))
                            }
                        }

                        // ── Bottom clickable area (combines Counter + Button)
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight()
                                .padding(top = 12.dp, bottom = 2.dp)
                                .clickable(
                                    if (isDone) actionRunCallback<AdhkarResetCycleAction>()
                                    else actionRunCallback<AdhkarIncrementAction>()
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // ── Counter: x/target or ✓ Done
                                if (isDone) {
                                    Text("✓ تم", style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color(0xFF4CAF50)),
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold
                                    ))
                                } else {
                                    val targetStr = if (item.target > 0) " / ${item.target}" else ""
                                    Text(
                                        "${item.count}$targetStr",
                                        style = TextStyle(
                                            color = androidx.glance.unit.ColorProvider(Color(0xFFD4AF37)),
                                            fontSize = 22.sp, fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(GlanceModifier.height(12.dp))

                                // ── Increment button (or Reset-cycle when done)
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .background(if (isDone) Color(0xFF1A1A1A) else Color(0xFFD4AF37))
                                        .cornerRadius(16.dp)
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isDone) "🔄 دورة جديدة" else "اذكر",
                                        style = TextStyle(
                                            color = androidx.glance.unit.ColorProvider(
                                                if (isDone) Color(0xFFD4AF37) else Color.Black
                                            ),
                                            fontSize = 16.sp, fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper: bump trigger to force UI refresh ───────────────────────────────────
private suspend fun bumpTrigger(context: Context, glanceId: GlanceId) {
    updateAppWidgetState(context, glanceId) { s ->
        s[TRIGGER_KEY] = (s[TRIGGER_KEY] ?: 0) + 1
    }
    AdhkarWidget.update(context, glanceId)
}

// ── Cycle to next category ─────────────────────────────────────────────────────
class AdhkarCycleCategoryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { s ->
            val current = s[WIDGET_CAT_ID_KEY] ?: 1
            val idx = CATEGORY_ORDER.indexOf(current)
            s[WIDGET_CAT_ID_KEY]  = CATEGORY_ORDER[(idx + 1) % CATEGORY_ORDER.size]
            s[WIDGET_ITEM_IDX_KEY] = 0
        }
        bumpTrigger(context, glanceId)
    }
}

// ── Increment current item ─────────────────────────────────────────────────────
class AdhkarIncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { s ->
            val catId   = s[WIDGET_CAT_ID_KEY] ?: 1
            val itemIdx = s[WIDGET_ITEM_IDX_KEY] ?: 0
            val prefs   = PreferencesManager(context)
            val cat     = AdhkarData.categories().find { it.id == catId } ?: return@updateAppWidgetState
            val item    = cat.items.getOrNull(itemIdx) ?: return@updateAppWidgetState

            val currentCount = prefs.getAdhkarCount(catId, item.id)
            if (currentCount < item.target) {
                val newCount = currentCount + 1
                prefs.saveAdhkarCount(catId, item.id, newCount)
                // Auto-advance when target reached
                if (newCount >= item.target && itemIdx < cat.items.size - 1) {
                    s[WIDGET_ITEM_IDX_KEY] = itemIdx + 1
                }
            }
        }
        bumpTrigger(context, glanceId)
    }
}

// ── Previous / Next item ───────────────────────────────────────────────────────
class AdhkarPrevItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { s ->
            val catId = s[WIDGET_CAT_ID_KEY] ?: 1
            val size  = AdhkarData.categories().find { it.id == catId }?.items?.size ?: 1
            val cur   = s[WIDGET_ITEM_IDX_KEY] ?: 0
            s[WIDGET_ITEM_IDX_KEY] = if (cur - 1 < 0) size - 1 else cur - 1
        }
        bumpTrigger(context, glanceId)
    }
}

class AdhkarNextItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { s ->
            val catId = s[WIDGET_CAT_ID_KEY] ?: 1
            val size  = AdhkarData.categories().find { it.id == catId }?.items?.size ?: 1
            val cur   = s[WIDGET_ITEM_IDX_KEY] ?: 0
            s[WIDGET_ITEM_IDX_KEY] = (cur + 1) % size
        }
        bumpTrigger(context, glanceId)
    }
}

// ── Reset all items in current category for a new cycle ───────────────────────
class AdhkarResetCycleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { s ->
            val catId = s[WIDGET_CAT_ID_KEY] ?: 1
            val prefs = PreferencesManager(context)
            val cat   = AdhkarData.categories().find { it.id == catId } ?: return@updateAppWidgetState
            cat.items.forEach { item -> prefs.saveAdhkarCount(catId, item.id, 0) }
            s[WIDGET_ITEM_IDX_KEY] = 0
        }
        bumpTrigger(context, glanceId)
    }
}

// ── Receiver ──────────────────────────────────────────────────────────────────
class AdhkarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AdhkarWidget
}
