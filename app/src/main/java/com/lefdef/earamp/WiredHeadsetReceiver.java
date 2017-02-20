package com.lefdef.earamp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;


/**
 * Created by Brigham on 12/20/2014.
 */
public class WiredHeadsetReceiver  extends BroadcastReceiver {
    private static final String TAG = "WiredHeadsetReceiver";
    public static final String HEADSET_ACTION_PLUG = "android.intent.action.HEADSET_PLUG";

    /* wired audio heaset specific values
     * https://developer.android.com/reference/android/media/AudioManager.html#ACTION_HEADSET_PLUG */
    private final int UNPLUGGED = 0;
    private final int PLUGGED_IN = 1;
    public boolean wiredHeadsetHasMicrophone = false;
    public String deviceName; // Headset type, human readable string


    @Override
    public void onReceive(final Context context, final Intent intent) {

        boolean broadcast;
        int state = UNPLUGGED;

        if (HEADSET_ACTION_PLUG.equals(intent.getAction())) {
            state = intent.getIntExtra("state", UNPLUGGED);
            deviceName = intent.getStringExtra("name");
            wiredHeadsetHasMicrophone = intent.getBooleanExtra("microphone", false);
        }

        switch(state) {
            case UNPLUGGED:
                broadcast = false;
                break;
            case PLUGGED_IN:
                broadcast = true;
                break;
            default:
                broadcast = false;
                break;
        }

        // Used to inform interested activities that the headset state has changed
        if (broadcast) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("headsetStateChange"));
        }
    }
}
