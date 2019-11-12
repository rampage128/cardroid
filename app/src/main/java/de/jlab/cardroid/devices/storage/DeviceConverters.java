package de.jlab.cardroid.devices.storage;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.room.TypeConverter;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class DeviceConverters {

    @TypeConverter
    public static ArrayList<String> readFeatures(String value) {
        return new ArrayList<>(Arrays.asList(TextUtils.split(value, ",")));
    }

    @TypeConverter
    public static String writeFeatures(ArrayList<String> list) {
        return TextUtils.join(",", list);
    }

    @TypeConverter
    public static DeviceUid readDeviceUid(String value) {
        return new DeviceUid(value);
    }

    @TypeConverter
    public static String writeDeviceUid(DeviceUid uid) {
        return uid.toString();
    }

}
