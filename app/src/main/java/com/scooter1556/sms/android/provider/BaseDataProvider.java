package com.scooter1556.sms.android.provider;

import com.scooter1556.sms.android.model.BaseModel;

import java.util.ArrayList;
import java.util.HashMap;

public class BaseDataProvider {

    private HashMap<Integer, ArrayList<BaseModel>> featuredData;
    private ArrayList<BaseModel> onlyItemsWithoutTitles;
    private ArrayList<String> titles;

    public BaseDataProvider(int totalItems) {
        featuredData = new HashMap<>();
        titles = new ArrayList<>();
        onlyItemsWithoutTitles = new ArrayList<>();

        for(int i = 0; i < totalItems; i++) {
            titles.add(i, "");
        }
    }

    public void addFeaturedData(String titleOfSection, ArrayList<BaseModel> data, int type) {
        featuredData.put(type, data);
        titles.set(type, titleOfSection);
    }

    public ArrayList<String> getTitles() {
        return titles;
    }

    public int getTotalSections() {
        return featuredData.size();
    }

    public int getTotalItemsOfSection(int section) {
        return featuredData.get(section).size();
    }

    public void createOnlyItemsWithoutTitles() {
        for(int i = 0; i < featuredData.keySet().size(); i++) {
            if(featuredData.get(i) != null) {
                onlyItemsWithoutTitles.addAll(featuredData.get(i));
            }
        }
    }

    public ArrayList<BaseModel> getOnlyItemsWithoutTitles() {
        return onlyItemsWithoutTitles;
    }

    public int getTypeOfItem(int position) {
        return onlyItemsWithoutTitles.get(getRelativePosition(position)).getType();
    }

    public BaseModel getItem(int position) {
        return onlyItemsWithoutTitles.get(getRelativePosition(position));
    }

    public boolean isHeaderSection(int position) {
        int itemCount = 0;

        for (int i = 0; i < getTotalSections(); i++) {
            if (itemCount == position) return true;
            itemCount++;
            itemCount += getTotalItemsOfSection(i);
        }

        return false;
    }

    public String getHeaderTitle(int position) {
        int totalItemsCount = 0;
        String titleResult = "";

        for(String title : titles) {
            ArrayList<BaseModel> section = featuredData.get(titles.indexOf(title));
            totalItemsCount += (section.size() + 1);

            if(position < totalItemsCount) {
                titleResult = title;
                break;
            }
        }

        return titleResult;
    }

    public int getRelativePosition(int position) {
        int totalSections = getTotalSections();
        int countSection = 0;
        int countItems = 0;

        for(int i = 0; i < totalSections; i++) {
            countSection++;
            countItems += featuredData.get(i).size();

            if(position <= countItems) {
                return position - countSection;
            }
        }

        return position - countItems;
    }
}
