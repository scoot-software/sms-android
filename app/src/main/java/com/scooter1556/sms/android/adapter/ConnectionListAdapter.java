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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.lib.android.domain.Connection;

import java.util.List;

public class ConnectionListAdapter extends ArrayAdapter<Connection> {

    private final Context context;
    private List<Connection> items;
    long selectedItemID, checkedItemID;

    public ConnectionListAdapter(Context context, List<Connection> items) {
        super(context, R.layout.list_connection, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Connection getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getID();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(context);

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.list_connection, parent, false);
        }

        // Title
        TextView title = (TextView) convertView.findViewById(R.id.title);
        title.setText(items.get(position).getTitle());

        // Url
        TextView url = (TextView) convertView.findViewById(R.id.url);
        url.setText(items.get(position).getUrl());

        // Radio Button
        RadioButton selected = (RadioButton) convertView.findViewById(R.id.selected);
        selected.setChecked(items.get(position).getID() == checkedItemID);

        return convertView;
    }

    public List<Connection> getItemList() {
        return items;
    }

    public void setItemList(List<Connection> itemList) {
        this.items = itemList;
    }

    public void setSelectedItemID(long id) {
        this.selectedItemID = id;
    }

    public long getSelectedItemID() {
        return selectedItemID;
    }

    public void setCheckedItemID(long id) {
        this.checkedItemID = id;
    }

    public long getCheckedItemID() {
        return checkedItemID;
    }
}
