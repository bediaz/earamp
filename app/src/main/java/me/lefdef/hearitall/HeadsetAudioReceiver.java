package me.lefdef.hearitall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

/**
 * Created by u0408275 on 12/8/14.
 */
public class HeadsetAudioReceiver extends BroadcastReceiver
{
    String TAG = "HEADSETAUDIORECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {

        // TODO: Stop playback when the user unplugs the headset

        // TODO: detect ACTION_HEADSET_PLUG
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            // Pause the playback
        } else if(intent.hasExtra("state")) {
            int headsetState = intent.getIntExtra("state", 0); // default to 0 for unplugged
            Log.i(TAG, "STATE=" + headsetState);

            if(headsetState == 0) { // assume anything else is plugged in.
                // TODO: stop audio (if playing), prompt user to plug it back in
            } else {
                // TODO: allow audio playback
            }
        }





    }

}