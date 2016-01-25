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

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.NavigationDrawerListItem;

public class NavigationDrawerListItemAdapter extends ArrayAdapter<NavigationDrawerListItem> {

    Context context;
    int layoutResourceId;
    NavigationDrawerListItem items[] = null;

    public NavigationDrawerListItemAdapter(Context context, int layoutResourceId, NavigationDrawerListItem[] items) {

        super(context, layoutResourceId, items);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View listItem = convertView;

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        listItem = inflater.inflate(layoutResourceId, parent, false);

        ImageView icon = (ImageView) listItem.findViewById(R.id.icon_image);
        TextView title = (TextView) listItem.findViewById(R.id.title_text);

        NavigationDrawerListItem item = items[position];

        icon.setImageResource(item.getIcon());
        title.setText(item.getTitle());

        return listItem;
    }

}