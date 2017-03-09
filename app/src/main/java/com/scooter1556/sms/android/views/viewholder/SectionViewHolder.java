package com.scooter1556.sms.android.views.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.scooter1556.sms.android.R;

public class SectionViewHolder extends BaseViewHolder {
    public RecyclerView recyclerView;

    public SectionViewHolder(View view) {
        super(view);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    }
}
