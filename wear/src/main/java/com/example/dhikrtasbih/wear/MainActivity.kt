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

    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    scrollState.scrollBy(event.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Position and Count indicator
            val targetStr = if (currentItem.target > 0) "/${currentItem.target}" else "/∞"
            Text(
                text = "${itemIndex + 1}/${items.size}  •  $count$targetStr",
                color = Color(0xFF888888),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Previous",
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(28.dp).clickable {
                        itemIndex = if (itemIndex - 1 < 0) items.size - 1 else itemIndex - 1
                        prefs.edit().putInt("index_$categoryId", itemIndex).apply()
                        vibrateWearDevice(context, 20L)
                    }.padding(4.dp)
                )
                
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(24.dp).clickable {
                        count = 0
                        prefs.edit().putInt("count_${categoryId}_${currentItem.id}", 0).apply()
                        vibrateWearDevice(context, 40L)
                    }.padding(2.dp)
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Next",
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(28.dp).clickable {
                        itemIndex = (itemIndex + 1) % items.size
                        prefs.edit().putInt("index_$categoryId", itemIndex).apply()
                        vibrateWearDevice(context, 20L)
                    }.padding(4.dp)
                )
            }

            val textLength = currentItem.textAr.length
            val dynamicFontSize = if (textLength > 300) 13.sp else if (textLength > 150) 15.sp else if (textLength > 50) 16.sp else 18.sp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\u200F${currentItem.textAr}\u200F",
                    color = Color.White,
                    fontSize = dynamicFontSize,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
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
                }
        )
    }
    LaunchedEffect(categoryId, itemIndex) { focusRequester.requestFocus() }
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
