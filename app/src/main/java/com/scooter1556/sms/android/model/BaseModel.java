package com.scooter1556.sms.android.model;

public class BaseModel {

    public int type;
    public String id;

    public BaseModel(int type, String id) {
        this.type = type;
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public String getId(){
        return id;
    }

    public boolean equals(BaseModel o) {
        return (this.id.equals(o.getId()));
    }
}
