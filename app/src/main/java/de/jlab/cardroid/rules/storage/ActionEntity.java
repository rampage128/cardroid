package de.jlab.cardroid.rules.storage;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import de.jlab.cardroid.rules.properties.Property;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "actions",
        foreignKeys = {
            @ForeignKey(entity = EventEntity.class, parentColumns = "uid", childColumns = "event_uid", onDelete = CASCADE)
        },
        indices = {
                @Index("event_uid")
        })
public class ActionEntity {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "class_name")
    public String className;

    @ColumnInfo(name = "event_uid")
    public int eventUid;

    @TypeConverters(ActionPropertyConverters.class)
    public List<Property<?>> properties;

    @NonNull
    public Property<?> getPropertyByKey(@NonNull String key, Object defaultValue) {
        if (properties != null && !properties.isEmpty()) {
            for (Property<?> property : this.properties) {
                if (key.equals(property.getKey())) {
                    return property;
                }
            }
        }

        return new Property<>(key, defaultValue);
    }

}
