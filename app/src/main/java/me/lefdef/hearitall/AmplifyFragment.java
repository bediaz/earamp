package me.lefdef.hearitall;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Brigham on 12/13/2014.
 */
public class AmplifyFragment extends Fragment {

    final static String TAG = "AMPLIFY_FRAGMENT";

    // region XML UI

    private CircleButton _off_onButton;
    private CircleButton _repeatButton;
    private TextView _volumeText;
    private SeekBar _volSeekBar;

    //endregion

    private Amplify _amplify;
    private Visualizer _visualizer;
    private VisualizerView _visualizerView;
    private AudioManager _audioManager;

    private HeadsetAudioReceiver _audioStreamReceiver;
    private IntentFilter _intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // layout stuff
        View rootView = inflater.inflate(R.layout.fragment_amplify, container, false);

        _off_onButton = (CircleButton) rootView.findViewById(R.id.off_on_button);
        _repeatButton = (CircleButton) rootView.findViewById(R.id.repeat_button);
        _volumeText = (TextView) rootView.findViewById(R.id.volume_percent);
        _volSeekBar = (SeekBar) rootView.findViewById(R.id.volume_bar);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // audio setup
        getActivity().setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);

        _audioStreamReceiver = new HeadsetAudioReceiver();

        _audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        _audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        _amplify = new Amplify();
        _amplify.setOnRecordStatusChangeListener(new Amplify.OnRecordStatusChangeListener() {
            @Override
            public void onRecordStatusChanged(String message, boolean status) {
//                _amplifyTextView.setText(String.format(message));//"Switch enabled? %s", _enable_switch.isChecked()));
                String state = String.format("Status: %s", status ? "Recording" : "Not Recording");
//                _amplifyTextViewState.setText(state);
                Log.i(TAG, state);

            }
        });

        _off_onButton.setSoundEffectsEnabled(false);

        _off_onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_amplify.isRecording()) {
                    _amplify.stopListeningAndPlay();
                    _off_onButton.setImageResource(R.drawable.ic_action_listen_off);
                    _off_onButton.setLabel(getResources().getString(R.string.action_listen_off));
                    _off_onButton.setColor(getResources().getColor(R.color.blue_gray));
                } else {
                    _amplify.startListeningAndPlay();
                    _off_onButton.setImageResource(R.drawable.ic_action_listen_on);
                    _off_onButton.setLabel(getResources().getString(R.string.action_listen_on));
                    _off_onButton.setColor(getResources().getColor(R.color.light_green));
                }

            }
        });

        _repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_amplify.isRecording()) {
                   _amplify.repeat();
                }
            }
        });

        _volSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                _volumeText.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        _visualizer = new Visualizer(_amplify.getAudioTrackSessionId());
        _visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        _visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {

            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {

            }
            //TODO: check if 44100Hz is enough for capture rate, or getMaxCaptureRate / 2
        }, Visualizer.getMaxCaptureRate(), true, false); // don't need onFftDataCapture

//1        _visualizerView.setEnabled(true);
    }

    private void startPlayback() {
        getActivity().registerReceiver(_audioStreamReceiver, _intentFilter);
    }

    private void stopPlayback() {
        getActivity().unregisterReceiver(_audioStreamReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_amplify.isRecording()) {
            _amplify.stopListeningAndPlay();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (_amplify.isRecording()) {
            _amplify.stopListeningAndPlay();
        }
    }
}

/**
 * A simple class that draws waveform data received from a
 * {@link android.media.audiofx.Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
class VisualizerView extends View {
    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();

    private Paint mForePaint = new Paint();

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mBytes = null;

        mForePaint.setStrokeWidth(1f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.rgb(0, 128, 255));
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        mRect.set(0, 0, getWidth(), getHeight());

        for (int i = 0; i < mBytes.length - 1; i++) {
            mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
            mPoints[i * 4 + 1] = mRect.height() / 2
                    + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
            mPoints[i * 4 + 3] = mRect.height() / 2
                    + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
        }

        canvas.drawLines(mPoints, mForePaint);
    }
}