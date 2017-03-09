package com.scooter1556.sms.android.views.viewholder;

import android.view.View;
import android.widget.TextView;

import com.scooter1556.sms.android.R;

public class SectionHeaderViewHolder extends BaseViewHolder {

    public TextView title;

    public SectionHeaderViewHolder(View view) {
        super(view);
        title = (TextView) itemView.findViewById(R.id.title);
    }
}
