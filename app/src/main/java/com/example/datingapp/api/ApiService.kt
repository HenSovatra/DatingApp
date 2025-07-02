package com.example.datingapp.api

import com.example.datingapp.models.AuthResponse
import com.example.datingapp.models.ConversationDetail
import com.example.datingapp.models.ConversationListItem
import com.example.datingapp.models.CreateDateRequestResponse
import com.example.datingapp.models.DateRequest
import com.example.datingapp.models.DateRequestResponse
import com.example.datingapp.models.EngagementCount
import com.example.datingapp.models.FcmTokenRequest
import com.example.datingapp.models.FcmTokenResponse
import com.example.datingapp.models.InteractionRequest
import com.example.datingapp.models.InteractionResponse
import com.example.datingapp.models.UserLoginRequest
import com.example.datingapp.models.UserRegistrationRequest
import com.example.datingapp.models.UserProfileResponse
import com.example.datingapp.models.KindOfDateResponse
import com.example.datingapp.models.Message
import com.example.datingapp.models.SendMessageRequest
import com.example.datingapp.models.SuggestedUsersResponse
import com.example.datingapp.models.UserInteractionNotification
import com.example.datingapp.models.UserProfile
import com.example.datingapp.models.UserResponse
import com.example.datingapp.models.UserResponseSetting
import com.example.datingapp.models.base64Data
import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("register/")
    suspend fun registerUser(@Body userRequest: UserRegistrationRequest): Response<AuthResponse>

    @POST("login/")
    suspend fun loginUser(@Body loginRequest: UserLoginRequest): Response<AuthResponse>

    @GET("users/{user_id}/")
    suspend fun getUserById(@Path("user_id") userId: Int, @Header("Authorization") authToken: String? = null): Response<UserResponse>

    @POST("users/{user_id}/save-image/")
    suspend fun saveImages(@Path("user_id") userId: Int,  @Body request: List<base64Data>): Response<ResponseBody> // ResponseBody if only message is returned\

    @GET("suggested-users/")
    suspend fun getSuggestedUsers(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): Response<SuggestedUsersResponse>

    @POST("interactions/")
    suspend fun sendInteraction(
        @Body request: InteractionRequest
    ): Response<InteractionResponse>

    @GET("liked-users/")
    suspend fun getLikedUsers(): Response<List<UserProfileResponse>>

    @POST("date-requests/create/")
    suspend fun createDateRequest(@Body dateRequest: DateRequest): Response<CreateDateRequestResponse> // New endpoint

    @POST("fcm_tokens/")
    suspend fun registerFCMToken(@Body fcmTokenRequest: FcmTokenRequest): Response<FcmTokenResponse>

    @GET("conversations/")
    suspend fun getConversations(): Response<List<ConversationListItem>>

    @GET("conversations/{conversationId}/")
    suspend fun getConversationDetail(@Path("conversationId") conversationId: Int): Response<ConversationDetail>

    @POST("messages/")
    suspend fun sendMessage(@Body message: SendMessageRequest): Response<Message>
    @GET("date-requests/received/")
    suspend fun getReceivedDateRequests(
        @Header("Authorization") authToken: String
    ): Response<List<DateRequestResponse>>

    @PATCH("date-requests/{id}/")
    suspend fun updateDateRequest(
        @Path("id") requestId: Int,
        @Header("Authorization") authToken: String,
        @Body updateData: Map<String, String>
    ): Response<DateRequestResponse>
    @GET("interactions/likes/")
    suspend fun getAggregatedLikes(@Header("Authorization") authToken: String): Response<List<UserInteractionNotification>>

    @DELETE("date-requests/{id}/delete")
    suspend fun deleteDateRequest(
        @Path("id") requestId: Int,
        @Header("Authorization") authToken: String
    ): Response<ResponseBody>
    @PATCH("users/{id}/profile/")
    suspend fun updateUserProfile(
        @Path("id") userId: Int,
        @Header("Authorization") authToken: String,
        @Body profile: UserProfile
    ): Response<UserProfile>

    @GET("users/{id}/")
    suspend fun getUserProfile(
        @Path("id") userId: Int,
        @Header("Authorization") authToken: String
    ): Response<UserResponseSetting>

    @GET("date-requests/sent/")
    suspend fun getSentDateRequests(@Header("Authorization") authToken: String): Response<List<DateRequestResponse>>
    @GET("likes/received/count/")
    suspend fun getLikesReceivedCount(@Header("Authorization") authToken: String): Response<EngagementCount>

    @GET("matches/count/")
    suspend fun getMatchesCount(@Header("Authorization") authToken: String): Response<EngagementCount>
}