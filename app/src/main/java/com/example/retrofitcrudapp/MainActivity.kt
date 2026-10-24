package com.example.retrofitcrudapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.retrofitcrudapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dogAdapter: DogAdapter
    private var images: MutableList<String> = ArrayList()
    private var request: Call<DogsResponse?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el diseño de la actividad y establecerlo como el contenido de la vista
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initRecyclerView()
        binding.searchDogs.setOnQueryTextListener(this)
        binding.openGithub.setOnClickListener {
            startActivity(Intent(this, GithubActivity::class.java))
        }
    }

    private fun initRecyclerView() {
        dogAdapter = DogAdapter(images)
        binding.listDogs.layoutManager = LinearLayoutManager(this)
        binding.listDogs.adapter = dogAdapter
    }

    private fun searchByName(raza: String) {
        request?.cancel()
        val batch: Call<DogsResponse?>? = RetrofitClient.instance.getDogsByBreed(raza)
        request = batch
        images.clear()
        dogAdapter.notifyDataSetChanged()
        batch?.enqueue(object : Callback<DogsResponse?> {
            override fun onResponse(call: Call<DogsResponse?>, response: Response<DogsResponse?>) {
                if (request !== call || isDestroyed) return
                if (response.isSuccessful && response.body()?.getStatus() == "success") {
                    val responseImages = response.body()?.getImages()?.filterNotNull() ?: emptyList()
                    images.clear()
                    images.addAll(responseImages)
                    dogAdapter.notifyDataSetChanged()
                    if (images.isEmpty()) Toast.makeText(this@MainActivity, R.string.no_dogs, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, R.string.dog_error, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DogsResponse?>, t: Throwable) {
                if (!call.isCanceled && request === call && !isDestroyed) showError()
            }
        })
    }

    private fun showError() {
        Toast
            .makeText(this, R.string.network_error, Toast.LENGTH_SHORT)
            .show()
    }

    override fun onQueryTextChange(query: String?): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        val breed = query.orEmpty().trim().lowercase(Locale.ROOT)
        if (breed.isNotEmpty()) {
            searchByName(breed)
            binding.searchDogs.clearFocus()
        }
        return true
    }

    override fun onDestroy() {
        request?.cancel()
        super.onDestroy()
    }
}
