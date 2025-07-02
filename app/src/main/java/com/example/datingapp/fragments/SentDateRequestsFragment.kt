package com.example.datingapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.R
import com.example.datingapp.adapters.SentDateRequestsAdapter
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.DateRequestResponse
import com.example.datingapp.utils.SwipeToDeleteCallback
import com.yourpackage.yourapp.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class SentDateRequestsFragment : Fragment() {

    private val TAG = "SentRequestsFragment"

    private lateinit var recyclerViewSentRequests: RecyclerView
    private lateinit var adapterSentRequests: SentDateRequestsAdapter
    private val sentRequestsList: MutableList<DateRequestResponse> = mutableListOf()

    private lateinit var emptyStateTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sent_date_requests, container, false)

        recyclerViewSentRequests = view.findViewById(R.id.recyclerViewSentDateRequests)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        progressBar = view.findViewById(R.id.progressBar)
        backButton = view.findViewById(R.id.backButton)

        setupRecyclerView()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchSentDateRequests()

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        recyclerViewSentRequests.layoutManager = LinearLayoutManager(context)
        adapterSentRequests = SentDateRequestsAdapter(
            sentRequestsList,
            onItemClick = { request ->
                openDateRequestDetailFragment(request)
            },
            onCancelClick = { request ->
                updateSentRequestStatus(request, "cancelled")
            }
        )
        recyclerViewSentRequests.adapter = adapterSentRequests

        val swipeToDeleteSentRequestsCallback =
            SwipeToDeleteCallback(adapterSentRequests) { position ->
                val requestIdToDelete = sentRequestsList[position].id
                deleteSentDateRequest(requestIdToDelete, position)
            }
        val sentRequestsTouchHelper = ItemTouchHelper(swipeToDeleteSentRequestsCallback)
        sentRequestsTouchHelper.attachToRecyclerView(recyclerViewSentRequests)
    }

    private fun fetchSentDateRequests() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                showLoading(true)
            }
            val sessionManager = SessionManager(requireContext())
            val authToken = sessionManager.getAuthToken()

            if (authToken.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT)
                        .show()
                    showLoading(false)
                }
                return@launch
            }
            Log.d(TAG, "Fetching sent date requests...")

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getSentDateRequests("Token $authToken")
                }
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val requests = response.body()!!
                        sentRequestsList.clear()
                        sentRequestsList.addAll(requests)
                        adapterSentRequests.notifyDataSetChanged()
                        Log.d(TAG, "Fetched ${requests.size} sent date requests successfully.")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage =
                            "Failed to fetch sent date requests: ${response.code()}" +
                                    (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Failed to load sent date requests.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Network error: Please check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error: ${e.code()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Server error: ${e.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "An unexpected error occurred: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    updateEmptyState()
                }
            }
        }
    }

    private fun deleteSentDateRequest(requestId: Int, position: Int) {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
            adapterSentRequests.notifyItemChanged(position)
            return
        }

        Log.d(TAG, "Attempting to delete sent date request with ID: $requestId at position $position")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.deleteDateRequest(requestId, "Token $authToken")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        sentRequestsList.removeAt(position)
                        adapterSentRequests.notifyItemRemoved(position)
                        Toast.makeText(context, "Sent request withdrawn!", Toast.LENGTH_SHORT)
                            .show()
                        Log.d(TAG, "Sent date request ID $requestId deleted successfully.")
                        updateEmptyState()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to withdraw request: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Failed to withdraw sent request.",
                            Toast.LENGTH_LONG
                        ).show()
                        adapterSentRequests.notifyItemChanged(position)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error withdrawing request: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Network error: Please check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                    adapterSentRequests.notifyItemChanged(position)
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error withdrawing request: ${e.code()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Server error: ${e.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                    adapterSentRequests.notifyItemChanged(position)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage =
                        "An unexpected error occurred withdrawing request: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                    adapterSentRequests.notifyItemChanged(position)
                }
            }
        }
    }

    private fun updateSentRequestStatus(request: DateRequestResponse, newStatus: String) {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "Updating sent date request status for ID: ${request.id} to $newStatus")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val updateData = mapOf("status" to newStatus)
                val response = RetrofitClient.apiService.updateDateRequest(
                    request.id,
                    "Token $authToken",
                    updateData
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Request ${newStatus}!", Toast.LENGTH_SHORT).show()
                        // Find and update the status in the local list
                        val index = sentRequestsList.indexOfFirst { it.id == request.id }
                        if (index != -1) {
                            val updatedRequest = sentRequestsList[index].copy(status = newStatus)
                            sentRequestsList[index] = updatedRequest
                            adapterSentRequests.notifyItemChanged(index)
                        }
                        Log.d(
                            TAG,
                            "Sent date request ID ${request.id} status updated to $newStatus."
                        )
                        updateEmptyState()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to update status: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Failed to update request status.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error updating status: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Network error: Please check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error: ${e.code()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Server error: ${e.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                val errorMessage = "An unexpected error occurred: ${e.message}"
                Log.e(TAG, errorMessage, e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerViewSentRequests.visibility = if (isLoading) View.GONE else View.VISIBLE
        emptyStateTextView.visibility = if (isLoading) View.GONE else View.GONE
    }

    private fun updateEmptyState() {
        if (sentRequestsList.isEmpty()) {
            emptyStateTextView.visibility = View.VISIBLE
            recyclerViewSentRequests.visibility = View.GONE
            emptyStateTextView.text = "You haven't sent any date requests yet."
            Log.d(TAG, "Empty state: Showing empty message for Sent Date Requests.")
        } else {
            emptyStateTextView.visibility = View.GONE
            recyclerViewSentRequests.visibility = View.VISIBLE
            Log.d(TAG, "Empty state: Hiding empty message, showing Sent Date Requests list.")
        }
    }

    private fun openDateRequestDetailFragment(dateRequest: DateRequestResponse) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DateRequestDetailFragment.newInstance(dateRequest))
            .addToBackStack(null)
            .commit()
    }
}