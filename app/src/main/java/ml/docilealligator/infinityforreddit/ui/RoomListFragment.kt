package ml.docilealligator.infinityforreddit.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import kotlinx.coroutines.launch
import ml.docilealligator.infinityforreddit.Infinity
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.SessionHolder
import ml.docilealligator.infinityforreddit.activities.ChatOverviewActivity
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper
import ml.docilealligator.infinityforreddit.data.RoomSummaryDialogWrapper
import ml.docilealligator.infinityforreddit.databinding.FragmentRoomListBinding
import ml.docilealligator.infinityforreddit.formatter.RoomListDateFormatter
import ml.docilealligator.infinityforreddit.utils.AvatarRenderer
import ml.docilealligator.infinityforreddit.utils.MatrixItemColorProvider
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject


class RoomListFragment : Fragment(), ToolbarConfigurable {

    private val session = SessionHolder.currentSession!!

    @Inject
    lateinit var mCustomThemeWrapper: CustomThemeWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        (requireActivity().application as Infinity).appComponent.inject(this)
        _views = FragmentRoomListBinding.inflate(inflater, container, false)
        applyCustomTheme()
        return views.root
    }

    private var _views: FragmentRoomListBinding? = null
    private val views get() = _views!!

    private val avatarRenderer by lazy {
        AvatarRenderer(MatrixItemColorProvider(requireContext()))
    }

    private val imageLoader = ImageLoader { imageView, url, _ ->
        avatarRenderer.render(url, imageView)
    }
    private val roomAdapter = DialogsListAdapter<RoomSummaryDialogWrapper>(imageLoader)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar(views.toolbar, displayBack = false)
        views.roomSummaryList.setAdapter(roomAdapter)
        roomAdapter.setDatesFormatter(RoomListDateFormatter())
        roomAdapter.setOnDialogClickListener {
            showRoomDetail(it.roomSummary)
        }

        // Create query to listen to room summary list
        val roomSummariesQuery = roomSummaryQueryParams {
            memberships = Membership.activeMemberships()
        }
        // Then you can subscribe to livedata..
        session.getRoomSummariesLive(roomSummariesQuery).observe(viewLifecycleOwner) {
            // ... And refresh your adapter with the list. It will be automatically updated when an item of the list is changed.
            updateRoomList(it)
        }

        // You can also listen to user. Here we listen to ourself to get our avatar
        session.getUserLive(session.myUserId).observe(viewLifecycleOwner) { user ->
            val userMatrixItem = user.map { it.toMatrixItem() }.getOrNull() ?: return@observe
            avatarRenderer.render(userMatrixItem, views.toolbarAvatarImageView)
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chat, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chat_menu -> {
                //signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                session.signOut(true)
            } catch (failure: Throwable) {
                activity?.let {
                    Toast.makeText(it, "Failure: $failure", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            SessionHolder.currentSession = null
            activity?.finish()
        }
    }

    private fun showRoomDetail(roomSummary: RoomSummary) {
        val roomDetailFragment = RoomDetailFragment.newInstance(roomSummary.roomId)
        (activity as ChatOverviewActivity).supportFragmentManager
            .beginTransaction()
            .addToBackStack(null)
            .replace(R.id.chatFragmentContainer, roomDetailFragment)
            .commit()
    }

    private fun updateRoomList(roomSummaryList: List<RoomSummary>?) {
        if (roomSummaryList == null) return
        val sortedRoomSummaryList = roomSummaryList.sortedByDescending {
            it.latestPreviewableEvent?.root?.originServerTs
        }.map {
            RoomSummaryDialogWrapper(it)
        }
        roomAdapter.setItems(sortedRoomSummaryList)
    }

    private fun applyCustomTheme() {
        _views?.toolbarTitleView?.setTextColor(mCustomThemeWrapper.primaryTextColor)
        //_views?.toolbarTitleView?.setTextColor(mCustomThemeWrapper.primaryTextColor)
    }
}