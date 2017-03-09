package com.scooter1556.sms.android.views.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.scooter1556.sms.android.R;

public class MediaFolderViewHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public TextView subtitle;
    public ImageView image1;
    public ImageView image2;
    public ImageView image3;
    public ImageView image4;

    public MediaFolderViewHolder(View view){
        super(view);

        title = (TextView) view.findViewById(R.id.title);
        subtitle = (TextView) view.findViewById(R.id.subtitle);
        image1 = (ImageView) view.findViewById(R.id.image_1);
        image2 = (ImageView) view.findViewById(R.id.image_2);
        image3 = (ImageView) view.findViewById(R.id.image_3);
        image4 = (ImageView) view.findViewById(R.id.image_4);
    }

}

