// fragments/UserInteractionListFragment.kt

package com.example.datingapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.R
import com.example.datingapp.adapters.UserInteractionListAdapter
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.InteractionActionRequest
import com.yourpackage.yourapp.auth.SessionManager // Your SessionManager import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class UserInteractionListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserInteractionListAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView

    // Use a companion object for constant arguments and factory method
    companion object {
        private const val ARG_LIST_TYPE = "list_type"
        const val TYPE_MUTUAL = "mutual"
        const val TYPE_OUTGOING = "outgoing"
        const val TYPE_INCOMING = "incoming"

        fun newInstance(listType: String): UserInteractionListFragment {
            val fragment = UserInteractionListFragment()
            val args = Bundle()
            args.putString(ARG_LIST_TYPE, listType)
            fragment.arguments = args
            return fragment
        }
    }

    private var listType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the list type from arguments
        arguments?.let {
            listType = it.getString(ARG_LIST_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_interaction_list, container, false)

        recyclerView = view.findViewById(R.id.userInteractionRecyclerView)
        progressBar = view.findViewById(R.id.loadingProgressBar)
        emptyStateText = view.findViewById(R.id.emptyStateTextView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        // Initialize adapter with the list type and action callback
        adapter = UserInteractionListAdapter(listType ?: "", emptyList()) { userId, action ->
            performInteractionAction(userId, action)
        }
        recyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fetch data when the view is created
        fetchUserInteractions()
    }

    // Function to fetch the specific list of users based on listType
    fun fetchUserInteractions() {
        // Show loading state
        progressBar.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE
        recyclerView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sessionManager = SessionManager(requireContext())
                val authToken = sessionManager.getAuthToken()

                if (authToken.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
                        showEmptyState("Authentication failed.")
                    }
                    return@launch
                }

                // Call the appropriate API based on listType
                val response = when (listType) {
                    TYPE_MUTUAL -> RetrofitClient.apiService.getMutualMatches("Token $authToken")
                    TYPE_OUTGOING -> RetrofitClient.apiService.getOutgoingLikes("Token $authToken")
                    TYPE_INCOMING -> RetrofitClient.apiService.getIncomingLikes("Token $authToken")
                    else -> {
                        withContext(Dispatchers.Main) { showEmptyState("Invalid list type."); progressBar.visibility = View.GONE }
                        return@launch
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE // Hide loading indicator
                    if (response.isSuccessful && response.body() != null) {
                        val users = response.body()
                        if (!users.isNullOrEmpty()) {
                            adapter.updateUsers(users) // Update adapter data
                            recyclerView.visibility = View.VISIBLE
                        } else {
                            // Show appropriate empty state message
                            val emptyMessage = when (listType) {
                                TYPE_MUTUAL -> "You don't have any mutual likes yet. Keep swiping!"
                                TYPE_OUTGOING -> "You haven't liked anyone that hasn't liked you back yet."
                                TYPE_INCOMING -> "No one has liked you yet. Maybe update your profile?"
                                else -> "No data found."
                            }
                            showEmptyState(emptyMessage)
                        }
                        Log.d("UserInteractionList", "Fetched ${users?.size} items for $listType.")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch $listType: ${response.code()}" + (errorBody?.let { " - $it" } ?: "")
                        Log.e("UserInteractionList", errorMessage)
                        showEmptyState("Error loading data: ${response.code()}")
                        Toast.makeText(requireContext(), "Failed to load data.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Log.e("UserInteractionList", "Network error for $listType: ${e.message}", e)
                    showEmptyState("Network error. Please check your internet connection.")
                    Toast.makeText(requireContext(), "Network error.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    Log.e("UserInteractionList", "Server error for $listType: ${e.code()}", e)
                    showEmptyState("Server error. Please try again later.")
                    Toast.makeText(requireContext(), "Server error.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("UserInteractionList", "Unexpected error for $listType: ${e.message}", e)
                    showEmptyState("An unexpected error occurred.")
                    Toast.makeText(requireContext(), "Error loading data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performInteractionAction(userId: Int, action: String) {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required to perform action.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = InteractionActionRequest(userId, action)
                val response = RetrofitClient.apiService.performUserInteractionAction("Token $authToken", requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        (parentFragment as? UserInteractionsContainerFragment)?.refreshAllLists()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("InteractionAction", "Action '$action' failed for user $userId: ${response.code()} - $errorBody")
                        Toast.makeText(requireContext(), "Failed to $action: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("InteractionAction", "Error performing action '$action' for user $userId: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error performing action: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEmptyState(message: String) {
        emptyStateText.text = message
        emptyStateText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
}