package de.jlab.cardroid.devices.storage;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;

@Entity(tableName = "devices")
public final class DeviceEntity {

    /**
     * Empty constructor for ROOM only.
     */
    public DeviceEntity() {
        this.features = new ArrayList<>();
    }

    public DeviceEntity(DeviceUid deviceUid, String displayName, Class<? extends DeviceHandler> deviceClass) {
        this();
        this.deviceUid = deviceUid;
        this.displayName = displayName;
        this.className = deviceClass.getSimpleName();
    }

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "display_name")
    public String displayName;

    @TypeConverters(DeviceConverters.class)
    @ColumnInfo(name = "device_uid")
    public DeviceUid deviceUid;

    @ColumnInfo(name = "class_name")
    public String className;

    // TODO: replace this with ArrayList<Class<? extends Feature>>
    @TypeConverters(DeviceConverters.class)
    public ArrayList<String> features;

    public boolean isDeviceType(@NonNull DeviceHandler device) {
        return device.getClass().getSimpleName().equals(this.className);
    }

    public void addFeature(Feature feature) {
        if (!this.features.contains(feature.getClass().getCanonicalName())) {
            this.features.add(feature.getClass().getCanonicalName());
        }
    }

}
