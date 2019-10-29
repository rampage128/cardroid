package de.jlab.cardroid.rules.storage;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.storage.DeviceConverters;

@Entity(tableName = "events", indices = {@Index(value = {"identifier", "device_uid"},
        unique = true)})
public class EventEntity {

    public EventEntity(int identifier, @NonNull DeviceUid deviceUid, @NonNull String name) {
        this.identifier = identifier;
        this.deviceUid = deviceUid;
        this.name = name;
    }

    @PrimaryKey(autoGenerate = true)
    public int uid;

    public int identifier;

    @TypeConverters(DeviceConverters.class)
    @ColumnInfo(name = "device_uid")
    public DeviceUid deviceUid;

    public String name;

    public boolean equals(EventEntity other) {
        return  other != null &&
                this.identifier == other.identifier &&
                Objects.equals(this.deviceUid, other.deviceUid);
    }
}
