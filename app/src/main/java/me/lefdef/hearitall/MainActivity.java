package me.lefdef.hearitall;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends Activity {

    TextView amplifyTextView;
    Switch enable_switch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        amplifyTextView = (TextView)findViewById(R.id.amplify_textview);
        enable_switch = (Switch)findViewById(R.id.amplify_switch);

        enable_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTextView();
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
