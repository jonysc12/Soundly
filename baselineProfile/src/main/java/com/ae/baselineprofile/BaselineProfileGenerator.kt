package com.ae.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // Obtenemos el ID del paquete, asegurándonos de que no sea nulo
        val targetAppId = InstrumentationRegistry.getArguments().getString("targetAppId") ?: "com.soundly"

        rule.collect(
            packageName = targetAppId,
            includeInStartupProfile = true,
            maxIterations = 3
        ) {
            pressHome()
            
            // Intentamos iniciar la actividad principal de forma explícita
            // Esto ayuda si el benchmark tiene problemas para encontrar el launcher
            try {
                startActivityAndWait()
            } catch (e: Exception) {
                // Si falla el inicio estándar, intentamos forzarlo vía shell
                device.executeShellCommand("am start -n $targetAppId/com.soundly.MainActivity")
                // Esperamos un poco a que el proceso aparezca
                device.wait(Until.hasObject(By.pkg(targetAppId)), 15_000)
            }

            // --- TRUCO: Saltador de Onboarding Mejorado ---
            // Si la app se queda en el Splash o Onboarding, el benchmark puede pensar que no arrancó
            val commonButtons = listOf("Continuar", "Siguiente", "Aceptar", "Permitir", "Allow", "Grant")
            
            repeat(15) {
                var actionTaken = false
                for (btnText in commonButtons) {
                    val obj = device.findObject(By.textContains(btnText)) ?: device.findObject(By.descContains(btnText))
                    if (obj != null) {
                        obj.click()
                        device.waitForIdle()
                        actionTaken = true
                        break
                    }
                }
                // Si ya vemos la Home, salimos del bucle
                if (device.hasObject(By.res("home_page_list"))) return@repeat
                if (!actionTaken) Thread.sleep(1000)
            }

            // Esperar a la lista de la Home para el perfil de scroll
            val homeList = device.wait(Until.findObject(By.res("home_page_list")), 10_000)
            if (homeList != null) {
                homeList.setGestureMargin(device.displayWidth / 5)
                homeList.fling(Direction.DOWN)
                device.waitForIdle()
                homeList.fling(Direction.UP)
                device.waitForIdle()
            }
        }
    }
}