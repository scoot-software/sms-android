package com.scooter1556.sms.android.views.viewholder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.scooter1556.sms.android.R;

public class MediaFolderViewHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public TextView subtitle;
    public ImageView image;

    public MediaFolderViewHolder(View view){
        super(view);

        title = (TextView) view.findViewById(R.id.title);
        subtitle = (TextView) view.findViewById(R.id.subtitle);
        image = (ImageView) view.findViewById(R.id.image);
    }

}

