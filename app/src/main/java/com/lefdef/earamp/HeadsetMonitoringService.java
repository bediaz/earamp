package com.lefdef.earamp;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Created by Brigham on 12/20/2014.
 * Reference:
 * http://stackoverflow.com/questions/16409309/how-to-detect-headphone-plug-event-in-offline-mode
 */
public class HeadsetMonitoringService extends Service {

    HeadsetAudioReceiver headsetStateReceiver;

    @Override
    public void onCreate() {

        headsetStateReceiver = new HeadsetAudioReceiver();
        final IntentFilter filter = new IntentFilter();
        for (String action : HeadsetAudioReceiver.HEADPHONE_ACTIONS) {
            filter.addAction(action);
        }

        registerReceiver(headsetStateReceiver, filter);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(headsetStateReceiver);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

}