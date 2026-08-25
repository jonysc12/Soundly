package com.soundly

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.aria2c.Aria2c
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltAndroidApp
class SoundlyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicialización diferida de librerías de descarga
        // Se espera un poco para no competir con el arranque de la base de datos de música
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500) 
            try {
                Log.d("SoundlyApp", "Iniciando carga de binarios yt-dlp en segundo plano...")
                YoutubeDL.getInstance().init(this@SoundlyApp)
                Aria2c.getInstance().init(this@SoundlyApp)
                Log.d("SoundlyApp", "YoutubeDL y dependencias inicializadas correctamente")
            } catch (e: Exception) {
                Log.e("SoundlyApp", "Fallo al inicializar YoutubeDL en el arranque: ${e.message}", e)
            }
        }

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {

                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) {
                    if (activity is ComponentActivity) {
                        activity.enableEdgeToEdge()
                        activity.window.isNavigationBarContrastEnforced = false
                    }
                }

                override fun onActivityStarted(activity: android.app.Activity) {}
                override fun onActivityResumed(activity: android.app.Activity) {}
                override fun onActivityPaused(activity: android.app.Activity) {}
                override fun onActivityStopped(activity: android.app.Activity) {}
                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: android.app.Activity) {}
            }
        )
    }
}
