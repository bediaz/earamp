package me.lefdef.hearitall;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.os.Bundle;


public class AmplifyActivity extends Activity {
    AudioRecord audioRecord;
    final int SAMPLE_RATE = 44100; // 44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.
    // TODO: test CHANNEL_IN_STEREO
    final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // Guaranteed to be supported by devices. 8BIT is not

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

        short[] buffer = new short[bufferSize];
        audioRecord.startRecording();
        while(AudioRecord.RECORDSTATE_RECORDING == audioRecord.getRecordingState()) {

        }


        //Intent intent = getIntent();
        // get the switch value, default = false
        //boolean enabled = intent.getBooleanExtra("enable", false);
        setContentView(R.layout.activity_amplify);
    }
}
