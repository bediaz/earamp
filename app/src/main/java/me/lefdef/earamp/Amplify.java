package me.lefdef.earamp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class Amplify {

    //region CONSTANTS
    final String TAG = "AMPLIFY";
    final int SAMPLE_RATE = 44100; // 44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.
    // TODO: test CHANNEL_IN_STEREO
    final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // Guaranteed to be supported by devices. 8BIT is not
    private final ScheduledExecutorService _audioCacheService = Executors.newScheduledThreadPool(1); // create a single thread
    //endregion
    private int _minInBufferSize;
    private AudioRecord _audioRecord;
    private AudioTrack _audioTrack;
    private short[] _buffer;
    private short[] _audioCache;
    private LinkedList<Integer> _cacheIndices;
    private boolean _isRecording;
    // cache variables
    private int repeatLength = 10; // seconds to keep of audio cache
    private int _totalBytesRead = 0;
    private ScheduledFuture _audioCacheServiceHandle;
    private int MAX_BUFFER;
    private boolean _isPlaying;

    private static Amplify _amplify;


    private Amplify() {
        _minInBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        _audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, _minInBufferSize);

        // TODO: remove blocking on play
        int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

        _audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AudioFormat.ENCODING_PCM_16BIT, outBufferSize, AudioTrack.MODE_STREAM);
        _buffer = new short[_minInBufferSize];
        _audioCache = new short[outBufferSize * 500];
        MAX_BUFFER = (int) (_audioCache.length * 0.80);

        _cacheIndices = new LinkedList<Integer>();

        // TODO: setup timer/scheduled executor
    }

    public static Amplify getInstance() {
        if(_amplify == null) {
            synchronized(Amplify.class) {
                if(_amplify == null) {
                    _amplify = new Amplify();
                }
            }
        }
        return _amplify;
    }



    private void refreshCacheIndices() {

        while (_cacheIndices.size() >= repeatLength && _cacheIndices.size() > 0) {
            _cacheIndices.removeFirst();
        }
        _cacheIndices.addLast(_totalBytesRead);

        //Log.i(TAG, String.format("refreshCacheIndices=%s, totalBytesRead=%s, capacity=%.2f", _intervals, _totalBytesRead, 100.0*(float)_totalBytesRead/_audioCache.length));
    }



    public void startRepeat() {
        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (_totalBytesRead == 0) {
                    return null;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int startFrame = _cacheIndices.isEmpty() ? 0 : _cacheIndices.getFirst();

                if (_audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    _audioTrack.play();
                    _isPlaying = _audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
                    //Log.i(TAG, String.format("audioTrack repeat isPlaying=%b", _audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING));
                }

                //Log.i(TAG, String.format("repeat(): refreshCacheIndices=%d, startFrame=%d, totalBytesRead=%d", _intervals, startFrame, _totalBytesRead));
                //_audioTrack.setNotificationMarkerPosition(_audioTrack.getPlaybackHeadPosition() + (duration * sampleRate));
                _audioTrack.setNotificationMarkerPosition(_totalBytesRead - 1);
                //Log.i(TAG, String.format("repeat(): setNotificationMarkerPosition=%d", _totalBytesRead - 1));
                _audioTrack.write(_audioCache, startFrame, _totalBytesRead - startFrame - 1);
                //Log.i(TAG, String.format("write: _audioCacheSize=%d, begin=%d, end=%d", _audioCache.length, startFrame, _totalBytesRead - startFrame - 1));

                _audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack track) {
                        //Log.i(TAG, String.format("repeat playback done. isPlaying=%s, playbackPosition=%d",_audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING, _audioTrack.getPlaybackHeadPosition()));
                    }

                    @Override
                    public void onPeriodicNotification(AudioTrack track) {

                    }
                });
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
            }
        }.execute();
    }


    public int getAudioRecordSessionId() {
        if (_audioRecord != null) {
            return _audioRecord.getAudioSessionId();
        }
        return 0;
    }

    public int getAudioTrackSessionId() {
        if (_audioTrack != null) {
            return _audioTrack.getAudioSessionId();
        }
        return 0;
    }
    final static Semaphore recordingLock = new Semaphore(1);


    public void startRecordingToFile() {
        Log.i(TAG, "startRecordingToFile trying to acquire recordingLock");
        if(!recordingLock.tryAcquire()) {
            Log.e(TAG, "startRecordingToFile failed to acquire recordingLock");
            return;
        }

        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                if (_audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    return null;
                }

                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd.HHmmss");
                Date now = new Date();
                String strDate = sdfDate.format(now);

                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recording_" + strDate + ".pcm";
                FileOutputStream outStream = null;
                try {
                    // file.createNewFile();

                    outStream = new FileOutputStream(filePath);
                    _audioRecord.startRecording();
                    _isRecording = AudioRecord.RECORDSTATE_RECORDING == _audioRecord.getRecordingState();
                    if(_isRecording) {
                        triggerObservers();
                    }
                    Log.i(TAG, "startRecordingToFile started recording");

                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] buffer = new byte[_minInBufferSize];
                while (_isRecording) {

                    int bytesRead = _audioRecord.read(buffer, 0, buffer.length);

                    try {
                        outStream.write(buffer, 0, bytesRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    outStream.close(); // save file
                } catch (IOException e) {
                    e.printStackTrace();
                }

                _audioRecord.stop();
                recordingLock.release();
                Log.i(TAG, "startRecordingToFile released recordingLock");

                return null;
            }
        }.execute();

    }

    public void startPlayingRecording(String fileName) {
        Log.i(TAG, "startPlayingRecording trying to acquire recordingLock");
        if(!recordingLock.tryAcquire()) {
            Log.e(TAG, "startPlayingRecording failed to acquire recordingLock");
            return;
        }

        new AsyncTask<String, String, Void>() {
            @Override
            protected Void doInBackground(String... params) {


                int interval = 512 * 1024; // 512 kb
                String fileName = params[0];
                File file = new File(fileName);


                if (!file.exists()) {
                    return null;
                }

                byte[] byteData = new byte[(int) file.length()];
                FileInputStream in = null;
                try {
                    in = new FileInputStream(file);

                    int totalBytesRead = 0;
                    int bytesRead = 0;
                    int size = (int) file.length();
                    _audioTrack.play();
                    _isPlaying = _audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;

                    while (_isPlaying && totalBytesRead < size) {
                        try {
                            bytesRead = in.read(byteData, 0, byteData.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (bytesRead != -1) {
                            _audioTrack.write(byteData, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        } else {
                            break;
                        }
                    }

                    in.close();
                    _audioTrack.pause();
                    _audioTrack.flush();
                    Log.i(TAG, "done playing");

                    recordingLock.release();
                    Log.i(TAG, "startPlayingRecording released recordingLock");

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute(fileName);
    }

    public void startListeningAndPlay() {
        Log.i(TAG, "startListeningAndPlay trying to acquire recordingLock");
        if(!recordingLock.tryAcquire()) {
            Log.e(TAG, "startListeningAndPlay failed to acquire recordingLock");
            return;
        }
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

                Log.i(TAG, "startListeningAndPlay started recording");

                if (_audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    _audioTrack.play();
                    _isPlaying = _audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
                }
                _isRecording = AudioRecord.RECORDSTATE_RECORDING == _audioRecord.getRecordingState();
                if(_isRecording) {
                    triggerObservers();
                }

                int bytesRead;

                if (_isRecording) { // call once
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

                if (_audioCacheServiceHandle != null) {
                    _audioCacheServiceHandle.cancel(true);
                }
                _audioRecord.stop();
                _audioTrack.pause();
                _audioTrack.flush();
                Log.i(TAG, "startListeningAndPlay stopped recording");
                recordingLock.release();
                Log.i(TAG, "startListeningAndPlay released recordingLock");

                //Log.i(TAG, String.format("isRecording=%b, audioCacheService isTerminated=%b", _isRecording, _audioCacheService.isTerminated()));

                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
            }
        }.execute();
    }

    private void audioBufferProcessing(int bytesRead) {
        System.arraycopy(_buffer, 0, _audioCache, _totalBytesRead, bytesRead);

        _totalBytesRead += bytesRead;
        if (_totalBytesRead >= MAX_BUFFER) { // if MAX_BUFFER hit then reset index to beginning of buffer
            //Log.i(TAG, "Trimming the audioCache");
            int startFrame = _cacheIndices.isEmpty() ? 0 : _cacheIndices.getFirst();
            short[] temp = new short[_audioCache.length];

            // API call: System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            // _audioCache << (totalBytesRead - startFrame)
            System.arraycopy(_audioCache, startFrame, temp, 0, _totalBytesRead - startFrame);
            _audioCache = new short[temp.length];
            System.arraycopy(temp, 0, _audioCache, 0, _totalBytesRead - startFrame);
            _totalBytesRead -= startFrame;

            // adjust frame values of linked list
            for (int position = 0; position < _cacheIndices.size(); position++) {
                _cacheIndices.set(position, _cacheIndices.get(position) - startFrame);
            }
        }
    }

    public boolean isRecording() {
        return _isRecording;
    }
    public boolean isPlaying() {
        return _isPlaying;
    }

    public void stopRecording() {
        _isRecording = false;
        triggerObservers();
    }

    public void stopPlaying() {
        _isPlaying = false;
        triggerObservers();
    }

    public void stopListeningAndPlay() {
        _isRecording = false;
        _isPlaying = false;
        triggerObservers();
    }

    public void killAll() {
        _audioRecord.stop();
        _audioTrack.pause();
        _audioTrack.flush();
    }

    public interface OnPlayStateChangedListener {
        public void OnPlayStateChanged();
    }

    public OnPlayStateChangedListener getOnPlayStateChangedListener() {
        return _onPlayStateChangedListener;
    }

    public void setOnPlayStateChangedListener(OnPlayStateChangedListener _onGameSetChangedListener) {
        this._onPlayStateChangedListener = _onGameSetChangedListener;
    }

    public OnPlayStateChangedListener _onPlayStateChangedListener = null;

    private void triggerObservers() {
        if(_onPlayStateChangedListener != null) {
            _onPlayStateChangedListener.OnPlayStateChanged();
        }
    }

}
