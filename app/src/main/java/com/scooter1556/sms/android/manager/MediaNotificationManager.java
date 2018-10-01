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
package com.scooter1556.sms.android.manager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.HomeActivity;
import com.scooter1556.sms.android.service.MediaService;

import static android.os.Build.*;

/**
 * Manages media notification and updates it automatically for a given MediaSession.
 */
@RequiresApi(api = VERSION_CODES.O)
public class MediaNotificationManager extends BroadcastReceiver {

    private static final String TAG = "NotificationManager";

    private static final String CHANNEL_ID = "com.scooter1556.sms.android.CHANNEL_ID";

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "com.scooter1556.sms.android.pause";
    public static final String ACTION_PLAY = "com.scooter1556.sms.android.play";
    public static final String ACTION_PREV = "com.scooter1556.sms.android.prev";
    public static final String ACTION_NEXT = "com.scooter1556.sms.android.next";
    public static final String ACTION_STOP = "com.scooter1556.sms.android.stop";
    public static final String ACTION_STOP_CASTING = "com.scooter1556.sms.android.stop_cast";

    public static final int NOTIFICATION_ICON_SIZE = 200;

    private final MediaService mediaService;
    private MediaSessionCompat.Token mediaSessionToken;
    private MediaControllerCompat mediaController;
    private MediaControllerCompat.TransportControls transportControls;

    private PlaybackStateCompat playbackState;
    private MediaMetadataCompat mediaMetadata;

    private final NotificationManager notificationManager;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent nextIntent;
    private final PendingIntent stopIntent;
    private final PendingIntent stopCastIntent;

    private boolean started = false;

    public MediaNotificationManager(MediaService service) throws RemoteException {
        mediaService = service;
        updateSessionToken();

        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mediaService.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(mediaService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(mediaService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(mediaService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(mediaService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopIntent = PendingIntent.getBroadcast(mediaService, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopCastIntent = PendingIntent.getBroadcast(mediaService, REQUEST_CODE,
                new Intent(ACTION_STOP_CASTING).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the service was killed and restarted by the system.
        notificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is destroyed.
     */
    public void startNotification() {
        if (!started) {
            mediaMetadata = mediaController.getMetadata();
            playbackState = mediaController.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();

            if (notification != null) {
                mediaController.registerCallback(mediaCallback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                filter.addAction(ACTION_STOP_CASTING);
                mediaService.registerReceiver(this, filter);

                mediaService.startForeground(NOTIFICATION_ID, notification);
                started = true;
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (started) {
            started = false;
            mediaController.unregisterCallback(mediaCallback);

            try {
                notificationManager.cancel(NOTIFICATION_ID);
                mediaService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // Ignore if the receiver is not registered.
            }

            mediaService.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case ACTION_PAUSE:
                transportControls.pause();
                break;
            case ACTION_PLAY:
                transportControls.play();
                break;
            case ACTION_NEXT:
                transportControls.skipToNext();
                break;
            case ACTION_PREV:
                transportControls.skipToPrevious();
                break;
            case ACTION_STOP_CASTING:
                Intent i = new Intent(context, MediaService.class);
                i.setAction(MediaService.ACTION_CMD);
                i.putExtra(MediaService.CMD_NAME, MediaService.CMD_STOP_CASTING);
                mediaService.startService(i);
                break;
            default:
                Log.w(TAG, "Unknown action ignored. Action=" +  action);
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session.
     */
    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token newToken = mediaService.getSessionToken();

        if (mediaSessionToken == null && newToken != null || mediaSessionToken != null && !mediaSessionToken.equals(newToken)) {
            if (mediaController != null) {
                mediaController.unregisterCallback(mediaCallback);
            }
            mediaSessionToken = newToken;

            if (mediaSessionToken != null) {
                mediaController = new MediaControllerCompat(mediaService, mediaSessionToken);
                transportControls = mediaController.getTransportControls();

                if (started) {
                    mediaController.registerCallback(mediaCallback);
                }
            }
        }
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        Intent intent = new Intent(mediaService, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(HomeActivity.EXTRA_START_FULLSCREEN, true);

        if (description != null) {
            intent.putExtra(HomeActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, description);
        }

        return PendingIntent.getActivity(mediaService, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private final MediaControllerCompat.Callback mediaCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            playbackState = state;

            if (state.getState() == PlaybackStateCompat.STATE_STOPPED || state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mediaMetadata = metadata;

            Notification notification = createNotification();

            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();

            try {
                updateSessionToken();
            } catch (RemoteException e) {
                Log.e(TAG, "Could not connect media controller", e);
            }
        }
    };

    private Notification createNotification() {
        Log.d(TAG, "createNotification()");

        if (mediaMetadata == null || playbackState == null) {
            return null;
        }

        MediaDescriptionCompat description = mediaMetadata.getDescription();
        Bitmap art = BitmapFactory.decodeResource(mediaService.getResources(), R.drawable.ic_placeholder_audio);

        // Notification channels are only supported on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mediaService, CHANNEL_ID);

        final int playPauseButtonPosition = addActions(notificationBuilder);

        notificationBuilder
                .setDeleteIntent(stopIntent)
                .setColor(ContextCompat.getColor(mediaService, R.color.primary))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_notification)
                .setOnlyAlertOnce(true)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);

        if (mediaController != null && mediaController.getExtras() != null) {
            String castName = mediaController.getExtras().getString(MediaService.EXTRA_CONNECTED_CAST);

            if (castName != null) {
                String castInfo = mediaService.getResources().getString(R.string.cast_to_device, castName);
                notificationBuilder.setSubText(castInfo);
                notificationBuilder.addAction(R.drawable.ic_clear_black_24dp, mediaService.getString(R.string.cast_stop), stopCastIntent);
            }
        }

        setNotificationPlaybackState(notificationBuilder);

        if (description.getIconUri() != null) {
            String url = description.getIconUri().toString();
            url = url + "?scale=" + NOTIFICATION_ICON_SIZE;

            Glide.with(mediaService)
                    .asBitmap()
                    .load(url)
                    .into(new SimpleTarget<Bitmap>(NOTIFICATION_ICON_SIZE , NOTIFICATION_ICON_SIZE) {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            notificationBuilder.setLargeIcon(resource);
                            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                        }
                    });
        }

        return notificationBuilder.build();
    }

    private int addActions(final NotificationCompat.Builder notificationBuilder) {
        int playPauseButtonPosition = 0;

        // If skip to previous action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(R.drawable.ic_skip_previous_white_24dp,
                    mediaService.getString(R.string.label_previous), previousIntent);

            /*
             * If there is a "skip to previous" button, the play/pause button will
             * be the second one. We need to keep track of it, because the MediaStyle notification
             * requires to specify the index of the buttons (actions) that should be visible
             * when in compact view.
             */
            playPauseButtonPosition = 1;
        }

        // Play or pause button, depending on the current state.
        final String label;
        final int icon;
        final PendingIntent intent;

        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = mediaService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause_white_24dp;
            intent = pauseIntent;
        } else {
            label = mediaService.getString(R.string.label_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
            intent = playIntent;
        }

        notificationBuilder.addAction(new NotificationCompat.Action(icon, label, intent));

        // If skip to next action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
                    mediaService.getString(R.string.label_next), nextIntent);
        }

        return playPauseButtonPosition;
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        if (playbackState == null || !started) {
            mediaService.stopForeground(true);
            return;
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    /**
     * Creates Notification Channel. This is required in Android O+ to display notifications.
     */
     private void createNotificationChannel() {
         if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            mediaService.getString(R.string.notification_channel),
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription(mediaService.getString(R.string.notification_channel_description));
            notificationManager.createNotificationChannel(notificationChannel);
         }
    }
}