package com.example.retrofitcrudapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.retrofitcrudapp.databinding.ActivityGithubBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GithubActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGithubBinding
    private val repositoryAdapter = RepositoryAdapter()
    private var request: Call<List<GithubRepository>>? = null
    private var username = ""
    private var nextPage = 1
    private var hasMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGithubBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.repositories.layoutManager = LinearLayoutManager(this)
        binding.repositories.adapter = repositoryAdapter
        binding.loadMore.setOnClickListener { loadPage() }
        binding.searchUsername.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?) = true
            override fun onQueryTextSubmit(query: String?): Boolean {
                request?.cancel()
                request = null
                setLoading(false)
                repositoryAdapter.clear()
                hasMore = false
                binding.loadMore.visibility = View.GONE
                val candidate = query.orEmpty().trim()
                if (!candidate.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")) || candidate.length > 39) {
                    binding.status.text = getString(R.string.invalid_username)
                    return true
                }
                username = candidate
                nextPage = 1
                binding.searchUsername.clearFocus()
                loadPage()
                return true
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.loadMore.isEnabled = !loading
    }

    private fun loadPage() {
        setLoading(true)
        binding.status.text = getString(R.string.loading_repositories, username)
        val call = GithubClient.instance.getRepositories(username, nextPage)
        request = call
        call.enqueue(object : Callback<List<GithubRepository>> {
            override fun onResponse(call: Call<List<GithubRepository>>, response: Response<List<GithubRepository>>) {
                if (request !== call || isDestroyed) return
                setLoading(false)
                val repositories = response.body()
                if (response.isSuccessful && repositories != null) {
                    repositoryAdapter.append(repositories)
                    nextPage++
                    hasMore = response.headers()["Link"]?.contains("rel=\"next\"") == true
                    binding.loadMore.setText(R.string.load_more)
                    binding.loadMore.visibility = if (hasMore) View.VISIBLE else View.GONE
                    binding.status.text = if (repositoryAdapter.itemCount == 0) {
                        getString(R.string.no_repositories, username)
                    } else {
                        getString(R.string.repository_count, repositoryAdapter.itemCount, username)
                    }
                } else {
                    binding.status.text = getString(when (response.code()) {
                        404 -> R.string.user_not_found
                        403, 429 -> R.string.github_limit
                        else -> R.string.github_error
                    })
                    showRetry()
                }
            }

            override fun onFailure(call: Call<List<GithubRepository>>, t: Throwable) {
                if (call.isCanceled || request !== call || isDestroyed) return
                setLoading(false)
                binding.status.setText(R.string.network_error)
                showRetry()
            }
        })
    }

    private fun showRetry() {
        binding.loadMore.setText(R.string.retry)
        binding.loadMore.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        request?.cancel()
        super.onDestroy()
    }
}
