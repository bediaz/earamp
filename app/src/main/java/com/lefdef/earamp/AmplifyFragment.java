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

    final static String TAG = "AMPLIFY_FRAGMENT";
    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    public static final int MY_PERMISSIONS_REQUEST_MODIFY_AUDIO_SETTINGS = 1;
    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    public static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 3;
    // region XML UI

    private CircleButton _off_onButton;
    private CircleButton _repeatButton;
    private SeekBar _volSeekBar;

    //endregion

    private Amplify _amplify;
    private AudioManager _audioManager;
    private Visualizer _visualizer;
    private VisualizerView _visualizerView;
    private RelativeLayout _rootView;
    private Context _context;

    private HeadsetAudioReceiver _audioStreamReceiver;
    private IntentFilter _audioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private HeadSetIntentReceiver _headSetIntentReceiver = new HeadSetIntentReceiver();

    private int _volumeLevel;
    private boolean _headsetConnected = false;
    private boolean _bluetoothConnected = false;


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
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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
            _amplify.stopListeningAndPlay();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        getActivity().registerReceiver(_headSetIntentReceiver, filter);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        _context = container.getContext();
        // layout stuff
        _rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_amplify, container, false);

        _off_onButton = (CircleButton) _rootView.findViewById(R.id.off_on_button);
        _repeatButton = (CircleButton) _rootView.findViewById(R.id.repeat_button);
        _volSeekBar = (SeekBar) _rootView.findViewById(R.id.volume_bar);

        // audio setup
        //getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //_audioStreamReceiver = new HeadsetAudioReceiver();
        _audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        // Request audio focus for playback
        int result = _audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        _amplify = Amplify.getInstance();
        _amplify.setOnPlayStateChangedListener(new Amplify.OnPlayStateChangedListener() {
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
        int max_volume = _audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        _volumeLevel = _audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        _volSeekBar.setMax(max_volume);
        Log.i(TAG, "max_volume=" + max_volume);
        _volSeekBar.setProgress(_volumeLevel);
        Log.i(TAG, "_volumeLevel=" + _volumeLevel);

        _visualizerView = (VisualizerView) _rootView.findViewById(R.id.visualizerView);

        requestPermissions();

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            setupVisualizerFxAndUI();
        }

        _visualizerView.refresh();

        return _rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        _off_onButton.setSoundEffectsEnabled(false);

        _off_onButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) { /* warning */
                if (_headsetConnected == false && !_amplify.isRecording()) {
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
                    if (_amplify.isRecording()) {
                        stopListeningAndPlayback();
                    } else {
                        startListeningAndPlayback();
                    }
                }

            }
        });

        _repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopListeningAndPlayback();
                _amplify.startRepeat();
                setupVisualizerFxAndUI();
            }
        });

        _volSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                _audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
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
        if (_amplify.isRecording()) {
            _off_onButton.setImageResource(R.drawable.ic_action_listen_on);
            //_off_onButton.setLabel(getResources().getString(R.string.action_listen_on));
            _off_onButton.setColor(getResources().getColor(R.color.light_green));
            _off_onButton.invalidate();
        } else {
            _off_onButton.setImageResource(R.drawable.ic_action_listen_off);
            //_off_onButton.setLabel(getResources().getString(R.string.action_listen_off));
            _off_onButton.setColor(getResources().getColor(R.color.blue_gray));
            _off_onButton.invalidate();
        }

    }

    private void stopListeningAndPlayback() {
        _amplify.stopListeningAndPlay();
        Log.i(TAG, "stopListeningAndPlayback");

    }

    private void startListeningAndPlayback() {
        Log.i(TAG, "startListeningAndPlayback");
        _amplify.startListeningAndPlay();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop listening for button presses
        _amplify.stopListeningAndPlay();
        _visualizer.release();
        getActivity().unregisterReceiver(_headSetIntentReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        _amplify.stopListeningAndPlay();
    }


    private void setupVisualizerFxAndUI() {
        // Assume thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

            // Create the Visualizer object and attach it to our media player.
        Log.i(TAG, "AudioTrack audio session ID: " + _amplify.getAudioTrackSessionId());
        _visualizer = new Visualizer(0);
        if (_visualizer.getEnabled()) {
            _visualizer.setEnabled(false);
        }
        _visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        _visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {
                _visualizerView.updateVisualizer(bytes);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }
        }, Visualizer.getMaxCaptureRate(), true, false);

        _visualizer.setEnabled(true);
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
                        _headsetConnected = false;
                        break;
                    case 1:
                        onMessageReceived(state);
                        Log.d(TAG, "Headset is plugged");
                        _headsetConnected = true;
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