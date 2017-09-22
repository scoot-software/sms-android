package com.scooter1556.sms.android.views.adapter;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.h6ah4i.android.widget.advrecyclerview.headerfooter.AbstractHeaderFooterWrapperAdapter;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.views.viewholder.HeaderFooterViewHolder;

import java.util.ArrayList;
import java.util.List;


public class HeaderFooterAdapter extends AbstractHeaderFooterWrapperAdapter<HeaderFooterViewHolder, HeaderFooterViewHolder> {

    int headerCounter;
    int footerCounter;
    List<HeaderFooterItem> headerItems;
    List<HeaderFooterItem> footerItems;

    static class HeaderFooterItem {
        public final int viewType;
        public final String text;

        public HeaderFooterItem(int viewType, String text) {
            this.viewType = viewType;
            this.text = text;
        }
    }

    public HeaderFooterAdapter(RecyclerView.Adapter adapter) {
        setAdapter(adapter);
        headerItems = new ArrayList<>();
        footerItems = new ArrayList<>();

        //this.setHasStableIds(true);
    }

    @Override
    public int getHeaderItemCount() {
        return headerItems.size();
    }

    @Override
    public int getFooterItemCount() {
        return footerItems.size();
    }

    @Override
    public HeaderFooterViewHolder onCreateHeaderItemViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header, parent, false);
        HeaderFooterViewHolder vh = new HeaderFooterViewHolder(v);
        return vh;
    }

    @Override
    public HeaderFooterViewHolder onCreateFooterItemViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_footer, parent, false);
        HeaderFooterViewHolder vh = new HeaderFooterViewHolder(v);
        return vh;
    }

    @Override
    public void onBindHeaderItemViewHolder(HeaderFooterViewHolder holder, int localPosition) {
        HeaderFooterItem item = headerItems.get(localPosition);
        holder.title.setText(item.text);
    }

    @Override
    public void onBindFooterItemViewHolder(HeaderFooterViewHolder holder, int localPosition) {
        HeaderFooterItem item = footerItems.get(localPosition);
        holder.title.setText(item.text);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void addHeaderItem(String title) {
        int viewType = headerCounter;
        String text = title;
        headerCounter += 1;
        headerItems.add(new HeaderFooterItem(viewType, text));
        getHeaderAdapter().notifyItemInserted(headerItems.size() - 1);
    }

    public void addFooterItem(String title) {
        int viewType = footerCounter;
        String text = title;
        footerCounter += 1;
        footerItems.add(new HeaderFooterItem(viewType, text));
        getFooterAdapter().notifyItemInserted(footerItems.size() - 1);
    }

    // Filling span for GridLayoutManager
    public void setupFullSpanForGridLayoutManager(RecyclerView recyclerView) {
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();

        if (!(lm instanceof GridLayoutManager)) {
            return;
        }

        final GridLayoutManager glm = (GridLayoutManager) lm;
        final GridLayoutManager.SpanSizeLookup origSizeLookup = glm.getSpanSizeLookup();
        final int spanCount = glm.getSpanCount();

        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                final long segmentedPosition = getSegmentedPosition(position);
                final int segment = extractSegmentPart(segmentedPosition);
                final int offset = extractSegmentOffsetPart(segmentedPosition);

                if (segment == SEGMENT_TYPE_HEADER) {
                    return spanCount;
                } else if (segment == SEGMENT_TYPE_FOOTER) {
                    return spanCount;
                } else {
                    return origSizeLookup.getSpanSize(offset);
                }
            }
        });
    }
}
