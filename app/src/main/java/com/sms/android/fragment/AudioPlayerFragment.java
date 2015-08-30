package com.sms.android.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sms.android.R;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioPlayerFragment extends Fragment {

    private AudioControllerListener audioControllerListener;

    // User Interface Elements
    private ImageButton playPauseButton;
    private ImageButton stopButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private SeekBar progressBar;
    private TextView positionTxt;
    private TextView durationTxt;
    private boolean seekInProgress = false;

    private ScheduledExecutorService executorService;

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment AudioPlayerFragment.
     */
    public static AudioPlayerFragment newInstance() {
        AudioPlayerFragment fragment = new AudioPlayerFragment();
        return fragment;
    }

    public AudioPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Action Bar
        setHasOptionsMenu(true);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_audio_player, container, false);

        // UI Elements
        playPauseButton = (ImageButton) view.findViewById(R.id.playPause);
        stopButton = (ImageButton) view.findViewById(R.id.stop);
        previousButton = (ImageButton) view.findViewById(R.id.previous);
        nextButton = (ImageButton) view.findViewById(R.id.next);
        progressBar = (SeekBar) view.findViewById(R.id.progress_bar);
        positionTxt = (TextView) view.findViewById(R.id.position);
        durationTxt = (TextView) view.findViewById(R.id.duration);

        // UI Listeners

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View status) {
                if(audioControllerListener.isPlaying()) {
                    audioControllerListener.pause();
                    playPauseButton.setImageResource(R.drawable.ic_action_play_dark);
                }
                else {
                    audioControllerListener.start();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View status) {
                audioControllerListener.stop();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View status) {
                audioControllerListener.playPrev();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View status) {
                audioControllerListener.playNext();
            }
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
                if (fromUser) {
                    positionTxt.setText(formatDuration((int)(position * 0.001)));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekInProgress = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekInProgress = false;
                int position = seekBar.getProgress();
                positionTxt.setText(formatDuration((int)(position * 0.001)));
                audioControllerListener.seek(position);
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // Set current button state
        updatePlayerControls();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            audioControllerListener = (AudioControllerListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AudioControllerListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        audioControllerListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        executorService.shutdown();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Player control update thread
        executorService = Executors.newSingleThreadScheduledExecutor();

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateProgressBar();
                    }
                });
            }
        };
        executorService.scheduleWithFixedDelay(runnable, 0L, 1000L, TimeUnit.MILLISECONDS);

        // Update player controls
        updatePlayerControls();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_audio_player, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.playlist:
                audioControllerListener.showPlaylist();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Updates player controls
     */
    public void updatePlayerControls() {
        if(audioControllerListener.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_action_pause_dark);
        }
        else {
            playPauseButton.setImageResource(R.drawable.ic_action_play_dark);
        }
    }

    public void updateProgressBar() {

        if(seekInProgress) { return; }

        if (audioControllerListener.isPlaying()) {
            int millisPlayed = Math.max(0, audioControllerListener.getCurrentPosition());
            Integer duration = audioControllerListener.getDuration();
            int millisTotal = duration == null ? 0 : duration;

            positionTxt.setText(formatDuration((int)(millisPlayed * 0.001)));
            durationTxt.setText(formatDuration((int)(millisTotal * 0.001)));
            progressBar.setMax(millisTotal == 0 ? 100 : millisTotal);
            if (!seekInProgress) {
                progressBar.setProgress(millisPlayed);
                progressBar.setEnabled(true);
            }
        } else if(!audioControllerListener.isPaused()) {
            positionTxt.setText("0:00");
            durationTxt.setText("-:--");
            progressBar.setProgress(0);
            progressBar.setEnabled(false);
        }
    }

    /**
     * Helper Function
     */
    public static String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }

        int minutes = seconds / 60;
        int secs = seconds % 60;

        StringBuilder builder = new StringBuilder(6);
        builder.append(minutes).append(":");
        if (secs < 10) {
            builder.append("0");
        }
        builder.append(secs);
        return builder.toString();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface AudioControllerListener {
        boolean isPlaying();
        boolean isPaused();
        void seek(int pos);
        int getCurrentPosition();
        int getDuration();
        void pause();
        void start();
        void stop();
        void playNext();
        void playPrev();
        void showPlaylist();
    }

}
