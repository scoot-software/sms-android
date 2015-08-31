package com.sms.android.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.sms.android.R;
import com.sms.android.adapter.NavigationDrawerListItemAdapter;
import com.sms.android.fragment.AudioPlayerSmallFragment;
import com.sms.lib.android.domain.MediaElement;
import com.sms.lib.android.domain.MediaFolder;
import com.sms.android.domain.NavigationDrawerListItem;
import com.sms.android.fragment.AudioPlayerFragment;
import com.sms.android.fragment.AudioPlaylistFragment;
import com.sms.android.fragment.MediaElementFragment;
import com.sms.android.fragment.MediaFolderFragment;
import com.sms.lib.android.service.AudioPlayerService;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MediaFolderFragment.MediaFolderListener, MediaElementFragment.MediaElementListener, AudioPlaylistFragment.AudioPlaylistListener, AudioPlayerService.AudioPlayerListener, AudioPlayerFragment.AudioControllerListener, FragmentManager.OnBackStackChangedListener {

    public static final int RESULT_CODE_SETTINGS = 101;

    // The index for the navigation drawer menus
    private static final int MENU_MEDIA_BROWSER = 0;
    private static final int MENU_SETTINGS = 1;
    private static final int MENU_LOGOUT = 2;
    private static final int MENU_EXIT = 3;

    // The index for the sliding panel views
    private static final int SLIDING_PANEL_SMALL_PLAYER = 10;
    private static final int SLIDING_PANEL_PLAYER = 11;
    private static final int SLIDING_PANEL_PLAYLIST = 12;

    // Save state index
    private static final String STATE_SLIDING_PANEL = "state_sliding_panel";

    // Fragments
    AudioPlaylistFragment audioPlaylistFragment;
    AudioPlayerFragment audioPlayerFragment;
    AudioPlayerSmallFragment audioPlayerSmallFragment;
    Fragment mediaBrowserFragment;

    // Sliding Panel
    private SlidingUpPanelLayout slidingPanel;

    // Audio Player Service
    private AudioPlayerService audioPlayerService;
    private Intent audioPlayerIntent;
    private boolean audioPlayerBound = false;

    // Navigation Drawer
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence title, mediaBrowserTitle, slidingPanelTitle;
    private int lastDrawerItem, currentDrawerItem = MENU_MEDIA_BROWSER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Action Bar
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Navigation Drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.navigation_drawer_list);

        NavigationDrawerListItem[] drawerListItems = new NavigationDrawerListItem[4];
        drawerListItems[MENU_MEDIA_BROWSER] = new NavigationDrawerListItem(R.drawable.ic_action_collection, getResources().getStringArray(R.array.navigation_drawer_list_items)[MENU_MEDIA_BROWSER]);
        drawerListItems[MENU_SETTINGS] = new NavigationDrawerListItem(R.drawable.ic_action_settings, getResources().getStringArray(R.array.navigation_drawer_list_items)[MENU_SETTINGS]);
        drawerListItems[MENU_LOGOUT] = new NavigationDrawerListItem(R.drawable.ic_action_accounts, getResources().getStringArray(R.array.navigation_drawer_list_items)[MENU_LOGOUT]);
        drawerListItems[MENU_EXIT] = new NavigationDrawerListItem(R.drawable.ic_action_remove, getResources().getStringArray(R.array.navigation_drawer_list_items)[MENU_EXIT]);
        NavigationDrawerListItemAdapter drawerAdapter = new NavigationDrawerListItemAdapter(this, R.layout.drawer_list_item, drawerListItems);
        drawerList.setAdapter(drawerAdapter);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(title);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                title = getSupportActionBar().getTitle();
                getSupportActionBar().setTitle(getString(R.string.navigation_drawer_title));
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        // Sliding Panel
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingPanel.setPanelHeight((int) getResources().getDimension(R.dimen.audio_player_small_fragment_height));

        PanelSlideListener slidingPanelListener = new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelAnchored(final View panel) {
            }

            @Override
            public void onPanelCollapsed(final View panel) {
                View audioPlayerSmallContainer = findViewById(R.id.sliding_panel_small_container);
                View audioPlayerContainer = findViewById(R.id.sliding_panel_container);
                audioPlayerSmallContainer.setVisibility(View.VISIBLE);
                audioPlayerSmallContainer.setAlpha(1.0f);
                audioPlayerContainer.setVisibility(View.GONE);
                audioPlayerContainer.setAlpha(1.0f);

                // Update menu items
                audioPlayerFragment.setMenuVisibility(false);
                audioPlaylistFragment.setMenuVisibility(false);
                mediaBrowserFragment.setMenuVisibility(true);

                //Update action bar title
                getSupportActionBar().setTitle(title);
            }

            @Override
            public void onPanelExpanded(final View panel) {
                View audioPlayerSmallContainer = findViewById(R.id.sliding_panel_small_container);
                View audioPlayerContainer = findViewById(R.id.sliding_panel_container);
                audioPlayerSmallContainer.setVisibility(View.GONE);
                audioPlayerSmallContainer.setAlpha(1.0f);
                audioPlayerContainer.setVisibility(View.VISIBLE);
                audioPlayerContainer.setAlpha(1.0f);

                // Update menu items
                audioPlayerFragment.setMenuVisibility(audioPlayerFragment.isVisible());
                audioPlaylistFragment.setMenuVisibility(audioPlaylistFragment.isVisible());
                mediaBrowserFragment.setMenuVisibility(false);

                //Update action bar title
                getSupportActionBar().setTitle(slidingPanelTitle);
            }

            @Override
            public void onPanelHidden(final View view) {
            }

            @Override
            public void onPanelSlide(final View panel, final float slideOffset) {
                View audioPlayerSmallContainer = findViewById(R.id.sliding_panel_small_container);
                View audioPlayerContainer = findViewById(R.id.sliding_panel_container);

                if (slideOffset < 1.0f) {
                    audioPlayerSmallContainer.setVisibility(View.VISIBLE);
                    audioPlayerContainer.setVisibility(View.VISIBLE);
                } else {
                    audioPlayerSmallContainer.setVisibility(View.GONE);
                    audioPlayerContainer.setVisibility(View.VISIBLE);
                }

                audioPlayerSmallContainer.setAlpha(1.0f - slideOffset);
                audioPlayerContainer.setAlpha(slideOffset);
            }
        };

        slidingPanel.setPanelSlideListener(slidingPanelListener);

        //
        // Initialise Fragments
        //

        // Initialise small audio player sliding panel fragment
        audioPlayerSmallFragment = new AudioPlayerSmallFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.sliding_panel_small_container, audioPlayerSmallFragment, Integer.toString(SLIDING_PANEL_SMALL_PLAYER)).commit();

        // Initialise audio player fragment
        audioPlayerFragment = new AudioPlayerFragment();
        audioPlayerFragment.setMenuVisibility(false);
        getSupportFragmentManager().beginTransaction().add(R.id.sliding_panel_container, audioPlayerFragment, Integer.toString(SLIDING_PANEL_PLAYER)).commit();
        slidingPanelTitle = getString(R.string.audio_player_title);

        // Initialise audio playlist fragment
        audioPlaylistFragment = new AudioPlaylistFragment();
        audioPlaylistFragment.setMenuVisibility(false);
        getSupportFragmentManager().beginTransaction().add(R.id.sliding_panel_container, audioPlaylistFragment, Integer.toString(SLIDING_PANEL_PLAYLIST)).hide(audioPlaylistFragment).commit();

        if (savedInstanceState == null) {
            // Initialise main view
            mediaBrowserFragment = new MediaFolderFragment();
            mediaBrowserTitle = title = getString(R.string.media_title);
            getSupportFragmentManager().beginTransaction().add(R.id.main_container, mediaBrowserFragment, Integer.toString(MENU_MEDIA_BROWSER)).commit();
            assert getSupportActionBar() != null;
            getSupportActionBar().setTitle(getString(R.string.media_title));
            updateDrawer(MENU_MEDIA_BROWSER);
        }
        else {
            // Reload fragments
            mediaBrowserFragment = getSupportFragmentManager().findFragmentByTag(Integer.toString(MENU_MEDIA_BROWSER));
/*
            // Set sliding panel state
            if(savedInstanceState.getSerializable(STATE_SLIDING_PANEL) == PanelState.EXPANDED) {
                slidingPanel.setPanelState(PanelState.EXPANDED);
                slidingPanelListener.onPanelSlide(slidingPanel, 1.0f);
                slidingPanelListener.onPanelExpanded(slidingPanel);
            }
            else {
                slidingPanel.setPanelState(PanelState.COLLAPSED);
                slidingPanelListener.onPanelSlide(slidingPanel, 0.0f);
                slidingPanelListener.onPanelCollapsed(slidingPanel);
            }
            */
        }

        // Add fragment back stack listener
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(final Bundle state) {
        // Save sliding panel state
        state.putSerializable(STATE_SLIDING_PANEL, slidingPanel.getPanelState());

        //getSupportFragmentManager().beginTransaction().remove(audioPlayerSmallFragment).commit();
        //getSupportFragmentManager().beginTransaction().remove(audioPlayerFragment).commit();
        //getSupportFragmentManager().beginTransaction().remove(audioPlaylistFragment).commit();

        super.onSaveInstanceState(state);
    }

    // Connect to the Audio Player service
    private ServiceConnection audioPlayerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bindAudioPlayerService(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioPlayerBound = false;
        }
    };

    private void bindAudioPlayerService(IBinder service) {
        // Get Service
        AudioPlayerService.AudioPlayerBinder binder = (AudioPlayerService.AudioPlayerBinder) service;
        audioPlayerService = binder.getService();
        audioPlayerService.registerListener(this);
        audioPlayerBound = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(audioPlayerIntent == null) {
            audioPlayerIntent = new Intent(this, AudioPlayerService.class);
            bindService(audioPlayerIntent, audioPlayerConnection, Context.BIND_AUTO_CREATE);
            startService(audioPlayerIntent);
        }
    }

    @Override
    protected void onDestroy() {
        if (audioPlayerService != null) {
            audioPlayerService.unregisterListener(this);
        }
        unbindService(audioPlayerConnection);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectDrawerItem(position);
        }

    }

    public void selectDrawerItem(int position) {

        switch (position) {

            case MENU_MEDIA_BROWSER:
                // If this option is already selected ignore the request and close the draw
                if(drawerList.getCheckedItemPosition() == position) {
                    drawerLayout.closeDrawer(drawerList);

                    // Check sliding panel is collapsed so library view is visible
                    if(slidingPanel.getPanelState() == PanelState.EXPANDED) {
                        slidingPanel.setPanelState(PanelState.COLLAPSED);
                    }

                    break;
                }

                // Create a new media browser fragment if one does not already exist, otherwise load from the previous position
                if(mediaBrowserFragment == null) {
                    mediaBrowserFragment = new MediaFolderFragment();
                    mediaBrowserTitle = getString(R.string.media_title);
                }

                // Load media browser fragment
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, mediaBrowserFragment, Integer.toString(position))
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .addToBackStack(Integer.toString(position))
                        .commit();

                // Set title ready to be set when drawer is updated
                title = mediaBrowserTitle;
                updateDrawer(position);
                break;

            case MENU_SETTINGS:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsIntent, RESULT_CODE_SETTINGS);
                updateDrawer(position);
                break;

            case MENU_LOGOUT:
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                loginIntent.putExtra("logout",true);
                startActivity(loginIntent);
                finish();

            case MENU_EXIT:
                stopService(audioPlayerIntent);
                audioPlayerService = null;
                finish();
                System.exit(0);

            default:
                break;
        }
    }

    public void updateDrawer(int position) {
        lastDrawerItem = currentDrawerItem;
        currentDrawerItem = position;
        drawerList.setItemChecked(position, true);
        drawerList.setSelection(position);
        drawerLayout.closeDrawer(drawerList);
    }

    @Override
    public void onBackStackChanged() {
        if(getFragmentManager().getBackStackEntryCount() > 0) {
            String name = getFragmentManager().getBackStackEntryAt(getFragmentManager().getBackStackEntryCount() - 1).getName();
            drawerList.setItemChecked(Integer.parseInt(name), true);
        }
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(drawerList)) {
            drawerLayout.closeDrawer(drawerList);
        }
        else if(slidingPanel.getPanelState() == PanelState.EXPANDED) {
            slidingPanel.setPanelState(PanelState.COLLAPSED);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void MediaFolderSelected(MediaFolder folder) {

        // Create fragment and give it an argument for the selected folder
        Bundle arguments = new Bundle();
        arguments.putLong("id", folder.getID());
        arguments.putString("title", folder.getName());
        arguments.putByte("directoryType", MediaElement.DirectoryMediaType.NONE);
        arguments.putBoolean("folder", true);

        mediaBrowserFragment = new MediaElementFragment();
        mediaBrowserFragment.setArguments(arguments);
        mediaBrowserTitle = folder.getName();

        getSupportFragmentManager().beginTransaction().replace(R.id.main_container, mediaBrowserFragment, Integer.toString(MENU_MEDIA_BROWSER))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .addToBackStack(Integer.toString(MENU_MEDIA_BROWSER))
                .commit();

        updateDrawer(MENU_MEDIA_BROWSER);
    }

    @Override
    public void MediaElementSelected(MediaElement element) {

        if(element.getType().equals(MediaElement.MediaElementType.DIRECTORY)) {

            // Create fragment and give it an argument for the selected element
            Bundle arguments = new Bundle();
            arguments.putLong("id", element.getID());
            arguments.putString("title", element.getTitle());
            arguments.putByte("directoryType", element.getDirectoryType());
            arguments.putBoolean("folder", false);

            mediaBrowserFragment = new MediaElementFragment();
            mediaBrowserFragment.setArguments(arguments);
            mediaBrowserTitle = element.getTitle();

            getSupportFragmentManager().beginTransaction().replace(R.id.main_container, mediaBrowserFragment, Integer.toString(MENU_MEDIA_BROWSER))
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .addToBackStack(Integer.toString(MENU_MEDIA_BROWSER))
                    .commit();

            updateDrawer(MENU_MEDIA_BROWSER);
        }
        else if(element.getType().equals(MediaElement.MediaElementType.AUDIO)) {
            audioPlayerService.addAndPlay(element);
        }
        else if(element.getType().equals(MediaElement.MediaElementType.VIDEO)) {
            // Make sure Audio Service is not playing
            if(audioPlayerService.isPlaying()) { audioPlayerService.stop(); }

            // Load video player
            Intent i = new Intent(this, VideoPlayerActivity.class);
            i.putExtra("mediaElement", element);
            startActivity(i);
        }
    }

    @Override
    public void PlayAll(ArrayList<MediaElement> mediaElements) {
        if(mediaElements == null) { return; }

        ArrayList<MediaElement> audioElements = new ArrayList<>();

        for (MediaElement element : mediaElements) {

            if(element.getType().equals(MediaElement.MediaElementType.AUDIO)) {
                audioElements.add(element);
            }
        }

        audioPlayerService.playAll(audioElements);
    }

    @Override
    public void AddAllToQueue(ArrayList<MediaElement> mediaElements) {
        if (mediaElements == null) {
            return;
        }

        ArrayList<MediaElement> audioElements = new ArrayList<>();

        for (MediaElement element : mediaElements) {

            if (element.getType().equals(MediaElement.MediaElementType.AUDIO)) {
                audioElements.add(element);
            }
        }

        audioPlayerService.addAllToQueue(audioElements);
    }

    /**
     * This method is called when an activity has quit which was called with
     * startActivityForResult method. Depending on the given request and result
     * code certain action can be done.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE_SETTINGS) {
            updateDrawer(lastDrawerItem);
        }
    }

    //
    // Audio Playlist callbacks
    //

    @Override
    public void PlaylistItemSelected(int position) {
        audioPlayerService.setMediaListPosition(position);
        audioPlayerService.play();
    }

    @Override
    public ArrayList<MediaElement> getCurrentPlaylist() {
        if(audioPlayerService == null) { return null; }
        return audioPlayerService.getMediaList();
    }

    @Override
    public int getPlaylistPosition() {
        if (audioPlayerService == null) {
            return 0;
        }
        return audioPlayerService.getMediaListPosition();
    }

    public void clearAll() { audioPlayerService.clearMediaList(); }

    @Override
    public void showNowPlaying() {
        // Load audio player fragment in sliding panel container and update menu items
        getSupportFragmentManager().beginTransaction().show(audioPlayerFragment).hide(audioPlaylistFragment).commit();
        audioPlaylistFragment.setMenuVisibility(false);
        audioPlayerFragment.setMenuVisibility(true);

        // Update action bar title
        assert getSupportActionBar() != null;
        slidingPanelTitle = getString(R.string.audio_player_title);
        getSupportActionBar().setTitle(slidingPanelTitle);
    }

    //
    // Audio Player Callbacks
    //

    @Override
    public void PlaybackStateChanged() {
        if(audioPlayerFragment != null) { audioPlayerFragment.updatePlayerControls(); }
        if(audioPlayerSmallFragment != null) { audioPlayerSmallFragment.updatePlayerControls(); }
    }

    @Override
    public void PlaylistPositionChanged() {
        if(audioPlaylistFragment != null) { audioPlaylistFragment.updateCurrentPosition(); }
        if(audioPlayerFragment != null) { audioPlayerFragment.updateMediaInfo(); }
        if(audioPlayerSmallFragment != null) { audioPlayerSmallFragment.updateMediaInfo(); }
    }

    @Override
    public void PlaylistChanged() {
        if(audioPlaylistFragment != null) { audioPlaylistFragment.updatePlaylist(); }
    }

    //
    // Audio Controller Callbacks
    //

    @Override
    public boolean isPlaying() {
        return !(audioPlayerService == null || !audioPlayerBound) &&  audioPlayerService.isPlaying();
    }

    @Override
    public boolean isPaused() {
        return !(audioPlayerService == null || !audioPlayerBound) &&  audioPlayerService.isPaused();
    }

    @Override
    public void seek(int pos) {
        if(audioPlayerService != null && audioPlayerBound) { audioPlayerService.seek(pos); }
    }

    @Override
    public int getCurrentPosition() {
        if(audioPlayerService == null || !audioPlayerBound) { return 0; }
        return audioPlayerService.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        if(audioPlayerService == null || !audioPlayerBound) { return -1; }
        return audioPlayerService.getDuration();
    }

    @Override
    public void pause() {
        if(audioPlayerService != null && audioPlayerBound) { audioPlayerService.pause(); }
    }

    @Override
    public void start() {
        if(audioPlayerService != null && audioPlayerBound) { audioPlayerService.start(); }
    }

    @Override
    public void stop() {
        if(audioPlayerService != null && audioPlayerBound) { audioPlayerService.stop(); }
    }

    @Override
    public void playNext() {
        if(audioPlayerService != null && audioPlayerBound) { audioPlayerService.playNext(); }
    }

    @Override
    public void playPrev() {
        if(audioPlayerService != null && audioPlayerBound) { audioPlayerService.playPrev(); }
    }

    @Override
    public MediaElement getMediaElement() {
        if(audioPlayerService == null) { return null; }
        return audioPlayerService.getMediaElement();
    }

    @Override
    public void showPlaylist() {
        // Load playlist fragment in sliding panel container and update menu items
        getSupportFragmentManager().beginTransaction().hide(audioPlayerFragment).show(audioPlaylistFragment).commit();
        audioPlaylistFragment.setMenuVisibility(true);
        audioPlayerFragment.setMenuVisibility(false);

        // Update action bar title
        assert getSupportActionBar() != null;
        slidingPanelTitle = getString(R.string.playlist_title);
        getSupportActionBar().setTitle(slidingPanelTitle);
    }
}
