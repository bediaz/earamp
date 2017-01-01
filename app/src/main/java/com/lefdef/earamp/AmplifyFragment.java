package com.lefdef.earamp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

/**
 * Created by Brigham on 12/13/2014.
 */
public class AmplifyFragment extends Fragment {

    static final String TAG = "AMPLIFY_FRAGMENT";
    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    public static final int MY_PERMISSIONS_REQUEST_MODIFY_AUDIO_SETTINGS = 1;
    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    public static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 3;
    // region XML UI

    private CircleButton btnOnOff;
    private CircleButton btnRepeat;
    private SeekBar seekBarVolume;

    private Amplify amplify;
    private AudioManager audioManager;
    private Visualizer visualizer;
    private VisualizerView visualizerView;

    private HeadsetAudioReceiver audioStreamReceiver;
    private IntentFilter audioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private HeadSetIntentReceiver headSetIntentReceiver = new HeadSetIntentReceiver();

    private Context context;
    private int volumeLevel;
    private boolean headsetConnected = false;
    private boolean bluetoothConnected = false;


    private void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS}, MY_PERMISSIONS_REQUEST_MODIFY_AUDIO_SETTINGS);
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupVisualizerFxAndUI();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    private void onMessageReceived(int state) {
        if (state != 1) {
            amplify.stopListeningAndPlay();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        getActivity().registerReceiver(headSetIntentReceiver, filter);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        context = container.getContext();
        // layout stuff
        final RelativeLayout rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_amplify, container, false);

        btnOnOff = (CircleButton) rootView.findViewById(R.id.off_on_button);
        btnRepeat = (CircleButton) rootView.findViewById(R.id.repeat_button);
        seekBarVolume = (SeekBar) rootView.findViewById(R.id.volume_bar);

        // audio setup
        //getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //audioStreamReceiver = new HeadsetAudioReceiver();
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        amplify = Amplify.getInstance();
        amplify.setOnPlayStateChangedListener(new Amplify.OnPlayStateChangedListener() {
            @Override
            public void OnPlayStateChanged() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshView();
                    }
                });
            }
        });
        int max_volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBarVolume.setMax(max_volume);
        Log.i(TAG, "max_volume=" + max_volume);
        seekBarVolume.setProgress(volumeLevel);
        Log.i(TAG, "volumeLevel=" + volumeLevel);

        visualizerView = (VisualizerView) rootView.findViewById(R.id.visualizerView);

        requestPermissions();

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            setupVisualizerFxAndUI();
        }

        visualizerView.refresh();

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        btnOnOff.setSoundEffectsEnabled(false);

        btnOnOff.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) { /* warning */
                if (!headsetConnected && !amplify.isRecording()) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Warning")
                            .setMessage("No headset was detected. This may cause a very loud feedback loop. Are you sure you want to proceed?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    startListeningAndPlayback();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    stopListeningAndPlayback();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                } else {
                    if (amplify.isRecording()) {
                        stopListeningAndPlayback();
                    } else {
                        startListeningAndPlayback();
                    }
                }

            }
        });

        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopListeningAndPlayback();
                amplify.startRepeat();
                setupVisualizerFxAndUI();
            }
        });

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    public void refreshView() {
        if (amplify.isRecording()) {
            btnOnOff.setImageResource(R.drawable.ic_action_listen_on);
            btnOnOff.setColor(ContextCompat.getColor(context, R.color.light_green));
            btnOnOff.invalidate();
        } else {
            btnOnOff.setImageResource(R.drawable.ic_action_listen_off);
            btnOnOff.setColor(ContextCompat.getColor(context, R.color.blue_gray));
            btnOnOff.invalidate();
        }
    }

    private void stopListeningAndPlayback() {
        amplify.stopListeningAndPlay();
        Log.i(TAG, "stopListeningAndPlayback");

    }

    private void startListeningAndPlayback() {
        Log.i(TAG, "startListeningAndPlayback");
        amplify.startListeningAndPlay();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop listening for button presses
        amplify.stopListeningAndPlay();
        visualizer.release();
        getActivity().unregisterReceiver(headSetIntentReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        amplify.stopListeningAndPlay();
    }


    private void setupVisualizerFxAndUI() {
        // Assume thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

            // Create the Visualizer object and attach it to our media player.
        Log.i(TAG, "AudioTrack audio session ID: " + amplify.getAudioTrackSessionId());
        visualizer = new Visualizer(0);
        if (visualizer.getEnabled()) {
            visualizer.setEnabled(false);
        }
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {
                visualizerView.updateVisualizer(bytes);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }
        }, Visualizer.getMaxCaptureRate(), true, false);

        visualizer.setEnabled(true);
    }

    private class HeadSetIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        onMessageReceived(state);
                        Log.d(TAG, "Headset is unplugged");
                        headsetConnected = false;
                        break;
                    case 1:
                        onMessageReceived(state);
                        Log.d(TAG, "Headset is plugged");
                        headsetConnected = true;
                        break;
                    default:
                        onMessageReceived(-1);
                        Log.d(TAG, "unrecognized state: " + state);
                        break;
                }
            }
        }
    }

}