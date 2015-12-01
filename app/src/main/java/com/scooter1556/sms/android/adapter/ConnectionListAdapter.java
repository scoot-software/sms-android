package com.scooter1556.sms.android.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.lib.android.domain.Connection;
import com.scooter1556.sms.lib.android.domain.MediaFolder;

import java.util.List;

/**
 * List adapter for connection.
 *
 * Created by scott2ware on 18/12/14.
 */
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

        // URL
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
