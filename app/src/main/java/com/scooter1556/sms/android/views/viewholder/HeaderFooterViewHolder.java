package com.scooter1556.sms.android.views.viewholder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.scooter1556.sms.android.R;

public class HeaderFooterViewHolder extends RecyclerView.ViewHolder {

    public TextView title;

    public HeaderFooterViewHolder(View view) {
        super(view);
        title = (TextView) view.findViewById(R.id.title);
    }
}
