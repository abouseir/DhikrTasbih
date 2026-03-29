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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.focusable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items

data class DhikrItem(val id: Int, val textAr: String, val target: Int, val desc: String = "", val trans: String = "", val virtue: String = "", val reference: String = "")
data class AdhkarCategory(val id: Int, val titleAr: String, val titleEn: String? = null, val iconVector: ImageVector? = null, val iconRes: Int? = null, val items: List<DhikrItem>)



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

val tasbihItems = AdhkarData.tasbihItems()

@Composable
fun WearApp() {
    MaterialTheme {
        val navController = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "menu"
        ) {
            composable("menu") {
                MenuScreen { categoryId ->
                    navController.navigate("counter/$categoryId")
                }
            }
            composable("counter/{categoryId}") { backStackEntry ->
                val catIdStr = backStackEntry.arguments?.getString("categoryId") ?: "0"
                val catId = catIdStr.toIntOrNull() ?: 0
                CounterScreen(catId)
            }
        }
    }
}

@Composable
fun MenuScreen(onCategorySelected: (Int) -> Unit) {
    val categories = AdhkarData.categories()
    
    val state = androidx.wear.compose.foundation.lazy.rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    ScalingLazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    state.scrollBy(event.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Button(
                onClick = { onCategorySelected(0) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD4AF37)),
                modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 4.dp).height(48.dp)
            ) {
                Text("📿 التسبيح", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        items(categories) { category ->
            Button(
                onClick = { onCategorySelected(category.id) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 4.dp).height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.titleAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (category.iconVector != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(category.iconVector, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun CounterScreen(categoryId: Int) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("wear_tasbih_prefs", Context.MODE_PRIVATE)
    
    val items = remember(categoryId) {
        if (categoryId == 0) tasbihItems
        else AdhkarData.categories().find { it.id == categoryId }?.items ?: emptyList()
    }
    if (items.isEmpty()) return

    var itemIndex by remember { mutableStateOf(prefs.getInt("index_$categoryId", 0).coerceIn(0, items.lastIndex)) }
    val currentItem = items[itemIndex]
    
    var count by remember { mutableStateOf(prefs.getInt("count_${categoryId}_${currentItem.id}", 0)) }

    LaunchedEffect(itemIndex) {
        count = prefs.getInt("count_${categoryId}_${items[itemIndex].id}", 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (currentItem.target == 0 || count < currentItem.target) {
                    count++
                    prefs.edit().putInt("count_${categoryId}_${currentItem.id}", count).apply()
                    vibrateWearDevice(context, 30L)
                    
                    if (count == currentItem.target && itemIndex < items.size - 1) {
                        itemIndex++
                        prefs.edit().putInt("index_$categoryId", itemIndex).apply()
                    }
                } else {
                    vibrateWearDevice(context, 100L)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (items.size > 1) {
                Text(
                    text = "${itemIndex + 1} / ${items.size}",
                    color = Color(0xFF888888),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            )
            if (currentItem.target > 0) {
                Text(
                    text = "من ${currentItem.target}",
                    color = Color(0xFFD4AF37),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(48.dp)
                .clickable {
                    count = 0
                    prefs.edit().putInt("count_${categoryId}_${currentItem.id}", 0).apply()
                    vibrateWearDevice(context, 40L)
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
