package com.scooter1556.sms.android.domain;

import android.graphics.drawable.Drawable;

public class MenuItem {
    private Drawable icon;
    private String title;

    public MenuItem(Drawable icon, String title) {
        this.icon = icon;
        this.title = title;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
