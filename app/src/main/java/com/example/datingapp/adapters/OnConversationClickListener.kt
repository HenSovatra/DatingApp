package com.example.datingapp

interface OnConversationClickListener {
    fun onConversationClick(conversationId: Int, participantName: String, participantImageUrl: String?)
}