package me.lefdef.hearitall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendAmplifyState(View view) {
        Intent intent = new Intent(this, AmplifyActivity.class);
        Switch enable_switch = (Switch)findViewById(R.id.amplify);
        boolean enabled = enable_switch.isEnabled();
        Bundle bundle = new Bundle();
        bundle.putBoolean("enable", enabled);
        intent.putExtras(bundle);
        startActivity(intent);


    }
}
