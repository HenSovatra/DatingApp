package com.example.datingapp.interfaces

import com.example.datingapp.models.ConversationListItem

interface ChatSearchableDataProvider {
    fun provideConversationsForSearch(): List<ConversationListItem>
}