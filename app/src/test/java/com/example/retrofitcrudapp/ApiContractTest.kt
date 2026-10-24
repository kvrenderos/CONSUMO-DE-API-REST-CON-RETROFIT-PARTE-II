package com.example.retrofitcrudapp

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Interceptor local: comprueba serialización y peticiones sin depender de Internet. */
class ApiContractTest {
    @Test fun githubUsesUserPathPaginationAndHeaders() {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("/users/octocat/repos", request.url.encodedPath)
            assertEquals("2", request.url.queryParameter("page"))
            assertEquals("30", request.url.queryParameter("per_page"))
            assertEquals("full_name", request.url.queryParameter("sort"))
            assertEquals("application/vnd.github+json", request.header("Accept"))
            okhttp3.Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(
                    """[{"id":1,"name":"Hello-World","description":null,"language":null,"stargazers_count":8}]""".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://api.github.com/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(GithubApiService::class.java)
        val repository = api.getRepositories("octocat", 2).execute().body()!!.single()
        assertEquals("Hello-World", repository.name)
        assertEquals(8, repository.stars)
        assertNull(repository.description)
        assertNull(repository.language)
    }

    @Test fun github404IsAnHttpErrorNotARepositoryList() {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(404).message("Not Found").body(
                    """{"message":"Not Found"}""".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://api.github.com/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(GithubApiService::class.java)
        val response = api.getRepositories("missing").execute()
        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
        assertNull(response.body())
        response.errorBody()?.close()
    }

    @Test fun dogResponseMapsMessageAndFiltersNullImages() {
        val result = Gson().fromJson(
            """{"status":"success","message":["https://example.com/dog.jpg",null]}""",
            DogsResponse::class.java
        )
        assertEquals("success", result.getStatus())
        assertEquals(listOf("https://example.com/dog.jpg"), result.getImages()?.filterNotNull())
    }
}
