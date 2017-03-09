package com.scooter1556.sms.android.views.adapter;

import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

import com.scooter1556.sms.android.provider.BaseDataProvider;
import com.scooter1556.sms.android.views.viewholder.BaseViewHolder;

public abstract class BaseAdapter extends Adapter {

    public static final int TYPE_HEADER = -1;

    protected BaseDataProvider dataProvider;

    public BaseAdapter(BaseDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.dataProvider.createOnlyItemsWithoutTitles();
    }

    @Override
    public abstract BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public abstract void onBindViewHolder(ViewHolder holder, int position);

    public abstract int getSpanSize(int position);


    @Override
    public int getItemCount() {
        return dataProvider.getOnlyItemsWithoutTitles().size() + dataProvider.getTitles().size();
    }

    @Override
    public int getItemViewType(int position) {
        if(dataProvider.isHeaderSection(position)) {
            return TYPE_HEADER;
        } else {
            return dataProvider.getTypeOfItem(position);
        }
    }

    public boolean isHeaderSection(int position) {
        return dataProvider.isHeaderSection(position);
    }

    public String getHeaderTitle(int position) {
        return dataProvider.getHeaderTitle(position);
    }

    public int getRealPosition(int position) {
        return dataProvider.getRelativePosition(position);
    }
}