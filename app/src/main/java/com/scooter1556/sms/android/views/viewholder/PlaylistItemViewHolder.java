package com.scooter1556.sms.android.views.viewholder;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.scooter1556.sms.android.R;

import androidx.recyclerview.widget.RecyclerView;

public class PlaylistItemViewHolder extends RecyclerView.ViewHolder {
    public ImageView image;
    public TextView title;
    public TextView subtitle;
    public ImageButton menu;

    public PlaylistItemViewHolder(View view) {
        super(view);

        image = (ImageView) view.findViewById(R.id.image);
        title = (TextView) view.findViewById(R.id.title);
        subtitle = (TextView) view.findViewById(R.id.subtitle);
        menu = (ImageButton) view.findViewById(R.id.menu);
    }
}
