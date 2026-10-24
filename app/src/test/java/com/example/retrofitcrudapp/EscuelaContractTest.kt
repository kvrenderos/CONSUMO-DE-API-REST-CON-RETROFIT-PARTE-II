package com.example.retrofitcrudapp

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EscuelaContractTest {
    private val requests = mutableListOf<Request>()
    private fun api(): EscuelaApi {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            val json = """{"id":"42","nombre":"Ana","apellido":"Pérez","edad":24}"""
            val body = when {
                request.method == "DELETE" -> ""
                request.method == "GET" && request.url.pathSegments.last() != "42" -> "[$json]"
                else -> json
            }
            okhttp3.Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(if (request.method == "DELETE") 204 else 200).message("OK")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return Retrofit.Builder().baseUrl("https://example.test/api/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(EscuelaApi::class.java)
    }

    @Test fun getListAndDetailForBothResources() {
        val api = api()
        for (resource in listOf("alumno", "profesor")) {
            assertEquals("42", api.listar("https://example.test/api/escuela/$resource").execute().body()!!.single().id)
            assertEquals("42", api.obtener("https://example.test/api/escuela/$resource/42").execute().body()!!.id)
            assertEquals("/api/escuela/$resource/42", requests.last().url.encodedPath)
            assertEquals("GET", requests.last().method)
        }
    }

    @Test fun postSendsDataWithoutId() {
        val api = api()
        for (resource in listOf("alumno", "profesor")) {
            api.crear("https://example.test/api/escuela/$resource", PersonaDatos("Ana", "Pérez", 24)).execute()
            val request = requests.last()
            assertEquals("POST", request.method)
            assertEquals("/api/escuela/$resource", request.url.encodedPath)
            val buffer = Buffer()
            request.body!!.writeTo(buffer)
            val json = JsonParser().parse(buffer.readUtf8()).asJsonObject
            assertFalse(json.has("id"))
            assertEquals(24, json["edad"].asInt)
            assertEquals("Ana", json["nombre"].asString)
        }
    }

    @Test fun putUsesSelectedId() {
        val api = api()
        for (resource in listOf("alumno", "profesor")) {
            api.actualizar("https://example.test/api/escuela/$resource/42", PersonaDatos("Ana", "Pérez", 25)).execute()
            assertEquals("PUT", requests.last().method)
            assertEquals("/api/escuela/$resource/42", requests.last().url.encodedPath)
        }
    }

    @Test fun deleteUsesDeleteAndAccepts204() {
        val api = api()
        for (resource in listOf("alumno", "profesor")) {
            val response = api.eliminar("https://example.test/api/escuela/$resource/42").execute()
            assertEquals(204, response.code())
            assertTrue(response.isSuccessful)
            assertNull(response.body())
            assertEquals("DELETE", requests.last().method)
            assertNull(requests.last().body)
        }
    }

    @Test fun malformedRemoteAgeDoesNotBreakTheList() {
        val gson = Gson()
        val malformed = gson.fromJson("""{"id":"1","nombre":"A","apellido":"B","edad":"edad 1"}""", Persona::class.java)
        val valid = gson.fromJson("""{"id":2,"nombre":"A","apellido":"B","edad":24}""", Persona::class.java)
        assertNull(malformed.edadValida())
        assertEquals(24, valid.edadValida())
        assertEquals("2", valid.id)
    }

    @Test fun urlValidationMatchesNetworkPolicy() {
        assertEquals("https://example.com/api", EscuelaClient.normalizarUrl(" https://example.com/api "))
        assertNotNull(EscuelaClient.normalizarUrl("http://10.0.2.2:8765/api/"))
        assertNull(EscuelaClient.normalizarUrl("http://example.com/api/"))
        assertNull(EscuelaClient.normalizarUrl("https://user:secret@example.com/api/"))
        assertNull(EscuelaClient.normalizarUrl("https://example.com/api/?token=x"))
    }

    @Test fun suppliedCollectionsCanUseDifferentHostsAndPaths() {
        val api = api()
        api.listar(EscuelaClient.ALUMNOS_URL).execute()
        api.listar(EscuelaClient.PROFESORES_URL).execute()
        assertEquals("66240d4504457d4aaf9b8530.mockapi.io", requests[0].url.host)
        assertEquals("/api/escuela/alumno", requests[0].url.encodedPath)
        assertEquals("6ad813a395e98f36aef2ea16.mockapi.io", requests[1].url.host)
        assertEquals("/profesor", requests[1].url.encodedPath)
        assertEquals(EscuelaClient.PROFESORES_URL + "/42", EscuelaClient.recordUrl(EscuelaClient.PROFESORES_URL, "42"))
    }
}
