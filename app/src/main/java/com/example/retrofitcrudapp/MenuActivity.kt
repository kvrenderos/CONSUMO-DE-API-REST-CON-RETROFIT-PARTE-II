package com.example.retrofitcrudapp

import android.content.Intent
import android.os.Bundle
import com.example.retrofitcrudapp.databinding.ActivityMenuBinding

class MenuActivity : PantallaActivity() {
    private lateinit var binding: ActivityMenuBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        mostrar(binding.root)
        binding.alumnosButton.setOnClickListener { abrir(Entidad.ALUMNO) }
        binding.profesoresButton.setOnClickListener { abrir(Entidad.PROFESOR) }
        binding.configButton.setOnClickListener { startActivity(Intent(this, ConfiguracionActivity::class.java)) }
        binding.dogsButton.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        binding.githubButton.setOnClickListener { startActivity(Intent(this, GithubActivity::class.java)) }
    }
    override fun onResume() {
        super.onResume()
        binding.apiUrl.text = getString(R.string.api_actual,
            EscuelaClient.resourceUrl(this, Entidad.ALUMNO), EscuelaClient.resourceUrl(this, Entidad.PROFESOR))
    }
    private fun abrir(entidad: Entidad) {
        startActivity(Intent(this, PersonasActivity::class.java).putExtra("entidad", entidad.recurso))
    }
}
