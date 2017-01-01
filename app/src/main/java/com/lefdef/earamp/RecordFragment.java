package com.lefdef.earamp;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brigham on 12/13/2014.
 */
public class RecordFragment extends Fragment {

    static final String TAG = "RECORD_FRAGMENT";

    private Amplify amplify;
    private AudioManager audioManager;

    private CircleButton btnRecord;
    private CircleButton btnStop;
    private List<String> paths;
    private Context context;
    private ScrollView scrollView;

    public RecordFragment() {
        scanFiles();

    }

    private void scanFiles() {
        paths = new ArrayList<String>();
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        File[] files = directory.listFiles();
        if(files != null){
            for (File file : files) {
                if (file.getAbsolutePath().contains("recording_")) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        amplify = Amplify.getInstance();


    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = container.getContext();
        final LinearLayout rootLayout = new LinearLayout(getActivity());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(LinearLayout.HORIZONTAL);


        LinearLayout.LayoutParams scrollViewLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        scrollView = new ScrollView(getActivity());
        populateScrollView(); //new ScrollView(getActivity());
        rootLayout.addView(scrollView, scrollViewLayoutParams);


        LinearLayout recordActionFragment = (LinearLayout) inflater.inflate(R.layout.fragment_record_action, container, false);
        LinearLayout.LayoutParams recActionLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 5);

        rootLayout.addView(recordActionFragment, recActionLayoutParams);

        btnRecord = (CircleButton) recordActionFragment.findViewById(R.id.recButton);
        btnStop = (CircleButton) recordActionFragment.findViewById(R.id.stopButton);
        return rootLayout;
    }

    private void populateScrollView() {
        LinearLayout recordingsFragment;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        recordingsFragment = (LinearLayout) inflater.inflate(R.layout.fragment_recordings, null);
        recordingsFragment.setTag("recordingsFragment");
        LinearLayout temp = (LinearLayout) scrollView.findViewWithTag("recordingsFragment");
        if (temp != null) {
            recordingsFragment = temp;
            scrollView.removeView(recordingsFragment);
        }

        for (String path : paths) {

            if (recordingsFragment.findViewWithTag(path) != null) {
                continue;
            }

            View rowItem = inflater.inflate(R.layout.row_item, null);
            ((TextView) rowItem.findViewById(R.id.rec_name)).setText(path);

            String size = Formatter.formatShortFileSize(getActivity(), (new File(path)).length());
            ((TextView) rowItem.findViewById(R.id.rec_length)).setText("size:" + size);


            CircleButton cb = (CircleButton) rowItem.findViewById(R.id.icon);
            cb.setImageResource(R.drawable.ic_action_play);
            cb.setLabel(path);
            cb.setLabelSize(1);
            cb.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CircleButton cb = (CircleButton) v;
                    CircleButton circleButton = (CircleButton) scrollView.findViewWithTag(cb.getTag());
                    if (amplify.isPlaying()) {
                        amplify.stopPlaying();
                        if (!amplify.isPlaying()) { // verify action was performed
                            if (circleButton != null) {
                                circleButton.setImageResource(R.drawable.ic_action_play);
                                circleButton.invalidate();
                                scrollView.invalidate();
                            }
                        }
                    } else {
                        amplify.startPlayingRecording(cb.getLabel());
                        if (circleButton != null) {
                            circleButton.setImageResource(R.drawable.ic_action_stop);
                            circleButton.invalidate();
                            scrollView.invalidate();
                        }

                    }

                }
            });
            rowItem.setTag(path);
            recordingsFragment.addView(rowItem);
        }

        scrollView.addView(recordingsFragment);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: start recording
                amplify.startRecordingToFile();
                btnRecord.setImageResource(R.drawable.ic_action_record_inactive);
                btnStop.setImageResource(R.drawable.ic_action_stop_circle);
                btnRecord.invalidate();
                btnStop.invalidate();
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: stop recording
                amplify.stopRecording();
                btnRecord.setImageResource(R.drawable.ic_action_record);
                btnStop.setImageResource(R.drawable.ic_action_stop_circle_inactive);
                btnRecord.invalidate();
                btnStop.invalidate();

                scanFiles();
                populateScrollView();
            }
        });
    }
}
