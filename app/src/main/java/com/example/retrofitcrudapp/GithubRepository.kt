package com.example.retrofitcrudapp

import com.google.gson.annotations.SerializedName

data class GithubRepository(
    val id: Long,
    val name: String,
    val description: String?,
    val language: String?,
    @SerializedName("stargazers_count") val stars: Int
)
