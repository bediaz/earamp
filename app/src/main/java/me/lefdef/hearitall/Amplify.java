package me.lefdef.hearitall;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

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
    private int[] _cacheIndices;
    private boolean _isRecording;

    // cache variables
    private int cacheIndex = 0;
    private int repeatLength = 10; // seconds to keep of audio cache
    private int _totalBytesRead = 0;

    private int _intervals = 0;

    private final ScheduledExecutorService _audioCacheService = Executors.newScheduledThreadPool(1); // create a single thread
    private ScheduledFuture _audioCacheServiceHandle;


    public Amplify() {

        int inBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, inBufferSize);

        // TODO: remove blocking on play
        int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
        _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, outBufferSize, AudioTrack.MODE_STREAM);
        _buffer = new short[inBufferSize];
        _audioCache = new short[_buffer.length*200];
        _cacheIndices = new int[repeatLength];

        // TODO: setup timer/scheduled executor
    }

    private void refreshCacheIndices() {

        if(cacheIndex < _cacheIndices.length) {
            _cacheIndices[cacheIndex] = _totalBytesRead;
        }
        else {
            Log.e(TAG, "Cache index is larger than cache size!");
            throw new IndexOutOfBoundsException("Cache index is larger than cache size!");
        }

        cacheIndex = cacheIndex >= _cacheIndices.length - 1 ? 0 : ++cacheIndex; // what did i just write? anyway it resets the cacheindex..

        _intervals++;
        Log.i(TAG, String.format("refreshCacheIndices=%s, cacheIndex=%s, totalBytesRead=%s, capacity=%.2f", _intervals, cacheIndex, _totalBytesRead, 100.0*(float)_totalBytesRead/_audioCache.length));
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

        // give it a second for audio to stop playing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        // logic here is that in a revolving array index the bucket farthest from current index will be index + 1. The bucket closes will be index - 1 since that was prior index.
        // Except if your in tail position then the head is farthest so 9 -> 0
        int startBucket = cacheIndex == _cacheIndices.length - 1 ? 0 : cacheIndex + 1;
        if(_intervals < 10) {
            startBucket = 0;
        }
        int startFrame = _cacheIndices[startBucket];

        if (_audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
            _audioTrack.play();
            Log.i(TAG, String.format("audioTrack repeat isPlaying=%b", _audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING));
        }

        Log.i(TAG, String.format("repeat(): refreshCacheIndices=%d, cacheIndex=%d, startFrame=%d, totalBytesRead=%d", _intervals, cacheIndex, startFrame, _totalBytesRead));
        _audioTrack.setNotificationMarkerPosition(_totalBytesRead - startFrame - 1);
        Log.i(TAG, String.format("repeat(): setNotificationMarkerPosition=%d", _totalBytesRead - startFrame));
        _audioTrack.write(_audioCache, startFrame, _totalBytesRead - startFrame - 1);

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
        if(_totalBytesRead >= _audioCache.length * 0.80) { // if nearing 80% capacity then reset index to beginning of buffer
            Log.i(TAG, "Trimming the audioCache");
            int startBucket = cacheIndex == _cacheIndices.length - 1 ? 0 : cacheIndex + 1;
            int startFrame = _cacheIndices[startBucket];
            short[] temp = new short[_audioCache.length];
            //arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            System.arraycopy(_audioCache, startFrame, temp, 0, _totalBytesRead - startFrame);
            for(int i = 0; i < _cacheIndices.length; i++) {
                _cacheIndices[i] -= startFrame;
            }

            _audioCache = new short[temp.length];
            System.arraycopy(temp, 0, _audioCache, 0, _totalBytesRead - startFrame);

            _totalBytesRead -= startFrame;
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
