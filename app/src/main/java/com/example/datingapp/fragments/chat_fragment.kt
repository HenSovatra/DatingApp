package com.example.datingapp.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.example.datingapp.adapters.FriendsAdapter
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.ConversationListItem
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.datingapp.services.MyFirebaseMessagingService
import com.yourpackage.yourapp.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class chat_fragment : Fragment() {

    private val TAG = "ChatListFragment"

    private lateinit var conversationsRecyclerView: RecyclerView
    private val conversationList: MutableList<ConversationListItem> = mutableListOf()
    private lateinit var conversationsAdapter: FriendsAdapter
    private lateinit var btnFindPartner: TextView

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var statusTextView: TextView

    private lateinit var chatListUpdateReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "ChatListFragment onCreate")

        chatListUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "Fragment: onReceive called for action: ${intent?.action}")

                if (intent?.action == MyFirebaseMessagingService.ACTION_NEW_CHAT_MESSAGE) {
                    Log.d(TAG, "Fragment: New chat message broadcast received. Refreshing conversation list.")
                    fetchConversations()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_fragment, container, false)

        conversationsRecyclerView = view.findViewById(R.id.friendsRecyclerView)
        btnFindPartner = view.findViewById(R.id.btnFindPartner)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        statusTextView = view.findViewById(R.id.statusTextView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "ChatListFragment onViewCreated")

        conversationsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        conversationsAdapter = FriendsAdapter(conversationList) { conversation ->
            Log.d(TAG, "Conversation clicked: ID ${conversation.id}, Participant: ${conversation.otherParticipant?.fullName}")

            val chatDetailFragment = ChatDetailFragment.newInstance(
                conversation.id,
                conversation.otherParticipant?.fullName ?: "Unknown",
                conversation.otherParticipant?.displayPictureUrl
            )

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, chatDetailFragment)
                .addToBackStack(null)
                .commit()
        }
        conversationsRecyclerView.adapter = conversationsAdapter

        setUiState(UiState.LOADING)
        fetchConversations()
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(MyFirebaseMessagingService.ACTION_NEW_CHAT_MESSAGE)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(chatListUpdateReceiver, intentFilter)
        Log.d(TAG, "ChatListFragment: BroadcastReceiver REGISTERED in onResume.")
        setUiState(UiState.LOADING)
        fetchConversations()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(chatListUpdateReceiver)
        Log.d(TAG, "ChatListFragment: BroadcastReceiver UNREGISTERED in onPause.")
    }

    private enum class UiState {
        LOADING,
        EMPTY,
        LOADED,
        ERROR
    }

    private fun setUiState(state: UiState, errorMessage: String? = null) {
        when (state) {
            UiState.LOADING -> {
                loadingProgressBar.visibility = View.VISIBLE
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = getString(R.string.loading_chats)
                conversationsRecyclerView.visibility = View.GONE
                btnFindPartner.visibility = View.GONE
            }
            UiState.EMPTY -> {
                loadingProgressBar.visibility = View.GONE
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = getString(R.string.no_chats_available)
                conversationsRecyclerView.visibility = View.GONE
                btnFindPartner.visibility = View.VISIBLE
            }
            UiState.LOADED -> {
                loadingProgressBar.visibility = View.GONE
                statusTextView.visibility = View.GONE
                conversationsRecyclerView.visibility = View.VISIBLE
                btnFindPartner.visibility = View.GONE
            }
            UiState.ERROR -> {
                loadingProgressBar.visibility = View.GONE
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = errorMessage ?: "An error occurred."
                conversationsRecyclerView.visibility = View.GONE
                btnFindPartner.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchConversations() {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            setUiState(UiState.ERROR, "Authentication required to fetch conversations.")
            Toast.makeText(
                requireContext(),
                "Authentication required to fetch conversations.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        Log.d(TAG, "Fetching conversations...")
        setUiState(UiState.LOADING)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getConversations()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val conversations = response.body()!!
                        Log.d(TAG, "Fetched ${conversations.size} conversations from API.")

                        conversationList.clear()
                        conversationList.addAll(conversations)
                        conversationsAdapter.notifyDataSetChanged()

                        if (conversations.isEmpty()) {
                            setUiState(UiState.EMPTY)
                            Log.d(TAG, "No conversations found.")
                        } else {
                            setUiState(UiState.LOADED)
                            Log.d(TAG, "Conversations found.")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch conversations: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        setUiState(UiState.ERROR, "Failed to load chats: ${response.code()}")
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error fetching conversations: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    setUiState(UiState.ERROR, "Network error: ${e.message}")
                    Toast.makeText(
                        requireContext(),
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}