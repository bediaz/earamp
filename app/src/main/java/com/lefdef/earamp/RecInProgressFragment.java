package com.lefdef.earamp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by brigham.diaz on 12/22/2014.
 */
public class RecInProgressFragment extends Fragment {
    private Amplify amplify;
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        amplify = Amplify.getInstance();
        context = container.getContext();

        final RelativeLayout rootLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_record_in_progress, container, false);
        final TextView recordingTime = (TextView) rootLayout.findViewById(R.id.rec_time);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
