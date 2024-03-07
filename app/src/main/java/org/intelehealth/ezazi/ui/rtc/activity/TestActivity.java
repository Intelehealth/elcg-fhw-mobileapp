package org.intelehealth.ezazi.ui.rtc.activity;

import androidx.annotation.NonNull;

import org.intelehealth.klivekit.ui.activity.CoreVideoCallActivity;

import io.livekit.android.renderer.SurfaceViewRenderer;
import io.livekit.android.renderer.TextureViewRenderer;
import io.livekit.android.room.track.VideoTrack;

/**
 * Created by Vaghela Mithun R. on 01-02-2024 - 12:09.
 * Email : mithun@intelehealth.org
 * Mob   : +919727206702
 **/
public class TestActivity extends CoreVideoCallActivity {
    @Override
    public void attachLocalVideo(@NonNull VideoTrack videoTrack) {

    }

    @Override
    public void attachRemoteVideo(@NonNull VideoTrack videoTrack) {

    }

    @NonNull
    @Override
    public TextureViewRenderer getLocalVideoRender() {
        return null;
    }

    @NonNull
    @Override
    public SurfaceViewRenderer getRemoteVideoRender() {
        return null;
    }
}
