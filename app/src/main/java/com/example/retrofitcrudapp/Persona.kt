package com.example.retrofitcrudapp

/** MockAPI entrega IDs como texto y algunas edades del servidor de la guía no son numéricas. */
data class Persona(val id: String?, val nombre: String?, val apellido: String?, val edad: String?) {
    fun edadValida(): Int? = edad?.toIntOrNull()?.takeIf { it in 1..120 }
}

// Ambas entidades de la guía comparten exactamente estos cuatro campos.
typealias Alumno = Persona
typealias Profesor = Persona

/** No se envía el ID en POST/PUT: lo administra el servidor. */
data class PersonaDatos(val nombre: String, val apellido: String, val edad: Int)

enum class Entidad(val recurso: String, val titulo: Int) {
    ALUMNO("alumno", R.string.alumnos), PROFESOR("profesor", R.string.profesores);

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.recurso == value } ?: ALUMNO
    }
}
