package com.example.retrofitcrudapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DogAdapter(private val images: List<String>?) : RecyclerView.Adapter<DogViewHolder>() {
    // Se llama cuando RecyclerView necesita un nuevo ViewHolder para representar un elemento
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DogViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dog, parent, false)
        return DogViewHolder(view)
    }

    // Se llama para mostrar los datos en una posición específica
    override fun onBindViewHolder(holder: DogViewHolder, position: Int) {
        holder.bind(images!![position])
    }

    // Devuelve el número total de elementos en el conjunto de datos
    override fun getItemCount(): Int {
        return images?.size ?: 0
    }
}