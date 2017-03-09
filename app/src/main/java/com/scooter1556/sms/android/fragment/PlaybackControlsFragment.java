package com.scooter1556.sms.android.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.FullScreenPlayerActivity;
import com.scooter1556.sms.android.activity.HomeActivity;
import com.scooter1556.sms.android.service.MediaService;

/**
 * A fragment which provides media controls to the user
 */
public class PlaybackControlsFragment extends Fragment {

    private static final String TAG = "PlaybackControlsFragment";

    private ImageButton playPause;
    private TextView title;
    private TextView subtitle;
    private TextView extraInfo;
    private ImageView coverArt;
    private String coverArtUrl;

    // Receive callbacks from the Media Controller. Here we update our state such as which item
    // is being shown, the current title, description and the Playback State.
    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }

            PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        playPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        playPause.setEnabled(true);
        playPause.setOnClickListener(buttonListener);

        title = (TextView) rootView.findViewById(R.id.title);
        subtitle = (TextView) rootView.findViewById(R.id.subtitle);
        extraInfo = (TextView) rootView.findViewById(R.id.extra_info);
        coverArt = (ImageView) rootView.findViewById(R.id.cover_art);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                MediaControllerCompat controller = ((FragmentActivity) getActivity()).getSupportMediaController();
                MediaMetadataCompat metadata = controller.getMetadata();

                if (metadata != null) {
                    intent.putExtra(HomeActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, metadata.getDescription());
                }

                startActivity(intent);
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        MediaControllerCompat controller = ((FragmentActivity) getActivity()).getSupportMediaController();

        if (controller != null) {
            onConnected();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        MediaControllerCompat controller = ((FragmentActivity) getActivity()).getSupportMediaController();
        if (controller != null) {
            controller.unregisterCallback(callback);
        }
    }

    public void onConnected() {
        MediaControllerCompat controller = ((FragmentActivity) getActivity()).getSupportMediaController();

        if (controller != null) {
            onMetadataChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(callback);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        if (getActivity() == null || metadata == null) {
            return;
        }

        title.setText(metadata.getDescription().getTitle());
        subtitle.setText(metadata.getDescription().getSubtitle());

        String iconUrl = null;

        if (metadata.getDescription().getIconUri() != null) {
            iconUrl = metadata.getDescription().getIconUri().toString();
        }

        if (!TextUtils.equals(iconUrl, coverArtUrl)) {
            coverArtUrl = iconUrl;

            Glide.with(getActivity())
                    .load(coverArtUrl)
                    .into(coverArt);
        }
    }

    public void setExtraInfo(String extra) {
        if (extra == null) {
            extraInfo.setVisibility(View.GONE);
        } else {
            extraInfo.setText(extra);
            extraInfo.setVisibility(View.VISIBLE);
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        if (getActivity() == null || state == null) {
            return;
        }

        boolean enablePlay = false;

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:

            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;

            case PlaybackStateCompat.STATE_ERROR:
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            playPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_36dp));
        } else {
            playPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_36dp));
        }

        MediaControllerCompat controller = ((FragmentActivity) getActivity()).getSupportMediaController();
        String extraInfo = null;

        if (controller != null && controller.getExtras() != null) {
            String castName = controller.getExtras().getString(MediaService.EXTRA_CONNECTED_CAST);

            if (castName != null) {
                extraInfo = getResources().getString(R.string.cast_to_device, castName);
            }
        }
        setExtraInfo(extraInfo);
    }

    private final View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaControllerCompat controller = ((FragmentActivity) getActivity()).getSupportMediaController();
            PlaybackStateCompat stateObj = controller.getPlaybackState();
            final int state = stateObj == null ? PlaybackStateCompat.STATE_NONE : stateObj.getState();

            switch (v.getId()) {
                case R.id.play_pause:
                    if (state == PlaybackStateCompat.STATE_PAUSED ||
                            state == PlaybackStateCompat.STATE_STOPPED ||
                            state == PlaybackStateCompat.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING ||
                            state == PlaybackStateCompat.STATE_BUFFERING ||
                            state == PlaybackStateCompat.STATE_CONNECTING) {
                        pauseMedia();
                    }

                    break;
            }
        }
    };

    private void playMedia() {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }
}