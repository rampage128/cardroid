package de.jlab.cardroid.rules.storage;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {

    public EventEntity(int identifier, String name) {
        this.identifier = identifier;
        this.name = name;
    }

    @PrimaryKey
    public int identifier;

    public String name;
}
