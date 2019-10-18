package de.jlab.cardroid.devices.storage;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceHandler;

@Entity(tableName = "devices")
public final class DeviceEntity {

    /**
     * Empty constructor for ROOM only.
     */
    public DeviceEntity() {
        this.features = new ArrayList<>();
    }

    public DeviceEntity(String deviceUid, String displayName, Class<? extends DeviceHandler> deviceClass) {
        this();
        this.deviceUid = deviceUid;
        this.displayName = displayName;
        this.className = deviceClass.getSimpleName();
    }

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "device_uid")
    public String deviceUid;

    @ColumnInfo(name = "class_name")
    public String className;

    @TypeConverters(DeviceFeatureConverters.class)
    public ArrayList<String> features;

    public boolean isDeviceType(@NonNull DeviceHandler device) {
        return device.getClass().getSimpleName().equals(this.className);
    }

    public void addFeature(Class<? extends DeviceDataProvider> feature) {
        if (!this.features.contains(feature.getSimpleName())) {
            this.features.add(feature.getSimpleName());
        }
    }

}
