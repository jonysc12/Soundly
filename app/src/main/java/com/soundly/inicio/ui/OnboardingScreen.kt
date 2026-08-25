package com.soundly.inicio.ui

import android.graphics.RuntimeShader
import android.os.Build
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.soundly.R
import com.soundly.inicio.viewmodel.OnboardingViewModel
import com.soundly.ui.theme.LocalIsDarkTheme
import com.soundly.ui.theme.SoundlyTheme
import com.soundly.ui.componentes.agslFrostedGlass
import kotlinx.coroutines.delay

private const val LIQUID_MORPH_SHADER = """
    uniform float2 iResolution;
    uniform float2 iCenter;
    uniform float iStretch;
    uniform float iExpansion;
    uniform float iTime;
    layout(color) uniform half4 color;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
    }

    float sdCircle(float2 p, float r) {
        return length(p) - r;
    }

    float sdRoundedBox(float2 p, float2 b, float4 r) {
        r.xy = (p.x > 0.0) ? r.xy : r.zw;
        r.x = (p.y > 0.0) ? r.x : r.y;
        float2 q = abs(p) - b + r.x;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float aspect = iResolution.x / iResolution.y;
        
        float2 p = uv - iCenter;
        p.x *= aspect;
        
        float r = 0.045; 
        float2 warpP = p;
        
        if (iStretch > 0.0) {
            float wobble = noise(warpP * 10.0 + iTime * 2.0) * iStretch * 0.05;
            warpP += wobble;
        }
        
        float d_circle = sdCircle(warpP, r);
        float2 uv_p = uv - iCenter;
        
        float growH = smoothstep(0.0, 0.6, iExpansion);
        float growV = smoothstep(0.4, 1.0, iExpansion);
        
        float2 targetSize = float2(
            mix(0.04, 2.0, growH), 
            mix(0.04, 2.0, growV) 
        );
        
        float cornerRadius = mix(0.1, 0.0, smoothstep(0.8, 1.0, iExpansion));
        float d_box = sdRoundedBox(uv_p, targetSize * 0.5, float4(cornerRadius));
        
        float morph = smoothstep(0.1, 0.9, iExpansion);
        float final_d = mix(d_circle, d_box, morph);
        
        float organic = noise(uv * 6.0 + iTime) * 0.1 * (1.0 - iExpansion) * iExpansion;
        final_d -= organic;
        
        float mask = smoothstep(0.005, -0.005, final_d);
        
        half4 fluidColor = color * mask;

        float borderSoftness = 0.015;
        float borderInner = smoothstep(-borderSoftness, 0.0, final_d);
        float borderOuter = smoothstep(0.005, 0.0, final_d);
        float softBorder = borderInner * borderOuter;
        half4 borderColor = half4(1.0, 1.0, 1.0, 0.2); 
        
        half4 withBorder = mix(fluidColor, borderColor, softBorder * (1.0 - morph));
        
        return withBorder * mix(0.9, 1.0, iExpansion);
    }
"""

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    OnboardingScreenContent(
        onFinished = { viewModel.completeOnboarding(onFinished) }
    )
}

@Composable
fun OnboardingScreenContent(
    onFinished: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardingUI(onFinished = onFinished)
        }
    }
}

@Composable
fun OnboardingUI(
    onFinished: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var visible by remember { mutableStateOf(false) }
    var isClicked by remember { mutableStateOf(false) }
    var isExpanding by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        visible = true 
    }

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_waves")
    val waveTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "waveTime"
    )

    val startX = 0.82f
    val endX = 0.82f 
    val startY = 0.85f
    val endY = 0.85f 

    LaunchedEffect(isClicked) {
        if (isClicked) {
            delay(500L) 
            isExpanding = true
            delay(2000L)
            onFinished()
        }
    }

    val iconAlpha: Float by animateFloatAsState(
        targetValue = if (isClicked) 0f else 1f,
        animationSpec = tween(500),
        label = "icon_fade"
    )

    val movementProgress: Float by animateFloatAsState(
        targetValue = if (isClicked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 25f),
        label = "movement"
    )

    val stretchStrength = if (isClicked && !isExpanding) {
        val bell = (1f - kotlin.math.abs(2f * movementProgress - 1f)).let { it * it }
        bell * 0.8f
    } else 0f

    val expansionProgress: Float by animateFloatAsState(
        targetValue = if (isExpanding) 1f else 0f,
        animationSpec = tween(1800, easing = FastOutSlowInEasing),
        label = "expansion"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val currentX = startX + (endX - startX) * movementProgress
        val currentY = startY + (endY - startY) * (movementProgress * movementProgress)

        Box(
            modifier = Modifier.align(Alignment.Center).offset(y = (-60).dp).padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            val overshootEasing = Easing { OvershootInterpolator(0.7f).getInterpolation(it) }
            AnimatedVisibility(
                visible = visible && !isExpanding,
                enter = fadeIn(tween(1200)) + scaleIn(tween(1200, easing = overshootEasing)),
                exit = fadeOut(tween(500))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_soundly),
                    contentDescription = "Soundly Logo",
                    modifier = Modifier.size(290.dp).clip(RoundedCornerShape(24.dp))
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val liquidShader = remember { RuntimeShader(LIQUID_MORPH_SHADER) }
            val shaderCenterX = currentX 
            val shaderCenterY = currentY 

            Canvas(modifier = Modifier.fillMaxSize()) {
                liquidShader.setFloatUniform("iResolution", size.width, size.height)
                liquidShader.setFloatUniform("iCenter", shaderCenterX, shaderCenterY)
                liquidShader.setFloatUniform("iStretch", stretchStrength)
                liquidShader.setFloatUniform("iExpansion", expansionProgress)
                liquidShader.setFloatUniform("iTime", waveTime)

                val glassColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.3f)
                liquidShader.setColorUniform("color", glassColor.toArgb())

                drawRect(brush = ShaderBrush(liquidShader))
            }
        }

        if (isClicked && !isExpanding) {
            if (iconAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .offset(
                            x = screenWidth * currentX - 45.dp,
                            y = screenHeight * currentY - 45.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).graphicsLayer(alpha = iconAlpha),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (!isClicked) {
            val borderAlpha = if (isDark) 0.2f else 0.08f
            val contentColor = MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .offset(
                        x = screenWidth * startX - 45.dp,
                        y = screenHeight * startY - 45.dp
                    )
                    .clip(CircleShape)
                    .agslFrostedGlass(radius = 20f, tint = Color.Transparent)
                    .border(
                        width = 2.0.dp,
                        brush = Brush.radialGradient(
                            0.85f to Color.Transparent,
                            1.0f to contentColor.copy(alpha = borderAlpha),
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isClicked = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(R.string.onboarding_continue_button),
                    modifier = Modifier.size(36.dp),
                    tint = contentColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    SoundlyTheme {
        OnboardingScreenContent(onFinished = {})
    }
}
