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

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import com.scooter1556.sms.android.domain.MediaElement;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        MediaElement element = (MediaElement) item;

        if (element == null) {
            return;
        }

        switch(element.getDirectoryType()) {
            case MediaElement.DirectoryMediaType.VIDEO:
                viewHolder.getTitle().setText(element.getTitle());

                if(element.getCollection() != null) {
                    viewHolder.getSubtitle().setText(element.getCollection());
                } else if (element.getYear() != null) {
                    viewHolder.getSubtitle().setText(String.format("%d", element.getYear()));
                }

                viewHolder.getBody().setText(element.getDescription());
                break;

            case MediaElement.DirectoryMediaType.AUDIO:
                viewHolder.getTitle().setText(element.getTitle());
                viewHolder.getSubtitle().setText(element.getArtist());
                viewHolder.getBody().setText(String.format("%d", element.getYear()));
                break;

            default:
                viewHolder.getTitle().setText(element.getTitle());
                viewHolder.getSubtitle().setText(null);
                viewHolder.getBody().setText(null);
        }
    }
}
