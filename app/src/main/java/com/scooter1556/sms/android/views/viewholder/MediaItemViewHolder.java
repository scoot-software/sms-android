package com.scooter1556.sms.android.views.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.scooter1556.sms.android.R;

public class MediaItemViewHolder extends RecyclerView.ViewHolder {
    public ImageView image;
    public TextView title;
    public TextView subtitle;

    public MediaItemViewHolder(View view) {
        super(view);

        image = (ImageView) view.findViewById(R.id.image);
        title = (TextView) view.findViewById(R.id.title);
        subtitle = (TextView) view.findViewById(R.id.subtitle);
    }
}
