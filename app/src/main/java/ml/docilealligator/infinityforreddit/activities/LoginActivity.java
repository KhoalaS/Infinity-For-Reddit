package ml.docilealligator.infinityforreddit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.docilealligator.infinityforreddit.FetchMyInfo;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.apis.RedditAccountsAPI;
import ml.docilealligator.infinityforreddit.asynctasks.ParseAndInsertNewAccount;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.customviews.slidr.Slidr;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import ml.docilealligator.infinityforreddit.utils.Utils;
import ml.docilealligator.infinityforreddit.utils.XHmac;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends BaseActivity {

    private static final String ENABLE_DOM_STATE = "EDS";
    private static final String IS_AGREE_TO_USER_AGGREMENT_STATE = "IATUAS";

    @BindView(R.id.login_btn)
    Button loginButton;
    @BindView(R.id.text_username)
    EditText textUsername;
    @BindView(R.id.text_password)
    EditText textPassword;
    @BindView(R.id.coordinator_layout_login_activity)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.appbar_layout_login_activity)
    AppBarLayout appBarLayout;
    @BindView(R.id.toolbar_login_activity)
    Toolbar toolbar;
    @BindView(R.id.two_fa_infO_text_view_login_activity)
    TextView twoFAInfoTextView;
    @BindView(R.id.fab_login_activity)
    FloatingActionButton fab;
    @Inject
    @Named("no_oauth")
    Retrofit mRetrofit;
    @Inject
    @Named("oauth")
    Retrofit mOauthRetrofit;
    @Inject
    @Named("login")
    Retrofit mLoginRetrofit;
    @Inject
    RedditDataRoomDatabase mRedditDataRoomDatabase;
    @Inject
    @Named("default")
    SharedPreferences mSharedPreferences;
    @Inject
    @Named("current_account")
    SharedPreferences mCurrentAccountSharedPreferences;
    @Inject
    CustomThemeWrapper mCustomThemeWrapper;
    @Inject
    Executor mExecutor;
    private String authCode;
    private boolean enableDom = false;

    public static final String formatting(String str, long seconds) {
        String format = String.format(Locale.US, "%d:%s:%d:%d:%s",
                Arrays.copyOf(new Object[] { 1, "android", 2, Long.valueOf(seconds), str }, 5));
        return format;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Infinity) getApplication()).getAppComponent().inject(this);

        setImmersiveModeNotApplicable();

        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_login);
        } catch (InflateException ie) {
            Log.e("LoginActivity", "Failed to inflate LoginActivity: " + ie.getMessage());
            Toast.makeText(LoginActivity.this, R.string.no_system_webview_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ButterKnife.bind(this);

        applyCustomTheme();

        if (mSharedPreferences.getBoolean(SharedPreferencesUtils.SWIPE_RIGHT_TO_GO_BACK, true)) {
            Slidr.attach(this);
        }

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            enableDom = savedInstanceState.getBoolean(ENABLE_DOM_STATE);
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("LOGMOD", "click");
                RedditAccountsAPI api = mLoginRetrofit.create(RedditAccountsAPI.class);
                String username = textUsername.getText().toString();
                String password = textPassword.getText().toString();

                String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

                Map<String, String> loginHeaders = APIUtils.getHttpBasicAuthHeader();

                Locale locale = Locale.US;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                String msg = String.format(locale, "Epoch:%d|Body:%s",
                        Arrays.copyOf(new Object[] { Long.valueOf(seconds), body }, 2));

                String hmacBody = XHmac.getSignedHexString(msg);
                loginHeaders.put("x-hmac-signed-body",formatting(hmacBody, seconds));

                String result = String.format(locale, "Epoch:%d|User-Agent:%s|Client-Vendor-ID:%s",
                        Arrays.copyOf(new Object[] { Long.valueOf(seconds), APIUtils.USER_AGENT, APIUtils.CLIENT_VENDOR_ID }, 3));
                String hmacResult = XHmac.getSignedHexString(result);
                loginHeaders.put("x-hmac-signed-result",formatting(hmacResult, seconds));


                Call<String> loginCall = api.login(loginHeaders, body);

                loginCall.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            try {
                                JSONObject responseJSON = new JSONObject(response.body());
                                if(!responseJSON.getBoolean("success")){
                                    return;
                                }
                                String sessionCookie = response.headers().get("set-cookie");
                                String reddit_session = sessionCookie.split("; ")[0];
                                mCurrentAccountSharedPreferences.edit().putString(SharedPreferencesUtils.SESSION_COOKIE, sessionCookie).putString(SharedPreferencesUtils.REDDIT_SESSION, reddit_session).apply();

                                Map<String, String> accessTokenHeaders = APIUtils.getHttpBasicAuthHeader();
                                accessTokenHeaders.put("cookie", reddit_session);
                                Call<String> accessTokenCall = api.getAccessToken(accessTokenHeaders, APIUtils.SCOPE_ALL);
                                accessTokenCall.enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        if (response.isSuccessful()) {
                                            String accountResponse = response.body();
                                            if (accountResponse == null) {
                                                //Handle error
                                                return;
                                            }

                                            JSONObject responseJSON = null;
                                            try {
                                                responseJSON = new JSONObject(accountResponse);
                                                String accessToken = responseJSON.getString(APIUtils.ACCESS_TOKEN_KEY);
                                                int expiry = responseJSON.getInt(APIUtils.EXPIRY_TS_KEY);

                                                FetchMyInfo.fetchAccountInfo(mOauthRetrofit, mRedditDataRoomDatabase,
                                                        accessToken, new FetchMyInfo.FetchMyInfoListener() {
                                                            @Override
                                                            public void onFetchMyInfoSuccess(String name, String profileImageUrl, String bannerImageUrl, int karma) {
                                                                mCurrentAccountSharedPreferences.edit().putString(SharedPreferencesUtils.ACCESS_TOKEN, accessToken)
                                                                        .putString(SharedPreferencesUtils.ACCOUNT_NAME, name)
                                                                        .putInt(APIUtils.EXPIRY_TS_KEY, expiry)
                                                                        .putString(SharedPreferencesUtils.ACCOUNT_IMAGE_URL, profileImageUrl).apply();
                                                                ParseAndInsertNewAccount.parseAndInsertNewAccount(mExecutor, new Handler(), name, accessToken, "", profileImageUrl, bannerImageUrl,
                                                                        karma, authCode, mRedditDataRoomDatabase.accountDao(),
                                                                        () -> {
                                                                            Intent resultIntent = new Intent();
                                                                            setResult(Activity.RESULT_OK, resultIntent);
                                                                            finish();
                                                                        });
                                                            }

                                                            @Override
                                                            public void onFetchMyInfoFailed(boolean parseFailed) {
                                                                if (parseFailed) {
                                                                    Toast.makeText(LoginActivity.this, R.string.parse_user_info_error, Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    Toast.makeText(LoginActivity.this, R.string.cannot_fetch_user_info, Toast.LENGTH_SHORT).show();
                                                                }

                                                                finish();
                                                            }
                                                        });
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        Toast.makeText(LoginActivity.this, R.string.retrieve_token_error, Toast.LENGTH_SHORT).show();
                                        t.printStackTrace();
                                        finish();
                                    }
                                });
                            } catch (JSONException e){

                            }


                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Toast.makeText(LoginActivity.this, "Login Error", Toast.LENGTH_SHORT).show();
                        t.printStackTrace();
                        finish();
                    }
                });

            }
        });

        fab.setOnClickListener(view -> {
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
                    .setTitle(R.string.have_trouble_login_title)
                    .setMessage(R.string.have_trouble_login_message)
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        enableDom = !enableDom;
                        ActivityCompat.recreate(this);
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });

        textPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginButton.performClick();
                    return true;
                }
                return false;
            }
        });

        if (enableDom) {
            twoFAInfoTextView.setVisibility(View.GONE);
        }

        Uri baseUri = Uri.parse(APIUtils.OAUTH_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        uriBuilder.appendQueryParameter(APIUtils.CLIENT_ID_KEY, APIUtils.CLIENT_ID);
        uriBuilder.appendQueryParameter(APIUtils.RESPONSE_TYPE_KEY, APIUtils.RESPONSE_TYPE);
        uriBuilder.appendQueryParameter(APIUtils.STATE_KEY, APIUtils.STATE);
        uriBuilder.appendQueryParameter(APIUtils.REDIRECT_URI_KEY, APIUtils.REDIRECT_URI);
        uriBuilder.appendQueryParameter(APIUtils.DURATION_KEY, APIUtils.DURATION);
        uriBuilder.appendQueryParameter(APIUtils.SCOPE_KEY, APIUtils.SCOPE);

        String url = uriBuilder.toString();

        CookieManager.getInstance().removeAllCookies(aBoolean -> {
        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ENABLE_DOM_STATE, enableDom);
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
    protected void applyCustomTheme() {
        coordinatorLayout.setBackgroundColor(mCustomThemeWrapper.getBackgroundColor());
        applyAppBarLayoutAndCollapsingToolbarLayoutAndToolbarTheme(appBarLayout, null, toolbar);
        twoFAInfoTextView.setTextColor(mCustomThemeWrapper.getPrimaryTextColor());
        Drawable infoDrawable = Utils.getTintedDrawable(this, R.drawable.ic_info_preference_24dp, mCustomThemeWrapper.getPrimaryIconColor());
        twoFAInfoTextView.setCompoundDrawablesWithIntrinsicBounds(infoDrawable, null, null, null);
        applyFABTheme(fab);
        if (typeface != null) {
            twoFAInfoTextView.setTypeface(typeface);
        }
        textUsername.setTextColor(mCustomThemeWrapper.getPrimaryTextColor());
        textPassword.setTextColor(mCustomThemeWrapper.getPrimaryTextColor());
        loginButton.setBackgroundColor(customThemeWrapper.getColorAccent());

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }
}
