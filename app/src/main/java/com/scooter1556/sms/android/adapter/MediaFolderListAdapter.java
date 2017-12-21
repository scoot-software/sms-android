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
package com.scooter1556.sms.android.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaFolder;

import java.util.List;

public class MediaFolderListAdapter extends ArrayAdapter<MediaFolder> {

    private final Context context;
    private List<MediaFolder> items;

    public MediaFolderListAdapter(Context context, List<MediaFolder> items) {
        super(context, R.layout.list_directory, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public MediaFolder getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getID().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(context);

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.list_directory, parent, false);
        }

        // Title
        TextView title = (TextView) convertView.findViewById(R.id.title);
        title.setText(items.get(position).getName());

        ImageView thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
        if(items.get(position).getType() == MediaFolder.ContentType.AUDIO) {
            thumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_library_audio));
        }
        else if(items.get(position).getType() == MediaFolder.ContentType.VIDEO) {
            thumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_library_video_dark));
        }

        return convertView;
    }

    public List<MediaFolder> getItemList() {
        return items;
    }

    public void setItemList(List<MediaFolder> itemList) {
        this.items = itemList;
    }
}
