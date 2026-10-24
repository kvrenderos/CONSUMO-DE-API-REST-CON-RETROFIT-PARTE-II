package com.example.retrofitcrudapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.retrofitcrudapp.databinding.ActivityPersonasBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PersonasActivity : PantallaActivity() {
    private lateinit var binding: ActivityPersonasBinding
    private lateinit var adapter: PersonaAdapter
    private lateinit var entidad: Entidad
    private lateinit var api: EscuelaApi
    private var request: Call<*>? = null
    private var busy = false
    private lateinit var collectionUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonasBinding.inflate(layoutInflater)
        mostrar(binding.root)
        entidad = Entidad.from(intent.getStringExtra("entidad"))
        collectionUrl = EscuelaClient.resourceUrl(this, entidad)
        binding.title.setText(entidad.titulo)
        adapter = PersonaAdapter { persona -> if (!busy) opciones(persona) }
        binding.personas.layoutManager = LinearLayoutManager(this)
        binding.personas.adapter = adapter
        binding.backButton.setOnClickListener { finish() }
        binding.addButton.setOnClickListener { editar(null) }
        binding.refreshButton.setOnClickListener { cargar() }
    }

    override fun onResume() {
        super.onResume()
        api = EscuelaClient.create(this)
        cargar()
    }

    private fun loading(value: Boolean) {
        busy = value
        binding.progress.visibility = if (value) View.VISIBLE else View.GONE
        binding.addButton.isEnabled = !value
        binding.refreshButton.isEnabled = !value
    }

    private fun cargar() {
        request?.cancel()
        loading(true)
        binding.status.setText(R.string.cargando_datos)
        val call = api.listar(collectionUrl)
        request = call
        call.enqueue(object : Callback<List<Persona>> {
            override fun onResponse(call: Call<List<Persona>>, response: Response<List<Persona>>) {
                if (request !== call || isDestroyed) return
                loading(false)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    adapter.submit(body)
                    binding.status.text = if (body.isEmpty()) getString(R.string.sin_registros)
                        else getString(R.string.registros_cargados, body.size)
                } else binding.status.text = getString(R.string.http_error, response.code())
            }
            override fun onFailure(call: Call<List<Persona>>, t: Throwable) {
                if (call.isCanceled || request !== call || isDestroyed) return
                loading(false)
                binding.status.setText(R.string.escuela_connection_error)
            }
        })
    }

    private fun opciones(persona: Persona) {
        if (persona.id.isNullOrBlank()) {
            binding.status.setText(R.string.id_invalido)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(listOfNotNull(persona.nombre, persona.apellido).joinToString(" "))
            .setItems(arrayOf(getString(R.string.modificar), getString(R.string.eliminar))) { _, index ->
                if (index == 0) editar(persona.id) else confirmarEliminar(persona)
            }.setNegativeButton(R.string.cancelar, null).show()
    }

    private fun editar(id: String?) {
        startActivity(Intent(this, EditarPersonaActivity::class.java)
            .putExtra("entidad", entidad.recurso).putExtra("persona_id", id))
    }

    private fun confirmarEliminar(persona: Persona) {
        AlertDialog.Builder(this).setTitle(R.string.confirmar_eliminar)
            .setMessage(getString(R.string.eliminar_mensaje, persona.nombre.orEmpty(), persona.apellido.orEmpty()))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ -> eliminar(persona.id!!) }.show()
    }

    private fun eliminar(id: String) {
        request?.cancel()
        loading(true)
        val call = api.eliminar(EscuelaClient.recordUrl(collectionUrl, id))
        request = call
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (request !== call || isDestroyed) return
                loading(false)
                if (response.isSuccessful) cargar()
                else binding.status.text = getString(R.string.http_error, response.code())
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                if (call.isCanceled || request !== call || isDestroyed) return
                loading(false)
                binding.status.setText(R.string.mutacion_no_confirmada)
            }
        })
    }

    override fun onDestroy() {
        request?.cancel()
        super.onDestroy()
    }
}
