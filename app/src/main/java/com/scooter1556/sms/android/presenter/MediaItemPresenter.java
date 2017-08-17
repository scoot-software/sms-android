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
package com.scooter1556.sms.android.presenter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.List;

public class MediaItemPresenter extends Presenter {

    private static int CARD_HEIGHT = 300;
    private static int CARD_WIDTH = 300;

    private static int selectedBackgroundColor;
    private static int defaultBackgroundColor;

    private static Drawable defaultDirectoryIcon;
    private static Drawable defaultAudioIcon;
    private static Drawable defaultVideoIcon;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        defaultBackgroundColor = ContextCompat.getColor(parent.getContext(), R.color.primary);
        selectedBackgroundColor = ContextCompat.getColor(parent.getContext(), R.color.primary_dark);

        defaultDirectoryIcon = ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_directory);
        defaultAudioIcon = ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_audio);
        defaultVideoIcon = ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_video);

        ImageCardView view = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        view.setFocusable(true);

        updateCardBackgroundColor(view, false);
        return new ViewHolder(view);
    }

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? selectedBackgroundColor : defaultBackgroundColor;

        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        MediaBrowserCompat.MediaItem element = (MediaBrowserCompat.MediaItem) item;

        if(element == null) {
            return;
        }

        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Get title
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER);
        cardView.setTitleText(element.getDescription().getTitle());
        cardView.setContentText(element.getDescription().getSubtitle());

        // Get default icon
        Drawable icon;

        switch(MediaUtils.getMediaTypeFromID(element.getMediaId())) {
            case MediaElement.MediaElementType.AUDIO:
                icon = defaultAudioIcon;
                break;

            case MediaElement.MediaElementType.VIDEO:
                icon = defaultVideoIcon;
                break;

            case MediaElement.MediaElementType.DIRECTORY:
                icon = defaultDirectoryIcon;
                break;

            default:
                icon = defaultDirectoryIcon;
                break;
        }

        List<String> id = MediaUtils.parseMediaId(element.getMediaId());

        if(id.size() > 1) {
            // Set image
            Glide.with(viewHolder.view.getContext())
                    .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/cover/" + CARD_HEIGHT)
                    .asBitmap()
                    .error(icon)
                    .into(cardView.getMainImageView());
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
