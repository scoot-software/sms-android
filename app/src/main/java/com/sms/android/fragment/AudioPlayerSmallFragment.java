package com.sms.android.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sms.android.R;
import com.sms.lib.android.domain.MediaElement;
import com.sms.lib.android.service.RESTService;
import com.squareup.picasso.Picasso;

public class AudioPlayerSmallFragment extends Fragment {

    private AudioPlayerFragment.AudioControllerListener audioControllerListener;

    // User Interface Elements
    private ImageView coverArt;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private TextView titleTxt;
    private TextView artistTxt;

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment AudioPlayerFragment.
     */
    public static AudioPlayerSmallFragment newInstance() {
        AudioPlayerSmallFragment fragment = new AudioPlayerSmallFragment();
        return fragment;
    }

    public AudioPlayerSmallFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_audio_player_small, container, false);

        // UI Elements
        coverArt = (ImageView) view.findViewById(R.id.cover_art);
        playPauseButton = (ImageButton) view.findViewById(R.id.play_pause);
        previousButton = (ImageButton) view.findViewById(R.id.previous);
        nextButton = (ImageButton) view.findViewById(R.id.next);
        titleTxt = (TextView) view.findViewById(R.id.title);
        titleTxt.setSelected(true);
        artistTxt = (TextView) view.findViewById(R.id.artist);
        artistTxt.setSelected(true);

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            audioControllerListener = (AudioPlayerFragment.AudioControllerListener) activity;
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
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update player controls
        updatePlayerControls();

        // Set current media info
        updateMediaInfo();
    }

    /**
     * Updates media info
     */
    public void updateMediaInfo() {
        MediaElement element = audioControllerListener.getMediaElement();

        if(element == null) {
            titleTxt.setText("");
            artistTxt.setText("");
            coverArt.setImageResource(R.drawable.ic_content_audio);
        }
        else {
            titleTxt.setText(element.getTitle());
            artistTxt.setText(element.getArtist());

            // Cover Art
            Picasso.with(getActivity())
                    .load(RESTService.getInstance().getBaseUrl() + "/image/" + element.getID() + "/cover/80")
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

}
