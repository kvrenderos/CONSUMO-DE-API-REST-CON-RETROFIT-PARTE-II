package com.example.retrofitcrudapp

import android.os.Bundle
import com.example.retrofitcrudapp.databinding.ActivityConfiguracionBinding

class ConfiguracionActivity : PantallaActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        mostrar(binding.root)
        binding.alumnosUrlInput.setText(EscuelaClient.resourceUrl(this, Entidad.ALUMNO))
        binding.profesoresUrlInput.setText(EscuelaClient.resourceUrl(this, Entidad.PROFESOR))
        binding.guideButton.setOnClickListener {
            binding.alumnosUrlInput.setText(EscuelaClient.ALUMNOS_URL)
            binding.profesoresUrlInput.setText(EscuelaClient.PROFESORES_URL)
        }
        binding.localButton.setOnClickListener {
            binding.alumnosUrlInput.setText(EscuelaClient.localUrl(Entidad.ALUMNO))
            binding.profesoresUrlInput.setText(EscuelaClient.localUrl(Entidad.PROFESOR))
        }
        binding.backButton.setOnClickListener { finish() }
        binding.saveConfig.setOnClickListener {
            val alumnos = EscuelaClient.normalizarUrl(binding.alumnosUrlInput.text.toString())
            val profesores = EscuelaClient.normalizarUrl(binding.profesoresUrlInput.text.toString())
            binding.alumnosUrlInput.error = if (alumnos == null) getString(R.string.url_invalida) else null
            binding.profesoresUrlInput.error = if (profesores == null) getString(R.string.url_invalida) else null
            if (alumnos == null || profesores == null) {
                return@setOnClickListener
            } else {
                getSharedPreferences(EscuelaClient.PREFS, MODE_PRIVATE).edit()
                    .putString("alumno_url", alumnos).putString("profesor_url", profesores).apply()
                finish()
            }
        }
    }
}
