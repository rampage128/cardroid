package de.jlab.cardroid.usb.carduino.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

import de.jlab.cardroid.R;
import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.car.CarSystemFactory;
import de.jlab.cardroid.car.UnknownCarSystemException;

public class CarSystemTreeAdapter extends BaseExpandableListAdapter {

    private Context context;
    private ArrayList<CarSystem> carSystemList = new ArrayList<>();
    private ArrayList<CarSystemFactory> typeList = new ArrayList<>();

    private LinkedHashMap<CarSystemFactory, String[]> propertyMap = new LinkedHashMap<>();

    static class GroupViewHolder {
        TextView label;
        TextView details;
    }

    static class ChildViewHolder {
        TextView label;
        TextView value;
    }

    public CarSystemTreeAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getGroupCount() {
        return this.carSystemList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.propertyMap.get(this.typeList.get(groupPosition)).length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.carSystemList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.propertyMap.get(this.typeList.get(groupPosition))[childPosition];
    }

    @Override
    public long getGroupId(int groupPosition) {
        return typeList.get(groupPosition).getIdentifier();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder holder;
        if (convertView == null) {
            holder = new GroupViewHolder();
            final LayoutInflater layoutInflater = LayoutInflater.from(this.context);
            convertView = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_2, null);
            holder.label = (TextView) convertView.findViewById(android.R.id.text1);
            holder.details = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        }
        else {
            holder = (GroupViewHolder)convertView.getTag();
        }

        Class<? extends CarSystem> carSystemClass = this.carSystemList.get(groupPosition).getClass();
        holder.label.setText(carSystemClass.getSimpleName());
        holder.details.setText(carSystemClass.getCanonicalName());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        if (convertView == null) {
            holder = new ChildViewHolder();
            final LayoutInflater layoutInflater = LayoutInflater.from(this.context);
            convertView = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_2, null);
            holder.label = (TextView) convertView.findViewById(android.R.id.text1);
            holder.value = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        }
        else {
            holder = (ChildViewHolder)convertView.getTag();
        }

        CarSystem carSystem = this.carSystemList.get(groupPosition);
        CarSystemFactory type = this.typeList.get(groupPosition);
        String propertyName = this.propertyMap.get(type)[childPosition];
        holder.label.setText(propertyName);
        try {
            holder.value.setText(Objects.toString(carSystem.get(propertyName)));
        } catch (Exception e) {
            holder.value.setText(this.context.getString(R.string.status_unavailable));
        }

        return convertView;

    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public void updateCarSystem(CarSystem carSystem) {
        try {
            CarSystemFactory type = CarSystemFactory.getType(carSystem);
            if (!this.propertyMap.containsKey(type)) {
                this.carSystemList.add(carSystem);
                this.typeList.add(type);
                this.propertyMap.put(type, carSystem.getPropertyKeys());
            }
        }
        catch (UnknownCarSystemException e) { /* Intentionally left blank */ }
    }

}
