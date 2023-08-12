package ml.docilealligator.infinityforreddit.data


import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.MessageContentType
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.util.Date


data class TimelineEventMessageWrapper(private val timelineEvent: TimelineEvent) : IMessage, MessageContentType.Image {

    override fun getId() = timelineEvent.localId.toString()

    override fun getText(): String {
        // This is where you can format according to the type
        // You might want to use getClearType in this case so you get the decrypted type if needed.
        return when (timelineEvent.root.getClearType()) {
            EventType.MESSAGE -> formatMessage(timelineEvent)
            else -> "Event of type ${timelineEvent.root.getClearType()} not rendered yet"
        }
    }

    private fun formatMessage(timelineEvent: TimelineEvent): String {
        // You can use the toModel extension method to serialize the json map to one of the sdk defined content.
        val messageContent = timelineEvent.root.getClearContent().toModel<MessageContent>() ?: return ""
        return messageContent.body
    }

    override fun getUser() = TimelineEventSenderWrapper(timelineEvent.senderInfo)

    override fun getCreatedAt() = Date(timelineEvent.root.originServerTs ?: 0)
    override fun getImageUrl(): String? {
        return timelineEvent.root.getClearContent()?.get("url")?.toString()
    }
}
