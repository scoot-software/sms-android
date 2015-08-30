package com.sms.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sms.android.R;
import com.sms.lib.android.domain.MediaElement;

import java.util.List;

/**
 * Created by scott2ware on 18/12/14.
 */
public class PlaylistAdapter extends ArrayAdapter<MediaElement> {

    private final Context context;
    private List<MediaElement> items;

    public PlaylistAdapter(Context context, List<MediaElement> items) {
        super(context, R.layout.list_playlist, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        if(items == null) { return 0; }
        return items.size();
    }

    @Override
    public MediaElement getItem(int position) {
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
            convertView = inflater.inflate(R.layout.list_playlist, parent, false);
        }

        // Title
        TextView audTitle = (TextView) convertView.findViewById(R.id.title);

        if(items.get(position).getTitle() != null) {
            audTitle.setText(items.get(position).getTitle());
        }
        else {
            audTitle.setText("");
        }

        // Artist
        TextView artist = (TextView) convertView.findViewById(R.id.artist);

        if(items.get(position).getArtist() != null) {
            artist.setText(items.get(position).getArtist());
        }
        else {
            artist.setText("");
        }

        // Disc Subtitle
        TextView discSubtitle = (TextView) convertView.findViewById(R.id.discSubtitle);

        if (items.get(position).getDiscSubtitle() != null) {
            discSubtitle.setText(items.get(position).getDiscSubtitle());
        }
        else {
            discSubtitle.setText("");
        }

        // Duration
        TextView audDuration = (TextView) convertView.findViewById(R.id.duration);

        if(items.get(position).getDuration() != null) {
            audDuration.setText(secondsToString(items.get(position).getDuration()));
        }
        else {
            audDuration.setText("");
        }

        return convertView;
    }

    //
    // Helper Functions
    //

    public List<MediaElement> getItemList() {
        return items;
    }

    public void setItemList(List<MediaElement> itemList) {
        this.items = itemList;
    }

    private static String secondsToString(int totalSecs) {

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        String timeString;

        if(totalSecs > 3600) {
            timeString = String.format("%01d:%02d:%02d", hours, minutes, seconds);
        }
        else {
            timeString = String.format("%01d:%02d", minutes, seconds);
        }

        return timeString;
    }
}
