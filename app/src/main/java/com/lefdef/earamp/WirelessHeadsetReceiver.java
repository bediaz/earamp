package com.lefdef.earamp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static android.bluetooth.BluetoothHeadset.STATE_AUDIO_CONNECTED;
import static android.bluetooth.BluetoothHeadset.STATE_AUDIO_CONNECTING;
import static android.bluetooth.BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.EXTRA_STATE;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;

/**
 * Created by Brigham on 12/20/2014.
 */
public class WirelessHeadsetReceiver extends BroadcastReceiver {
    private static final String TAG = "WirelessHeadsetReceiver";
    public static final String HEADSET_STATE_CHANGED = "android.bluetooth.headset.action.STATE_CHANGED";
    public static final String HEADSET_AUDIO_STATE_CHANGED = "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED";
    public static final String HEADSET_CONNECTION_STATE_CHANGED = "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED";

    @Override
    public void onReceive(final Context context, final Intent intent) {

        boolean broadcast;
        int state = STATE_AUDIO_DISCONNECTED;
        BluetoothDevice btDevice;

        if (HEADSET_STATE_CHANGED.equals(intent.getAction())
                || HEADSET_AUDIO_STATE_CHANGED.equals(intent.getAction())
                || HEADSET_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
            state = intent.getIntExtra(EXTRA_STATE, STATE_AUDIO_DISCONNECTED);
            // TODO: pass the bluetooth device name to service
            btDevice = intent.getParcelableExtra(EXTRA_DEVICE);
        }

        switch (state) {
            case STATE_CONNECTING:
            case STATE_AUDIO_CONNECTING: // TODO: determine when this state occur
                // TODO: test for this case
                broadcast = false;
                break;
            case STATE_CONNECTED:
            case STATE_AUDIO_CONNECTED:
                broadcast = true;
                break;
            case STATE_DISCONNECTING:
            case STATE_DISCONNECTED:
            case STATE_AUDIO_DISCONNECTED:
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