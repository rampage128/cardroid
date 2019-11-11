package de.jlab.cardroid.gps.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;
import de.jlab.cardroid.devices.serial.gps.GpsSatellite;
import de.jlab.cardroid.gps.GpsObservable;
import de.jlab.cardroid.utils.ui.FeatureActivity.FeatureActivity;

public final class SatelliteFragment extends FeatureActivity.FeatureFragment {

    private GpsSatView satView;
    private GpsSatellite[] satellites = new GpsSatellite[0];
    private int fixedSatellites = 0;

    private Device.FeatureChangeObserver<GpsObservable> onGpsStateChange = this::onGpsStateChange;
    private GpsObservable.PositionListener onPositionChange = this::onPositionChange;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gps_satellites, container, false);

        this.satView = rootView.findViewById(R.id.gpsSatView);

        return rootView;
    }

    @Override
    protected void initDataItems(@NonNull DataItemConsumer consumer, @NonNull Context context) {
        String na = context.getResources().getString(R.string.status_unavailable);
        consumer.put(R.string.gps_status_fix, na);
        consumer.put(R.string.gps_status_accuracy, na);
        consumer.put(R.string.gps_status_satellites_visible, na);
        consumer.put(R.string.gps_status_satellites_used, na);
        /*
            this.statusGridAdapter.update(R.string.gps_status_fix, fixId > 0 ? this.getString(fixId) : translationString);
            this.statusGridAdapter.update(R.string.gps_status_latitude, Double.toString(position.getLocation().getLatitude()));
            this.statusGridAdapter.update(R.string.gps_status_longitude, Double.toString(position.getLocation().getLongitude()));
            this.statusGridAdapter.update(R.string.gps_status_altitude, Double.toString(position.getLocation().getAltitude()));
            this.statusGridAdapter.update(R.string.gps_status_speed, Float.toString(position.getLocation().getSpeed()));
            this.statusGridAdapter.update(R.string.gps_status_bearing, Float.toString(position.getLocation().getBearing()));
            this.statusGridAdapter.update(R.string.gps_status_time, DateUtils.formatDateTime(this, position.getLocation().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL));
            this.statusGridAdapter.update(R.string.gps_status_accuracy, Float.toString(position.getLocation().getAccuracy()));
            this.statusGridAdapter.updateStatistics(
                    R.string.gps_status_frequency,
                    count,
                    Math.round(statistics.getAverage()),
                    R.string.unit_hertz
            );
         */
    }

    @Override
    protected void onStart(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureSubscriptionConsumer consumer) {
        consumer.subscribeFeature(this.onGpsStateChange, GpsObservable.class);
    }

    @Override
    protected void onStop(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureUnsubscriptionConsumer consumer) {
        consumer.unsubscribeFeature(this.onGpsStateChange, GpsObservable.class);
    }

    private int countSatellitesWithFix() {
        int fixedSatellites = 0;
        for (GpsSatellite sat : this.satellites) {
            if (sat.getSnr() > -1) {
                fixedSatellites++;
            }
        }
        return fixedSatellites;
    }

    private void onPositionChange(GpsPosition position, String sentence) {
        GpsSatellite[] satellites = position.getGpsSatellites(this.satellites);
        if (this.satellites.length != satellites.length) {
            this.satellites = satellites;
            this.satView.setSatellites(this.satellites);
            this.updateDataItem(R.string.gps_status_satellites_visible, Integer.toString(this.satellites.length));
        }
        this.satView.invalidate();

        int fixedSatellites = this.countSatellitesWithFix();
        if (fixedSatellites != this.fixedSatellites) {
            this.fixedSatellites = fixedSatellites;
            this.updateDataItem(R.string.gps_status_satellites_used, Integer.toString(this.fixedSatellites));
        }

        String translationString = "gps_status_fix_" + position.getFix();
        int fixId = getResources().getIdentifier(translationString, "string", getContext().getPackageName());
        this.updateDataItem(R.string.gps_status_fix, fixId > 0 ? this.getString(fixId) : translationString);
        this.updateDataItem(R.string.gps_status_accuracy, Float.toString(position.getLocation().getAccuracy()));
    }

    private void onGpsStateChange(@NonNull GpsObservable feature, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            feature.addListener(this.onPositionChange);
        } else {
            feature.removeListener(this.onPositionChange);
            this.satView.setSatellites(new GpsSatellite[0]);
        }
    }
}
