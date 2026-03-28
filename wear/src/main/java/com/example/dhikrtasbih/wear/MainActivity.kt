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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

val tasbihItems = listOf(
    DhikrItem(1001, "سُبْحَانَ اللَّهِ", 33, "", "", "", ""),
    DhikrItem(1002, "الْحَمْدُ لِلَّهِ", 33, "", "", "", ""),
    DhikrItem(1003, "اللَّهُ أَكْبَرُ", 33, "", "", "", ""),
    DhikrItem(1004, "لَا إِلَهَ إِلَّا اللَّهُ", 33, "", "", "", ""),
    DhikrItem(1005, "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", 0, "", "", "", ""),
    DhikrItem(1006, "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", 100, "", "", "", ""),
    DhikrItem(1007, "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", 100, "", "", "", ""),
    DhikrItem(1008, "سُبْحَانَ اللَّهِ الْعَظِيمِ", 0, "", "", "", "")
)

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
    
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
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
                Text(category.titleAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
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
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
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

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "\u200F${currentItem.textAr}\u200F",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            val targetStr = if (currentItem.target > 0) " / ${currentItem.target}" else " / ∞"
            Text(
                text = "$count$targetStr",
                color = Color(0xFFD4AF37),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(Color(0xFFD4AF37))
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
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "سَبِّحْ",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
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
