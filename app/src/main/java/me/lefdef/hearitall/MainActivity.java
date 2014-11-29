package me.lefdef.hearitall;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends Activity {

    final String TAG = "MAINACTIVITY";

    TextView amplifyTextViewState;
    TextView amplifyTextView;
    Switch enable_switch;
    Amplify _amplify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _amplify = new Amplify();
        _amplify.setOnRecordStatusChangeListener(new Amplify.OnRecordStatusChangeListener() {
            @Override
            public void onRecordStatusChanged(String message, boolean status) {
                if(status) {
                    amplifyTextView.setText(String.format(message));//"Switch enabled? %s", enable_switch.isChecked()));
                }

                String state = String.format("Status: %s", status ? "Recording" : "Not Recording");
                amplifyTextViewState.setText(state);
                Log.i(TAG, state);

            }
        });

        setContentView(R.layout.activity_main);


        amplifyTextViewState = (TextView)findViewById(R.id.amplify_textview_state);
        amplifyTextView = (TextView)findViewById(R.id.amplify_textview);
        enable_switch = (Switch)findViewById(R.id.amplify_switch);

        enable_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(enable_switch.isChecked()) {
                    _amplify.startListeningAndPlay();
                } else {
                    _amplify.stopListeningAndPlay();
                }
            }
        });
    }

    private void updateTextView() {
        amplifyTextView.setText(String.format("Switch enabled? %s", enable_switch.isChecked()));
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
