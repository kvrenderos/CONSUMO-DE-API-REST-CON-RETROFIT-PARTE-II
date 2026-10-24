package com.example.retrofitcrudapp

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.retrofitcrudapp.databinding.ItemDogBinding
import com.squareup.picasso.Picasso

class DogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    // Declaración de una instancia de ItemDogBinding para acceder a las vistas en el diseño del elemento
    private val itemDogBinding: ItemDogBinding = ItemDogBinding.bind(view)

    // Vincula la URL de la imagen al ImageView en el diseño del elemento
    fun bind(imageUrl: String?) {
        // Carga la imagen desde la URL usando Picasso y la muestra en el ImageView ivDog
        Picasso.get().load(imageUrl).into(itemDogBinding.ivDog)
    }
}
