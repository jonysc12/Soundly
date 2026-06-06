package com.soundly

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SoundlyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {

                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) {
                    if (activity is ComponentActivity) {
                        activity.enableEdgeToEdge()
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
