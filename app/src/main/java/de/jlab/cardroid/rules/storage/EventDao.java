package de.jlab.cardroid.rules.storage;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

@Dao
public interface EventDao {

    @Query("SELECT * FROM events")
    LiveData<List<EventEntity>> getAll();

    @Query("SELECT * FROM events WHERE identifier =:identifier")
    LiveData<EventEntity> get(int identifier);

    @Transaction
    @Query("SELECT * FROM events")
    LiveData<List<RuleDefinition>> getAllRules();

    @Transaction
    @Query("SELECT * FROM events WHERE uid =:eventUid")
    LiveData<RuleDefinition> getAsRule(int eventUid);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(EventEntity... eventEntities);

    @Update
    void update(EventEntity... eventEntities);

}
