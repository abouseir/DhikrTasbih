package com.example.dhikrtasbih // Vérifie bien que c'est le bon nom de package

import android.content.Context
import android.view.WindowManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.glance.appwidget.updateAll

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

// ==========================================
// 1. THÈME, POLICES & COULEURS PREMIUM
// ==========================================

val MeQuranFont = FontFamily(Font(R.font.elmessiri, FontWeight.Normal))
val DuaQuranFont = FontFamily(Font(R.font.alfont_tahadath_regular, FontWeight.Normal))
val NumberFont = FontFamily(Font(R.font.montserrat_light, FontWeight.Normal))

object PremiumTheme {
    // ── Ghost Aesthetic Palette ──
    val Background = Color(0xFF000000)          // Pure Black
    val TextMain = Color(0xFFFFFFFF)            // Pure White
    val TextSecondary = Color(0xFF888888)       // Muted Gray
    val CardBackground = Color.Transparent      // Ghost Transparent
    val DialogBackground = Color(0xFF111111)    // Subtle fill for sheets/dialogs
    val CardBorder = Color(0xFF222222)          // Thin Subtle Gray
    val GoldBorderStrong = Color(0x33D4AF37)    // 20% Gold
    val InnerCard = Color(0xFF111111)           // Subtle fill

    val AccentGold = Color(0xFFD4AF37)          // Desaturated Gold
    val AccentGoldSoft = Color(0xFFE5B869)      // Lighter Gold
    val SuccessGreen = Color(0xFF10B981)        // Emerald
    val HighlightPulse = Color(0xFFFCD34D)      // Pulse Gold
    val ListSelectedText = Color(0xFFD4AF37)
    val DangerRed = Color(0xFFEF4444)           // Red

    val SurfaceGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(Color(0xFF111111), Color(0xFF000000))
    )
    val HeaderGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(Color(0xFF161616), Color(0xFF000000))
    )
}

@OptIn(DelicateCoroutinesApi::class)
fun updateWidget(context: Context) {
    GlobalScope.launch {
        try {
            val appWidgetManager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            val glanceIds = appWidgetManager.getGlanceIds(TasbihWidget::class.java)
            glanceIds.forEach { id ->
                androidx.glance.appwidget.state.updateAppWidgetState(context, id) { prefs ->
                    val key = androidx.datastore.preferences.core.intPreferencesKey("trigger")
                    prefs[key] = (prefs[key] ?: 0) + 1
                }
                TasbihWidget.update(context, id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// TON COMPOSANT MAGIQUE POUR LA BARRE RTL
@Composable
fun RtlLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = PremiumTheme.AccentGold,
    trackColor: Color = PremiumTheme.InnerCard
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(modifier = modifier.background(trackColor, CircleShape)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clampedProgress)
                .align(Alignment.CenterEnd)
                .background(color, CircleShape)
        )
    }
}

private fun formatArabicDisplayText(text: String): String {
    val normalized = text.replace("...", "…")
    return BidiFormatter.getInstance(true)
        .unicodeWrap(normalized, TextDirectionHeuristicsCompat.RTL)
}

data class DhikrItem(
    val id: Int,
    val textAr: String,
    var count: Int = 0,
    var target: Int = 0,
    val virtue: String = "",    // Authentic Hadith reference — display only, not persisted
    val reference: String = ""  // Source (e.g. "صحيح البخاري")
)

data class AdhkarCategory(
    val id: Int,
    val titleAr: String,
    val iconVector: ImageVector? = null,
    val iconRes: Int? = null,
    val items: androidx.compose.runtime.snapshots.SnapshotStateList<DhikrItem>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        installSplashScreen()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        
        setContent {
            MaterialTheme { MainAppHost() }
        }
    }
}

fun vibrateDevice(context: Context, durationMs: Long = 30L) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)
            ?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)
            ?.vibrate(durationMs)
    }
}

// ==========================================
// 2. LE CŒUR DE L'APP AVEC NAVIGATION
// ==========================================
@Composable
fun MainAppHost() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var currentRoute by remember { mutableStateOf("home") }

    val isNewDay = remember { prefs.isNewDay() }
    if (isNewDay) {
        prefs.resetAllAdhkar()
    }

    val tasbihList = remember { 
        mutableStateListOf<DhikrItem>().apply { 
            val saved = prefs.getTasbihList()
            if (saved.isNullOrEmpty()) {
                val defaults = AdhkarData.tasbihItems()
                addAll(defaults)
                prefs.saveTasbihList(defaults)
                updateWidget(context)
            } else {
                addAll(saved)
            }
        } 
    }

    val adhkarCategories = remember { 
        mutableStateListOf<AdhkarCategory>().apply { 
            val cats = AdhkarData.categories()
            cats.forEach { cat ->
                cat.items.forEach { item ->
                    item.count = prefs.getAdhkarCount(cat.id, item.id)
                }
            }
            addAll(cats)
        } 
    }

    Scaffold(
        containerColor = PremiumTheme.Background,
        bottomBar = {
            NavigationBar(
                containerColor = PremiumTheme.Background,
                contentColor = PremiumTheme.AccentGold,
                modifier = Modifier.border(width = 0.5.dp, color = PremiumTheme.CardBorder, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                NavigationBarItem(
                    icon = { Icon(if (currentRoute == "home") Icons.Rounded.Home else Icons.Outlined.Home, contentDescription = "Home") }, label = { Text("الرئيسية", fontFamily = MeQuranFont) },
                    selected = currentRoute == "home", onClick = { currentRoute = "home" }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PremiumTheme.AccentGold, unselectedIconColor = PremiumTheme.TextSecondary, selectedTextColor = PremiumTheme.AccentGold, unselectedTextColor = PremiumTheme.TextSecondary, indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(ImageVector.vectorResource(id = R.drawable.ic_tasbih), contentDescription = "Tasbih", modifier = Modifier.size(24.dp)) }, label = { Text("السبحة", fontFamily = MeQuranFont) },
                    selected = currentRoute.startsWith("tasbih"), onClick = { currentRoute = "tasbih" }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PremiumTheme.AccentGold, unselectedIconColor = PremiumTheme.TextSecondary, selectedTextColor = PremiumTheme.AccentGold, unselectedTextColor = PremiumTheme.TextSecondary, indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(ImageVector.vectorResource(id = R.drawable.ic_dua), contentDescription = "Adhkar", modifier = Modifier.size(24.dp)) }, label = { Text("الأذكار", fontFamily = MeQuranFont) },
                    selected = currentRoute.startsWith("adhkar"), onClick = { currentRoute = "adhkar" }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PremiumTheme.AccentGold, unselectedIconColor = PremiumTheme.TextSecondary, selectedTextColor = PremiumTheme.AccentGold, unselectedTextColor = PremiumTheme.TextSecondary, indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(if (currentRoute == "settings") Icons.Rounded.Settings else Icons.Outlined.Settings, contentDescription = "Settings") }, label = { Text("الإعدادات", fontFamily = MeQuranFont) },
                    selected = currentRoute == "settings", onClick = { currentRoute = "settings" }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PremiumTheme.AccentGold, unselectedIconColor = PremiumTheme.TextSecondary, selectedTextColor = PremiumTheme.AccentGold, unselectedTextColor = PremiumTheme.TextSecondary, indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(if (currentRoute == "analytics") Icons.Rounded.BarChart else Icons.Outlined.BarChart, contentDescription = "Analytics") }, label = { Text("إحصائيات", fontFamily = MeQuranFont) },
                    selected = currentRoute == "analytics", onClick = { currentRoute = "analytics" }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PremiumTheme.AccentGold, unselectedIconColor = PremiumTheme.TextSecondary, selectedTextColor = PremiumTheme.AccentGold, unselectedTextColor = PremiumTheme.TextSecondary, indicatorColor = Color.Transparent)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            androidx.compose.animation.Crossfade(targetState = currentRoute, animationSpec = androidx.compose.animation.core.tween(300), label = "ScreenTransition") { route ->
                when {
                    route == "home" -> HomeScreen(categories = adhkarCategories, tasbihList = tasbihList, prefs = prefs, onNavigateToTasbih = { id -> currentRoute = if (id != null) "tasbih/$id" else "tasbih" }, onNavigateToAdhkar = { catId -> currentRoute = "adhkar/$catId" }, onNavigateToCategoryMenu = { currentRoute = "adhkar" })
                    route.startsWith("tasbih") -> {
                        val id = route.substringAfter("tasbih/", "").toIntOrNull()
                        TasbihScreen(dhikrList = tasbihList, prefs = prefs, initialDhikrId = id)
                    }
                    route == "adhkar" -> AdhkarCategoryMenuScreen(categories = adhkarCategories, onNavigateToCategory = { catId -> currentRoute = "adhkar/$catId" })
                    route.startsWith("adhkar/") -> {
                        val catId = route.substringAfter("adhkar/").toIntOrNull()
                        val selectedCategory = adhkarCategories.find { it.id == catId }
                        if (selectedCategory != null) {
                            AdhkarDetailScreen(category = selectedCategory, onBack = { currentRoute = "adhkar" }, prefs = prefs)
                        }
                    }
                    route == "settings" -> SettingsScreen(prefs = prefs, adhkarCategories = adhkarCategories, tasbihList = tasbihList)
                    route == "analytics" -> AnalyticsScreen(prefs = prefs)
                }
            }
        }
    }
}

// ==========================================
// 4. ÉCRAN D'ACCUEIL
// ==========================================
@Composable
fun HomeScreen(categories: List<AdhkarCategory>, tasbihList: androidx.compose.runtime.snapshots.SnapshotStateList<DhikrItem>, prefs: PreferencesManager, onNavigateToTasbih: (Int?) -> Unit, onNavigateToAdhkar: (Int) -> Unit, onNavigateToCategoryMenu: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val gregorianDate = remember { java.text.SimpleDateFormat("EEEE، d MMMM yyyy", java.util.Locale("ar")).format(java.util.Date()) }
    val hijriDate = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val uLocale = android.icu.util.ULocale("ar@calendar=islamic-umalqura")
            val icl = android.icu.util.Calendar.getInstance(uLocale)
            val sdf = android.icu.text.SimpleDateFormat("d MMMM yyyy 'هـ'", uLocale)
            sdf.format(icl.time)
        } else "يوم جديد"
    }

    val totalAdhkarCount = categories.sumOf { it.items.size }
    val completedAdhkarCount = categories.sumOf { cat -> cat.items.count { it.count >= it.target && it.target > 0 } }
    val progressPercent = if (totalAdhkarCount > 0) ((completedAdhkarCount.toFloat() / totalAdhkarCount.toFloat()) * 100).toInt() else 0

    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when (currentHour) {
            in 5..11 -> "صباح الخير"
            in 12..17 -> "مساء الخير"
            else -> "ليلة طيبة"
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 16.dp)) {
        // ═══ HEADER ═══
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(PremiumTheme.HeaderGradient)
                .border(0.5.dp, PremiumTheme.GoldBorderStrong, RoundedCornerShape(28.dp)).padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = PremiumTheme.AccentGold.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(greeting, color = PremiumTheme.TextSecondary, fontSize = 11.sp, fontFamily = MeQuranFont, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(hijriDate, color = PremiumTheme.AccentGold, fontSize = 14.sp, fontFamily = MeQuranFont)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(gregorianDate, color = PremiumTheme.TextMain, fontSize = 12.sp, fontFamily = MeQuranFont)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // ═══ STATS ═══
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), title = "الأقسام", value = categories.size.toString(), icon = Icons.Outlined.AutoStories)
            StatCard(modifier = Modifier.weight(1f), title = "المكتمل", value = completedAdhkarCount.toString(), icon = Icons.Outlined.CheckCircle)
            StatCard(modifier = Modifier.weight(1f), title = "التقدم", value = "$progressPercent%", icon = Icons.Outlined.Insights)
        }

        Spacer(modifier = Modifier.height(28.dp))
        // ═══ SECTION LABEL ═══
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = PremiumTheme.CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("الوصول السريع", color = PremiumTheme.TextSecondary, fontSize = 11.sp, fontFamily = MeQuranFont, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickShortcutCard(modifier = Modifier.weight(1f), title = "أذكار المساء", icon = Icons.Outlined.NightsStay, onClick = { onNavigateToAdhkar(2) })
                QuickShortcutCard(modifier = Modifier.weight(1f), title = "أذكار الصباح", icon = Icons.Outlined.WbSunny, onClick = { onNavigateToAdhkar(1) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickShortcutCard(modifier = Modifier.weight(1f), title = "أذكار النوم", icon = Icons.Outlined.Bedtime, onClick = { onNavigateToAdhkar(3) })
                QuickShortcutCard(modifier = Modifier.weight(1f), title = "تسبيح مفتوح", icon = ImageVector.vectorResource(id = R.drawable.ic_tasbih), onClick = { onNavigateToTasbih(null) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickShortcutCard(modifier = Modifier.weight(1f), title = "قائمة الأذكار", icon = ImageVector.vectorResource(id = R.drawable.ic_dua), onClick = { onNavigateToCategoryMenu() })
                QuickShortcutCard(modifier = Modifier.weight(1f), title = "أذكار بعد الصلاة", icon = Icons.Outlined.Mosque, onClick = { onNavigateToAdhkar(5) })
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        // ═══ SECTION LABEL ═══
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = PremiumTheme.CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("التسبيح اليومي", color = PremiumTheme.TextSecondary, fontSize = 11.sp, fontFamily = MeQuranFont, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        val recommendedIds = listOf(10, 6, 5, 9, 14)
        val haptic = LocalHapticFeedback.current
        val isHapticEnabled = prefs.getHaptic()
        
        recommendedIds.forEach { id ->
            val index = tasbihList.indexOfFirst { it.id == id }
            if (index != -1) {
                val dhikr = tasbihList[index]
                TasbihListCard(
                    dhikr = dhikr,
                    onIncrement = {
                        val newCount = dhikr.count + 1
                        val updatedDhikr = dhikr.copy(count = newCount)
                        tasbihList[index] = updatedDhikr
                        prefs.saveTasbihList(tasbihList)
                        updateWidget(context)
                        
                        if (isHapticEnabled) {
                            if (dhikr.target > 0 && newCount == dhikr.target) {
                                vibrateDevice(context, 60L)
                            } else {
                                vibrateDevice(context, 30L)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector) {
    Row(
        modifier = modifier.height(90.dp).clip(RoundedCornerShape(18.dp)).background(PremiumTheme.SurfaceGradient)
            .border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(18.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gold accent left bar
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(PremiumTheme.AccentGold.copy(alpha=0.8f), PremiumTheme.AccentGold.copy(alpha=0.1f))
            )
        ))
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = PremiumTheme.TextMain, fontSize = 20.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold)
            Text(title, color = PremiumTheme.TextSecondary, fontSize = 10.sp, fontFamily = MeQuranFont)
        }
    }
}

@Composable
fun QuickShortcutCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = 500f),
        label = "scale"
    )
    Box(
        modifier = modifier.height(115.dp).graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(22.dp)).background(PremiumTheme.SurfaceGradient)
            .border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(22.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                pressed = true; onClick(); pressed = false
            }.padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(44.dp).background(PremiumTheme.AccentGold.copy(alpha=0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, color = PremiumTheme.TextMain, fontSize = 12.sp, fontFamily = MeQuranFont, textAlign = TextAlign.Center)
        }
    }
}

// ==========================================
// 5. ÉCRAN MENU DES CATÉGORIES ADHKAR
// ==========================================
@Composable
fun AdhkarCategoryMenuScreen(categories: List<AdhkarCategory>, onNavigateToCategory: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.HeaderGradient)
                .border(0.5.dp, PremiumTheme.GoldBorderStrong, RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("الأذكار", color = PremiumTheme.TextMain, fontSize = 23.sp, fontFamily = MeQuranFont)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("اختر القسم", color = PremiumTheme.TextSecondary, fontSize = 13.sp, fontFamily = MeQuranFont, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                AdhkarCategoryCard(category = category, onClick = { onNavigateToCategory(category.id) })
            }
        }
    }
}

@Composable
fun AdhkarCategoryCard(category: AdhkarCategory, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = 500f),
        label = "scale"
    )

    val totalTarget = category.items.sumOf { it.target }
    val totalCount = category.items.sumOf { it.count }
    val progressFraction = if (totalTarget > 0) (totalCount.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f) else 0f
    val progressPercent = (progressFraction * 100).toInt()
    val isCompleted = totalTarget > 0 && totalCount >= totalTarget

    Box(
        modifier = Modifier.defaultMinSize(minHeight = 180.dp).graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient)
            .border(0.5.dp, if (isCompleted) PremiumTheme.SuccessGreen.copy(alpha=0.5f) else PremiumTheme.CardBorder, RoundedCornerShape(24.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                pressed = true; onClick(); pressed = false
            }.padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(PremiumTheme.AccentGold.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                if (category.iconVector != null) {
                    Icon(category.iconVector, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(30.dp))
                } else if (category.iconRes != null) {
                    Icon(ImageVector.vectorResource(id = category.iconRes), contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = category.titleAr, color = PremiumTheme.TextMain, fontSize = 18.sp, fontFamily = MeQuranFont, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$progressPercent%",
                    color = if (isCompleted) PremiumTheme.SuccessGreen else PremiumTheme.TextSecondary,
                    fontSize = 14.sp,
                    fontFamily = NumberFont,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 52.dp).wrapContentWidth(Alignment.CenterHorizontally)
                )

                RtlLinearProgressBar(
                    progress = progressFraction,
                    color = if (isCompleted) PremiumTheme.SuccessGreen else PremiumTheme.AccentGold,
                    modifier = Modifier.weight(1f).height(4.dp)
                )
            }
        }
    }
}

// ==========================================
// 6. ÉCRAN DÉTAILLÉ D'UNE ROUTINE
// ==========================================
@Composable
fun AdhkarDetailScreen(category: AdhkarCategory, onBack: () -> Unit, prefs: PreferencesManager) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isHapticEnabled = remember { prefs.getHaptic() }
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { category.items.size })

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White) }
            Text(category.titleAr, color = Color.White, fontSize = 23.sp, fontFamily = MeQuranFont, modifier = Modifier.weight(1f).padding(end = 48.dp), textAlign = TextAlign.Center)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true
        ) { page ->
            val currentItem = category.items[page]
            val progress = if (currentItem.target > 0) (currentItem.count.toFloat() / currentItem.target.toFloat()).coerceIn(0f, 1f) else 0f

            val textLength = currentItem.textAr.length
            val dynamicFontSize = when {
                textLength > 400 -> 24.sp
                textLength > 250 -> 28.sp
                textLength > 150 -> 32.sp
                textLength > 80 -> 38.sp
                else -> 48.sp
            }
            val dynamicLineHeight = when {
                textLength > 400 -> 46.sp
                textLength > 250 -> 52.sp
                textLength > 150 -> 58.sp
                textLength > 80 -> 68.sp
                else -> 84.sp
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 48.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(PremiumTheme.SurfaceGradient)
                    .border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(32.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (currentItem.count < currentItem.target) {
                            category.items[page] = currentItem.copy(count = currentItem.count + 1)
                            prefs.saveAdhkarCount(category.id, currentItem.id, category.items[page].count)

                            if (isHapticEnabled) {
                                vibrateDevice(context, if (category.items[page].count == currentItem.target) 60L else 30L)
                            }

                            if (category.items[page].count == currentItem.target) {
                                coroutineScope.launch {
                                    delay(400)
                                    if (pagerState.currentPage < category.items.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            }
                        }
                    }
                    .padding(28.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(PremiumTheme.InnerCard).padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("${page + 1} / ${category.items.size}", color = PremiumTheme.TextSecondary, fontSize = 16.sp, fontFamily = NumberFont)
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = formatArabicDisplayText(currentItem.textAr),
                            color = PremiumTheme.TextMain,
                            fontSize = dynamicFontSize,
                            fontFamily = DuaQuranFont,
                            textAlign = TextAlign.Right,
                            lineHeight = dynamicLineHeight,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (currentItem.target > 0 && currentItem.count >= currentItem.target) {
                            Box(
                                modifier = Modifier
                                    .background(PremiumTheme.SuccessGreen.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, PremiumTheme.SuccessGreen.copy(alpha=0.5f), CircleShape)
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("تم الانتهاء", color = PremiumTheme.SuccessGreen, fontSize = 13.sp, fontFamily = MeQuranFont, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, PremiumTheme.CardBorder, CircleShape)
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${currentItem.count} / ${currentItem.target}",
                                    color = PremiumTheme.TextSecondary,
                                    fontSize = 16.sp,
                                    fontFamily = NumberFont,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        RtlLinearProgressBar(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(0.85f).height(4.dp)
                        )

                        // ═══ FADAIL CARD ═══
                        if (currentItem.virtue.isNotBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            FadailCard(virtue = currentItem.virtue, reference = currentItem.reference)
                        }

                        // ═══ NEW CYCLE BUTTON — shown when ALL items are done ═══
                        val allDone = category.items.all { it.target > 0 && it.count >= it.target }
                        if (allDone) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(PremiumTheme.AccentGold.copy(alpha = 0.15f))
                                    .border(1.dp, PremiumTheme.AccentGold.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        category.items.forEachIndexed { idx, item ->
                                            category.items[idx] = item.copy(count = 0)
                                            prefs.saveAdhkarCount(category.id, item.id, 0)
                                        }
                                        coroutineScope.launch { pagerState.scrollToPage(0) }
                                    }
                                    .padding(horizontal = 28.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "🔄 بدء دورة جديدة",
                                    color = PremiumTheme.AccentGold,
                                    fontSize = 15.sp,
                                    fontFamily = MeQuranFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FadailCard(virtue: String, reference: String) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(PremiumTheme.AccentGold.copy(alpha = 0.12f), PremiumTheme.AccentGold.copy(alpha = 0.04f))
                )
            )
            .border(0.5.dp, PremiumTheme.AccentGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .animateContentSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = PremiumTheme.AccentGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "فضل هذا الذكر",
                    color = PremiumTheme.AccentGold,
                    fontSize = 11.sp,
                    fontFamily = MeQuranFont
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PremiumTheme.AccentGold.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    virtue,
                    color = PremiumTheme.TextMain,
                    fontSize = 18.sp,
                    fontFamily = DuaQuranFont,
                    textAlign = TextAlign.Center,
                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                    lineHeight = 36.sp
                )
                if (reference.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "— $reference",
                        color = PremiumTheme.TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = MeQuranFont,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


// ==========================================
// 7. ÉCRAN TASBIH
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(dhikrList: androidx.compose.runtime.snapshots.SnapshotStateList<DhikrItem>, prefs: PreferencesManager, initialDhikrId: Int? = null) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var selectedDhikr by remember(initialDhikrId) { mutableStateOf(dhikrList.find { it.id == initialDhikrId } ?: dhikrList.firstOrNull() ?: DhikrItem(0, "تسبيح", 0, 0)) }
    var isHapticEnabled by remember { mutableStateOf(prefs.getHaptic()) }
    var isSoundEnabled by remember { mutableStateOf(prefs.getSoundEnabled()) }
    var showBottomSheet by remember { mutableStateOf(false) }

    var showAddDhikrScreen by remember { mutableStateOf(false) }
    var newDhikrNameAr by remember { mutableStateOf("") }
    var newDhikrTarget by remember { mutableStateOf("0") }

    var showTargetDialog by remember { mutableStateOf(false) }
    var tempTargetInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<DhikrItem?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val view = androidx.compose.ui.platform.LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    // Keep screen on
    val keepScreenOn = remember { mutableStateOf(prefs.getKeepScreenOn()) }
    DisposableEffect(keepScreenOn.value) {
        if (keepScreenOn.value) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulsingGold by infiniteTransition.animateColor(
        initialValue = PremiumTheme.AccentGold, targetValue = PremiumTheme.HighlightPulse,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse"
    )

    if (!showAddDhikrScreen) {
        Column(
            modifier = Modifier.fillMaxSize().background(PremiumTheme.Background).clickable(interactionSource = interactionSource, indication = null) {
                coroutineScope.launch {
                    scale.animateTo(0.95f, animationSpec = tween(50))
                    scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                if (isSoundEnabled) view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                if (selectedDhikr.target > 0) {
                    if (selectedDhikr.count >= selectedDhikr.target) {
                        selectedDhikr = selectedDhikr.copy(count = 1)
                        if (isHapticEnabled) vibrateDevice(context, 30L)
                    } else {
                        selectedDhikr = selectedDhikr.copy(count = selectedDhikr.count + 1)
                        if (selectedDhikr.count == selectedDhikr.target) {
                            if (isHapticEnabled) vibrateDevice(context, 60L)
                        } else {
                            if (isHapticEnabled) vibrateDevice(context, 30L)
                        }
                    }
                } else {
                    selectedDhikr = selectedDhikr.copy(count = selectedDhikr.count + 1)
                    if (isHapticEnabled) vibrateDevice(context, 30L)
                }
                val index = dhikrList.indexOfFirst { it.id == selectedDhikr.id }
                if (index != -1) { 
                    dhikrList[index] = selectedDhikr 
                    prefs.saveTasbihList(dhikrList)
                    updateWidget(context)
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), horizontalArrangement = Arrangement.End) {
                Row {
                    IconButton(onClick = { tempTargetInput = if (selectedDhikr.target > 0) selectedDhikr.target.toString() else ""; showTargetDialog = true }) { Icon(Icons.Outlined.Settings, contentDescription = "Paramètres", tint = PremiumTheme.TextSecondary) }
                    IconButton(onClick = { 
                        isHapticEnabled = !isHapticEnabled
                        prefs.saveHaptic(isHapticEnabled)
                    }) { Icon(if (isHapticEnabled) Icons.Rounded.Vibration else Icons.Outlined.Vibration, contentDescription = "Vibration", tint = if (isHapticEnabled) PremiumTheme.AccentGold else PremiumTheme.TextSecondary) }
                    IconButton(onClick = {
                        selectedDhikr = selectedDhikr.copy(count = 0)
                        val index = dhikrList.indexOfFirst { it.id == selectedDhikr.id }
                        if (index != -1) { 
                            dhikrList[index] = selectedDhikr 
                            prefs.saveTasbihList(dhikrList)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) { Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = PremiumTheme.TextSecondary) }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().graphicsLayer { scaleX = scale.value; scaleY = scale.value }, contentAlignment = Alignment.Center) {
                val progress = if (selectedDhikr.target > 0) (selectedDhikr.count.toFloat() / selectedDhikr.target.toFloat()).coerceIn(0f, 1f) else 0f
                val isTargetReached = selectedDhikr.target > 0 && selectedDhikr.count >= selectedDhikr.target
                val indicatorColor = if (isTargetReached) pulsingGold else PremiumTheme.AccentGold

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(280.dp).graphicsLayer { scaleX = -1f },
                    color = indicatorColor,
                    strokeWidth = 4.dp,
                    trackColor = PremiumTheme.InnerCard
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = selectedDhikr.count.toString(), color = Color.White, fontSize = 96.sp, fontFamily = NumberFont)
                    if (selectedDhikr.target > 0) {
                        if (isTargetReached) {
                            Text(text = "تم الانتهاء", color = PremiumTheme.SuccessGreen, fontSize = 16.sp, fontFamily = MeQuranFont, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            Text(text = "${selectedDhikr.count} / ${selectedDhikr.target}", color = PremiumTheme.TextSecondary, fontSize = 20.sp, fontFamily = NumberFont, letterSpacing = 2.sp)
                        }
                    }
                }
            }

            val textLength = selectedDhikr.textAr.length
            val dynamicFontSize = if (textLength > 150) 26.sp else if (textLength > 50) 31.sp else 36.sp
            val dynamicLineHeight = if (textLength > 150) 52.sp else if (textLength > 50) 58.sp else 66.sp

            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
                    .background(PremiumTheme.CardBackground, RoundedCornerShape(32.dp))
                    .border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(32.dp)).clickable { showBottomSheet = true }
                    .heightIn(min = 100.dp, max = 160.dp).padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formatArabicDisplayText(selectedDhikr.textAr),
                        color = PremiumTheme.TextMain,
                        fontSize = dynamicFontSize,
                        fontFamily = DuaQuranFont,
                        textAlign = TextAlign.Center,
                        lineHeight = dynamicLineHeight,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(end = 10.dp)
                    )
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Menu", tint = PremiumTheme.TextSecondary, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (showTargetDialog) {
            AlertDialog(
                onDismissRequest = { showTargetDialog = false }, containerColor = PremiumTheme.DialogBackground, shape = RoundedCornerShape(32.dp),
                title = { Text("تحديد الهدف", color = PremiumTheme.TextMain, fontFamily = MeQuranFont, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 20.sp) },
                text = {
                    TextField(
                        value = tempTargetInput, onValueChange = { tempTargetInput = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = PremiumTheme.AccentGold, unfocusedTextColor = Color.White, focusedIndicatorColor = PremiumTheme.AccentGold, unfocusedIndicatorColor = PremiumTheme.TextSecondary, cursorColor = PremiumTheme.AccentGold),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 40.sp, fontFamily = NumberFont),
                        singleLine = true, placeholder = { Text("0", color = PremiumTheme.TextSecondary, fontFamily = NumberFont, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 40.sp) }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newTarget = tempTargetInput.toIntOrNull() ?: 0
                        selectedDhikr = selectedDhikr.copy(target = newTarget)
                        val index = dhikrList.indexOfFirst { it.id == selectedDhikr.id }
                        if (index != -1) { 
                            dhikrList[index] = selectedDhikr 
                            prefs.saveTasbihList(dhikrList)
                            updateWidget(context)
                        }
                        showTargetDialog = false
                    }) { Text("حفظ", color = PremiumTheme.AccentGold, fontFamily = MeQuranFont, fontSize = 18.sp) }
                },
                dismissButton = { TextButton(onClick = { showTargetDialog = false }) { Text("إلغاء", color = PremiumTheme.TextSecondary, fontFamily = MeQuranFont, fontSize = 18.sp) } }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, containerColor = PremiumTheme.DialogBackground) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("اختيار الذكر", color = PremiumTheme.TextMain, fontSize = 20.sp, fontFamily = MeQuranFont, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), textAlign = TextAlign.Center)
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                        items(dhikrList) { dhikr ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedDhikr = dhikr; showBottomSheet = false }.padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Delete button (only for non-default items with id > 17)
                                val defaultIds = AdhkarData.tasbihItems().map { it.id }.toSet()
                                if (!defaultIds.contains(dhikr.id)) {
                                    IconButton(onClick = { showDeleteDialog = dhikr }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = PremiumTheme.DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(32.dp))
                                }
                                val targetDisplay = if (dhikr.target > 0) dhikr.target.toString() else "∞"
                                Text(text = "${dhikr.count} / $targetDisplay", color = PremiumTheme.TextSecondary, fontSize = 14.sp, fontFamily = NumberFont, modifier = Modifier.width(56.dp))
                                Text(text = dhikr.textAr, color = if (dhikr.id == selectedDhikr.id) PremiumTheme.ListSelectedText else PremiumTheme.TextMain, fontSize = 29.sp, fontFamily = DuaQuranFont, textAlign = TextAlign.Right, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), modifier = Modifier.weight(1f).padding(start = 12.dp))
                            }
                            HorizontalDivider(color = PremiumTheme.CardBorder, thickness = 0.5.dp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            showBottomSheet = false
                            newDhikrNameAr = ""
                            newDhikrTarget = "0"
                            showAddDhikrScreen = true
                        }.padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ذكر جديد", color = PremiumTheme.AccentGold, fontSize = 18.sp, fontFamily = MeQuranFont)
                        Icon(Icons.Rounded.Add, contentDescription = "Ajouter", tint = PremiumTheme.AccentGold, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { dhikrToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                containerColor = PremiumTheme.DialogBackground,
                shape = RoundedCornerShape(24.dp),
                title = { Text("حذف الذكر", color = PremiumTheme.TextMain, fontFamily = MeQuranFont, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 20.sp) },
                text = { Text("هل تريد حذف هذا الذكر؟", color = PremiumTheme.TextSecondary, fontFamily = MeQuranFont, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 15.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        dhikrList.removeAll { it.id == dhikrToDelete.id }
                        if (selectedDhikr.id == dhikrToDelete.id) {
                            selectedDhikr = dhikrList.firstOrNull() ?: DhikrItem(0, "تسبيح", 0, 0)
                        }
                        prefs.saveTasbihList(dhikrList)
                        updateWidget(context)
                        showDeleteDialog = null
                    }) { Text("حذف", color = PremiumTheme.DangerRed, fontFamily = MeQuranFont, fontSize = 18.sp) }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("إلغاء", color = PremiumTheme.TextSecondary, fontFamily = MeQuranFont, fontSize = 18.sp) } }
            )
        }
    } else {
        // --- ÉCRAN D'AJOUT ---
        BackHandler { showAddDhikrScreen = false }

        Column(modifier = Modifier.fillMaxSize().background(PremiumTheme.Background).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showAddDhikrScreen = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = PremiumTheme.TextMain) }
                Text("إضافة ذكر", color = PremiumTheme.TextMain, fontSize = 20.sp, fontFamily = MeQuranFont, modifier = Modifier.weight(1f).padding(end = 48.dp), textAlign = TextAlign.Center)
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text("الذكر", color = PremiumTheme.TextSecondary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(end = 12.dp), textAlign = TextAlign.Right, fontFamily = MeQuranFont)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newDhikrNameAr, onValueChange = { newDhikrNameAr = it }, modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 26.sp, fontFamily = DuaQuranFont, textDirection = TextDirection.Rtl),
                    placeholder = { Text("مثال: سُبْحَانَ اللَّهِ", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, color = PremiumTheme.InnerCard, fontFamily = MeQuranFont) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTheme.AccentGold, unfocusedBorderColor = PremiumTheme.CardBorder, focusedTextColor = PremiumTheme.TextMain, unfocusedTextColor = PremiumTheme.TextMain, cursorColor = PremiumTheme.AccentGold),
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text("الهدف (0 = مفتوح)", color = PremiumTheme.TextSecondary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(end = 12.dp), textAlign = TextAlign.Right, fontFamily = MeQuranFont)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newDhikrTarget, onValueChange = { newDhikrTarget = it }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontFamily = NumberFont),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTheme.AccentGold, unfocusedBorderColor = PremiumTheme.CardBorder, focusedTextColor = PremiumTheme.TextMain, unfocusedTextColor = PremiumTheme.TextMain, cursorColor = PremiumTheme.AccentGold),
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val targets = listOf("100", "33", "10", "0")
                    targets.forEach { targetVal ->
                        val isSelected = newDhikrTarget == targetVal
                        Box(
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(50.dp)
                                .clip(RoundedCornerShape(16.dp)).background(if (isSelected) PremiumTheme.AccentGold.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (isSelected) PremiumTheme.AccentGold else PremiumTheme.CardBorder, RoundedCornerShape(16.dp))
                                .clickable { newDhikrTarget = targetVal },
                            contentAlignment = Alignment.Center
                        ) { Text(text = targetVal, color = if (isSelected) PremiumTheme.AccentGold else PremiumTheme.TextSecondary, fontSize = 18.sp, fontFamily = NumberFont) }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (newDhikrNameAr.isNotBlank()) {
                            val newTarget = newDhikrTarget.toIntOrNull() ?: 0
                            val newItem = DhikrItem(id = dhikrList.size + 1, textAr = newDhikrNameAr, target = newTarget)
                            dhikrList.add(newItem)
                            selectedDhikr = newItem
                            prefs.saveTasbihList(dhikrList)
                            updateWidget(context)
                            showAddDhikrScreen = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumTheme.AccentGold), shape = RoundedCornerShape(16.dp)
                ) { Text("حفظ", color = Color.Black, fontSize = 18.sp, fontFamily = MeQuranFont, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// 8. ÉCRAN DES REGLAGES
// ==========================================
@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    adhkarCategories: androidx.compose.runtime.snapshots.SnapshotStateList<AdhkarCategory>,
    tasbihList: androidx.compose.runtime.snapshots.SnapshotStateList<DhikrItem>
) {
    var haptic by remember { mutableStateOf(prefs.getHaptic()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var morningNotif by remember { mutableStateOf(prefs.getMorningNotification()) }
    var eveningNotif by remember { mutableStateOf(prefs.getEveningNotification()) }
    var keepScreenOn by remember { mutableStateOf(prefs.getKeepScreenOn()) }
    var soundEnabled by remember { mutableStateOf(prefs.getSoundEnabled()) }

    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("الإعدادات", color = PremiumTheme.TextMain, fontSize = 26.sp, fontFamily = MeQuranFont)
        }
        Spacer(modifier = Modifier.height(32.dp))

        // ══ Haptic toggle
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient).border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = haptic, onCheckedChange = { haptic = it; prefs.saveHaptic(it) }, colors = SwitchDefaults.colors(checkedThumbColor = PremiumTheme.AccentGold, checkedTrackColor = PremiumTheme.AccentGold.copy(alpha=0.3f), uncheckedThumbColor = PremiumTheme.TextSecondary, uncheckedTrackColor = Color.Transparent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("الاهتزاز عند التسبيح", color = PremiumTheme.TextMain, fontSize = 16.sp, fontFamily = MeQuranFont)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(if (haptic) Icons.Rounded.Vibration else Icons.Outlined.Vibration, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(28.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // ══ Sound toggle
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient).border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it; prefs.saveSoundEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = PremiumTheme.AccentGold, checkedTrackColor = PremiumTheme.AccentGold.copy(alpha=0.3f), uncheckedThumbColor = PremiumTheme.TextSecondary, uncheckedTrackColor = Color.Transparent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تفعيل الصوت", color = PremiumTheme.TextMain, fontSize = 16.sp, fontFamily = MeQuranFont)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ══ Keep screen on toggle
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient).border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it; prefs.saveKeepScreenOn(it) }, colors = SwitchDefaults.colors(checkedThumbColor = PremiumTheme.AccentGold, checkedTrackColor = PremiumTheme.AccentGold.copy(alpha=0.3f), uncheckedThumbColor = PremiumTheme.TextSecondary, uncheckedTrackColor = Color.Transparent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("إبقاء الشاشة مضاءة", color = PremiumTheme.TextMain, fontSize = 16.sp, fontFamily = MeQuranFont)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Outlined.LightMode, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ══ Morning notification toggle
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient).border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = morningNotif, onCheckedChange = { enabled ->
                morningNotif = enabled
                prefs.saveMorningNotification(enabled)
                scheduleAdhkarReminder(context, "morning", enabled, 6, 0)
            }, colors = SwitchDefaults.colors(checkedThumbColor = PremiumTheme.AccentGold, checkedTrackColor = PremiumTheme.AccentGold.copy(alpha=0.3f), uncheckedThumbColor = PremiumTheme.TextSecondary, uncheckedTrackColor = Color.Transparent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تذكير أذكار الصباح 🌅", color = PremiumTheme.TextMain, fontSize = 16.sp, fontFamily = MeQuranFont)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ══ Evening notification toggle
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient).border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = eveningNotif, onCheckedChange = { enabled ->
                eveningNotif = enabled
                prefs.saveEveningNotification(enabled)
                scheduleAdhkarReminder(context, "evening", enabled, 16, 30)
            }, colors = SwitchDefaults.colors(checkedThumbColor = PremiumTheme.AccentGold, checkedTrackColor = PremiumTheme.AccentGold.copy(alpha=0.3f), uncheckedThumbColor = PremiumTheme.TextSecondary, uncheckedTrackColor = Color.Transparent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تذكير أذكار المساء 🌙", color = PremiumTheme.TextMain, fontSize = 16.sp, fontFamily = MeQuranFont)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = PremiumTheme.AccentGold, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ══ Reset all
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient).border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).clickable {
            showResetDialog = true
        }.padding(24.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("تصفير جميع العدادات", color = PremiumTheme.DangerRed, fontSize = 16.sp, fontFamily = MeQuranFont)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = PremiumTheme.DangerRed, modifier = Modifier.size(28.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = PremiumTheme.DialogBackground,
            shape = RoundedCornerShape(32.dp),
            title = { Text("تأكيد التصفير", color = PremiumTheme.TextMain, fontFamily = MeQuranFont, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 20.sp) },
            text = { Text("هل أنت متأكد من تصفير جميع العدادات؟ \nلا يمكن التراجع عن هذا الإجراء.", color = PremiumTheme.TextSecondary, fontFamily = MeQuranFont, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.resetAllAdhkar()
                    adhkarCategories.forEach { cat -> cat.items.forEach { it.count = 0 } }
                    tasbihList.forEach { it.count = 0 }
                    prefs.saveTasbihList(tasbihList)
                    android.widget.Toast.makeText(context, "تم تصفير جميع العدادات", android.widget.Toast.LENGTH_SHORT).show()
                    showResetDialog = false
                }) { Text("تصفير", color = PremiumTheme.DangerRed, fontFamily = MeQuranFont, fontSize = 18.sp) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("إلغاء", color = PremiumTheme.TextSecondary, fontFamily = MeQuranFont, fontSize = 18.sp) } }
        )
    }
}

/** Schedule or cancel a daily WorkManager periodic notification */
fun scheduleAdhkarReminder(context: Context, type: String, enable: Boolean, hour: Int, minute: Int) {
    val workManager = androidx.work.WorkManager.getInstance(context)
    val tag = "adhkar_reminder_$type"
    if (!enable) {
        workManager.cancelAllWorkByTag(tag)
        return
    }
    val now = java.util.Calendar.getInstance()
    val target = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, hour); set(java.util.Calendar.MINUTE, minute); set(java.util.Calendar.SECOND, 0) }
    if (target.before(now)) target.add(java.util.Calendar.DAY_OF_YEAR, 1)
    val initialDelay = target.timeInMillis - now.timeInMillis

    val data = androidx.work.Data.Builder().putString("type", type).build()
    val request = androidx.work.PeriodicWorkRequest.Builder(AdhkarReminderWorker::class.java, 1, java.util.concurrent.TimeUnit.DAYS)
        .setInitialDelay(initialDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
        .setInputData(data)
        .addTag(tag)
        .build()
    workManager.enqueueUniquePeriodicWork(tag, androidx.work.ExistingPeriodicWorkPolicy.REPLACE, request)
}

@Composable
fun TasbihListCard(dhikr: DhikrItem, onIncrement: () -> Unit) {
    val progress = if (dhikr.target > 0) (dhikr.count.toFloat() / dhikr.target.toFloat()).coerceIn(0f, 1f) else 0f
    val isCompleted = dhikr.target > 0 && dhikr.count >= dhikr.target
    
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient).border(0.5.dp, if (isCompleted) PremiumTheme.SuccessGreen.copy(alpha = 0.5f) else PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).clickable { if (!isCompleted) onIncrement() }.padding(20.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                if (isCompleted) {
                    Box(modifier = Modifier.background(PremiumTheme.SuccessGreen.copy(alpha=0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(text = "تم ✓", color = PremiumTheme.SuccessGreen, fontSize = 12.sp, fontFamily = MeQuranFont, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(modifier = Modifier.background(PremiumTheme.AccentGold.copy(alpha=0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(text = if (dhikr.target > 0) "الهدف: ${dhikr.target}" else "مفتوح", color = PremiumTheme.AccentGold, fontSize = 12.sp, fontFamily = MeQuranFont)
                    }
                }
                
                Text(
                    text = dhikr.textAr,
                    color = PremiumTheme.TextMain,
                    fontSize = 23.sp,
                    fontFamily = DuaQuranFont,
                    textAlign = TextAlign.Right,
                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!isCompleted) {
                    Button(
                        onClick = onIncrement,
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTheme.AccentGold),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("+1", color = Color.Black, fontSize = 18.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier.height(44.dp).background(PremiumTheme.SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("تم الانتهاء", color = PremiumTheme.SuccessGreen, fontSize = 13.sp, fontFamily = MeQuranFont, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    val targetText = if (dhikr.target > 0) dhikr.target.toString() else "∞"
                    Text("${dhikr.count} / $targetText", color = PremiumTheme.TextSecondary, fontSize = 14.sp, fontFamily = NumberFont, modifier = Modifier.padding(bottom = 8.dp))
                    RtlLinearProgressBar(progress = progress, color = if (isCompleted) PremiumTheme.SuccessGreen else PremiumTheme.AccentGold, modifier = Modifier.fillMaxWidth().height(4.dp))
                }
            }
        }
    }
}

// ==========================================
// 9. ANALYTICS SCREEN (EPIC 2)
// ==========================================
@Composable
fun AnalyticsScreen(prefs: PreferencesManager) {
    val streak = remember { prefs.getStreak() }
    val totalDays = remember { prefs.getTotalCompletedDays() }
    val completedDates = remember { prefs.getCompletionDates() }
    val fmt = remember { java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US) }
    val scrollState = rememberScrollState()

    // Build list of last 35 days (oldest → newest)
    val days = remember {
        val list = mutableListOf<Pair<String, Boolean>>() // (label, isCompleted)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -34)
        repeat(35) {
            val key = fmt.format(cal.time)
            val dayLabel = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
            list.add(dayLabel to completedDates.contains(key))
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 16.dp)) {

        // Header
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(PremiumTheme.HeaderGradient)
                .border(0.5.dp, PremiumTheme.GoldBorderStrong, RoundedCornerShape(28.dp)).padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("إحصائياتي", color = PremiumTheme.TextMain, fontSize = 23.sp, fontFamily = MeQuranFont)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Streak cards row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Current streak
            Box(
                modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(22.dp)).background(PremiumTheme.SurfaceGradient)
                    .border(0.5.dp, PremiumTheme.AccentGold.copy(alpha=0.4f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(streak.toString(), color = PremiumTheme.AccentGold, fontSize = 28.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold)
                    Text("السلسلة الحالية", color = PremiumTheme.TextSecondary, fontSize = 10.sp, fontFamily = MeQuranFont)
                }
            }

            // Total days
            Box(
                modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(22.dp)).background(PremiumTheme.SurfaceGradient)
                    .border(0.5.dp, PremiumTheme.SuccessGreen.copy(alpha=0.4f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = PremiumTheme.SuccessGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(totalDays.toString(), color = PremiumTheme.SuccessGreen, fontSize = 28.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold)
                    Text("إجمالي الأيام", color = PremiumTheme.TextSecondary, fontSize = 10.sp, fontFamily = MeQuranFont)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section label
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = PremiumTheme.CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("سجل الـ 35 يومًا الماضية", color = PremiumTheme.TextSecondary, fontSize = 11.sp, fontFamily = MeQuranFont, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Heatmap — 7 columns × 5 rows
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PremiumTheme.SurfaceGradient)
                .border(0.5.dp, PremiumTheme.CardBorder, RoundedCornerShape(24.dp)).padding(20.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                modifier = Modifier.heightIn(max = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days) { (label, completed) ->
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(
                            if (completed) PremiumTheme.AccentGold.copy(alpha = 0.85f) else PremiumTheme.InnerCard
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (completed) Color.Black else PremiumTheme.TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = NumberFont,
                            fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(PremiumTheme.AccentGold))
            Spacer(modifier = Modifier.width(6.dp))
            Text("يوم مكتمل", color = PremiumTheme.TextSecondary, fontSize = 10.sp, fontFamily = MeQuranFont)
            Spacer(modifier = Modifier.width(20.dp))
            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(PremiumTheme.InnerCard))
            Spacer(modifier = Modifier.width(6.dp))
            Text("لم يُكتمل", color = PremiumTheme.TextSecondary, fontSize = 10.sp, fontFamily = MeQuranFont)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}
