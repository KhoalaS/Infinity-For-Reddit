package ml.docilealligator.infinityforreddit.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.coroutines.launch
import ml.docilealligator.infinityforreddit.Infinity
import ml.docilealligator.infinityforreddit.SessionHolder
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper
import ml.docilealligator.infinityforreddit.databinding.FragmentRoomDetailBinding
import ml.docilealligator.infinityforreddit.utils.AvatarRenderer
import ml.docilealligator.infinityforreddit.utils.MatrixItemColorProvider
import ml.docilealligator.infinityforreddit.utils.RecyclerScrollMoreListener
import ml.docilealligator.infinityforreddit.utils.TimelineEventListProcessor
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.session.room.timeline.*
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomDetailFragment : Fragment(), Timeline.Listener, ToolbarConfigurable {

    @Inject
    lateinit var mCustomThemeWrapper: CustomThemeWrapper

    companion object {

        private const val ROOM_ID_ARGS = "ROOM_ID_ARGS"

        fun newInstance(roomId: String): RoomDetailFragment {
            val args = bundleOf(
                Pair(ROOM_ID_ARGS, roomId)
            )
            return RoomDetailFragment().apply {
                arguments = args
            }
        }
    }

    private var _views: FragmentRoomDetailBinding? = null
    private val views get() = _views!!

    private val session = SessionHolder.currentSession!!
    private var timeline: Timeline? = null
    private var room: Room? = null

    private val avatarRenderer by lazy {
        AvatarRenderer(MatrixItemColorProvider(requireContext()))
    }

    private val imageLoader = ImageLoader { imageView, url, _ ->
        avatarRenderer.render(url, imageView)
    }

    private val adapter = MessagesListAdapter<IMessage>(session.myUserId, imageLoader)
    private val timelineEventListProcessor = TimelineEventListProcessor(adapter)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _views = FragmentRoomDetailBinding.inflate(inflater, container, false)
        (requireActivity().application as Infinity).appComponent.inject(this)
        applyCustomTheme()
        return views.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar(views.toolbar, displayBack = true)
        views.textComposer.setInputListener {
            // Sending message can be as simple as that.
            // Timeline will be automatically updated with local echo
            // and when receiving from sync so you don't have anything else to do
            room?.sendTextMessage(it)
            true
        }

        views.textComposer.setTypingListener(object : MessageInput.TypingListener {
            override fun onStartTyping() {
                room?.userIsTyping()
            }

            override fun onStopTyping() {
                room?.userStopsTyping()
            }
        })

        views.timelineEventList.setAdapter(adapter)
        views.timelineEventList.itemAnimator = null
        views.timelineEventList.addOnScrollListener(RecyclerScrollMoreListener(views.timelineEventList.layoutManager as LinearLayoutManager) {
            if (timeline?.hasMoreToLoad(Timeline.Direction.BACKWARDS).orTrue()) {
                timeline?.paginate(Timeline.Direction.BACKWARDS, 50)
            }
        })
        val roomId = arguments?.getString(ROOM_ID_ARGS)!!
        // You can grab a room from the session
        // If the room is not known (not received from sync) it will return null
        room = session.getRoom(roomId)

        lifecycleScope.launch {
            room?.markAsRead(ReadService.MarkAsReadParams.READ_RECEIPT)
        }

        // Create some settings to configure timeline
        val timelineSettings = TimelineSettings(
            initialSize = 30
        )
        // Then you can retrieve a timeline from this room.
        timeline = room?.createTimeline(null, timelineSettings)?.also {
            // Don't forget to add listener and start the timeline so it start listening to changes
            it.addListener(this)
            it.start()
        }

        // You can also listen to room summary from the room
        room?.getRoomSummaryLive()?.observe(viewLifecycleOwner) { roomSummary ->
            val roomSummaryAsMatrixItem =
                roomSummary.map { it.toMatrixItem() }.getOrNull() ?: return@observe
            avatarRenderer.render(roomSummaryAsMatrixItem, views.toolbarAvatarImageView)
            views.toolbarTitleView.text = roomSummaryAsMatrixItem.let {
                it.displayName?.takeIf { dn -> dn.isNotBlank() } ?: it.id
            }
        }
    }

    override fun onDestroyView() {
        timeline?.also {
            // Don't forget to remove listener and dispose timeline to avoid memory leaks
            it.removeAllListeners()
            it.dispose()
        }
        timeline = null
        room = null
        super.onDestroyView()
    }

    override fun onNewTimelineEvents(eventIds: List<String>) {
        // This is new event ids coming from sync
    }

    override fun onTimelineFailure(throwable: Throwable) {
        // When a failure is happening when trying to retrieve an event.
        // This is an unrecoverable error, you might want to restart the timeline
        // timeline?.restartWithEventId("")
    }

    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        // Each time the timeline is updated it will be called.
        // It can happens when sync returns, paginating, and updating (local echo, decryption finished...)
        // You probably want to process with DiffUtil before dispatching to your recyclerview
        timelineEventListProcessor.onNewSnapshot(snapshot)
    }

    private fun applyCustomTheme() {
        _views?.toolbarTitleView?.setTextColor(mCustomThemeWrapper.primaryTextColor)
    }
}