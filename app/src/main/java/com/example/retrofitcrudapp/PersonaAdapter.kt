package com.example.retrofitcrudapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.retrofitcrudapp.databinding.ItemPersonaBinding

class PersonaAdapter(private val onClick: (Persona) -> Unit) : RecyclerView.Adapter<PersonaAdapter.Holder>() {
    private var items: List<Persona> = emptyList()
    fun submit(personas: List<Persona>) {
        val old = items.size
        items = emptyList()
        notifyItemRangeRemoved(0, old)
        items = personas
        notifyItemRangeInserted(0, items.size)
    }
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemPersonaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    inner class Holder(private val binding: ItemPersonaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(persona: Persona) {
            binding.nombre.text = listOfNotNull(persona.nombre, persona.apellido).joinToString(" ")
            binding.edad.text = persona.edadValida()?.let { itemView.context.getString(R.string.edad_valor, it) }
                ?: itemView.context.getString(R.string.edad_desconocida)
            binding.identifier.text = itemView.context.getString(R.string.id_valor, persona.id.orEmpty())
            binding.root.setOnClickListener { onClick(persona) }
        }
    }
}
