package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.R

@Composable
fun SplashScreen(onNavigateNext: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        progress = 1f
        delay(2500)
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2F1), // Light teal
                        Color(0xFF80CBC4), // Mid teal
                        Color(0xFF26A69A)  // Darker teal
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "MEDICINA",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF01579B),
                letterSpacing = 1.sp
            )
            Text(
                text = "FAMILIAR",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00695C),
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFE0F7FA),
                trackColor = Color(0x40FFFFFF),
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "CARGANDO...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
    }
}
