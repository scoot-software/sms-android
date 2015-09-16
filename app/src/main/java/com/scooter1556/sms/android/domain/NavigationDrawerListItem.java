package com.scooter1556.sms.android.domain;

/**
 * Created by scott2ware on 19/02/15.
 */
public class NavigationDrawerListItem {

    private int icon;
    private String title;

    // Constructor.
    public NavigationDrawerListItem(int icon, String title) {

        this.icon = icon;
        this.title = title;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getIcon() {
        return icon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
