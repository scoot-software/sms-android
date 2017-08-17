package com.scooter1556.sms.android.presenter;

import android.support.v17.leanback.widget.AbstractMediaListHeaderPresenter;

import com.scooter1556.sms.android.R;

public class HeaderPresenter extends AbstractMediaListHeaderPresenter {

    public HeaderPresenter() {
        super();
    }

    @Override
    protected void onBindMediaListHeaderViewHolder(ViewHolder vh, Object item) {
        String header = (String) item;
        vh.getHeaderView().setText(header);
    }

}
