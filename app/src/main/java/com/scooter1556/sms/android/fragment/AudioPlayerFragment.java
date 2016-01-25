/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.android.fragment;

import android.content.Context;
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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.lib.android.domain.MediaElement;
import com.scooter1556.sms.lib.android.service.RESTService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioPlayerFragment extends Fragment {

    private AudioControllerListener audioControllerListener;

    // User Interface Elements
    private ImageView coverArt;
    private TextView title;
    private TextView artist;
    private TextView album;
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
        coverArt = (ImageView) view.findViewById(R.id.cover_art);
        title = (TextView) view.findViewById(R.id.title);
        artist = (TextView) view.findViewById(R.id.artist);
        album = (TextView) view.findViewById(R.id.album);
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
                    playPauseButton.setImageResource(R.drawable.ic_play_dark);
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

        // Set current media info
        updateMediaInfo();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            audioControllerListener = (AudioControllerListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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

        // Set current media info
        updateMediaInfo();
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
     * Updates media info
     */
    public void updateMediaInfo() {
        MediaElement element = audioControllerListener.getMediaElement();

        if(element == null) {
            title.setText("");
            artist.setText("");
            album.setText("");
            coverArt.setImageResource(R.drawable.ic_content_audio);
        }
        else {
            title.setText(element.getTitle());
            artist.setText(element.getArtist());
            album.setText(element.getAlbum());

            // Cover Art
            Glide.with(getActivity())
                    .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + element.getID() + "/cover/500")
                    .error(R.drawable.ic_content_audio)
                    .into(coverArt);
        }
    }

    /**
     * Updates player controls
     */
    public void updatePlayerControls() {
        if(audioControllerListener.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_pause_dark);
        }
        else {
            playPauseButton.setImageResource(R.drawable.ic_play_dark);
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
        MediaElement getMediaElement();
        void showPlaylist();
    }

}
