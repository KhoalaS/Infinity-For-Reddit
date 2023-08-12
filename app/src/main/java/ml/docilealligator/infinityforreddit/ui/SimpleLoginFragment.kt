package ml.docilealligator.infinityforreddit.ui


import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ml.docilealligator.infinityforreddit.Infinity
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.SessionHolder
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper
import ml.docilealligator.infinityforreddit.databinding.FragmentLoginBinding
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import timber.log.Timber
import java.lang.AssertionError
import javax.inject.Inject

class SimpleLoginFragment : Fragment() {
    private var _views: FragmentLoginBinding? = null
    private val views get() = _views!!

    @Inject
    lateinit var mCustomThemeWrapper: CustomThemeWrapper

    var accessToken = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity().application as Infinity).appComponent.inject(this)
        accessToken = requireArguments().getString("access_token").toString();
        _views = FragmentLoginBinding.inflate(inflater, container, false)
        applyCustomTheme()
        return views.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launchAuthProcess()
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
            if(accessToken.isBlank()){
                return@launch
            }
            try {
                val result = Matrix.getInstance(requireContext()).authenticationService().getLoginFlow(homeServerConnectionConfig);
                Toast.makeText(
                    requireContext(),
                    "Homeserver is ${result.homeServerUrl}",
                    Toast.LENGTH_SHORT
                ).show()

                val data: MutableMap<String, Any> = HashMap()
                data["token"] = accessToken
                data["initial_device_display_name"] = "Reddit Matrix Android"
                data["type"] = "com.reddit.token"
                val session = Matrix.getInstance(requireContext()).authenticationService().getLoginWizard()
                    .loginCustom(data);
                Toast.makeText(
                    requireContext(),
                    "Welcome ${session.myUserId}",
                    Toast.LENGTH_SHORT
                ).show()
                SessionHolder.currentSession = session
                if(session.isOpenable){
                    try {
                        session.open()
                    } catch (e : AssertionError){
                        Timber.tag("MATRIX").e("Session already open")
                    }
                }
                session.startSync(true)

                displayRoomList()

            } catch (failure: Throwable) {
                Toast.makeText(requireContext(), "Failure: $failure", Toast.LENGTH_SHORT).show()
                displayRoomList()
            }
        }
    }

    private fun displayRoomList() {
        val fragment = RoomListFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.chatFragmentContainer, fragment).commit()
    }

    private fun applyCustomTheme() {
        _views?.container?.setBackgroundColor(mCustomThemeWrapper.backgroundColor)
        _views?.textView?.setTextColor(mCustomThemeWrapper.primaryTextColor)
        _views?.loading?.setBackgroundColor(mCustomThemeWrapper.colorPrimary)
    }
}