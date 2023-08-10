package ml.docilealligator.infinityforreddit.data


import com.stfalcon.chatkit.commons.models.IDialog
import com.stfalcon.chatkit.commons.models.IUser
import org.matrix.android.sdk.api.session.room.model.RoomSummary

class RoomSummaryDialogWrapper(val roomSummary: RoomSummary) : IDialog<TimelineEventMessageWrapper> {

    override fun getId() = roomSummary.roomId

    override fun getDialogPhoto() = roomSummary.avatarUrl

    override fun getDialogName() = roomSummary.displayName

    override fun getUsers(): MutableList<out IUser> {
        return ArrayList<TimelineEventSenderWrapper>()
    }

    override fun getLastMessage() = roomSummary.latestPreviewableEvent?.let { TimelineEventMessageWrapper(it) }

    override fun setLastMessage(message: TimelineEventMessageWrapper?) {
        // noop
    }

    override fun getUnreadCount() = roomSummary.notificationCount
}
