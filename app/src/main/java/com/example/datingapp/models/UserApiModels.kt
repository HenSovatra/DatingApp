
package com.example.datingapp.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class UserRegistrationRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirm")
    val passwordConfirm: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    val gender: String,
    @SerializedName("kind_of_date_looking_for")
    val kindOfDateLookingFor: Int,
    val bio: String?,
    val location: String?
) : Serializable

data class UserLoginRequest(
    val username: String,
    val password: String
) : Serializable


data class AuthResponse(
    val user: UserProfileResponse,
    val token: String
) : Serializable

data class UserProfileResponse(
    val id: Int,
    val username: String,
    val email: String,

    @SerializedName("profile")
    val profile: ProfileDataResponse?
) : Serializable

data class ProfileDataResponse(
    val id: Int? = null,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    val gender: String?,
    @SerializedName("kind_of_date_looking_for")
    val kindOfDateLookingFor: KindOfDateResponse?,
    val bio: String?,
    val location: String? ,
    @SerializedName("profile_image_url")
    val profileImageUrl: String?
) : Serializable


data class KindOfDateResponse(
    val id: Int,
    val name: String
) : Serializable


data class base64Data(
    @SerializedName("file_name")
    val fileName : String?,
    @SerializedName("base64_data")
    val base64Data: String?
): Serializable


data class SuggestedUsersResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<UserProfileResponse>
)

data class InteractionRequest(
    @SerializedName("interacted_user") val interactedUserId: Int,
    @SerializedName("interaction_type") val interactionType: String
)

data class InteractionResponse(
    val status: String,
    val message: String?
)

data class DateTypeItem(
    val id: Int,
    val name: String,
    val drawableResId: Int
) : Serializable


data class DateRequest(
    @SerializedName("date_type_name")
    val dateTypeName: String?,
    @SerializedName("partner_id")
    val partnerId: String?,
    val location: String?,
    @SerializedName("date_time")
    val dateTime: String?,
    val message: String?,
    val status: String? = "pending"
)
data class Profile(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    val gender: String?,
    @SerializedName("kind_of_date_looking_for") val kindOfDateLookingFor: KindOfDate?,
    val bio: String?,
    val location: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?
)
data class UserWithProfile(
    val id: Int,
    val username: String,
    val email: String?,
    val profile: Profile?
)

data class DateRequestResponse(
    val id: Int,
    @SerializedName("requester")
    val requester: UserWithProfile,
    @SerializedName("partner")
    val partner: UserWithProfile,
    @SerializedName("date_type_name") val dateTypeName: String,
    val location: String?,
    @SerializedName("date_time") val dateTime: String?,
    val message: String?,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class CreateDateRequestResponse(
    val message: String,
    val dateRequest: DateRequest
)

data class FcmTokenRequest(
    val token: String
)

data class FcmTokenResponse(
    val registration_id: String,
    val active: Boolean
)

data class UserProfileDataMessage(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("display_picture_url") val displayPictureUrl: String?
) : Serializable

data class ConversationListItem(
    val id: Int,
    @SerializedName("other_participant") val otherParticipant: UserProfileDataMessage?,
    @SerializedName("last_message") val lastMessageContent: String?,
    @SerializedName("time_ago") val timeAgo: String?,
    @SerializedName("has_unread") val hasUnread: Boolean,
    @SerializedName("updated_at") val updatedAt: String
) : Serializable

data class ConversationDetail(
    val id: Int,
    val participants: List<UserProfileDataMessage>,
    val messages: List<Message>,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
) : Serializable


data class SendMessageRequest(
    val conversation: Int?,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("partner_id") val partnerId: Int?,
    @SerializedName("message_content") val messageContent: String
) : Serializable

 data class Conversation(
     val id: Int,
     val otherParticipant: OtherParticipant,
     val lastMessage: String?,
     val timeAgo: String?,
     val hasUnread: Boolean,
     val updatedAt: String
 )

data class OtherParticipant(
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("display_picture_url")
    val displayPictureUrl: String?
)

data class Message(
    val id: Int,
    val conversation: Int,
    val sender: OtherParticipant,
    @SerializedName("message_content")
    val messageContent: String,
    val timestamp: String,
    val read: Boolean,
    @SerializedName("time_ago")
    val timeAgo: String
)

data class UserInteractionNotification(
    val id: String,
    val firstLikerName: String,
    val firstLikerImageUrl: String?,
    val otherLikersCount: Int
)

data class EngagementCount(
    @SerializedName("count")
    val count: Int
)

data class UserProfile(
    val id: Int?,
    val username: String?,
    val email: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    val gender: String?,
    @SerializedName("preferred_date_type")
    val preferredDateType: String?,
    val bio: String?,
    val location: String?,
    @SerializedName("profile_image_url")
    val profileImageUrl: String?,
    @SerializedName("kind_of_date_looking_for")
    val kindOfDateLookingFor: KindOfDateLookingFor?
)
data class KindOfDateLookingFor(
    val id: Int,
    val name: String
)

data class UserResponseSetting(
    val id: Int,
    val username: String?,
    val email: String?,
    @SerializedName("profile")
    val profileData: ProfileSetting?
)

data class ProfileSetting(
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    val gender: String?,
    @SerializedName("bio")
    val bio: String?,
    @SerializedName("location")
    val location: String?,
    @SerializedName("profile_image_url")
    val profileImageUrl: String?,
    @SerializedName("kind_of_date_looking_for")
    val kindOfDateLookingFor: KindOfDate?
)