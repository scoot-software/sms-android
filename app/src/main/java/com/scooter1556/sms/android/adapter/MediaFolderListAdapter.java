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
import com.scooter1556.sms.lib.android.domain.MediaFolder;

import java.util.List;

/**
 * Created by scott2ware on 18/12/14.
 */
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
        return items.get(position).getID();
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
