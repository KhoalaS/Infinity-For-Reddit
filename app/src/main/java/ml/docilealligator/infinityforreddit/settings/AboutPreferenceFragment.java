package ml.docilealligator.infinityforreddit.settings;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import ml.docilealligator.infinityforreddit.BuildConfig;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.activities.LinkResolverActivity;
import ml.docilealligator.infinityforreddit.customviews.CustomFontPreferenceFragmentCompat;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;

/**
 * A simple {@link PreferenceFragmentCompat} subclass.
 */
public class AboutPreferenceFragment extends CustomFontPreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.about_preferences, rootKey);

        Preference versionPreference = findPreference(SharedPreferencesUtils.VERSION_KEY);

        if (versionPreference != null) {
            versionPreference.setSummary(getString(R.string.settings_version_summary, BuildConfig.VERSION_NAME));

            versionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                int clickedTimes = 0;

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clickedTimes++;
                    if (clickedTimes > 6) {
                        Toast.makeText(activity, R.string.no_developer_easter_egg, Toast.LENGTH_SHORT).show();
                        clickedTimes = 0;
                    }
                    return true;
                }
            });
        }
    }
}
