package com.example.retrofitcrudapp

import android.os.SystemClock
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Solo GET: comprueba las URL reales desde Android sin modificar registros. */
@RunWith(AndroidJUnit4::class)
class OnlineReadTest {
    private fun openOnlineList(buttonId: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val prefs = instrumentation.targetContext.getSharedPreferences(EscuelaClient.PREFS, 0)
        val previous = listOf("alumno_url", "profesor_url").associateWith { prefs.getString(it, null) }
        prefs.edit().putString("alumno_url", EscuelaClient.ALUMNOS_URL)
            .putString("profesor_url", EscuelaClient.PROFESORES_URL).commit()
        try {
            ActivityScenario.launch(MenuActivity::class.java).use {
                onView(withId(buttonId)).perform(click())
                val deadline = SystemClock.elapsedRealtime() + 30000
                var status = ""
                while (SystemClock.elapsedRealtime() < deadline) {
                    instrumentation.runOnMainSync {
                        val activity = ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED).firstOrNull()
                        status = activity?.findViewById<TextView>(R.id.status)?.text?.toString().orEmpty()
                    }
                    if (status.contains("registros.") || status == instrumentation.targetContext.getString(R.string.sin_registros)) return
                    SystemClock.sleep(150)
                }
                fail("El listado no cargó: $status")
            }
        } finally {
            val editor = prefs.edit()
            previous.forEach { (key, value) -> if (value == null) editor.remove(key) else editor.putString(key, value) }
            editor.commit()
        }
    }

    @Test fun alumnosButtonLoadsRealApi() = openOnlineList(R.id.alumnosButton)
    @Test fun profesoresButtonLoadsRealApi() = openOnlineList(R.id.profesoresButton)

    @Test fun suppliedApisAreReadableFromAndroid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val api = EscuelaClient.create(context)
        for (url in listOf(EscuelaClient.ALUMNOS_URL, EscuelaClient.PROFESORES_URL)) {
            val response = api.listar(url).execute()
            assertTrue("$url devolvió HTTP ${response.code()}", response.isSuccessful)
            assertNotNull("$url no devolvió una lista JSON", response.body())
        }
    }
}
