package me.lefdef.hearitall;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;


public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
    final String TAG = "MAIN_ACTIVITY";
    private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;
    private String[] tabTitles = {"Amplify", "Record"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        for (String tabName : tabTitles) {
            actionBar.addTab(actionBar.newTab().setText(tabName).setTabListener(this));
        }

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // change the selected tab when swiping or tapping action bar
                Log.i(TAG, "pageSelected=" + position);
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
//        Switch _enable_switch = (Switch)findViewById(R.id.amplify);
//        boolean enabled = _enable_switch.isEnabled();
//        Bundle bundle = new Bundle();
//        bundle.putBoolean("enable", enabled);
//        intent.putExtras(bundle);
//        startActivity(intent);
//
//
//    }
}
