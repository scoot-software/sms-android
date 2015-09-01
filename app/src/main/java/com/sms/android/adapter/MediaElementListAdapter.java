package com.sms.android.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sms.android.R;
import com.sms.lib.android.domain.MediaElement;
import com.sms.lib.android.service.RESTService;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by scott2ware on 18/12/14.
 */
public class MediaElementListAdapter extends ArrayAdapter<MediaElement> {

    private static final int DIRECTORY_VIEW = 0;
    private static final int AUDIO_DIRECTORY_VIEW = 1;
    private static final int VIDEO_DIRECTORY_VIEW = 2;
    private static final int VIDEO_SMALL_VIEW = 3;
    private static final int VIDEO_VIEW = 4;
    private static final int AUDIO_VIEW = 5;

    private final Context context;
    private List<MediaElement> items;

    public MediaElementListAdapter(Context context, List<MediaElement> items) {
        super(context, R.layout.list_directory, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public MediaElement getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getID();
    }

    @Override
    public int getViewTypeCount() {
        return 6;
    }

    @Override
    public int getItemViewType(int position) {

        switch (items.get(position).getType()) {

            case MediaElement.MediaElementType.DIRECTORY:

                switch(items.get(position).getDirectoryType()) {

                    case MediaElement.DirectoryMediaType.NONE: case MediaElement.DirectoryMediaType.MIXED:
                        return DIRECTORY_VIEW;

                    case MediaElement.DirectoryMediaType.AUDIO:
                        return AUDIO_DIRECTORY_VIEW;

                    case MediaElement.DirectoryMediaType.VIDEO:
                        return VIDEO_DIRECTORY_VIEW;
                }


            case MediaElement.MediaElementType.VIDEO:

                if (items.get(position).getDescription() == null) {
                    return VIDEO_SMALL_VIEW;
                } else {
                    return VIDEO_VIEW;
                }

            case MediaElement.MediaElementType.AUDIO:
                return AUDIO_VIEW;
        }

        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(context);

        int viewType = getItemViewType(position);

        if(convertView == null) {

            switch(viewType) {

                case DIRECTORY_VIEW:
                    convertView = inflater.inflate(R.layout.list_directory, parent, false);
                    break;

                case AUDIO_DIRECTORY_VIEW:
                    convertView = inflater.inflate(R.layout.list_audio_directory, parent, false);
                    break;

                case VIDEO_DIRECTORY_VIEW:
                    convertView = inflater.inflate(R.layout.list_video_directory, parent, false);
                    break;

                case VIDEO_VIEW:
                    convertView = inflater.inflate(R.layout.list_video, parent, false);
                    break;

                case VIDEO_SMALL_VIEW:
                    convertView = inflater.inflate(R.layout.list_video_small, parent, false);
                    break;

                case AUDIO_VIEW:
                    convertView = inflater.inflate(R.layout.list_audio, parent, false);
                    break;

            }
        }

        switch(viewType) {

            case DIRECTORY_VIEW:

                // Title
                TextView dirTitle = (TextView) convertView.findViewById(R.id.title);

                if(items.get(position).getTitle() != null) {
                    dirTitle.setText(items.get(position).getTitle());
                }
                else {
                    dirTitle.setText("");
                }

                // Cover Art
                ImageView dirThumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);

                Picasso.with(context)
                        .load(RESTService.getInstance().getBaseUrl() + "/image/" + items.get(position).getID() + "/cover/80")
                        .placeholder(R.drawable.ic_content_loading)
                        .error(R.drawable.ic_content_folder)
                        .into(dirThumbnail);

                break;

            case AUDIO_DIRECTORY_VIEW:

                // Title
                TextView aDirTitle = (TextView) convertView.findViewById(R.id.title);

                if(items.get(position).getTitle() != null) {
                    aDirTitle.setText(items.get(position).getTitle());
                }
                else {
                    aDirTitle.setText("");
                }

                // Artist
                TextView aDirCollection = (TextView) convertView.findViewById(R.id.artist);

                if(items.get(position).getArtist() != null) {
                    aDirCollection.setText(items.get(position).getArtist());
                }
                else {
                    aDirCollection.setText("");
                }

                // Description

                TextView aDirDescription = (TextView) convertView.findViewById(R.id.description);

                if (items.get(position).getDescription() != null) {
                    aDirDescription.setText(items.get(position).getDescription());
                }
                else {
                    aDirDescription.setText("");
                }

                // Year
                TextView aDirYear = (TextView) convertView.findViewById(R.id.year);

                if(items.get(position).getYear() != null) {
                    aDirYear.setText(items.get(position).getYear().toString());
                }
                else {
                    aDirYear.setText("");
                }

                // Cover Art
                ImageView aDirThumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);

                Picasso.with(context)
                        .load(RESTService.getInstance().getBaseUrl() + "/image/" + items.get(position).getID() + "/cover/80")
                        .placeholder(R.drawable.ic_content_loading)
                        .error(R.drawable.ic_content_album)
                        .into(aDirThumbnail);

                break;

            case VIDEO_DIRECTORY_VIEW:

                // Title
                final TextView vDirTitle = (TextView) convertView.findViewById(R.id.title);

                if(items.get(position).getTitle() != null) {
                    vDirTitle.setText(items.get(position).getTitle());
                }
                else {
                    vDirTitle.setText("(Unknown_Directory)");
                }

                // Collection
                final TextView vDirCollection = (TextView) convertView.findViewById(R.id.collection);

                if(items.get(position).getCollection() != null) {
                    vDirCollection.setText(items.get(position).getCollection());
                }
                else {
                    vDirCollection.setVisibility(View.GONE);
                }

                // Year
                TextView vDirYear = (TextView) convertView.findViewById(R.id.year);

                if(items.get(position).getYear() != null) {
                    vDirYear.setText(items.get(position).getYear().toString());
                }
                else {
                    vDirYear.setVisibility(View.GONE);
                }

                // Cover Art
                final ImageView vDirThumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);

                Picasso.with(context)
                        .load(RESTService.getInstance().getBaseUrl() + "/image/" + items.get(position).getID() + "/cover/80")
                        .into(vDirThumbnail, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                ((RelativeLayout.LayoutParams) vDirTitle.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, vDirThumbnail.getId());
                                ((RelativeLayout.LayoutParams) vDirCollection.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, vDirThumbnail.getId());
                            }

                            @Override
                            public void onError() {
                                vDirThumbnail.setVisibility(View.GONE);
                            }
                        });

                break;

            case VIDEO_VIEW:

                // Title
                TextView vidTitle = (TextView) convertView.findViewById(R.id.title);

                if(items.get(position).getTitle() != null) {
                    vidTitle.setText(items.get(position).getTitle());
                }
                else {
                    vidTitle.setText("");
                }

                // Rating
                TextView rating = (TextView) convertView.findViewById(R.id.rating);

                if(items.get(position).getRating() != null) {
                    rating.setText(Html.fromHtml("<b>Rating:</b> " + items.get(position).getRating().toString()));
                }
                else {
                    rating.setText("");
                }

                // Year
                TextView vidYear = (TextView) convertView.findViewById(R.id.year);

                if(items.get(position).getYear() != null) {
                    vidYear.setText(Html.fromHtml("<b>Year:</b> " + items.get(position).getYear().toString()));
                }
                else {
                    vidYear.setText("");
                }

                // Certification
                TextView certification = (TextView) convertView.findViewById(R.id.certification);

                if(items.get(position).getCertificate() != null) {
                    certification.setText(Html.fromHtml("<b>Certificate:</b> " + items.get(position).getCertificate()));
                }
                else {
                    certification.setText("");
                }

                // Duration
                TextView vidDuration = (TextView) convertView.findViewById(R.id.duration);

                if(items.get(position).getDuration() != null) {
                    vidDuration.setText(Html.fromHtml("<b>Duration:</b> " + secondsToString(items.get(position).getDuration())));
                }
                else {
                    vidDuration.setText("");
                }

                // Description

                TextView description = (TextView) convertView.findViewById(R.id.description);

                if (items.get(position).getDescription() != null) {
                    description.setText(items.get(position).getDescription());
                }
                else {
                    description.setText("");
                }

                // Cover Art
                final ImageView vidThumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);

                Picasso.with(context)
                        .load(RESTService.getInstance().getBaseUrl() + "/image/" + items.get(position).getID() + "/cover/150")
                        .error(R.drawable.ic_content_video)
                        .into(vidThumbnail);

                break;

            case VIDEO_SMALL_VIEW:

                // Title
                final TextView sVidTitle = (TextView) convertView.findViewById(R.id.title);

                if(items.get(position).getTitle() != null) {
                    sVidTitle.setText(items.get(position).getTitle());
                }
                else {
                    sVidTitle.setText("(Unknown_Video_File)");
                }

                // Duration
                TextView sVidDuration = (TextView) convertView.findViewById(R.id.duration);

                if(items.get(position).getDuration() != null) {
                    sVidDuration.setText(secondsToString(items.get(position).getDuration()));
                }
                else {
                    sVidDuration.setVisibility(View.GONE);
                }

                // Cover Art
                final ImageView sVidThumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);

                Picasso.with(context)
                        .load(RESTService.getInstance().getBaseUrl() + "/image/" + items.get(position).getID() + "/cover/80")
                        .into(sVidThumbnail, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                ((RelativeLayout.LayoutParams) sVidTitle.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, sVidThumbnail.getId());
                            }

                            @Override
                            public void onError() {
                                sVidThumbnail.setVisibility(View.GONE);
                            }
                        });

                break;

            case AUDIO_VIEW:

                // Track Number
                TextView audTrackNumber = (TextView) convertView.findViewById(R.id.track_number);

                if(items.get(position).getTrackNumber() != null) {
                    audTrackNumber.setText(items.get(position).getTrackNumber().toString());
                }
                else {
                    audTrackNumber.setText("0");
                }

                // Title
                TextView audTitle = (TextView) convertView.findViewById(R.id.title);

                if(items.get(position).getTitle() != null) {
                    audTitle.setText(items.get(position).getTitle());
                }
                else {
                    audTitle.setVisibility(View.GONE);
                }

                // Artist
                TextView artist = (TextView) convertView.findViewById(R.id.artist);

                if(items.get(position).getArtist() != null) {
                    artist.setText(items.get(position).getArtist());
                }
                else {
                    artist.setVisibility(View.GONE);
                }

                // Disc Subtitle
                TextView discSubtitle = (TextView) convertView.findViewById(R.id.discSubtitle);

                if (items.get(position).getDiscSubtitle() != null) {
                    discSubtitle.setText(items.get(position).getDiscSubtitle());
                }
                else {
                    discSubtitle.setVisibility(View.GONE);
                }

                // Duration
                TextView audDuration = (TextView) convertView.findViewById(R.id.duration);

                if(items.get(position).getDuration() != null) {
                    audDuration.setText(secondsToString(items.get(position).getDuration()));
                }
                else {
                    audDuration.setVisibility(View.GONE);
                }

                break;
        }

        return convertView;
    }

    //
    // Helper Functions
    //

    public List<MediaElement> getItemList() {
        return items;
    }

    public void setItemList(List<MediaElement> itemList) {
        this.items = itemList;
    }

    private static String secondsToString(int totalSecs) {

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        String timeString;

        if(totalSecs > 3600) {
            timeString = String.format("%01d:%02d:%02d", hours, minutes, seconds);
        }
        else {
            timeString = String.format("%01d:%02d", minutes, seconds);
        }

        return timeString;
    }
}
