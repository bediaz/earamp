package me.lefdef.hearitall;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


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
    private short[] _audioCache;
    private LinkedList<Integer> _cacheIndices;
    private boolean _isRecording;

    // cache variables
    //private int cacheIndex = 0;
    private int repeatLength = 10; // seconds to keep of audio cache
    private int _totalBytesRead = 0;

    private int _intervals = 0;

    private final ScheduledExecutorService _audioCacheService = Executors.newScheduledThreadPool(1); // create a single thread
    private ScheduledFuture _audioCacheServiceHandle;
    private int MAX_BUFFER;


    public Amplify() {

        int inBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, inBufferSize);

        // TODO: remove blocking on play
        int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

        _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, outBufferSize, AudioTrack.MODE_STREAM);
        _buffer = new short[inBufferSize];
        _audioCache = new short[outBufferSize*200];
        MAX_BUFFER =  (int)(_audioCache.length * 0.80);

        _cacheIndices = new LinkedList<Integer>();

        // TODO: setup timer/scheduled executor
    }

    private void refreshCacheIndices() {

        while(_cacheIndices.size() >= repeatLength && _cacheIndices.size() > 0) {
            _cacheIndices.removeFirst();
        }
        _cacheIndices.addLast(_totalBytesRead);

        _intervals++;
        Log.i(TAG, String.format("refreshCacheIndices=%s, totalBytesRead=%s, capacity=%.2f", _intervals, _totalBytesRead, 100.0*(float)_totalBytesRead/_audioCache.length));
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
        if(_totalBytesRead == 0) { return; }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int startFrame = _cacheIndices.isEmpty() ? 0 : _cacheIndices.getFirst();

        if (_audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
            _audioTrack.play();
            Log.i(TAG, String.format("audioTrack repeat isPlaying=%b", _audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING));
        }

        Log.i(TAG, String.format("repeat(): refreshCacheIndices=%d, startFrame=%d, totalBytesRead=%d", _intervals, startFrame, _totalBytesRead));
        //_audioTrack.setNotificationMarkerPosition(_audioTrack.getPlaybackHeadPosition() + (duration * sampleRate));
        _audioTrack.setNotificationMarkerPosition(_totalBytesRead - 1);
        Log.i(TAG, String.format("repeat(): setNotificationMarkerPosition=%d", _totalBytesRead - 1));
        _audioTrack.write(_audioCache, startFrame, _totalBytesRead - startFrame - 1);
        Log.i(TAG, String.format("write: _audioCacheSize=%d, begin=%d, end=%d", _audioCache.length, startFrame, _totalBytesRead - startFrame - 1));

        _audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.i(TAG, String.format("repeat playback done. isPlaying=%s, playbackPosition=%d",_audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING, _audioTrack.getPlaybackHeadPosition()));
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {

            }
        });
    }

    public int getAudioRecordSessionId() {
        if (_audioRecord != null) {
            return _audioRecord.getAudioSessionId();
        }

        return 0;
    }

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
                _isRecording = AudioRecord.RECORDSTATE_RECORDING == _audioRecord.getRecordingState();

                int bytesRead;

                if(_isRecording) { // call once
                    _audioCacheServiceHandle = _audioCacheService.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            refreshCacheIndices();
                        }
                    }, 0, 1000, TimeUnit.MILLISECONDS); // start immediately, run every 1000ms
                }

                while (_isRecording) {

                    bytesRead = _audioRecord.read(_buffer, 0, _buffer.length);
                    _audioTrack.write(_buffer, 0, bytesRead);
                    audioBufferProcessing(bytesRead);
                    //Log.i("TAG", String.format("_totalBytesRead=%s", _totalBytesRead));

                }

                if(_audioCacheServiceHandle != null) {
                    _audioCacheServiceHandle.cancel(true);
                }
                _audioRecord.stop();

                _audioTrack.pause();
                _audioTrack.flush();

                Log.i(TAG, String.format("isRecording=%b, audioCacheService isTerminated=%b",
                        _isRecording,
                        _audioCacheService.isTerminated()));

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

    private void audioBufferProcessing(int bytesRead) {
        System.arraycopy(_buffer, 0, _audioCache, _totalBytesRead, bytesRead);

        _totalBytesRead += bytesRead;
        if(_totalBytesRead >= MAX_BUFFER) { // if MAX_BUFFER hit then reset index to beginning of buffer
            Log.i(TAG, "Trimming the audioCache");
            int startFrame = _cacheIndices.isEmpty() ? 0 : _cacheIndices.getFirst();
            short[] temp = new short[_audioCache.length];

            // API call: System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            // _audioCache << (totalBytesRead - startFrame)
            System.arraycopy(_audioCache, startFrame, temp, 0, _totalBytesRead - startFrame);
            _audioCache = new short[temp.length];
            System.arraycopy(temp, 0, _audioCache, 0, _totalBytesRead - startFrame);
            _totalBytesRead -= startFrame;

            // adjust frame values of linked list
            for(int position = 0; position < _cacheIndices.size(); position++) {
                _cacheIndices.set(position, _cacheIndices.get(position) - startFrame);
            }
        }
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
