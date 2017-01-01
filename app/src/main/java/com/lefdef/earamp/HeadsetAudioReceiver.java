package com.lefdef.earamp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Brigham on 12/8/14.
 * Reference:
 * http://stackoverflow.com/questions/16409309/how-to-detect-headphone-plug-event-in-offline-mode
 */
public class HeadsetAudioReceiver extends BroadcastReceiver {
    private static final String TAG = "HEADSETAUDIORECEIVER";

    public static final String[] HEADPHONE_ACTIONS = {
            Intent.ACTION_HEADSET_PLUG,
            "android.bluetooth.headset.action.STATE_CHANGED",
            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"
    };


    @Override
    public void onReceive(final Context context, final Intent intent) {

        boolean broadcast = false;

        // Wired headset monitoring
        if (intent.getAction().equals(HEADPHONE_ACTIONS[0])) {
            final int state = intent.getIntExtra("state", 0);
            broadcast = true;
        }

        // Bluetooth monitoring
        // Works up to and including Honeycomb
        if (intent.getAction().equals(HEADPHONE_ACTIONS[1])) {
            int state = intent.getIntExtra("android.bluetooth.headset.extra.STATE", 0);
            broadcast = true;
        }

        // Works for Ice Cream Sandwich
        if (intent.getAction().equals(HEADPHONE_ACTIONS[2])) {
            int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
            broadcast = true;
        }

        // Used to inform interested activities that the headset state has changed
        if (broadcast) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("headsetStateChange"));
        }

    }

}