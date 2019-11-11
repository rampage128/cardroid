package de.jlab.cardroid.car.ui;

import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceServiceConnection;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.utils.ui.FeatureActivity.FeatureActivity;

public class CarMonitorActivity extends FeatureActivity {

    @Nullable
    @Override
    protected DeviceUid getDefaultDeviceUid() {
        String deviceUid = PreferenceManager.getDefaultSharedPreferences(this).getString("car_device_uid", null);
        return deviceUid != null ? new DeviceUid(deviceUid) : null;
    }

    @NonNull
    @Override
    protected Class<? extends Feature> getFeatureType() {
        return CanObservable.class;
    }

    @Override
    protected void onDeviceServiceStateChange(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action) {

    }

    @Override
    protected void onDataItemClicked(int id, @Nullable String value) {

    }

    @Override
    protected void initContentViews(@NonNull ContentViewConsumer consumer) {
        consumer.addContentView(R.id.action_status, new CanVariableFragment());
        consumer.addContentView(R.id.action_connection, new CanDataFragment());
    }

    @Override
    protected int getMenuId() {
        return R.menu.bottom_navigation_carduino_monitor;
    }
}
