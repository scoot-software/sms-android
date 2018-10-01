package com.scooter1556.sms.android.presenter;

import androidx.leanback.widget.AbstractMediaListHeaderPresenter;

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
