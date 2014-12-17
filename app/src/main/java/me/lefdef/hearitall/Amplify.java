package me.lefdef.hearitall;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;


public class Amplify {

    //region CONSTANTS
    final String TAG = "AMPLIFY";
    final int SAMPLE_RATE = 44100; // 44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.
    // TODO: test CHANNEL_IN_STEREO
    final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // Guaranteed to be supported by devices. 8BIT is not
    //endregion
    public OnRecordStatusChangeListener _onRecordStatusChangeListener;
    private AudioRecord _audioRecord;
    private AudioTrack _audioTrack;
    private short[] _buffer;
    private short[] _audioBuffer;
    private boolean _isRecording;


    public Amplify() {

        int inBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, inBufferSize);

        // TODO: remove blocking on play
        int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
        _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, outBufferSize, AudioTrack.MODE_STREAM);
        _buffer = new short[inBufferSize];
        _audioBuffer = new short[26214400];
    }

    public boolean isRecording() {
        return _isRecording;
    }

    public int getAudioTrackSessionId() {
        if (_audioTrack != null) {
            return _audioTrack.getAudioSessionId();
        }

        return 0;
    }

    public void repeat() {
        stopListeningAndPlay();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

            _audioTrack.pause();
            _audioTrack.flush();

        int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
        AudioTrack _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, outBufferSize, AudioTrack.MODE_STATIC);
        _audioTrack.mof=


        _audioTrack.write(_audioBuffer, 0, prevBytesRead);
        _audioTrack.setLoopPoints(0, prevBytesRead/2, -1);


        if (_audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
            _audioTrack.play();

        }
        Log.i(TAG, String.format("audiotrack playing=%s", _audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
    }

    public int getAudioRecordSessionId() {
        if (_audioRecord != null) {
            return _audioRecord.getAudioSessionId();
        }

        return 0;
    }

    int prevBytesRead = 0;

    public void startListeningAndPlay() {
        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if (_audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    // publishProgress("AudioRecord Failed to Initialize...");
                    // Log.i(TAG, "AudioRecord Failed to Initialize...");
                    return null;
                }

                int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
                _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, outBufferSize, AudioTrack.MODE_STREAM);

                _audioRecord.startRecording();

                if (_audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    _audioTrack.play();
                }

                //publishProgress("AudioRecord Initialized, AudioTrack playing...");
                //Log.i(TAG, "AudioRecord Initialized, AudioTrack playing...");

                _isRecording = AudioRecord.RECORDSTATE_RECORDING == _audioRecord.getRecordingState();

                int bytesRead;

                while (_isRecording) {
                    bytesRead = _audioRecord.read(_buffer, 0, _buffer.length);
                    System.arraycopy(_buffer, 0, _audioBuffer, prevBytesRead, bytesRead);
                    prevBytesRead += bytesRead;
//                    if(_audioBuffer.length >= prevBytesRead) {
//                        prevBytesRead = 0;
                        Log.i("TAG", String.format("prevBytesRead=%s", prevBytesRead));
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

                if (_onRecordStatusChangeListener != null) {
                    _onRecordStatusChangeListener.onRecordStatusChanged(values[0], _audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);
                }
            }
        }.execute();
    }

    //region OnRecordStatusChangeListener

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

    public OnRecordStatusChangeListener getOnRecordStatusChangeListener() {
        return _onRecordStatusChangeListener;
    }

    public void setOnRecordStatusChangeListener(OnRecordStatusChangeListener onRecordStatusChangeListener) {
        this._onRecordStatusChangeListener = onRecordStatusChangeListener;
    }

    public interface OnRecordStatusChangeListener {
        public void onRecordStatusChanged(String message, boolean status);
    }
    //endregion
}
