package com.scooter1556.sms.android.views.adapter;

import android.content.Context;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.model.SectionDataModel;
import com.scooter1556.sms.android.views.viewholder.SectionViewHolder;

import java.util.ArrayList;

public class SectionDataAdapter extends RecyclerView.Adapter<SectionViewHolder> {

    private Context context;
    ArrayList<SectionDataModel> sections;

    public SectionDataAdapter(Context ctx) {
        this.context = ctx;
    }

    @Override
    public SectionViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_section, viewGroup, false);
        SectionViewHolder vh = new SectionViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final SectionViewHolder holder, int position) {
        RecyclerView.Adapter adapter = sections.get(position).getAdapter();
        holder.recyclerView.hasFixedSize();
        holder.recyclerView.setItemAnimator(new DefaultItemAnimator());

        final GridLayoutManager layoutManager = new GridLayoutManager(context, 2);
        holder.recyclerView.setLayoutManager(layoutManager);

        holder.recyclerView.setAdapter(adapter);

        holder.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        holder.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        // Determine span count for current configuration
                        int viewWidth = holder.recyclerView.getWidth();
                        float cardViewWidth = context.getResources().getDimension(R.dimen.card_media_width);
                        int spanCount = (int) Math.floor(viewWidth / cardViewWidth);

                        Log.d("FUCK", "View Width=" + viewWidth + " Card Width=" + cardViewWidth + " Span Count=" + spanCount);

                        layoutManager.setSpanCount(spanCount);
                        layoutManager.requestLayout();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return (null != sections ? sections.size() : 0);
    }

    public void setSections(ArrayList<SectionDataModel> sections) {
        this.sections = sections;
    }
}