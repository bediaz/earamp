package com.lefdef.earamp;

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
    private static final String TAG = "AMPLIFY";
    private static final Semaphore recordingLock = new Semaphore(1);

    private static Amplify amplify;
    //region CONSTANTS
    private final int SAMPLE_RATE = 44100; // 44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.
    // TODO: test CHANNEL_IN_STEREO
    private final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    private final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // Guaranteed to be supported by devices. 8BIT is not
    private final ScheduledExecutorService audioCacheServiceoCacheService = Executors.newScheduledThreadPool(1); // create a single thread
    private OnPlayStateChangedListener onPlayStateChangedListener = null;
    //endregion
    private int minInBufferSize;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private short[] buffer;
    private short[] audioCache;
    private LinkedList<Integer> _cacheIndices;
    private boolean isRecording;
    // cache variables
    private int repeatLength = 10; // seconds to keep of audio cache
    private int totalBytesRead = 0;
    private ScheduledFuture _audioCacheServiceHandle;
    private int MAX_BUFFER;
    private boolean isPlaying;

    private Amplify() {
        minInBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, minInBufferSize);

        // TODO: remove blocking on play
        int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AudioFormat.ENCODING_PCM_16BIT, outBufferSize, AudioTrack.MODE_STREAM);
        buffer = new short[minInBufferSize];
        audioCache = new short[outBufferSize * 500];
        MAX_BUFFER = (int) (audioCache.length * 0.80);

        _cacheIndices = new LinkedList<Integer>();

        // TODO: setup timer/scheduled executor
    }

    public static Amplify getInstance() {
        if (amplify == null) {
            synchronized (Amplify.class) {
                if (amplify == null) {
                    amplify = new Amplify();
                }
            }
        }
        return amplify;
    }

    private void refreshCacheIndices() {

        while (_cacheIndices.size() >= repeatLength && _cacheIndices.size() > 0) {
            _cacheIndices.removeFirst();
        }
        _cacheIndices.addLast(totalBytesRead);

        //Log.i(TAG, String.format("refreshCacheIndices=%s, totalBytesRead=%s, capacity=%.2f", _intervals, totalBytesRead, 100.0*(float)totalBytesRead/audioCache.length));
    }

    public void startRepeat() {
        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (totalBytesRead == 0) {
                    return null;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int startFrame = _cacheIndices.isEmpty() ? 0 : _cacheIndices.getFirst();

                if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.play();
                    isPlaying = audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
                    //Log.i(TAG, String.format("audioTrack repeat isPlaying=%b", audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING));
                }

                //Log.i(TAG, String.format("repeat(): refreshCacheIndices=%d, startFrame=%d, totalBytesRead=%d", _intervals, startFrame, totalBytesRead));
                //audioTrack.setNotificationMarkerPosition(audioTrack.getPlaybackHeadPosition() + (duration * sampleRate));
                audioTrack.setNotificationMarkerPosition(totalBytesRead - 1);
                //Log.i(TAG, String.format("repeat(): setNotificationMarkerPosition=%d", totalBytesRead - 1));
                audioTrack.write(audioCache, startFrame, totalBytesRead - startFrame - 1);
                //Log.i(TAG, String.format("write: _audioCacheSize=%d, begin=%d, end=%d", audioCache.length, startFrame, totalBytesRead - startFrame - 1));

                audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack track) {
                        //Log.i(TAG, String.format("repeat playback done. isPlaying=%s, playbackPosition=%d",audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING, audioTrack.getPlaybackHeadPosition()));
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
        if (audioRecord != null) {
            return audioRecord.getAudioSessionId();
        }
        return 0;
    }

    public int getAudioTrackSessionId() {
        if (audioTrack != null) {
            return audioTrack.getAudioSessionId();
        }
        return 0;
    }

    public void startRecordingToFile() {
        Log.i(TAG, "startRecordingToFile trying to acquire recordingLock");
        if (!recordingLock.tryAcquire()) {
            Log.e(TAG, "startRecordingToFile failed to acquire recordingLock");
            return;
        }

        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    return null;
                }

                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd.HHmmss");
                Date now = new Date();
                String strDate = sdfDate.format(now);

                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recording_" + strDate + ".pcm";
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(filePath);
                    audioRecord.startRecording();
                    isRecording = AudioRecord.RECORDSTATE_RECORDING == audioRecord.getRecordingState();
                    if (isRecording) {
                        triggerObservers();
                    }
                    Log.i(TAG, "startRecordingToFile started recording");

                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] buffer = new byte[minInBufferSize];
                while (isRecording) {

                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                    try {
                        if (outStream != null) {
                            outStream.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (outStream != null) {
                        outStream.close(); // save file
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                audioRecord.stop();
                recordingLock.release();
                Log.i(TAG, "startRecordingToFile released recordingLock");

                return null;
            }
        }.execute();

    }

    public void startPlayingRecording(String fileName) {
        Log.i(TAG, "startPlayingRecording trying to acquire recordingLock");
        if (!recordingLock.tryAcquire()) {
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
                FileInputStream in;
                try {
                    in = new FileInputStream(file);

                    int totalBytesRead = 0;
                    int bytesRead = 0;
                    int size = (int) file.length();
                    audioTrack.play();
                    isPlaying = audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;

                    while (isPlaying && totalBytesRead < size) {
                        try {
                            bytesRead = in.read(byteData, 0, byteData.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (bytesRead != -1) {
                            audioTrack.write(byteData, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        } else {
                            break;
                        }
                    }

                    in.close();
                    audioTrack.pause();
                    audioTrack.flush();
                    Log.i(TAG, "done playing");

                    recordingLock.release();
                    Log.i(TAG, "startPlayingRecording released recordingLock");

                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute(fileName);
    }

    public void startListeningAndPlay() {
        Log.i(TAG, "startListeningAndPlay trying to acquire recordingLock");
        if (!recordingLock.tryAcquire()) {
            Log.e(TAG, "startListeningAndPlay failed to acquire recordingLock");
            return;
        }
        new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {


                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    // publishProgress("AudioRecord Failed to Initialize...");
                    // Log.i(TAG, "AudioRecord Failed to Initialize...");
                    return null;
                }

                int outBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, outBufferSize, AudioTrack.MODE_STREAM);

                audioRecord.startRecording();

                Log.i(TAG, "startListeningAndPlay started recording");

                if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.play();
                    isPlaying = audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
                }
                isRecording = AudioRecord.RECORDSTATE_RECORDING == audioRecord.getRecordingState();
                if (isRecording) {
                    triggerObservers();
                }

                int bytesRead;

                if (isRecording) { // call once
                    _audioCacheServiceHandle = audioCacheServiceoCacheService.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            refreshCacheIndices();
                        }
                    }, 0, 1000, TimeUnit.MILLISECONDS); // start immediately, run every 1000ms
                }

                while (isRecording) {

                    bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    audioTrack.write(buffer, 0, bytesRead);
                    audioBufferProcessing(bytesRead);
                    //Log.i("TAG", String.format("totalBytesRead=%s", totalBytesRead));

                }

                if (_audioCacheServiceHandle != null) {
                    _audioCacheServiceHandle.cancel(true);
                }
                audioRecord.stop();
                audioTrack.pause();
                audioTrack.flush();
                Log.i(TAG, "startListeningAndPlay stopped recording");
                recordingLock.release();
                Log.i(TAG, "startListeningAndPlay released recordingLock");

                //Log.i(TAG, String.format("isRecording=%b, audioCacheService isTerminated=%b", isRecording, audioCacheServiceoCacheService.isTerminated()));

                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
            }
        }.execute();
    }

    private void audioBufferProcessing(int bytesRead) {
        System.arraycopy(buffer, 0, audioCache, totalBytesRead, bytesRead);

        totalBytesRead += bytesRead;
        if (totalBytesRead >= MAX_BUFFER) { // if MAX_BUFFER hit then reset index to beginning of buffer
            //Log.i(TAG, "Trimming the audioCache");
            int startFrame = _cacheIndices.isEmpty() ? 0 : _cacheIndices.getFirst();
            short[] temp = new short[audioCache.length];

            // API call: System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            // audioCache << (totalBytesRead - startFrame)
            System.arraycopy(audioCache, startFrame, temp, 0, totalBytesRead - startFrame);
            audioCache = new short[temp.length];
            System.arraycopy(temp, 0, audioCache, 0, totalBytesRead - startFrame);
            totalBytesRead -= startFrame;

            // adjust frame values of linked list
            for (int position = 0; position < _cacheIndices.size(); position++) {
                _cacheIndices.set(position, _cacheIndices.get(position) - startFrame);
            }
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void stopRecording() {
        isRecording = false;
        triggerObservers();
    }

    public void stopPlaying() {
        isPlaying = false;
        triggerObservers();
    }

    public void stopListeningAndPlay() {
        isRecording = false;
        isPlaying = false;
        triggerObservers();
    }

    public void killAll() {
        audioRecord.stop();
        audioTrack.pause();
        audioTrack.flush();
    }

    public OnPlayStateChangedListener getOnPlayStateChangedListener() {
        return onPlayStateChangedListener;
    }

    public void setOnPlayStateChangedListener(OnPlayStateChangedListener _onGameSetChangedListener) {
        this.onPlayStateChangedListener = _onGameSetChangedListener;
    }

    private void triggerObservers() {
        if (onPlayStateChangedListener != null) {
            onPlayStateChangedListener.OnPlayStateChanged();
        }
    }

    public interface OnPlayStateChangedListener {
        public void OnPlayStateChanged();
    }

}
