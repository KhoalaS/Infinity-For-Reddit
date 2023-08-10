package ml.docilealligator.infinityforreddit.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;

import org.matrix.android.sdk.api.Matrix;
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig;
import org.matrix.android.sdk.api.auth.data.LoginFlowResult;
import org.matrix.android.sdk.api.session.Session;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.SessionHolder;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.customviews.slidr.Slidr;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;


public class ChatOverviewActivity extends BaseActivity {

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
        HomeServerConnectionConfig homeServerConnectionConfig;

        try{
            homeServerConnectionConfig = new HomeServerConnectionConfig
                    .Builder()
                    .withHomeServerUri(Uri.parse("https://reddit.matrixspace.com/"))
                    .build();
        } catch (Exception e) {
            Toast.makeText(this, "Home server is not valid", Toast.LENGTH_SHORT).show();
            return;
        }

        Matrix.Companion.getInstance(this).authenticationService().getLoginFlow(homeServerConnectionConfig, new Continuation<>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                login();
            }
        });
    }

    public void login(){
        String accessToken = mCurrentAccountSharedPreferences.getString(SharedPreferencesUtils.ACCESS_TOKEN, "");
        Map<String, Object> data = new HashMap<>();
        data.put("token", accessToken);
        data.put("initial_device_display_name", "Reddit Matrix Android");
        data.put("type", "com.reddit.token");

        try {
            Matrix.Companion.getInstance(this).authenticationService().getLoginWizard().loginCustom(data, new Continuation<Session>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    Session session = (Session) o;
                    SessionHolder.INSTANCE.setCurrentSession(session);
                    session.open();
                    session.startSync(true);
                    displayRoomList();
                }
            });
        } catch (Exception e){
            Toast.makeText(this, String.format("Failure: %s", e.getCause()), Toast.LENGTH_SHORT).show();
        }
    }

    public void displayRoomList(){
        FragmentManager fragmentManager = getSupportFragmentManager();

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
}
