package me.lefdef.earamp;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

    final static String TAG = "RECORD_FRAGMENT";

    private Amplify _amplify;
    private AudioManager _audioManager;

    private LinearLayout _rootLayout;
    private CircleButton _recButton;
    private CircleButton _stopButton;
    private List<String> _paths;
    private Context _context;
    private ScrollView _scrollView;

    public RecordFragment() {
        scanFiles();

    }

    private void scanFiles() {
        _paths = new ArrayList<String>();
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.getAbsolutePath().indexOf("recording_") >= 0) {
                _paths.add(file.getAbsolutePath());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        _audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        _amplify = Amplify.getInstance();


    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _context = container.getContext();
        _rootLayout = new LinearLayout(getActivity());
        _rootLayout.setOrientation(LinearLayout.VERTICAL);
        _rootLayout.setGravity(LinearLayout.HORIZONTAL);


        LinearLayout.LayoutParams scrollViewLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        _scrollView = new ScrollView(getActivity());
        populateScrollView(); //new ScrollView(getActivity());
        _rootLayout.addView(_scrollView, scrollViewLayoutParams);


        LinearLayout recordActionFragment = (LinearLayout) inflater.inflate(R.layout.fragment_record_action, container, false);
        LinearLayout.LayoutParams recActionLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 5);

        _rootLayout.addView(recordActionFragment, recActionLayoutParams);

        _recButton = (CircleButton) recordActionFragment.findViewById(R.id.recButton);
        _stopButton = (CircleButton) recordActionFragment.findViewById(R.id.stopButton);
        return _rootLayout;
    }

    private void populateScrollView() {
        LinearLayout recordingsFragment;
        LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        recordingsFragment = (LinearLayout) inflater.inflate(R.layout.fragment_recordings, null);
        recordingsFragment.setTag("recordingsFragment");
        LinearLayout temp = (LinearLayout) _scrollView.findViewWithTag("recordingsFragment");
        if (temp != null) {
            recordingsFragment = temp;
            _scrollView.removeView(recordingsFragment);
        }

        for (String path : _paths) {

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
            cb.setTextColor(getResources().getColor(R.color.light_gray));

            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CircleButton cb = (CircleButton) v;
                    CircleButton circleButton = (CircleButton) _scrollView.findViewWithTag(cb.getTag());
                    if (_amplify.isPlaying()) {
                        _amplify.stopPlaying();
                        if (!_amplify.isPlaying()) { // verify action was performed
                            if (circleButton != null) {
                                circleButton.setImageResource(R.drawable.ic_action_play);
                                circleButton.invalidate();
                                _scrollView.invalidate();
                            }
                        }
                    } else {
                        _amplify.startPlayingRecording(cb.getLabel());
                        if (circleButton != null) {
                            circleButton.setImageResource(R.drawable.ic_action_stop);
                            circleButton.invalidate();
                            _scrollView.invalidate();
                        }

                    }

                }
            });
            rowItem.setTag(path);
            recordingsFragment.addView(rowItem);
        }

        _scrollView.addView(recordingsFragment);
        _scrollView.setVerticalScrollBarEnabled(false);
        _scrollView.setHorizontalScrollBarEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        _recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: start recording
                _amplify.startRecordingToFile();
                _recButton.setImageResource(R.drawable.ic_action_record_inactive);
                _stopButton.setImageResource(R.drawable.ic_action_stop_circle);
                _recButton.invalidate();
                _stopButton.invalidate();
            }
        });
        _stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: stop recording
                _amplify.stopRecording();
                _recButton.setImageResource(R.drawable.ic_action_record);
                _stopButton.setImageResource(R.drawable.ic_action_stop_circle_inactive);
                _recButton.invalidate();
                _stopButton.invalidate();

                scanFiles();
                populateScrollView();
            }
        });
    }
}
