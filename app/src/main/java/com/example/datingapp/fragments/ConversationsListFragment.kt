package com.example.datingapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.ConversationAdapter
import com.example.datingapp.OnConversationClickListener
import com.example.datingapp.R
import com.example.datingapp.models.Conversation

class ConversationsListFragment : Fragment(), OnConversationClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private var conversationList: List<Conversation> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_conversations_list, container, false)

        recyclerView = view.findViewById(R.id.conversations_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ConversationAdapter(conversationList, this)
        recyclerView.adapter = adapter

        return view
    }

    override fun onConversationClick(conversationId: Int, participantName: String, participantImageUrl: String?) {
        val chatDetailFragment = ChatDetailFragment.newInstance(
            conversationId,
            participantName,
            participantImageUrl
        )

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatDetailFragment)
            .addToBackStack(null)
            .commit()
    }
}