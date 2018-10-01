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

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;

import android.view.ViewGroup;
import android.widget.ImageView;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MenuItem;

public class MenuItemPresenter extends Presenter {

    private static int selectedBackgroundColor;
    private static int defaultBackgroundColor;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        defaultBackgroundColor = ContextCompat.getColor(parent.getContext(), R.color.primary);
        selectedBackgroundColor = ContextCompat.getColor(parent.getContext(), R.color.primary_dark);

        ImageCardView view = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        updateCardBackgroundColor(view, false);
        return new ViewHolder(view);
    }

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? selectedBackgroundColor : defaultBackgroundColor;

        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        MenuItem menuItem = (MenuItem) item;

        if(menuItem == null) {
            return;
        }

        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Get title
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER);
        cardView.setTitleText(menuItem.getTitle());
        cardView.setMainImage(menuItem.getIcon());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
