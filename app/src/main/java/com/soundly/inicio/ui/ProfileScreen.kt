package com.soundly.inicio.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.soundly.R
import com.soundly.inicio.viewmodel.ProfileViewModel
import com.soundly.ui.components.SoundlyColors
import com.soundly.ui.components.SoundlyPrimaryButton
import com.soundly.ui.components.SoundlySecondaryButton
import com.soundly.ui.components.rememberLogoColor
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

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

                // Calcular todos los vértices
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

                // Dibujar el polígono con esquinas redondeadas
                for (i in vertices.indices) {
                    val current = vertices[i]
                    val next = vertices[(i + 1) % vertices.size]
                    val prev = vertices[(i - 1 + vertices.size) % vertices.size]

                    // Calcular vectores desde el vértice actual
                    val toPrev = Pair(prev.first - current.first, prev.second - current.second)
                    val toNext = Pair(next.first - current.first, next.second - current.second)

                    // Normalizar vectores
                    val toPrevLen = sqrt(toPrev.first.pow(2) + toPrev.second.pow(2))
                    val toNextLen = sqrt(toNext.first.pow(2) + toNext.second.pow(2))
                    val toPrevNorm = Pair(toPrev.first / toPrevLen, toPrev.second / toPrevLen)
                    val toNextNorm = Pair(toNext.first / toNextLen, toNext.second / toNextLen)

                    // Puntos de inicio y fin del arco
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

                    // Dibujar arco redondeado usando quadraticBezierTo
                    quadraticBezierTo(
                        current.first, current.second, // Punto de control (el vértice)
                        arcEnd.first, arcEnd.second // Punto final
                    )
                }
                close()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    onProfileCreated: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val extractedColor = rememberLogoColor()

    val buttonColor = extractedColor?.let { color ->
        SoundlyColors.adaptBlueForTheme(color)
    } ?: MaterialTheme.colorScheme.primary

    // Manejo del botón de retroceso del sistema
    BackHandler { viewModel.onBackClick() }

    // Animación de rotación infinita para paso 3
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

    // Animación de escala para transiciones
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

    // Animación de pulso para paso 3
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

    // Animación de elevación para el contenedor de imagen
    val elevation by animateDpAsState(
        targetValue = if (uiState.step == 3) 16.dp else 4.dp,
        label = "elevation"
    )

    // Animación de radio de esquina para el polígono
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
        onResult = { uri -> viewModel.onImageSelected(uri) }
    )

    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_screen_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (uiState.showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onBottomSheetDismiss() },
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
                            onClick = { viewModel.onBottomSheetDismiss() },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(stringResource(R.string.exit_dialog_cancel))
                        }
                        TextButton(
                            onClick = { viewModel.onExitConfirm() },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(stringResource(R.string.exit_dialog_confirm))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Animación de contenido con transiciones
                Spacer(modifier = Modifier.height(8.dp))
                ProfileStepProgress(
                    currentStep = uiState.step,
                    activeColor = buttonColor
                )

                AnimatedContent(
                    targetState = uiState.step,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            initialOffsetY = { it }
                        ) + fadeIn(
                            animationSpec = tween(300)
                        )).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(200),
                                targetOffsetY = { -it }
                            ) + fadeOut(
                                animationSpec = tween(200)
                            )
                        )
                    },
                    label = "stepTransition"
                ) { currentStep ->
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        when (currentStep) {
                            1 -> {
                                Text(
                                    text = stringResource(R.string.profile_screen_title),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.profile_screen_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            2 -> {
                                Text(
                                    text = stringResource(R.string.profile_complete_title),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.profile_add_username),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            3 -> {
                                Text(
                                    text = uiState.username,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.profile_completed_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // CONTENEDOR DE IMAGEN - CENTRADO HORIZONTALMENTE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Determinar la forma según el paso
                    val containerShape = when (uiState.step) {
                        1 -> CircleShape
                        2 -> RoundedCornerShape(40.dp)
                        else -> RoundedPolygonShape(7, animatedRadius.value)
                    }

                    // Determinar la forma de la imagen según el paso
                    val imageShape = when (uiState.step) {
                        1 -> CircleShape
                        2 -> RoundedCornerShape(40.dp)
                        else -> CircleShape
                    }

                    var isImagePressed by remember { mutableStateOf(false) }

                    if (uiState.imageUri == null || (uiState.step == 1 && uiState.isEditingImage)) {
                        // Placeholder inicial o modo edición
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
                                            try {
                                                awaitRelease()
                                            } finally {
                                                isImagePressed = false
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = if (isImagePressed) 1.05f else 1f
                                    scaleY = if (isImagePressed) 1.05f else 1f
                                }
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = stringResource(R.string.cd_camera_icon),
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    } else if (uiState.imageUri != null && !uiState.isEditingImage) {
                        // Contenedor que rota (solo si no estamos en modo edición)
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .scale(imageScale * pulseScale.value)
                                .graphicsLayer {
                                    if (uiState.step == 3) {
                                        rotationZ = rotation.value
                                    }
                                }
                                .clip(containerShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .shadow(elevation = elevation, shape = containerShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // La imagen con contra-rotación
                            Box(
                                modifier = Modifier
                                    .size(if (uiState.step == 3) 280.dp else 260.dp)
                                    .graphicsLayer {
                                        if (uiState.step == 3) {
                                            rotationZ = -rotation.value
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Crossfade para cambio suave de imagen
                                Crossfade(targetState = uiState.imageUri, label = "imageTransition") { uri ->
                                    if (uri != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = stringResource(R.string.cd_profile_image),
                                            modifier = Modifier
                                                .size(if (uiState.step == 3) 280.dp else 260.dp)
                                                .clip(imageShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.step == 1 || uiState.step == 3) {
                            Box(
                                modifier = Modifier
                                    .size(260.dp)
                                    .scale(imageScale)
                                    .clip(containerShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isImagePressed = true
                                                try {
                                                    awaitRelease()
                                                } finally {
                                                    isImagePressed = false
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        )
                                    }
                                    .graphicsLayer {
                                        scaleX = if (isImagePressed) 1.05f else 1f
                                        scaleY = if (isImagePressed) 1.05f else 1f
                                    }
                                    .clickable {
                                        if (uiState.step == 3) {
                                            viewModel.onEditClick()
                                        } else {
                                            viewModel.onEditImage()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Overlay semi-transparente para indicar que es clickable
                                if (uiState.step == 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CameraAlt,
                                            contentDescription = stringResource(R.string.change_image),
                                            modifier = Modifier.size(60.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Campo de texto solo para paso 2
                AnimatedVisibility(
                    visible = uiState.step == 2 && !uiState.isUsernameConfirmed,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        initialOffsetY = { it }
                    ),
                    exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                        animationSpec = tween(200),
                        targetOffsetY = { -it }
                    )
                ) {
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        label = { Text(stringResource(R.string.profile_screen_username_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isUsernameConfirmed
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botones según el paso
                Spacer(modifier = Modifier.height(24.dp))
            }

            when (uiState.step) {
                1 -> {
                    if (uiState.imageUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            SoundlyPrimaryButton(
                                extractedColor = extractedColor,
                                onClick = { viewModel.onContinueClick() },
                                modifier = Modifier.widthIn(min = 168.dp),
                                text = stringResource(R.string.onboarding_continue_button)
                            )
                        }
                    }
                }

                2 -> {
                    if (!uiState.isUsernameConfirmed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            SoundlyPrimaryButton(
                                extractedColor = extractedColor,
                                onClick = { viewModel.onContinueClick() },
                                modifier = Modifier.widthIn(min = 168.dp),
                                text = stringResource(R.string.onboarding_continue_button),
                                enabled = uiState.username.isNotBlank()
                            )
                        }
                    }
                }

                3 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoundlySecondaryButton(
                            extractedColor = extractedColor,
                            onClick = { viewModel.onEditClick() },
                            modifier = Modifier.widthIn(min = 56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit),
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        SoundlyPrimaryButton(
                            extractedColor = extractedColor,
                            onClick = { viewModel.onProfileCreationConfirmed(onProfileCreated) },
                            modifier = Modifier.widthIn(min = 168.dp),
                            text = stringResource(R.string.onboarding_continue_button)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileStepProgress(
    currentStep: Int,
    activeColor: Color,
    totalSteps: Int = 3
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Paso $currentStep de $totalSteps",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (step in 1..totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (step <= currentStep) activeColor
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }
        }
    }
}
