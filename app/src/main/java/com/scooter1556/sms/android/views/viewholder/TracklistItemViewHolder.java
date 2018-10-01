package com.scooter1556.sms.android.views.viewholder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.scooter1556.sms.android.R;

public class TracklistItemViewHolder extends RecyclerView.ViewHolder {
    private final View rootView;
    private final TextView track;
    private final TextView title;
    private final TextView subtitle;

    public TracklistItemViewHolder(View view) {
        super(view);

        rootView = view;
        track = (TextView) view.findViewById(R.id.track);
        title = (TextView) view.findViewById(R.id.title);
        subtitle = (TextView) view.findViewById(R.id.subtitle);
    }
}
