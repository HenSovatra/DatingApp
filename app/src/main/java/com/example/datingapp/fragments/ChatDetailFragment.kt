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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.datingapp.R
import com.example.datingapp.adapters.MessagesAdapter
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.Message
import com.example.datingapp.models.SendMessageRequest
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.datingapp.services.MyFirebaseMessagingService
import com.yourpackage.yourapp.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class ChatDetailFragment : Fragment() {

    private val TAG = "ChatDetailFragment"

    private var conversationId: Int = -1
    private var participantName: String? = null
    private var participantImageUrl: String? = null

    private lateinit var chatHeaderTextView: TextView
    private lateinit var participantImageView: ImageView
    private lateinit var backButton: ImageButton
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var messagesRecyclerView: RecyclerView

    private lateinit var messagesAdapter: MessagesAdapter
    private val messagesList: MutableList<Message> = mutableListOf()

    // --- NEW: BroadcastReceiver for handling new chat messages ---
    private lateinit var chatMessageReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationId = it.getInt(ARG_CONVERSATION_ID)
            participantName = it.getString(ARG_PARTICIPANT_NAME)
            participantImageUrl = it.getString(ARG_PARTICIPANT_IMAGE_URL)
        }
        Log.d(TAG, "Fragment onCreate: Current conversationId: $conversationId")

        chatMessageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "Fragment: onReceive called for action: ${intent?.action}")

                if (intent?.action == MyFirebaseMessagingService.ACTION_NEW_CHAT_MESSAGE) {
                    val receivedConversationIdStr = intent.getStringExtra("conversation_id")
                    val receivedConversationId = receivedConversationIdStr?.toIntOrNull()

                    Log.d(TAG, "Fragment: Received broadcast. Extracted conversation_id: $receivedConversationIdStr (as Int: $receivedConversationId)")
                    Log.d(TAG, "Fragment: Current fragment's conversationId: $conversationId")

                    if (receivedConversationId != null && receivedConversationId == conversationId) {
                        Log.d(TAG, "Fragment: Matched conversation ID. Triggering fetchChatMessages.")
                        fetchChatMessages(conversationId)
                    } else {
                        Log.d(TAG, "Fragment: Conversation ID MISMATCH or NULL. Received: $receivedConversationId, Current: $conversationId")
                        if (receivedConversationIdStr == null) {
                            Log.e(TAG, "Fragment: 'conversation_id' was NULL in the broadcast extras!")
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_detail, container, false)

        chatHeaderTextView = view.findViewById(R.id.chat_header_name)
        participantImageView = view.findViewById(R.id.chat_header_image)
        backButton = view.findViewById(R.id.chat_back_button)
        messageEditText = view.findViewById(R.id.message_edit_text)
        sendButton = view.findViewById(R.id.send_button)
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatHeaderTextView.text = participantName
        participantImageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.defaultpfp)
                .error(R.drawable.defaultpfp)
                .into(participantImageView)
        } ?: run {
            participantImageView.setImageResource(R.drawable.defaultpfp)
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        messagesRecyclerView.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        val sessionManager = SessionManager(requireContext())
        val userid = sessionManager.getUserId()
        messagesAdapter = MessagesAdapter(messagesList, userid)
        messagesRecyclerView.adapter = messagesAdapter

        sendButton.setOnClickListener {
            val messageContent = messageEditText.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                sendMessage(conversationId, null, messageContent)
                messageEditText.text.clear()
            } else {
                Toast.makeText(requireContext(), "Message cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }

        fetchChatMessages(conversationId)
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(MyFirebaseMessagingService.ACTION_NEW_CHAT_MESSAGE)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(chatMessageReceiver, intentFilter)
        Log.d(TAG, "Fragment: BroadcastReceiver REGISTERED in onResume.")
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(chatMessageReceiver)
        Log.d(TAG, "Fragment: BroadcastReceiver UNREGISTERED in onPause.")
    }

    private fun fetchChatMessages(id: Int) {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()
        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required.", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "Fragment: Calling fetchChatMessages for conversation ID: $id")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getConversationDetail(id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val conversationDetail = response.body()!!
                        Log.d(TAG, "API call successful. Fetched ${conversationDetail.messages.size} messages.")

                        Log.d(TAG, "messagesList size BEFORE clear: ${messagesList.size}")
                        messagesList.clear()
                        messagesList.addAll(conversationDetail.messages)
                        Log.d(TAG, "messagesList size AFTER addAll: ${messagesList.size}")

                        messagesAdapter.notifyDataSetChanged()
                        Log.d(TAG, "notifyDataSetChanged called.")

                        if (messagesList.isNotEmpty()) {
                            messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                            Log.d(TAG, "Scrolled to position ${messagesList.size - 1}.")
                        } else {
                            Log.d(TAG, "messagesList is empty, not scrolling.")
                        }

                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch messages: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(requireContext(), "Failed to load messages.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error fetching messages: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendMessage(conversationId: Int, partnerId: Int?, messageContent: String) {
        val sessionManager = SessionManager(requireContext())
        if (sessionManager.getAuthToken().isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required to send message.", Toast.LENGTH_SHORT).show()
            return
        }
        val userid = sessionManager.getUserId()
        val requestBody = SendMessageRequest(
            conversation = conversationId,
            partnerId = partnerId,
            messageContent = messageContent,
            senderId = userid
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.sendMessage(requestBody)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val sentMessage = response.body()!!
                        Log.d(TAG, "Message sent: ${sentMessage.messageContent}")
                        Toast.makeText(requireContext(), "Message sent!", Toast.LENGTH_SHORT).show()

                        messagesList.add(sentMessage)
                        messagesAdapter.notifyItemInserted(messagesList.size - 1)
                        messagesRecyclerView.scrollToPosition(messagesList.size - 1)

                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Error sending message: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e(TAG, errorMessage)
                        Toast.makeText(requireContext(), "Failed to send message.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Network error sending message: ${e.message}"
                    Log.e(TAG, errorMessage, e)
                    Toast.makeText(requireContext(), "Network error.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_PARTICIPANT_NAME = "participant_name"
        private const val ARG_PARTICIPANT_IMAGE_URL = "participant_image_url"

        @JvmStatic
        fun newInstance(conversationId: Int, participantName: String, participantImageUrl: String?) =
            ChatDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CONVERSATION_ID, conversationId)
                    putString(ARG_PARTICIPANT_NAME, participantName)
                    putString(ARG_PARTICIPANT_IMAGE_URL, participantImageUrl)
                }
            }
    }
}