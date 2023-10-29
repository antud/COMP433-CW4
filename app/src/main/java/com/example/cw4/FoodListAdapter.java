package com.example.cw4;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class FoodListAdapter extends ArrayAdapter<FoodItem> {
    FoodListAdapter(Context context, int resource, ArrayList<FoodItem> objects) {
        super(context, resource, objects);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item, parent, false);
        }
        FoodItem currentItem = getItem(position);
        ImageView foodImage = convertView.findViewById(R.id.foodImage);
        TextView foodName = convertView.findViewById(R.id.foodName);
        foodImage.setImageResource(currentItem.imageResource);
        foodName.setText(currentItem.tagText);
        return convertView;
    }
}