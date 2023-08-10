package ml.docilealligator.infinityforreddit.utils


import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.messages.MessagesListAdapter
import ml.docilealligator.infinityforreddit.data.TimelineEventMessageWrapper
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class TimelineEventListProcessor(private val adapter: MessagesListAdapter<IMessage>) {

    private var previousSnapshot: List<TimelineEventMessageWrapper> = emptyList()
    private var currentSnapshot: List<TimelineEventMessageWrapper> = emptyList()

    private val listUpdateCallback = object : ListUpdateCallback {

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            (0 until count).forEach { index ->
                val item = currentSnapshot.getOrNull(position + index) ?: return
                adapter.update(item)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            // noop
        }

        override fun onInserted(position: Int, count: Int) {
            if (position == 0) {
                (count - 1 downTo 0).forEach { index ->
                    val item = currentSnapshot.getOrNull(index) ?: return
                    adapter.addToStart(item, true)
                }
            } else {
                val subSnapshot = currentSnapshot.subList(position, position + count)
                adapter.addToEnd(subSnapshot, false)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            val subSnapshot = previousSnapshot.subList(position, position + count)
            adapter.delete(subSnapshot)
        }
    }

    fun onNewSnapshot(newSnapshot: List<TimelineEvent>) {
        val mappedNewSnapshot = newSnapshot.map {
            // This is where formatting of event happens, so you should look
            TimelineEventMessageWrapper(it)
        }
        val diffCallback = TimelineEventMessagesDiffUtilCallback(currentSnapshot, mappedNewSnapshot)
        previousSnapshot = currentSnapshot.toList()
        currentSnapshot = mappedNewSnapshot
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(listUpdateCallback)
    }
}
