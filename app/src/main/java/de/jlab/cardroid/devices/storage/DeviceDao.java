package de.jlab.cardroid.devices.storage;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DeviceDao {

    @Query("SELECT * FROM devices")
    List<DeviceEntity> getAllSynchronous();

    @Query("SELECT * FROM devices")
    LiveData<List<DeviceEntity>> getAll();

    @Query("SELECT * FROM devices WHERE uid =:uid")
    LiveData<DeviceEntity> get(int uid);

    @Query("SELECT * FROM devices WHERE device_uid =:deviceUid")
    List<DeviceEntity> getSynchronous(String deviceUid);

    @Insert
    void insert(DeviceEntity... actionEntities);

    @Update
    void update(DeviceEntity... actionEntities);

    @Delete()
    void delete(DeviceEntity... actionEntities);

}
