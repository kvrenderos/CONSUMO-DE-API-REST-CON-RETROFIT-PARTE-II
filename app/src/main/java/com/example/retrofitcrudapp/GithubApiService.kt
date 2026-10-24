package com.example.retrofitcrudapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApiService {
    @Headers("Accept: application/vnd.github+json", "User-Agent: RetrofitDogApp")
    @GET("users/{username}/repos")
    fun getRepositories(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30,
        @Query("sort") sort: String = "full_name"
    ): Call<List<GithubRepository>>
}
