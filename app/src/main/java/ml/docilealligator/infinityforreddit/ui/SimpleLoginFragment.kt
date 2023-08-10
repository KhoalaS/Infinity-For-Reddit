package ml.docilealligator.infinityforreddit.ui


import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.SessionHolder
import ml.docilealligator.infinityforreddit.databinding.FragmentLoginBinding
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import javax.inject.Inject
import javax.inject.Named

class SimpleLoginFragment : Fragment() {

    @Inject
    @Named("current_account")
    var mCurrentAccountSharedPreferences: SharedPreferences? = null

    private var _views: FragmentLoginBinding? = null
    private val views get() = _views!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _views = FragmentLoginBinding.inflate(inflater, container, false)
        return views.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun launchAuthProcess() {

        // First, create a homeserver config
        // Be aware than it can throw if you don't give valid info
        val homeServerConnectionConfig = try {
            HomeServerConnectionConfig
                .Builder()
                .withHomeServerUri(Uri.parse("https://matrix.redditspace.com/"))
                .build()
        } catch (failure: Throwable) {
            Toast.makeText(requireContext(), "Home server is not valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Then you can retrieve the authentication service.
        // Here we use the direct authentication, but you get LoginWizard and RegistrationWizard for more advanced feature
        //
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Matrix.getInstance(requireContext()).authenticationService().getLoginFlow(homeServerConnectionConfig);
            } catch (failure: Throwable) {
                Toast.makeText(requireContext(), "Failure: $failure", Toast.LENGTH_SHORT).show()
                null
            }?.let { result ->
                // When you got your session, open and launch sync
                Toast.makeText(
                    requireContext(),
                    "Homeserver is ${result.homeServerUrl}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            val accessToken = mCurrentAccountSharedPreferences!!.getString(
                SharedPreferencesUtils.ACCESS_TOKEN,
                ""
            )

            if (accessToken!!.isNotEmpty()){
                try {
                    val data: MutableMap<String, Any> = HashMap()
                    data["token"] = accessToken
                    data["initial_device_display_name"] = "Reddit Matrix Android"
                    data["type"] = "com.reddit.token"

                    Matrix.getInstance(requireContext()).authenticationService().getLoginWizard().loginCustom(data);
                } catch (failure: Throwable) {
                    Toast.makeText(requireContext(), "Failure: $failure", Toast.LENGTH_SHORT).show()
                    null
                }?.let { session ->
                    // When you got your session, open and launch sync
                    Toast.makeText(
                        requireContext(),
                        "Welcome ${session.myUserId}",
                        Toast.LENGTH_SHORT
                    ).show()
                    SessionHolder.currentSession = session
                    session.open()
                    session.startSync(true)
                    displayRoomList()
                }
            }
        }
    }

    private fun displayRoomList() {
        val fragment = RoomListFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.activity_chat_overview_pager, fragment).commit()
    }
}