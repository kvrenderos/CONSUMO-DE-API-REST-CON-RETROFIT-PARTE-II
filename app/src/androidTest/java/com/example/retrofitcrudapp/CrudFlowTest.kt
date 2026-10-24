package com.example.retrofitcrudapp

import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Matcher
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Requiere api_local/servidor.py iniciado en el host, puerto 8765. */
@RunWith(AndroidJUnit4::class)
class CrudFlowTest {
    private var previousUrls = mapOf<String, String?>()
    @Before fun useLocalApi() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(EscuelaClient.PREFS, 0)
        previousUrls = listOf("alumno_url", "profesor_url").associateWith { prefs.getString(it, null) }
        prefs.edit()
            .putString("alumno_url", EscuelaClient.localUrl(Entidad.ALUMNO))
            .putString("profesor_url", EscuelaClient.localUrl(Entidad.PROFESOR)).commit()
    }

    @After fun restoreUrls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val editor = context.getSharedPreferences(EscuelaClient.PREFS, 0).edit()
        previousUrls.forEach { (key, value) ->
            if (value == null) editor.remove(key) else editor.putString(key, value)
        }
        editor.commit()
    }

    private fun awaitView(id: Int, matcher: Matcher<View>) {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            var ready = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).firstOrNull()
                val view = activity?.findViewById<View>(id)
                ready = view != null && matcher.matches(view)
            }
            if (ready) return
            SystemClock.sleep(150)
        }
        onView(withId(id)).check(matches(matcher))
    }

    private fun cycle(resource: String, menuButton: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val api = EscuelaClient.create(context)
        val collection = EscuelaClient.resourceUrl(context, Entidad.from(resource))
        val nombre = "Prueba" + UUID.randomUUID().toString().take(8)
        val apellido = "Guía Once"
        val updated = nombre + " Editado"
        try {
            ActivityScenario.launch(MenuActivity::class.java).use {
                onView(withId(menuButton)).perform(click())
                awaitView(R.id.addButton, isEnabled())
                onView(withId(R.id.addButton)).perform(click())
                onView(withId(R.id.saveButton)).perform(scrollTo(), click())
                onView(withId(R.id.edadInput)).check(matches(hasErrorText(context.getString(R.string.edad_invalida))))
                onView(withId(R.id.nombreInput)).perform(scrollTo(), replaceText(nombre))
                onView(withId(R.id.apellidoInput)).perform(scrollTo(), replaceText(apellido))
                onView(withId(R.id.edadInput)).perform(scrollTo(), replaceText("24"))
                closeSoftKeyboard()
                onView(withId(R.id.saveButton)).perform(scrollTo(), click())
                awaitView(R.id.addButton, isEnabled())
                onView(withText("$nombre $apellido")).perform(click())
                onView(withText(R.string.modificar)).perform(click())
                awaitView(R.id.saveButton, isEnabled())
                onView(withId(R.id.nombreInput)).check(matches(withText(nombre)))
                onView(withId(R.id.nombreInput)).perform(scrollTo(), replaceText(updated))
                onView(withId(R.id.edadInput)).perform(scrollTo(), replaceText("25"))
                closeSoftKeyboard()
                onView(withId(R.id.saveButton)).perform(scrollTo(), click())
                awaitView(R.id.addButton, isEnabled())
                onView(withText("$updated $apellido")).perform(click())
                onView(withText(R.string.eliminar)).perform(click())
                // Cancelar no debe borrar el registro.
                onView(withText(R.string.cancelar)).perform(click())
                onView(withText("$updated $apellido")).check(matches(isDisplayed()))
                onView(withText("$updated $apellido")).perform(click())
                onView(withText(R.string.eliminar)).perform(click())
                onView(withId(android.R.id.button1)).perform(click())
                awaitView(R.id.addButton, isEnabled())
                assertFalse(api.listar(collection).execute().body()!!.any { persona -> persona.nombre == updated })
            }
        } finally {
            // Limpieza limitada a los registros únicos que crea esta prueba local.
            api.listar(collection).execute().body().orEmpty()
                .filter { it.nombre == nombre || it.nombre == updated }
                .forEach { api.eliminar(EscuelaClient.recordUrl(collection, it.id!!)).execute() }
        }
    }

    @Test fun alumnosCreateEditCancelDeleteAndDelete() = cycle("alumno", R.id.alumnosButton)
    @Test fun profesoresCreateEditCancelDeleteAndDelete() = cycle("profesor", R.id.profesoresButton)
}
