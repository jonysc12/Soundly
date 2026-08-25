package com.soundly.inicio.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.soundly.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.soundly.R
import com.soundly.inicio.viewmodel.PermissionViewModel
import com.soundly.ui.components.M3ListItem
import com.soundly.ui.components.rememberLogoColor
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.componentes.agslFrostedGlass
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.soundly.ui.theme.SoundlyTheme
import kotlinx.coroutines.delay

// ── Bloques reutilizables ──────────────────────────────────────────────────

@Composable
private fun PermissionSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun PermissionList(
    storagePermissionState: com.soundly.inicio.viewmodel.PermissionState,
    notificationPermissionState: com.soundly.inicio.viewmodel.PermissionState,
    bluetoothPermissionState: com.soundly.inicio.viewmodel.PermissionState,
    listAlpha: Float,
    onStorageClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.alpha(listAlpha)) {
        PermissionSection(title = stringResource(R.string.permission_screen_section_scanning)) {
            M3ListItem(
                icon = storagePermissionState.iconProvider(),
                tempIcon = storagePermissionState.tempIcon,
                title = stringResource(R.string.permission_screen_storage_title),
                description = stringResource(R.string.permission_screen_storage_description),
                color = Color.Transparent,
                drawDivider = false,
                enabled = storagePermissionState.isAvailable,
                onClick = onStorageClick
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        PermissionSection(title = stringResource(R.string.permission_screen_section_functions)) {
            if (notificationPermissionState.isAvailable) {
                M3ListItem(
                    icon = notificationPermissionState.iconProvider(),
                    tempIcon = notificationPermissionState.tempIcon,
                    title = stringResource(R.string.permission_screen_notifications_title),
                    description = stringResource(R.string.permission_screen_notifications_description),
                    color = Color.Transparent,
                    drawDivider = bluetoothPermissionState.isAvailable,
                    enabled = true,
                    onClick = onNotificationClick
                )
            }
            if (bluetoothPermissionState.isAvailable) {
                M3ListItem(
                    icon = bluetoothPermissionState.iconProvider(),
                    tempIcon = bluetoothPermissionState.tempIcon,
                    title = stringResource(R.string.permission_screen_bluetooth_title),
                    description = stringResource(R.string.permission_screen_bluetooth_description),
                    color = Color.Transparent,
                    drawDivider = false,
                    enabled = true,
                    onClick = onBluetoothClick
                )
            }
        }
    }
}

@Composable
private fun GrantButton(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val borderAlpha = if (isDark) 0.3f else 0.05f
    val containerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f)
    val contentColor = MaterialTheme.colorScheme.onSurface

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
            .background(containerColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onGrantClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = stringResource(R.string.permission_screen_grant_permission_button),
            modifier = Modifier.size(36.dp),
            tint = contentColor
        )
    }
}

@Composable
private fun SuccessCheckOverlay(visible: Boolean) {
    if (!visible) return

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 400))
        delay(200)
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 400))
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .alpha(alpha.value),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}



// ── Screen principal ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    navController: NavHostController,
    onPermissionsGranted: () -> Unit,
    viewModel: PermissionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val storagePermissionState = uiState.storagePermissionState
    val notificationPermissionState = uiState.notificationPermissionState
    val bluetoothPermissionState = uiState.bluetoothPermissionState

    var permissionToRequest by remember { mutableStateOf<String?>(null) }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionToRequest?.let { permission ->
            viewModel.onPermissionResult(permission, isGranted)
        }
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val storageGranted = permissions.getOrDefault(storagePermissionState.permission, storagePermissionState.isGranted)
        val notificationGranted = permissions.getOrDefault(notificationPermissionState.permission, notificationPermissionState.isGranted)
        val bluetoothGranted = permissions.getOrDefault(bluetoothPermissionState.permission, bluetoothPermissionState.isGranted)
        viewModel.updateGrantedStatus(storageGranted, notificationGranted, bluetoothGranted)
    }

    var allPermissionsGranted by remember { mutableStateOf(false) }
    val listAlpha = remember { Animatable(1f) }

    val onStorageClick = {
        if (storagePermissionState.isAvailable && !storagePermissionState.isGranted) {
            permissionToRequest = storagePermissionState.permission
            singlePermissionLauncher.launch(storagePermissionState.permission)
        }
    }
    val onNotificationClick = {
        if (!notificationPermissionState.isGranted) {
            permissionToRequest = notificationPermissionState.permission
            singlePermissionLauncher.launch(notificationPermissionState.permission)
        }
    }
    val onBluetoothClick = {
        if (!bluetoothPermissionState.isGranted) {
            permissionToRequest = bluetoothPermissionState.permission
            singlePermissionLauncher.launch(bluetoothPermissionState.permission)
        }
    }
    val onGrantClick = {
        val permissionsToRequestList = mutableListOf<String>()
        if (!storagePermissionState.isGranted) permissionsToRequestList.add(storagePermissionState.permission)
        if (notificationPermissionState.isAvailable && !notificationPermissionState.isGranted) permissionsToRequestList.add(notificationPermissionState.permission)
        if (bluetoothPermissionState.isAvailable && !bluetoothPermissionState.isGranted) permissionsToRequestList.add(bluetoothPermissionState.permission)
        
        if (permissionsToRequestList.isNotEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequestList.toTypedArray())
        } else {
            // Si ya están concedidos, proceder directamente
            onPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        val storageGranted = ContextCompat.checkSelfPermission(context, storagePermissionState.permission) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (notificationPermissionState.isAvailable) {
            ContextCompat.checkSelfPermission(context, notificationPermissionState.permission) == PackageManager.PERMISSION_GRANTED
        } else true
        val bluetoothGranted = if (bluetoothPermissionState.isAvailable) {
            ContextCompat.checkSelfPermission(context, bluetoothPermissionState.permission) == PackageManager.PERMISSION_GRANTED
        } else true

        if (storageGranted && notificationGranted && bluetoothGranted) {
            onPermissionsGranted()
        } else {
            viewModel.updateGrantedStatus(storageGranted, notificationGranted, bluetoothGranted)
        }
    }

    val areAllGranted by remember {
        derivedStateOf {
            uiState.storagePermissionState.isGranted &&
                    (uiState.notificationPermissionState.isGranted || !uiState.notificationPermissionState.isAvailable) &&
                    (uiState.bluetoothPermissionState.isGranted || !uiState.bluetoothPermissionState.isAvailable)
        }
    }

    LaunchedEffect(areAllGranted) {
        if (areAllGranted && !allPermissionsGranted) {
            allPermissionsGranted = true
            listAlpha.animateTo(0f, animationSpec = tween(durationMillis = 400))
            delay(1000)
            onPermissionsGranted()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsLayout(
            title = stringResource(R.string.permission_screen_title),
            onBack = { navController.popBackStack() }
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            

            
            Spacer(modifier = Modifier.height(44.dp))
            
            PermissionList(
                storagePermissionState = storagePermissionState,
                notificationPermissionState = notificationPermissionState,
                bluetoothPermissionState = bluetoothPermissionState,
                listAlpha = listAlpha.value,
                onStorageClick = onStorageClick,
                onNotificationClick = onNotificationClick,
                onBluetoothClick = onBluetoothClick
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = stringResource(R.string.permission_screen_explanation),
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    letterSpacing = 0.25.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .alpha(0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.permission_screen_note),
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .alpha(0.7f)
            )
            
            Spacer(modifier = Modifier.height(140.dp))
        }

        GrantButton(
            onGrantClick = onGrantClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 48.dp)
        )

        SuccessCheckOverlay(visible = allPermissionsGranted)
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    SoundlyTheme {
        PermissionScreen(
            navController = rememberNavController(),
            onPermissionsGranted = {}
        )
    }
}
