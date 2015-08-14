package com.sms.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sms.android.R;
import com.sms.android.domain.NavigationDrawerListItem;

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