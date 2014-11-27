package me.lefdef.hearitall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class AmplifyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        // get the switch value, default = false
        boolean enabled = intent.getBooleanExtra("enable", false);


        setContentView(R.layout.activity_amplify);
    }
}
