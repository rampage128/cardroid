package de.jlab.cardroid.gps.ui;

import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceServiceConnection;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.gps.GpsObservable;
import de.jlab.cardroid.utils.ui.FeatureActivity.FeatureActivity;

public final class GpsMonitorActivity extends FeatureActivity {

    @Nullable
    @Override
    protected DeviceUid getDefaultDeviceUid() {
        String deviceUid = PreferenceManager.getDefaultSharedPreferences(this).getString("gps_device_uid", null);
        return deviceUid != null ? new DeviceUid(deviceUid) : null;
    }

    @NonNull
    @Override
    protected Class<? extends Feature> getFeatureType() {
        return GpsObservable.class;
    }

    @Override
    protected void onDeviceServiceStateChange(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action) {

    }

    @Override
    protected void onDataItemClicked(int id, @Nullable String value) {

    }

    @Override
    protected void initContentViews(@NonNull ContentViewConsumer consumer) {
        consumer.addContentView(R.id.action_status, new SatelliteFragment());
        consumer.addContentView(R.id.action_raw_data, new SentenceFragment());
    }

    @Override
    protected int getMenuId() {
        return R.menu.bottom_navigation_gps_monitor;
    }

}
