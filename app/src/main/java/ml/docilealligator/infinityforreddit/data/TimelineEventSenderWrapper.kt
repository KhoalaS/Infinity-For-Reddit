package ml.docilealligator.infinityforreddit.data


import com.stfalcon.chatkit.commons.models.IUser
import org.matrix.android.sdk.api.session.room.sender.SenderInfo

data class TimelineEventSenderWrapper(private val senderInfo: SenderInfo) : IUser {
    override fun getId() = senderInfo.userId

    override fun getName() = senderInfo.disambiguatedDisplayName

    override fun getAvatar() = senderInfo.avatarUrl
}
