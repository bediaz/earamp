package me.lefdef.hearitall;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends FragmentActivity implements ActionBar.TabListener {


    private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;
    private String[] tabTitles = {"Amplify", "Record"};


    final String TAG = "MAINACTIVITY";

    AudioManager _am = null;

    TextView amplifyTextViewState;
    TextView amplifyTextView;
    Switch enable_switch;
    Amplify _amplify;
    Visualizer _visualizer;

    HeadsetAudioReceiver myNoisyAudioStreamReceiver;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private void startPlayback() {
        registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
    }

    private void stopPlayback() {
        unregisterReceiver(myNoisyAudioStreamReceiver);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_amplify, menu);
//
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        myNoisyAudioStreamReceiver = new HeadsetAudioReceiver();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        for (String tabName : tabTitles) {
            actionBar.addTab(actionBar.newTab().setText(tabName).setTabListener(this));
        }

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // change the selected tab when swiping or tapping action bar
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int position, float v, int i2) {

            }

            @Override
            public void onPageScrollStateChanged(int position) {

            }
        });


    }

//        viewPager = (ViewPager)findViewById(R.id.pager);
//        actionBar
//
//        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);
//        _am = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
//        _am.setMode(AudioManager.MODE_IN_COMMUNICATION);
//        _amplify = new Amplify();
//        _amplify.setOnRecordStatusChangeListener(new Amplify.OnRecordStatusChangeListener() {
//            @Override
//            public void onRecordStatusChanged(String message, boolean status) {
//                amplifyTextView.setText(String.format(message));//"Switch enabled? %s", enable_switch.isChecked()));
//
//                String state = String.format("Status: %s", status ? "Recording" : "Not Recording");
//                amplifyTextViewState.setText(state);
//                Log.i(TAG, state);
//
//            }
//        });
//
//        //amplifyTextViewState = (TextView)findViewById(R.id.amplify_textview_state);
//        //amplifyTextView = (TextView)findViewById(R.id.amplify_textview);
//        //enable_switch = (Switch)findViewById(R.id.amplify_switch);
//        enable_switch.setSoundEffectsEnabled(false);
//
//        enable_switch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(enable_switch.isChecked()) {
//                    _amplify.startListeningAndPlay();
//                } else {
//                    _amplify.stopListeningAndPlay();
//                }
//            }
//        });
//
//        _visualizer = new Visualizer(_amplify.getAudioTrackSessionId());
//        _visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
//        _visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
//            @Override
//            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {
//
//            }
//
//            @Override
//            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int i) {
//
//            }
//            //TODO: check if 44100Hz is enough for capture rate, or getMaxCaptureRate / 2
//        }, Visualizer.getMaxCaptureRate(), true, false); // don't need onFftDataCapture
//
//        _visualizer.setEnabled(true);
//
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if(_amplify.isRecording()) {
//            _amplify.stopListeningAndPlay();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if(_amplify.isRecording()) {
//            _amplify.stopListeningAndPlay();
//        }
//    }
//
//    private void updateTextView() {
//        amplifyTextView.setText(String.format("Switch enabled? %s", enable_switch.isChecked()));
//    }
//
    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    //    public void sendAmplifyState(View view) {
//        Intent intent = new Intent(this, AmplifyActivity.class);
//        Switch enable_switch = (Switch)findViewById(R.id.amplify);
//        boolean enabled = enable_switch.isEnabled();
//        Bundle bundle = new Bundle();
//        bundle.putBoolean("enable", enabled);
//        intent.putExtras(bundle);
//        startActivity(intent);
//
//
//    }
}
