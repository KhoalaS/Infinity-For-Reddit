package ml.docilealligator.infinityforreddit.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.matrix.android.sdk.api.Matrix;
import org.matrix.android.sdk.api.MatrixConfiguration;
import org.matrix.android.sdk.api.SyncConfig;
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig;
import org.matrix.android.sdk.api.auth.data.LoginFlowResult;
import org.matrix.android.sdk.api.crypto.MXCryptoConfig;
import org.matrix.android.sdk.api.session.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import ml.docilealligator.SessionHolder;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.adapters.OpenChatAdapter;
import ml.docilealligator.infinityforreddit.apis.GqlAPI;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.customviews.slidr.Slidr;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import ml.docilealligator.infinityforreddit.utils.RoomDisplayNameFallbackProviderImpl;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import okhttp3.ConnectionSpec;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class ChatOverviewActivity extends BaseActivity {

    Session currentSession;

    @Inject
    @Named("default")
    SharedPreferences mSharedPreferences;

    @Inject
    @Named("current_account")
    SharedPreferences mCurrentAccountSharedPreferences;
    @Inject
    CustomThemeWrapper mCustomThemeWrapper;


    @BindView(R.id.appbar_layout_chat_overview_activity)
    AppBarLayout appBarLayout;
    @BindView(R.id.toolbar_chat_overview_activity)
    Toolbar toolbar;
    @BindView(R.id.activity_chat_overview_pager)
    ViewPager2 viewPager;

    private FragmentManager fragmentManager;
    DemoCollectionAdapter demoCollectionAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Infinity) getApplication()).getAppComponent().inject(this);

        setImmersiveModeNotApplicable();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        if (mSharedPreferences.getBoolean(SharedPreferencesUtils.SWIPE_RIGHT_TO_GO_BACK, true)) {
            mSliderPanel = Slidr.attach(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();

            if (isChangeStatusBarIconColor()) {
                addOnOffsetChangedListener(appBarLayout);
            }

            if (isImmersiveInterface()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(false);
                } else {
                    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                }
                adjustToolbar(toolbar);
            }
        }

        lockSwipeRightToGoBack();

        toolbar.setTitle("Chats");

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarGoToTop(toolbar);

        String accessToken = mCurrentAccountSharedPreferences.getString(SharedPreferencesUtils.ACCESS_TOKEN, "");

        HomeServerConnectionConfig homeServerConnectionConfig;
        try{
            homeServerConnectionConfig = new HomeServerConnectionConfig
                    .Builder()
                    .withHomeServerUri(Uri.parse("https://matrix.redditspace.com/"))
                    .build();
            homeServerConnectionConfig.getHomeServerUri().toString();
            Infinity.getMatrix().authenticationService().getLoginFlow(homeServerConnectionConfig, new Continuation<LoginFlowResult>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    for(String s : ((LoginFlowResult)o).getSupportedLoginTypes()){
                        Log.d("SupportedLogin", s);
                    }
                }
            });

            Map<String, Object> data = new HashMap<>();
            data.put("token", accessToken);
            data.put("initial_device_display_name", "Reddit Matrix Android");
            data.put("type", "com.reddit.token");


            Infinity.getMatrix().authenticationService().getLoginWizard().loginCustom(data, new Continuation<Session>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    Session session = (Session) o;
                    SessionHolder.getInstance().setCurrentSession(session);
                    session.open();
                    session.syncService().startSync(true);
                    initializeFragment();
                }
            });
        }catch (Exception e){
            Toast.makeText(this, "Home server is not valid", Toast.LENGTH_SHORT).show();
        }
    }

    public void initializeFragment(){
        fragmentManager = getSupportFragmentManager();
        demoCollectionAdapter = new DemoCollectionAdapter(this);
        viewPager.setAdapter(demoCollectionAdapter);
        viewPager.setBackgroundColor(mCustomThemeWrapper.getBackgroundColor());
    }

    @Override
    public SharedPreferences getDefaultSharedPreferences() {
        return mSharedPreferences;
    }

    @Override
    protected CustomThemeWrapper getCustomThemeWrapper() {
        return mCustomThemeWrapper;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.history_activity, menu);
        //applyMenuItemTheme(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void applyCustomTheme() {

    }

    @Override
    public void lockSwipeRightToGoBack() {
        if (mSliderPanel != null) {
            mSliderPanel.lock();
        }
    }

    public class DemoCollectionAdapter extends FragmentStateAdapter {
        public DemoCollectionAdapter(FragmentActivity fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return a NEW fragment instance in createFragment(int)
            // create a different Fragment if in Request position
            // fragment still needs impl
            Fragment fragment = new DemoObjectFragment();
            Bundle args = new Bundle();
            args.putInt(DemoObjectFragment.ARG_OBJECT, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }


    public static class DemoObjectFragment extends Fragment {

        @Inject
        @Named("gql")
        Retrofit mGqlRetrofit;
        @Inject
        @Named("current_account")
        SharedPreferences mCurrentAccountSharedPreferences;
        @Inject
        RedditDataRoomDatabase mRedditDataRoomDatabase;

        Session session = SessionHolder.getInstance().getCurrentSession();

        private BaseActivity activity;

        public static final String ARG_OBJECT = "object";
        private RecyclerView.LayoutManager layoutManager;

        public class MainThreadExecutor implements Executor {
            private final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void execute(Runnable r) {
                handler.post(r);
            }
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            this.activity = (BaseActivity) context;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_chat_open, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            ((Infinity) activity.getApplication()).getAppComponent().inject(this);

            Bundle args = getArguments();
            int position = args.getInt(ARG_OBJECT);
            layoutManager = new LinearLayoutManager(this.getActivity());
            GqlAPI gqlAPI = mGqlRetrofit.create(GqlAPI.class);

            String accessToken = mCurrentAccountSharedPreferences.getString(APIUtils.ACCESS_TOKEN_KEY, "");

            Map headers = APIUtils.getOAuthHeader(accessToken);

            JSONObject data = createChatrequestBody("JOINED_ONLY");

            RequestBody body = RequestBody.create(data.toString(), okhttp3.MediaType.parse("application/json; charset=utf-8"));

            Call<String> getJoined = gqlAPI.getJoined(headers, body);
            getJoined.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        try {
                            Log.d("GQL", response.body());
                            JSONObject responseJSON = new JSONObject(response.body());
                            JSONArray edges = responseJSON.getJSONObject("data").getJSONObject("searchChatUserChannels").getJSONArray("edges");

                            ListenableFuture<String> future = mRedditDataRoomDatabase.accountDao().getCurrentAccountUsername();
                            Futures.addCallback(future, new FutureCallback<String>() {
                                @Override
                                public void onSuccess(String result) {
                                    Log.d("GQL", result);
                                    ((RecyclerView) view.findViewById(R.id.fragment_chat_open_recyclerview)).setAdapter(new OpenChatAdapter(edges, result));
                                    ((RecyclerView) view.findViewById(R.id.fragment_chat_open_recyclerview)).setLayoutManager(layoutManager);

                                }

                                @Override
                                public void onFailure(Throwable t) {

                                }
                            }, new MainThreadExecutor());

                        } catch (JSONException e) {

                        }
                    } else {
                        Log.d("GQL", response.toString());
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(getContext(), "Loading Error", Toast.LENGTH_SHORT).show();
                    t.printStackTrace();
                    //finish();
                }
            });


        }

        public JSONObject createChatrequestBody(String type) {
            JSONObject data = new JSONObject();
            try {
                data.put("id", "944dff740766");
                JSONObject variables = new JSONObject();
                variables.put("memberStateFilter", type);
                variables.put("after", null);
                variables.put("limit", 20);
                variables.put("order", "LATEST_LAST_MESSAGE");

                JSONArray channelTypes = new JSONArray();
                channelTypes.put("DIRECT");
                channelTypes.put("GROUP");
                variables.put("channelTypes", channelTypes);
                variables.put("isShowingReplicationInfo", true);
                data.put("variables", variables);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return data;
        }
    }
}
