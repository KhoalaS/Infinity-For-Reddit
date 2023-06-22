package ml.docilealligator.infinityforreddit.user;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

import ml.docilealligator.infinityforreddit.utils.APIUtils;

public class UseragentUtil {
    public static String getUserAgent(String appname, String username) {
        return String.format("android:%s:v1.0 (by /u/%s)", appname, username);
    }
    

}
