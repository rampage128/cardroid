package de.jlab.cardroid.storage;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import de.jlab.cardroid.devices.storage.DeviceDao;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.rules.storage.ActionDao;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.EventDao;
import de.jlab.cardroid.rules.storage.EventEntity;

@androidx.room.Database(entities = { ActionEntity.class, EventEntity.class, DeviceEntity.class }, version = 1, exportSchema = false)
public abstract class CardroidDatabase extends RoomDatabase {
    public abstract ActionDao actionDao();
    public abstract EventDao eventDao();
    public abstract DeviceDao deviceDao();

    private static volatile CardroidDatabase INSTANCE;

    public static CardroidDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (CardroidDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            CardroidDatabase.class, "cardroid")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
