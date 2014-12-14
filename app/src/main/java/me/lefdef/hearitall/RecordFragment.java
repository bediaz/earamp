package me.lefdef.hearitall;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by Brigham on 12/13/2014.
 */
public class RecordFragment extends Fragment {

    RecordingTilesView _recordTiles;
    LinearLayout _rootLayout;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _rootLayout = new LinearLayout(getActivity());
        _rootLayout.setOrientation(LinearLayout.VERTICAL);
        _rootLayout.setGravity(LinearLayout.HORIZONTAL);

        LinearLayout recordingsFragment = (LinearLayout) inflater.inflate(R.layout.fragment_recordings, container, false);

        for(int idNum = 0; idNum < 10; idNum++) {
            View rowItem = inflater.inflate(R.layout.row_item, container, false);
            ((TextView) rowItem.findViewById(R.id.rec_name)).setText("idNum="+idNum);
            ((ImageView) rowItem.findViewById(R.id.icon)).setImageResource(R.drawable.play);
            rowItem.setTag(idNum);
            recordingsFragment.addView(rowItem);//, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }

        LinearLayout.LayoutParams recLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        ScrollView scrollView = new ScrollView(getActivity());
        scrollView.addView(recordingsFragment);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);
        _rootLayout.addView(scrollView, recLayoutParams);


        LinearLayout recordActionFragment = (LinearLayout) inflater.inflate(R.layout.fragment_record_action, container, false);

        LinearLayout.LayoutParams recActionLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 5);
        _rootLayout.addView(recordActionFragment, recActionLayoutParams);
        return _rootLayout;
    }
}
