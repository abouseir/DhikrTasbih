package com.example.dhikrtasbih

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dhikrtasbih_prefs", Context.MODE_PRIVATE)
    private val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)

    // ── Daily reset
    fun isNewDay(): Boolean {
        val today = fmt.format(Date())
        val lastDate = prefs.getString("last_opened_date", "")
        if (today != lastDate) {
            prefs.edit().putString("last_opened_date", today).apply()
            return true
        }
        return false
    }

    // ── Adhkar counts
    fun getAdhkarCount(categoryId: Int, itemId: Int) = prefs.getInt("adhkar_${categoryId}_${itemId}", 0)
    fun saveAdhkarCount(categoryId: Int, itemId: Int, count: Int) {
        prefs.edit().putInt("adhkar_${categoryId}_${itemId}", count).apply()
    }

    // ── Haptic
    fun saveHaptic(enabled: Boolean) { prefs.edit().putBoolean("haptic_enabled", enabled).apply() }
    fun getHaptic() = prefs.getBoolean("haptic_enabled", true)

    // ── Font scale
    fun saveFontSizeMultiplier(multiplier: Float) { prefs.edit().putFloat("font_scale", multiplier).apply() }
    fun getFontSizeMultiplier() = prefs.getFloat("font_scale", 1.0f)

    // ── Tasbih list serialization
    fun saveTasbihList(list: List<DhikrItem>) {
        val serialized = list.joinToString("||") { "${it.id}|${it.textAr.replace("|", "")}|${it.count}|${it.target}" }
        prefs.edit().putString("tasbih_list", serialized).apply()
    }
    fun getTasbihList(): List<DhikrItem>? {
        val st = prefs.getString("tasbih_list", null) ?: return null
        if (st.isEmpty()) return emptyList()
        return st.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) DhikrItem(parts[0].toInt(), parts[1], parts[2].toInt(), parts[3].toInt()) else null
        }
    }

    // ── Reset all
    fun resetAllAdhkar() {
        val editor = prefs.edit()
        prefs.all.keys.forEach { if (it.startsWith("adhkar_")) editor.remove(it) }
        editor.apply()
    }

    // ── Widget State
    fun saveWidgetIndex(index: Int) { prefs.edit().putInt("widget_index", index).apply() }
    fun getWidgetIndex() = prefs.getInt("widget_index", 0)

    // ══════════════════════════════════════════
    // EPIC 2 — STREAK TRACKING
    // ══════════════════════════════════════════

    /** Record today as a completed Adhkar day */
    fun recordCompletionDay() {
        val today = fmt.format(Date())
        val existing = prefs.getStringSet("completion_days", mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet().apply { add(today) }
        prefs.edit().putStringSet("completion_days", updated).apply()
    }

    /** Returns the Set of all completion date strings (YYYYMMDD) */
    fun getCompletionDates(): Set<String> = prefs.getStringSet("completion_days", emptySet()) ?: emptySet()

    /** Current daily consecutive streak (counting from yesterday back) */
    fun getStreak(): Int {
        val dates = getCompletionDates()
        val cal = Calendar.getInstance()
        var streak = 0
        // Check yesterday first (today's session might not be done yet)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        while (true) {
            val key = fmt.format(cal.time)
            if (dates.contains(key)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        // Also count today if already completed
        val today = fmt.format(Date())
        if (dates.contains(today)) streak++
        return streak
    }

    /** All-time total days completed */
    fun getTotalCompletedDays(): Int = getCompletionDates().size

    // ══════════════════════════════════════════
    // EPIC 3 — NOTIFICATION TOGGLES
    // ══════════════════════════════════════════

    fun saveMorningNotification(enabled: Boolean) { prefs.edit().putBoolean("notif_morning", enabled).apply() }
    fun getMorningNotification() = prefs.getBoolean("notif_morning", false)

    fun saveEveningNotification(enabled: Boolean) { prefs.edit().putBoolean("notif_evening", enabled).apply() }
    fun getEveningNotification() = prefs.getBoolean("notif_evening", false)

    // ── Keep screen on
    fun saveKeepScreenOn(enabled: Boolean) { prefs.edit().putBoolean("keep_screen_on", enabled).apply() }
    fun getKeepScreenOn() = prefs.getBoolean("keep_screen_on", true)

    // ── Sound
    fun saveSoundEnabled(enabled: Boolean) { prefs.edit().putBoolean("sound_enabled", enabled).apply() }
    fun getSoundEnabled() = prefs.getBoolean("sound_enabled", true)
}
