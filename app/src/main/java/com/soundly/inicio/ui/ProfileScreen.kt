package com.soundly.inicio.ui

import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.soundly.R
import com.soundly.inicio.viewmodel.ProfileUiState
import com.soundly.inicio.viewmodel.ProfileViewModel
import com.soundly.ui.components.SoundlyColors
import com.soundly.ui.components.rememberLogoColor
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.componentes.agslFrostedGlass
import com.soundly.ui.theme.LocalIsDarkTheme
import com.soundly.ui.theme.SoundlyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

private const val FLUID_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    layout(color) uniform half4 color1;
    layout(color) uniform half4 color2;
    layout(color) uniform half4 color3;

    float2 hash(float2 p) {
        p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
        return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(dot(hash(i + float2(0.0, 0.0)), f - float2(0.0, 0.0)),
                       dot(hash(i + float2(1.0, 0.0)), f - float2(1.0, 0.0)), u.x),
                   mix(dot(hash(i + float2(0.0, 1.0)), f - float2(0.0, 1.0)),
                       dot(hash(i + float2(1.0, 1.0)), f - float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float t = iTime * 0.2;
        
        float n = noise(uv * 1.5 + t);
        float n2 = noise(uv * 2.0 - t * 0.3);
        
        float mixVal = smoothstep(-0.3, 0.7, n + n2);
        half4 baseColor = mix(color1, color2, mixVal);
        
        float mixVal2 = smoothstep(-0.1, 0.9, noise(uv * 3.0 + t * 0.1));
        half4 finalColor = mix(baseColor, color3, mixVal2 * 0.3);
        
        // Ondas dinámicas en la parte inferior
        float wave = sin(uv.x * 6.0 + t * 3.0) * 0.02;
        wave += sin(uv.x * 3.0 - t * 2.0) * 0.03;
        
        // Límite de altura (aprox 12% de la pantalla)
        float heightLimit = 0.12 + wave;
        float mask = smoothstep(heightLimit + 0.08, heightLimit - 0.04, uv.y);
        
        return finalColor * mask;
    }
"""

// Shape personalizado para polígono de N lados con esquinas redondeadas
class RoundedPolygonShape(
    private val sides: Int,
    private val cornerRadiusPx: Float = 40f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                val radius = minOf(size.width, size.height) / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val angleStep = 2 * PI / sides
                val startAngle = -PI / 2

                val vertices = mutableListOf<Pair<Float, Float>>()
                for (i in 0 until sides) {
                    val angle = startAngle + angleStep * i
                    vertices.add(
                        Pair(
                            centerX + (radius * cos(angle)).toFloat(),
                            centerY + (radius * sin(angle)).toFloat()
                        )
                    )
                }

                for (i in vertices.indices) {
                    val current = vertices[i]
                    val next = vertices[(i + 1) % vertices.size]
                    val prev = vertices[(i - 1 + vertices.size) % vertices.size]

                    val toPrev = Pair(prev.first - current.first, prev.second - current.second)
                    val toNext = Pair(next.first - current.first, next.second - current.second)

                    val toPrevLen = sqrt(toPrev.first.pow(2) + toPrev.second.pow(2))
                    val toNextLen = sqrt(toNext.first.pow(2) + toNext.second.pow(2))
                    val toPrevNorm = Pair(toPrev.first / toPrevLen, toPrev.second / toPrevLen)
                    val toNextNorm = Pair(toNext.first / toNextLen, toNext.second / toNextLen)

                    val arcStart = Pair(
                        current.first + toPrevNorm.first * cornerRadiusPx,
                        current.second + toPrevNorm.second * cornerRadiusPx
                    )
                    val arcEnd = Pair(
                        current.first + toNextNorm.first * cornerRadiusPx,
                        current.second + toNextNorm.second * cornerRadiusPx
                    )

                    if (i == 0) {
                        moveTo(arcStart.first, arcStart.second)
                    } else {
                        lineTo(arcStart.first, arcStart.second)
                    }

                    quadraticBezierTo(current.first, current.second, arcEnd.first, arcEnd.second)
                }
                close()
            }
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector = Icons.AutoMirrored.Rounded.ArrowForward,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val borderAlpha = if (isDark) 0.3f else 0.05f
    val containerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f)
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .size(90.dp)
            .clip(CircleShape)
            .agslFrostedGlass(
                radius = 25f,
                tint = Color.Transparent
            )
            .border(
                1.dp,
                contentColor.copy(alpha = borderAlpha),
                CircleShape
            )
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.05f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = contentColor
        )
    }
}

@Composable
fun ProfileScreen(
    navController: NavHostController,
    onProfileCreated: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ProfileScreenContent(
        uiState = uiState,
        onBackClick = { viewModel.onBackClick() },
        onBottomSheetDismiss = { viewModel.onBottomSheetDismiss() },
        onExitConfirm = { 
            if (onBack != null) onBack()
            else viewModel.onExitConfirm()
        },
        onImageSelected = { viewModel.onImageSelected(it) },
        onUsernameChanged = { viewModel.onUsernameChanged(it) },
        onContinueClick = { viewModel.onContinueClick() },
        onEditClick = { viewModel.onEditClick() },
        onEditImage = { viewModel.onEditImage() },
        onProfileCreationConfirmed = { viewModel.onProfileCreationConfirmed(onProfileCreated) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onBackClick: () -> Unit,
    onBottomSheetDismiss: () -> Unit,
    onExitConfirm: () -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onContinueClick: () -> Unit,
    onEditClick: () -> Unit,
    onEditImage: () -> Unit,
    onProfileCreationConfirmed: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    
    var effectColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(uiState.imageUri, isDark) {
        val uri = uiState.imageUri
        if (uri != null) {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .crossfade(true)
                .build()
            
            val result = withContext(Dispatchers.IO) { loader.execute(request) }
            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                val palette = Palette.from(bitmap).generate()
                
                val dominant = palette.getDominantColor(if (isDark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                val vibrant = palette.getVibrantColor(dominant)
                val lightVibrant = palette.getLightVibrantColor(vibrant)
                val darkVibrant = palette.getDarkVibrantColor(dominant)
                
                effectColors = if (isDark) {
                    listOf(
                        Color(vibrant).copy(alpha = 0.7f), 
                        Color(lightVibrant).copy(alpha = 0.4f), 
                        Color(darkVibrant).copy(alpha = 0.2f)
                    )
                } else {
                    // Colores más visibles y vibrantes en modo claro
                    listOf(
                        Color(vibrant).copy(alpha = 0.6f), 
                        Color(lightVibrant).copy(alpha = 0.4f), 
                        Color(dominant).copy(alpha = 0.2f)
                    )
                }
            }
        } else {
            effectColors = emptyList()
        }
    }

    val animatedC1 by animateColorAsState(
        targetValue = effectColors.getOrElse(0) { Color.Transparent },
        animationSpec = tween(1000),
        label = "color1"
    )
    val animatedC2 by animateColorAsState(
        targetValue = effectColors.getOrElse(1) { Color.Transparent },
        animationSpec = tween(1000),
        label = "color2"
    )
    val animatedC3 by animateColorAsState(
        targetValue = effectColors.getOrElse(2) { Color.Transparent },
        animationSpec = tween(1000),
        label = "color3"
    )

    BackHandler { onBackClick() }

    val rotation = remember { Animatable(0f) }
    LaunchedEffect(uiState.step) {
        if (uiState.step == 3) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.snapTo(0f)
        }
    }

    val imageScale by animateFloatAsState(
        targetValue = when (uiState.step) {
            1 -> 1f
            2 -> 0.9f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "imageScale"
    )

    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(uiState.step) {
        if (uiState.step == 3) {
            pulseScale.animateTo(
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = { fraction ->
                        sin(fraction * PI).toFloat() * 0.05f + 1f
                    }),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScale.snapTo(1f)
        }
    }

    val elevation by animateDpAsState(
        targetValue = if (uiState.step == 3) 16.dp else 4.dp,
        label = "elevation"
    )

    val animatedRadius = remember { Animatable(72f) }
    LaunchedEffect(uiState.step) {
        if (uiState.step == 3) {
            animatedRadius.animateTo(96f, tween(800))
        } else {
            animatedRadius.snapTo(72f)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> onImageSelected(uri) }
    )

    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && uiState.imageUri != null && effectColors.isNotEmpty()) {
            FluidBackgroundEffect(colors = listOf(animatedC1, animatedC2, animatedC3))
        }

        if (uiState.showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { onBottomSheetDismiss() },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.profile_exit_dialog_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.profile_exit_dialog_text),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { onBottomSheetDismiss() },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(stringResource(R.string.exit_dialog_cancel))
                        }
                        TextButton(
                            onClick = { onExitConfirm() },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(stringResource(R.string.exit_dialog_confirm))
                        }
                    }
                }
            }
        }

        SettingsLayout(
            title = stringResource(R.string.profile_screen_title),
            onBack = { onBackClick() },
            containerColor = Color.Transparent
        ) {
            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    val enterTransition = if (targetState > initialState) {
                        slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(400))
                    } else {
                        slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
                            initialOffsetX = { -it }
                        ) + fadeIn(animationSpec = tween(400))
                    }

                    val exitTransition = if (targetState > initialState) {
                        slideOutHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            targetOffsetX = { -it }
                        ) + fadeOut(animationSpec = tween(300))
                    } else {
                        slideOutHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            targetOffsetX = { it }
                        ) + fadeOut(animationSpec = tween(300))
                    }

                    enterTransition togetherWith exitTransition
                },
                label = "stepTransition"
            ) { currentStep ->
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    val subtitle = when (currentStep) {
                        1 -> stringResource(R.string.profile_screen_subtitle)
                        2 -> stringResource(R.string.profile_add_username)
                        else -> stringResource(R.string.profile_completed_subtitle)
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp).alpha(0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val containerShape = when (uiState.step) {
                    1 -> CircleShape
                    2 -> RoundedCornerShape(40.dp)
                    else -> RoundedPolygonShape(7, animatedRadius.value)
                }
                val imageShape = if (uiState.step == 2) RoundedCornerShape(40.dp) else CircleShape

                var isImagePressed by remember { mutableStateOf(false) }

                if (uiState.imageUri == null || (uiState.step == 1 && uiState.isEditingImage)) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .scale(imageScale)
                            .clip(containerShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isImagePressed = true
                                        try { awaitRelease() } finally {
                                            isImagePressed = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                )
                            }
                            .clickable {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(60.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .scale(imageScale * pulseScale.value)
                            .graphicsLayer { if (uiState.step == 3) rotationZ = rotation.value }
                            .clip(containerShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .shadow(elevation = elevation, shape = containerShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uiState.imageUri)
                                    .allowHardware(true)
                                    .build()
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(if (uiState.step == 3) 280.dp else 260.dp)
                                .graphicsLayer { if (uiState.step == 3) rotationZ = -rotation.value }
                                .clip(imageShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    if (uiState.step == 1 || uiState.step == 3) {
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(containerShape)
                                .clickable {
                                    if (uiState.step == 3) onEditClick() else onEditImage()
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(visible = uiState.step == 2 && !uiState.isUsernameConfirmed) {
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { onUsernameChanged(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    label = { Text(stringResource(R.string.profile_screen_username_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(140.dp))
        }

        val showFab = when(uiState.step) {
            1 -> uiState.imageUri != null
            2 -> uiState.username.isNotBlank() && !uiState.isUsernameConfirmed
            3 -> true
            else -> false
        }

        if (showFab) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val startX = 0.82f
                val startY = 0.85f
                
                Row(
                    modifier = Modifier
                        .offset(
                            x = maxWidth * startX - (if (uiState.step == 3) 145.dp else 45.dp),
                            y = maxHeight * startY - 45.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (uiState.step == 3) {
                        ActionButton(
                            icon = Icons.Default.Edit,
                            onClick = { onEditClick() },
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    ActionButton(
                        icon = if (uiState.step == 3) Icons.Default.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        onClick = { 
                            if (uiState.step < 3) onContinueClick()
                            else onProfileCreationConfirmed()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FluidBackgroundEffect(colors: List<Color>) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "time"
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(FLUID_SHADER) }
        Canvas(modifier = Modifier.fillMaxSize().onSizeChanged { size ->
            shader.setFloatUniform("iResolution", size.width.toFloat(), size.height.toFloat())
        }) {
            shader.setFloatUniform("iTime", time)
            shader.setColorUniform("color1", colors[0].toArgb())
            shader.setColorUniform("color2", colors[1].toArgb())
            shader.setColorUniform("color3", colors[2].toArgb())
            drawRect(brush = ShaderBrush(shader))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    SoundlyTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                step = 1,
                username = "Usuario de Prueba"
            ),
            onBackClick = {},
            onBottomSheetDismiss = {},
            onExitConfirm = {},
            onImageSelected = {},
            onUsernameChanged = {},
            onContinueClick = {},
            onEditClick = {},
            onEditImage = {},
            onProfileCreationConfirmed = {}
        )
    }
}
