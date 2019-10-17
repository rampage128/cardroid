package de.jlab.cardroid.devices.storage;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.room.TypeConverter;

public final class DeviceFeatureConverters {

    @TypeConverter
    public static ArrayList<String> fromString(String value) {
        return new ArrayList<>(Arrays.asList(TextUtils.split(value, ",")));
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<String> list) {
        return TextUtils.join(",", list);
    }

}
