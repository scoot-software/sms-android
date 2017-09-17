package com.scooter1556.sms.android.fragment.tv;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnChildLaidOutListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scooter1556.sms.android.R;

/**
 * A fragment for rendering items in a vertical grids.
 */
public class TvGridFragment extends Fragment implements BrowseFragment.MainFragmentAdapterProvider {
    private static final String TAG = "VerticalGridFragment";

    private ObjectAdapter adapter;
    private VerticalGridPresenter gridPresenter;
    private VerticalGridPresenter.ViewHolder gridViewHolder;
    private OnItemViewSelectedListener onItemViewSelectedListener;
    private OnItemViewClickedListener onItemViewClickedListener;
    private Object sceneAfterEntranceTransition;
    private int selectedPosition = -1;

    private BrowseFragment.MainFragmentAdapter mainFragmentAdapter =
            new BrowseFragment.MainFragmentAdapter(this) {
                @Override
                public void setEntranceTransitionState(boolean state) {
                    TvGridFragment.this.setEntranceTransitionState(state);
                }
            };

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }

        this.gridPresenter = gridPresenter;
        gridPresenter.setOnItemViewSelectedListener(mViewSelectedListener);

        if (onItemViewClickedListener != null) {
            gridPresenter.setOnItemViewClickedListener(onItemViewClickedListener);
        }
    }

    /**
     * Returns the grid presenter.
     */
    public VerticalGridPresenter getGridPresenter() {
        return this.gridPresenter;
    }

    /**
     * Sets the object adapter for the fragment.
     */
    public void setAdapter(ObjectAdapter adapter) {
        this.adapter = adapter;
        updateAdapter();
    }

    /**
     * Returns the object adapter.
     */
    public ObjectAdapter getAdapter() {
        return this.adapter;
    }

    final private OnItemViewSelectedListener mViewSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = gridViewHolder.getGridView().getSelectedPosition();
                    gridOnItemSelected(position);

                    if (onItemViewSelectedListener != null) {
                        onItemViewSelectedListener.onItemSelected(itemViewHolder, item, rowViewHolder, row);
                    }
                }
            };

    final private OnChildLaidOutListener childLaidOutListener =
            new OnChildLaidOutListener() {
                @Override
                public void onChildLaidOut(ViewGroup parent, View view, int position, long id) {
                    if (position == 0) {
                        showOrHideTitle();
                    }
                }
            };

    /**
     * Sets an item selection listener.
     */
    public void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        onItemViewSelectedListener = listener;
    }

    private void gridOnItemSelected(int position) {
        if (position != selectedPosition) {
            selectedPosition = position;
            showOrHideTitle();
        }
    }

    private void showOrHideTitle() {
        if (gridViewHolder.getGridView().findViewHolderForAdapterPosition(selectedPosition) == null) {
            return;
        }

        if (!gridViewHolder.getGridView().hasPreviousViewInSameRow(selectedPosition)) {
            mainFragmentAdapter.getFragmentHost().showTitleView(true);
        } else {
            mainFragmentAdapter.getFragmentHost().showTitleView(false);
        }
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        onItemViewClickedListener = listener;

        if (gridPresenter != null) {
            gridPresenter.setOnItemViewClickedListener(onItemViewClickedListener);
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return onItemViewClickedListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tv_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup gridDock = (ViewGroup) view.findViewById(R.id.browse_grid_dock);
        gridViewHolder = gridPresenter.onCreateViewHolder(gridDock);
        gridDock.addView(gridViewHolder.view);
        gridViewHolder.getGridView().setOnChildLaidOutListener(childLaidOutListener);

        sceneAfterEntranceTransition = TransitionHelper.createScene(gridDock, new Runnable() {
            @Override
            public void run() {
                setEntranceTransitionState(true);
            }
        });

        getMainFragmentAdapter().getFragmentHost().notifyViewCreated(mainFragmentAdapter);
        updateAdapter();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        gridViewHolder = null;
    }

    @Override
    public BrowseFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mainFragmentAdapter;
    }

    /**
     * Sets the selected item position.
     */
    public void setSelectedPosition(int position) {
        selectedPosition = position;

        if(gridViewHolder != null && gridViewHolder.getGridView().getAdapter() != null) {
            gridViewHolder.getGridView().setSelectedPositionSmooth(position);
        }
    }

    private void updateAdapter() {
        if (gridViewHolder != null) {
            gridPresenter.onBindViewHolder(gridViewHolder, adapter);

            if (selectedPosition != -1) {
                gridViewHolder.getGridView().setSelectedPosition(selectedPosition);
            }
        }
    }

    void setEntranceTransitionState(boolean afterTransition) {
        gridPresenter.setEntranceTransitionState(gridViewHolder, afterTransition);
    }
}
