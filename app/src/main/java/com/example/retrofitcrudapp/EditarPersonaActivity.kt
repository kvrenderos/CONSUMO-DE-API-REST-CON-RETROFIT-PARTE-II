package com.example.retrofitcrudapp

import android.os.Bundle
import android.view.View
import com.example.retrofitcrudapp.databinding.ActivityEditarPersonaBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditarPersonaActivity : PantallaActivity() {
    private lateinit var binding: ActivityEditarPersonaBinding
    private lateinit var api: EscuelaApi
    private lateinit var entidad: Entidad
    private var id: String? = null
    private var request: Call<Persona>? = null
    private var busy = false
    private var loaded = false
    private lateinit var collectionUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPersonaBinding.inflate(layoutInflater)
        mostrar(binding.root)
        entidad = Entidad.from(intent.getStringExtra("entidad"))
        collectionUrl = EscuelaClient.resourceUrl(this, entidad)
        id = intent.getStringExtra("persona_id")
        api = EscuelaClient.create(this)
        binding.title.text = getString(if (id == null) R.string.crear_persona else R.string.editar_persona, getString(entidad.titulo))
        binding.cancelButton.setOnClickListener { finish() }
        binding.saveButton.setOnClickListener { guardar() }
        binding.retryButton.setOnClickListener { obtener() }
        loaded = id == null || savedInstanceState?.getBoolean("loaded") == true
        if (!loaded) obtener()
    }

    private fun loading(value: Boolean) {
        busy = value
        binding.progress.visibility = if (value) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !value
        binding.cancelButton.isEnabled = !value
        binding.nombreInput.isEnabled = !value
        binding.apellidoInput.isEnabled = !value
        binding.edadInput.isEnabled = !value
        binding.retryButton.visibility = View.GONE
    }

    private fun obtener() {
        val identifier = id ?: return
        loading(true)
        val call = api.obtener(EscuelaClient.recordUrl(collectionUrl, identifier))
        request = call
        call.enqueue(object : Callback<Persona> {
            override fun onResponse(call: Call<Persona>, response: Response<Persona>) {
                if (request !== call || isDestroyed) return
                loading(false)
                val persona = response.body()
                if (response.isSuccessful && persona != null) {
                    loaded = true
                    binding.nombreInput.setText(persona.nombre.orEmpty())
                    binding.apellidoInput.setText(persona.apellido.orEmpty())
                    binding.edadInput.setText(persona.edadValida()?.toString().orEmpty())
                    if (persona.edadValida() == null) binding.status.setText(R.string.edad_desconocida)
                    else binding.status.text = ""
                } else errorCarga(getString(R.string.http_error, response.code()))
            }
            override fun onFailure(call: Call<Persona>, t: Throwable) {
                if (!call.isCanceled && request === call && !isDestroyed) {
                    loading(false)
                    errorCarga(getString(R.string.escuela_connection_error))
                }
            }
        })
    }

    private fun errorCarga(message: String) {
        binding.status.text = message
        binding.saveButton.isEnabled = false
        binding.retryButton.visibility = View.VISIBLE
    }

    private fun guardar() {
        if (busy) return
        val nombre = binding.nombreInput.text.toString().trim()
        val apellido = binding.apellidoInput.text.toString().trim()
        val edad = binding.edadInput.text.toString().toIntOrNull()
        binding.nombreInput.error = null
        binding.apellidoInput.error = null
        binding.edadInput.error = null
        var valid = true
        if (nombre.isBlank() || nombre.length > 100) {
            binding.nombreInput.error = getString(R.string.nombre_invalido); valid = false
        }
        if (apellido.isBlank() || apellido.length > 100) {
            binding.apellidoInput.error = getString(R.string.nombre_invalido); valid = false
        }
        if (edad == null || edad !in 1..120) {
            binding.edadInput.error = getString(R.string.edad_invalida); valid = false
        }
        if (!valid) return
        loading(true)
        binding.status.setText(R.string.guardando)
        val datos = PersonaDatos(nombre, apellido, edad!!)
        val call = id?.let { api.actualizar(EscuelaClient.recordUrl(collectionUrl, it), datos) } ?: api.crear(collectionUrl, datos)
        request = call
        call.enqueue(object : Callback<Persona> {
            override fun onResponse(call: Call<Persona>, response: Response<Persona>) {
                if (request !== call || isDestroyed) return
                loading(false)
                if (response.isSuccessful && !response.body()?.id.isNullOrBlank()) {
                    setResult(RESULT_OK)
                    finish()
                } else binding.status.text = getString(R.string.http_error, response.code())
            }
            override fun onFailure(call: Call<Persona>, t: Throwable) {
                if (!call.isCanceled && request === call && !isDestroyed) {
                    loading(false)
                    binding.status.setText(R.string.mutacion_no_confirmada)
                    if (BuildConfig.DEBUG) android.util.Log.w("Escuela", "No se confirmó el guardado", t)
                }
            }
        })
    }

    override fun onDestroy() {
        request?.cancel()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("loaded", loaded)
        super.onSaveInstanceState(outState)
    }
}
