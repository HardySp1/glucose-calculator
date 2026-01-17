// MainActivity.kt
package com.example.glucosecalculator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class XDripData(
    val glucose: Double,
    val timestamp: Long,
    val delta: Double,
    val trend: String
)

data class CalculationResult(
    val dextroseAmount: Double,
    val dextroseTime: Int,
    val dextroseDecay: Double,
    val bananaAmount: Double,
    val bananaCount: Double,
    val bananaTime: Int,
    val bananaDecay: Double,
    val khFactor: Double,
    val trendAdjustment: String
)

class MainActivity : ComponentActivity() {
    private var xdripData by mutableStateOf<XDripData?>(null)
    
    private val xdripReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val glucose = it.getDoubleExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate", 0.0)
                val timestamp = it.getLongExtra("com.eveningoutpost.dexdrip.Extras.Time", 0L)
                val delta = it.getDoubleExtra("com.eveningoutpost.dexdrip.Extras.BgDelta", 0.0)
                val slope = it.getDoubleExtra("com.eveningoutpost.dexdrip.Extras.BgSlope", 0.0)
                
                val trend = when {
                    slope > 3.5 -> "↑↑" // Stark steigend
                    slope > 2.0 -> "↑"  // Steigend
                    slope > -2.0 -> "→" // Stabil
                    slope > -3.5 -> "↓" // Fallend
                    else -> "↓↓"        // Stark fallend
                }
                
                xdripData = XDripData(glucose, timestamp, delta, trend)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // xDrip+ Broadcast registrieren
        val filter = IntentFilter("com.eveningoutpost.dexdrip.BgEstimate")
        registerReceiver(xdripReceiver, filter)
        
        setContent {
            MaterialTheme {
                GlucoseCalculatorScreen(xdripData)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(xdripReceiver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseCalculatorScreen(xdripData: XDripData?) {
    var targetGlucose by remember { mutableStateOf("100") }
    var bodyWeight by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<CalculationResult?>(null) }
    
    val scrollState = rememberScrollState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF0F4F8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🩺 Glukose-Rechner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // xDrip+ Daten Anzeige
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (xdripData != null) Color(0xFF10B981) else Color(0xFFF59E0B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (xdripData != null) {
                        val isRecent = System.currentTimeMillis() - xdripData.timestamp < 10 * 60 * 1000
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "xDrip+ Verbunden ✓",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = formatTimestamp(xdripData.timestamp),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                            }
                            
                            if (!isRecent) {
                                Text(
                                    text = "⚠️ Alt",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Aktuell",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${xdripData.glucose.toInt()} mg/dl",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Trend",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = xdripData.trend,
                                    color = Color.White,
                                    fontSize = 32.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Delta: ${if (xdripData.delta > 0) "+" else ""}${String.format("%.1f", xdripData.delta)} mg/dl",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "⏳ Warte auf xDrip+ Daten...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stelle sicher, dass xDrip+ läuft und Broadcasts aktiviert sind",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Eingabefelder
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = targetGlucose,
                        onValueChange = { targetGlucose = it },
                        label = { Text("Zielwert (mg/dl)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = bodyWeight,
                        onValueChange = { bodyWeight = it },
                        label = { Text("Körpergewicht (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            xdripData?.let { data ->
                                bodyWeight.toDoubleOrNull()?.let { weight ->
                                    targetGlucose.toDoubleOrNull()?.let { target ->
                                        result = calculateRecommendations(
                                            data.glucose,
                                            target,
                                            weight,
                                            data.delta,
                                            data.trend
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = xdripData != null && bodyWeight.isNotBlank() && targetGlucose.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Berechnen", fontSize = 16.sp)
                    }
                }
            }
            
            // Ergebnisse
            result?.let { res ->
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Empfohlene Mengen:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF4F46E5)
                        )
                        
                        if (res.trendAdjustment.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = res.trendAdjustment,
                                fontSize = 12.sp,
                                color = Color(0xFF7C3AED)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Traubenzucker
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🍬 Traubenzucker",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${String.format("%.1f", res.dextroseAmount)}g",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⏱️ Zielzeit: ${res.dextroseTime} Min",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Inkl. ${String.format("%.1f", res.dextroseDecay)} mg/dl Abbau",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Banane
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🍌 Banane",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${res.bananaAmount.toInt()}g",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "≈ ${String.format("%.1f", res.bananaCount)} Banane(n)",
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "⏱️ Zielzeit: ${res.bananaTime} Min",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Inkl. ${String.format("%.1f", res.bananaDecay)} mg/dl Abbau",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Technische Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Dein KH-Faktor: ${String.format("%.2f", res.khFactor)} mg/dl pro 1g KH",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Warnung
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Text(text = "⚠️ ", fontSize = 16.sp)
                    Text(
                        text = "Diese Berechnung dient nur zur Orientierung. Bitte konsultiere bei medizinischen Entscheidungen immer deinen Arzt oder Diabetesberater.",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }
    }
}

fun calculateRecommendations(
    currentGlucose: Double,
    targetGlucose: Double,
    bodyWeight: Double,
    delta: Double,
    trend: String
): CalculationResult? {
    if (currentGlucose >= targetGlucose) {
        return null
    }
    
    val difference = targetGlucose - currentGlucose
    val khFactor = 500.0 / bodyWeight
    
    // Trend-basierte Anpassung
    val trendMultiplier = when (trend) {
        "↓↓" -> 1.3  // Stark fallend: 30% mehr
        "↓" -> 1.15   // Fallend: 15% mehr
        "→" -> 1.0    // Stabil: keine Anpassung
        "↑" -> 0.9    // Steigend: 10% weniger
        "↑↑" -> 0.8   // Stark steigend: 20% weniger
        else -> 1.0
    }
    
    val trendAdjustment = when (trend) {
        "↓↓" -> "⚠️ Stark fallender Trend: +30% Korrektur"
        "↓" -> "📉 Fallender Trend: +15% Korrektur"
        "↑" -> "📈 Steigender Trend: -10% Korrektur"
        "↑↑" -> "⚠️ Stark steigender Trend: -20% Korrektur"
        else -> ""
    }
    
    // Präzisere Abbaurate basierend auf Delta (mg/dl pro Minute)
    val glucoseDecayRate = if (abs(delta) > 0.1) {
        abs(delta) / 5.0  // Delta ist über 5 Minuten
    } else {
        1.5  // Standard-Schätzung
    }
    
    // TRAUBENZUCKER
    val dextroseTime = 15
    val dextroseDecay = glucoseDecayRate * dextroseTime
    val dextroseNeededRise = (difference + dextroseDecay) * trendMultiplier
    val dextroseKH = dextroseNeededRise / khFactor
    val dextroseAmount = dextroseKH
    
    // BANANE
    val bananaTime = 30
    val bananaDecay = glucoseDecayRate * bananaTime
    val bananaNeededRise = (difference + bananaDecay) * trendMultiplier
    val bananaKH = bananaNeededRise / khFactor
    val bananaKHAdjusted = bananaKH * (100.0 / 55.0)
    val bananaAmount = bananaKHAdjusted / 0.20
    val bananaCount = bananaAmount / 120.0
    
    return CalculationResult(
        dextroseAmount = dextroseAmount,
        dextroseTime = dextroseTime,
        dextroseDecay = dextroseDecay,
        bananaAmount = bananaAmount,
        bananaCount = bananaCount,
        bananaTime = bananaTime,
        bananaDecay = bananaDecay,
        khFactor = khFactor,
        trendAdjustment = trendAdjustment
    )
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return "Zuletzt: ${sdf.format(Date(timestamp))}"
}
