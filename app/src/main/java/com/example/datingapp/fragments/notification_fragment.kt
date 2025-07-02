package com.example.datingapp.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper // Import ItemTouchHelper
import com.example.datingapp.R
import com.example.datingapp.adapters.ReceivedDateRequestsAdapter
import com.example.datingapp.adapters.UserInteractionsAdapter
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.DateRequestResponse
import com.example.datingapp.models.UserInteractionNotification
import com.example.datingapp.services.MyFirebaseMessagingService
import com.example.datingapp.utils.SwipeToDeleteCallback // Import your new callback
import com.yourpackage.yourapp.auth.SessionManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class notification_fragment : Fragment() {

    private val TAG = "NotificationFragment"

    private lateinit var recyclerViewDateRequests: RecyclerView
    private lateinit var adapterDateRequests: ReceivedDateRequestsAdapter
    private val dateRequestsList: MutableList<DateRequestResponse> = mutableListOf()

    private lateinit var recyclerViewUserInteractions: RecyclerView
    private lateinit var adapterUserInteractions: UserInteractionsAdapter
    private val userInteractionsList: MutableList<UserInteractionNotification> = mutableListOf()

    private lateinit var emptyStateTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRequestDates: TextView
    private lateinit var varbtnInteractions: TextView

    private lateinit var notificationReceiver: BroadcastReceiver

    private var currentTab: TabType = TabType.REQUEST_DATES

    enum class TabType {
        REQUEST_DATES, INTERACTION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "NotificationFragment onCreate")

        notificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "Fragment: onReceive called for action: ${intent?.action}")

                when (intent?.action) {
                    MyFirebaseMessagingService.ACTION_LIKE_NOTIFICATION -> {
                        Log.d(TAG, "New like notification broadcast received. Refreshing interactions list.")
                        fetchUserInteractions()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_fragment, container, false)
        Log.d(TAG, "NotificationFragment onCreateView")

        recyclerViewDateRequests = view.findViewById(R.id.recyclerViewReceivedDateRequests)
        recyclerViewUserInteractions = view.findViewById(R.id.recyclerViewUserInteractions)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        progressBar = view.findViewById(R.id.progressBar)
        btnRequestDates = view.findViewById(R.id.btnRequestDates)
        varbtnInteractions = view.findViewById(R.id.btnInteractions)

        setupRecyclerViews()
        setupTabListeners()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "NotificationFragment onViewCreated")
        switchTab(TabType.REQUEST_DATES)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "NotificationFragment onResume. Registering BroadcastReceiver.")
        val intentFilter = IntentFilter().apply {
            addAction(MyFirebaseMessagingService.ACTION_LIKE_NOTIFICATION)
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(notificationReceiver, intentFilter)
        when (currentTab) {
            TabType.REQUEST_DATES -> fetchReceivedDateRequests()
            TabType.INTERACTION -> fetchUserInteractions()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "NotificationFragment onPause. Unregistering BroadcastReceiver.")
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(notificationReceiver)
    }

    private fun setupRecyclerViews() {
        recyclerViewDateRequests.layoutManager = LinearLayoutManager(context)
        adapterDateRequests = ReceivedDateRequestsAdapter(
            dateRequestsList,
            onItemClick = { request ->
                openDateRequestDetailFragment(request)
            },
            onAcceptClick = { request -> updateDateRequestStatus(request, "accepted") },
            onDenyClick = { request -> updateDateRequestStatus(request, "denied") }
        )
        recyclerViewDateRequests.adapter = adapterDateRequests

        val swipeToDeleteDateRequestsCallback = SwipeToDeleteCallback(adapterDateRequests) { position ->
            val requestIdToDelete = dateRequestsList[position].id
            deleteDateRequest(requestIdToDelete, position)
        }
        val dateRequestsTouchHelper = ItemTouchHelper(swipeToDeleteDateRequestsCallback)
        dateRequestsTouchHelper.attachToRecyclerView(recyclerViewDateRequests)

        recyclerViewUserInteractions.layoutManager = LinearLayoutManager(context)
        adapterUserInteractions = UserInteractionsAdapter(
            userInteractionsList,
            onItemClick = { interaction ->
                Toast.makeText(context, "Clicked on interaction: ${interaction.firstLikerName}", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerViewUserInteractions.adapter = adapterUserInteractions

    }

    private fun setupTabListeners() {
        btnRequestDates.setOnClickListener {
            switchTab(TabType.REQUEST_DATES)
        }
        varbtnInteractions.setOnClickListener { 
            switchTab(TabType.INTERACTION)
        }
    }

    private fun switchTab(tabType: TabType) {
        currentTab = tabType
        Log.d(TAG, "Switching to tab: $tabType")

        when (tabType) {
            TabType.REQUEST_DATES -> {
                btnRequestDates.setBackgroundResource(R.drawable.segmented_button_selected_bg)
                btnRequestDates.setTextColor(ContextCompat.getColor(requireContext(), R.color.pink_accent))
                varbtnInteractions.setBackgroundResource(android.R.color.transparent)
                varbtnInteractions.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_grey))

                emptyStateTextView.text = "No received date requests yet."
                fetchReceivedDateRequests()
            }
            TabType.INTERACTION -> {
                varbtnInteractions.setBackgroundResource(R.drawable.segmented_button_selected_bg)
                varbtnInteractions.setTextColor(ContextCompat.getColor(requireContext(), R.color.pink_accent))
                btnRequestDates.setBackgroundResource(android.R.color.transparent)
                btnRequestDates.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_grey))

                emptyStateTextView.text = "No user interactions yet."
                fetchUserInteractions()
            }
        }
        updateEmptyState()
    }

    private fun fetchReceivedDateRequests() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                showLoading(true)
            }
            val sessionManager = SessionManager(requireContext())
            val authToken = sessionManager.getAuthToken()

            if (authToken.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
                return@launch
            }
            Log.d(TAG, "Fetching received date requests...")

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getReceivedDateRequests("Token $authToken")
                }
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val requests = response.body()!!
                        dateRequestsList.clear()
                        dateRequestsList.addAll(requests)
                        adapterDateRequests.notifyDataSetChanged()
                        Log.d(TAG, "Fetched ${requests.size} received date requests successfully.")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch date requests: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(requireContext(), "Failed to load date requests.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Network error: Please check your internet connection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error: ${e.code()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Server error: ${e.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "An unexpected error occurred: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun fetchUserInteractions() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                showLoading(true)
            }
            val sessionManager = SessionManager(requireContext())
            val authToken = sessionManager.getAuthToken()

            if (authToken.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
                return@launch
            }
            Log.d(TAG, "Fetching user interactions...")

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getAggregatedLikes("Token $authToken")
                }
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val interactions = response.body()!!
                        userInteractionsList.clear()
                        userInteractionsList.addAll(interactions)
                        adapterUserInteractions.notifyDataSetChanged()
                        Log.d(TAG, "Fetched ${interactions.size} user interactions successfully.")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch user interactions: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(requireContext(), "Failed to load interactions.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Network error: Please check your internet connection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error: ${e.code()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Server error: ${e.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "An unexpected error occurred: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false) // This will call updateEmptyState()
                }
            }
        }
    }

    private fun deleteDateRequest(requestId: Int, position: Int) {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
            adapterDateRequests.notifyItemChanged(position) // Bring item back if no auth
            return
        }

        Log.d(TAG, "Attempting to delete date request with ID: $requestId at position $position")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.deleteDateRequest(requestId, "Token $authToken")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        dateRequestsList.removeAt(position)
                        adapterDateRequests.notifyItemRemoved(position)
                        Toast.makeText(context, "Date request deleted!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Date request ID $requestId deleted successfully.")
                        updateEmptyState()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to delete request: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(requireContext(), "Failed to delete date request.", Toast.LENGTH_LONG).show()
                        adapterDateRequests.notifyItemChanged(position)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error deleting request: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Network error: Please check your internet connection.", Toast.LENGTH_LONG).show()
                    adapterDateRequests.notifyItemChanged(position)
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error deleting request: ${e.code()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Server error: ${e.message()}", Toast.LENGTH_LONG).show()
                    adapterDateRequests.notifyItemChanged(position)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "An unexpected error occurred deleting request: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    adapterDateRequests.notifyItemChanged(position)
                }
            }
        }
    }

    private fun updateDateRequestStatus(request: DateRequestResponse, newStatus: String) {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "Updating date request status for ID: ${request.id} to $newStatus")

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
                        adapterDateRequests.updateRequestStatus(request.id, newStatus)
                        Log.d(TAG, "Date request ID ${request.id} status updated to $newStatus.")
                        updateEmptyState()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to update status: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(requireContext(), "Failed to update request status.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error updating status: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Network error: Please check your internet connection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Server error: ${e.code()}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Server error: ${e.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMessage = "An unexpected error occurred: ${e.message}"
                Log.e(TAG, errorMessage, e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            recyclerViewDateRequests.visibility = View.GONE
            recyclerViewUserInteractions.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            updateEmptyState()
        }
        Log.d(TAG, "ShowLoading: $isLoading")
    }

    private fun updateEmptyState() {
        progressBar.visibility = View.GONE

        when (currentTab) {
            TabType.REQUEST_DATES -> {
                if (dateRequestsList.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    recyclerViewDateRequests.visibility = View.GONE
                    Log.d(TAG, "Empty state: Showing empty message for Date Requests.")
                } else {
                    emptyStateTextView.visibility = View.GONE
                    recyclerViewDateRequests.visibility = View.VISIBLE
                    Log.d(TAG, "Empty state: Hiding empty message, showing Date Requests list.")
                }
                recyclerViewUserInteractions.visibility = View.GONE
            }
            TabType.INTERACTION -> {
                if (userInteractionsList.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    recyclerViewUserInteractions.visibility = View.GONE
                    Log.d(TAG, "Empty state: Showing empty message for User Interactions.")
                } else {
                    emptyStateTextView.visibility = View.GONE
                    recyclerViewUserInteractions.visibility = View.VISIBLE
                    Log.d(TAG, "Empty state: Hiding empty message, showing User Interactions list.")
                }
                recyclerViewDateRequests.visibility = View.GONE
            }
        }
    }

    private fun openDateRequestDetailFragment(dateRequest: DateRequestResponse) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DateRequestDetailFragment.newInstance(dateRequest))
            .addToBackStack(null)
            .commit()
    }
}