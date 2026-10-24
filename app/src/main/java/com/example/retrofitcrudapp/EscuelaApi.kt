package com.example.retrofitcrudapp

import retrofit2.Call
import retrofit2.http.*

interface EscuelaApi {
    @GET
    fun listar(@Url url: String): Call<List<Persona>>

    @GET
    fun obtener(@Url url: String): Call<Persona>

    @POST
    fun crear(@Url url: String, @Body datos: PersonaDatos): Call<Persona>

    @PUT
    fun actualizar(@Url url: String, @Body datos: PersonaDatos): Call<Persona>

    @DELETE
    fun eliminar(@Url url: String): Call<Void>
}
