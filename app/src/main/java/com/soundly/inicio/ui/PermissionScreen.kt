package com.soundly.inicio.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.soundly.R
import com.soundly.inicio.viewmodel.PermissionViewModel
import com.soundly.ui.components.M3ListItem
import com.soundly.ui.components.SoundlyPrimaryButton
import com.soundly.ui.components.rememberLogoColor
import kotlinx.coroutines.delay

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
    val extractedColor = rememberLogoColor()

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

    LaunchedEffect(Unit) {
        val storageGranted = ContextCompat.checkSelfPermission(context, storagePermissionState.permission) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (notificationPermissionState.isAvailable) {
            ContextCompat.checkSelfPermission(context, notificationPermissionState.permission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val bluetoothGranted = if (bluetoothPermissionState.isAvailable) {
            ContextCompat.checkSelfPermission(context, bluetoothPermissionState.permission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (storageGranted && notificationGranted && bluetoothGranted) {
            onPermissionsGranted()
        } else {
            viewModel.updateGrantedStatus(storageGranted, notificationGranted, bluetoothGranted)
        }
    }

    LaunchedEffect(uiState) {
        val areAllGranted = storagePermissionState.isGranted &&
            (notificationPermissionState.isGranted || !notificationPermissionState.isAvailable) &&
            (bluetoothPermissionState.isGranted || !bluetoothPermissionState.isAvailable)

        if (areAllGranted && !allPermissionsGranted) {
            allPermissionsGranted = true
            listAlpha.animateTo(0f, animationSpec = tween(durationMillis = 400))
            delay(1000)
            onPermissionsGranted()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.permission_screen_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_screen_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(listAlpha.value)
                ) {
                    Column {
                        M3ListItem(
                            icon = storagePermissionState.iconProvider(),
                            tempIcon = storagePermissionState.tempIcon,
                            title = stringResource(R.string.permission_screen_storage_title),
                            description = stringResource(R.string.permission_screen_storage_description),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            drawDivider = true,
                            enabled = storagePermissionState.isAvailable,
                            onClick = {
                                if (storagePermissionState.isAvailable && !storagePermissionState.isGranted) {
                                    permissionToRequest = storagePermissionState.permission
                                    singlePermissionLauncher.launch(storagePermissionState.permission)
                                }
                            }
                        )
                        if (notificationPermissionState.isAvailable) {
                            M3ListItem(
                                icon = notificationPermissionState.iconProvider(),
                                tempIcon = notificationPermissionState.tempIcon,
                                title = stringResource(R.string.permission_screen_notifications_title),
                                description = stringResource(R.string.permission_screen_notifications_description),
                                shape = if (bluetoothPermissionState.isAvailable) {
                                    RoundedCornerShape(0.dp)
                                } else {
                                    RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                },
                                drawDivider = bluetoothPermissionState.isAvailable,
                                enabled = true,
                                onClick = {
                                    if (!notificationPermissionState.isGranted) {
                                        permissionToRequest = notificationPermissionState.permission
                                        singlePermissionLauncher.launch(notificationPermissionState.permission)
                                    }
                                }
                            )
                        }
                        if (bluetoothPermissionState.isAvailable) {
                            M3ListItem(
                                icon = bluetoothPermissionState.iconProvider(),
                                tempIcon = bluetoothPermissionState.tempIcon,
                                title = stringResource(R.string.permission_screen_bluetooth_title),
                                description = stringResource(R.string.permission_screen_bluetooth_description),
                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                drawDivider = false,
                                enabled = true,
                                onClick = {
                                    if (!bluetoothPermissionState.isGranted) {
                                        permissionToRequest = bluetoothPermissionState.permission
                                        singlePermissionLauncher.launch(bluetoothPermissionState.permission)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.permission_screen_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SoundlyPrimaryButton(
                    extractedColor = extractedColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    onClick = {
                        val permissionsToRequestList = mutableListOf<String>()
                        if (!storagePermissionState.isGranted) {
                            permissionsToRequestList.add(storagePermissionState.permission)
                        }
                        if (notificationPermissionState.isAvailable && !notificationPermissionState.isGranted) {
                            permissionsToRequestList.add(notificationPermissionState.permission)
                        }
                        if (bluetoothPermissionState.isAvailable && !bluetoothPermissionState.isGranted) {
                            permissionsToRequestList.add(bluetoothPermissionState.permission)
                        }
                        if (permissionsToRequestList.isNotEmpty()) {
                            multiplePermissionsLauncher.launch(permissionsToRequestList.toTypedArray())
                        }
                    },
                    text = stringResource(R.string.permission_screen_grant_permission_button)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (allPermissionsGranted) {
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
        }
    }
}
