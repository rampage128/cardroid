package de.jlab.cardroid.rules.storage;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = { ActionEntity.class, EventEntity.class }, version = 1, exportSchema = false)
public abstract class RulesDatabase extends RoomDatabase {
    public abstract ActionDao actionDao();
    public abstract EventDao eventDao();

    private static volatile RulesDatabase INSTANCE;

    static RulesDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RulesDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            RulesDatabase.class, "rules_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
