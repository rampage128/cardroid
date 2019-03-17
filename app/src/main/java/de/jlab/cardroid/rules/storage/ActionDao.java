package de.jlab.cardroid.rules.storage;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

@Dao
public interface ActionDao {

    @Query("SELECT * FROM actions")
    List<ActionEntity> getAll();

    @Query("SELECT * FROM actions WHERE uid =:uid")
    LiveData<ActionEntity> get(int uid);

    @Insert
    void insert(ActionEntity... actionEntities);

    @Update
    void update(ActionEntity... actionEntities);

    @Delete()
    void delete(ActionEntity... actionEntities);

}
