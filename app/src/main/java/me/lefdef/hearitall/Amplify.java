package me.lefdef.hearitall;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Date;


public class Amplify {

    //region CONSTANTS
    final String TAG = "AMPLIFY";
    final int SAMPLE_RATE = 44100; // 44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.
    // TODO: test CHANNEL_IN_STEREO
    final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_STEREO;
    final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // Guaranteed to be supported by devices. 8BIT is not
    //endregion


    AudioRecord _audioRecord;
    AudioTrack _audioTrack;
    byte[] _buffer;



    public Amplify() {

        int inBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, inBufferSize);

        // TODO: remove blocking on play
        int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
        _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, outBufferSize, AudioTrack.MODE_STREAM);
        _buffer = new byte[inBufferSize];
    }

    boolean _isRecording;

    public void startListeningAndPlay() {
        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if(_audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    publishProgress("AudioRecord Failed to Initialize...");
                    Log.i(TAG, "AudioRecord Failed to Initialize...");
                    return null;
                }



                _audioRecord.startRecording();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                long lastTime = 0;
                if(_audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    _audioTrack.play();
                }

                publishProgress("AudioRecord Initialized, AudioTrack playing...");
                Log.i(TAG, "AudioRecord Initialized, AudioTrack playing...");

                _isRecording = AudioRecord.RECORDSTATE_RECORDING == _audioRecord.getRecordingState();

                int bytesRead;

                while (_isRecording) {
                    bytesRead = _audioRecord.read(_buffer, 0, 1024);
                   // stream.write(_buffer, 0, bytesRead);

//                    if(System.currentTimeMillis() - lastTime > 1000) {
//                        publishProgress(String.format("bytesRead=%1$s, outputStreamSize=%2$s", bytesRead, stream.size()));
//                        Log.i(TAG, String.format("bytesRead=%1$s, outputStreamSize=%2$s", bytesRead, stream.size()));
//                        lastTime = System.currentTimeMillis();
//                    }

                    _audioTrack.write(_buffer, 0, bytesRead);
                }

                publishProgress("Stopping AudioRecord and AudioTrack...");

                _audioRecord.stop();
                _audioTrack.pause();

                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);

                if(_onRecordStatusChangeListener != null) {
                    _onRecordStatusChangeListener.onRecordStatusChanged(values[0], _audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);
                }
            }
        }.execute();
    }

    public void stopListeningAndPlay() {
        _isRecording = false;

//        new AsyncTask<Void, String, Void>() {
//            @Override
//            protected Void doInBackground(Void... voids) {
//                _audioRecord.stop();
//                publishProgress("stopped...");
//                Log.i(TAG, "stopped...");
//
//                return null;
//            }
//
//            @Override
//            protected void onProgressUpdate(String... values) {
//                super.onProgressUpdate(values);
//                if(_onRecordStatusChangeListener != null) {
//                    _onRecordStatusChangeListener.onRecordStatusChanged(values[0], _audioRecord.getState() == AudioRecord.STATE_INITIALIZED);
//                }
//            }
//        }.execute();
    }

    //region OnRecordStatusChangeListener

    public interface OnRecordStatusChangeListener {
        public void onRecordStatusChanged(String message, boolean status);
    }

    public OnRecordStatusChangeListener _onRecordStatusChangeListener;

    public OnRecordStatusChangeListener getOnRecordStatusChangeListener() {
        return _onRecordStatusChangeListener;
    }

    public void setOnRecordStatusChangeListener(OnRecordStatusChangeListener onRecordStatusChangeListener) {
        this._onRecordStatusChangeListener = onRecordStatusChangeListener;
    }
    //endregion
}
