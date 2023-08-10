package ml.docilealligator.infinityforreddit.utils;

import androidx.annotation.NonNull;

import org.matrix.android.sdk.api.provider.RoomDisplayNameFallbackProvider;

import java.util.List;

public class RoomDisplayNameFallbackProviderImpl implements RoomDisplayNameFallbackProvider {

    @NonNull
    @Override
    public String getNameFor1member(@NonNull String s) {
        return s;
    }

    @NonNull
    @Override
    public String getNameFor2members(@NonNull String s, @NonNull String s1) {
        return String.format("%s and %s", s, s1);
    }

    @NonNull
    @Override
    public String getNameFor3members(@NonNull String s, @NonNull String s1, @NonNull String s2) {
        return String.format("%s, %s and %s", s, s1, s2);
    }

    @NonNull
    @Override
    public String getNameFor4members(@NonNull String s, @NonNull String s1, @NonNull String s2, @NonNull String s3) {
        return "";
    }

    @NonNull
    @Override
    public String getNameFor4membersAndMore(@NonNull String s, @NonNull String s1, @NonNull String s2, int i) {
        return "";
    }

    @NonNull
    @Override
    public String getNameForEmptyRoom(boolean b, @NonNull List<String> list) {
        return "Empty room";
    }

    @NonNull
    @Override
    public String getNameForRoomInvite() {
        return "Room invite";
    }
}

