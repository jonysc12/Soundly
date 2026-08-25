package com.soundly.player

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.util.UnstableApi
import com.soundly.data.repository.AudioSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AudioSettingsRepository

    companion object {
        private val KEY_QUEUE_IDS = stringPreferencesKey("queue_ids")
        private const val ACTION_A2DP_PROFILE_CHANGED = "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED"
    }

    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BluetoothReceiver", "Received intent: $action")

        var connectedDeviceAddress: String? = null

        val shouldTrigger = when (action) {
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, ACTION_A2DP_PROFILE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                Log.d("BluetoothReceiver", "A2DP state: $state")
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    connectedDeviceAddress = device?.address
                    true
                } else false
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.d("BluetoothReceiver", "ACL_CONNECTED detected")
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                connectedDeviceAddress = device?.address
                true
            }
            else -> false
        }

        if (shouldTrigger) {
            Log.d("BluetoothReceiver", "Potential audio device connected ($connectedDeviceAddress), checking settings...")
            
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val settings = repository.audioSettingsFlow.first()
                    Log.d("BluetoothReceiver", "Settings check: bluetoothAutoplayEnabled=${settings.bluetoothAutoplayEnabled}")
                    
                    if (settings.bluetoothAutoplayEnabled) {
                        // Solo autoplay si el dispositivo conectado está en la lista de permitidos
                        if (connectedDeviceAddress == null || !settings.bluetoothAutoplayDevices.contains(connectedDeviceAddress)) {
                            Log.d("BluetoothReceiver", "Device $connectedDeviceAddress not in allowed list, skipping autoplay")
                            return@launch
                        }

                        Log.d("BluetoothReceiver", "Autoplay criteria met")
                        // Pequeña espera para asegurar que el ruteo de audio de Android se haya completado
                        delay(1000)
                        
                        val playbackPrefs = context.playbackDataStore.data.first()
                        val queueIds = playbackPrefs[KEY_QUEUE_IDS]
                        Log.d("BluetoothReceiver", "Queue IDs in DataStore: $queueIds")
                        
                        if (!queueIds.isNullOrBlank()) {
                            Log.d("BluetoothReceiver", "Queue found, preparing to send PLAY intent")
                            val playIntent = Intent(context, PlaybackService::class.java).apply {
                                this.action = "PLAY"
                            }
                            // Intentar iniciar el servicio. startForegroundService es necesario si la app está en background
                            try {
                                Log.d("BluetoothReceiver", "Calling startForegroundService with PLAY action")
                                context.startForegroundService(playIntent)
                            } catch (e: Exception) {
                                Log.e("BluetoothReceiver", "Failed to startForegroundService, trying startService", e)
                                context.startService(playIntent)
                            }
                        } else {
                            Log.w("BluetoothReceiver", "Queue is empty or null, cannot autoplay")
                        }
                    } else {
                        Log.d("BluetoothReceiver", "Autoplay is DISABLED in settings, doing nothing")
                    }
                } catch (e: Exception) {
                    Log.e("BluetoothReceiver", "CRITICAL ERROR in BluetoothReceiver processing", e)
                }
            }
        } else {
            Log.d("BluetoothReceiver", "Intent ignored (not a 'connected' state or unknown action)")
        }
    }
}
