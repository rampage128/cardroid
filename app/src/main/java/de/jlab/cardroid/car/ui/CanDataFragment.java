package de.jlab.cardroid.car.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import de.jlab.cardroid.R;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.car.CanPacket;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.utils.UsageStatistics;
import de.jlab.cardroid.utils.ui.FeatureActivity.FeatureActivity;
import de.jlab.cardroid.utils.ui.UnitValues;

public final class CanDataFragment extends FeatureActivity.FeatureFragment {

    private CanView packetListView;
    private ScrollView packetListViewContainer;

    private Device.FeatureChangeObserver<CanObservable> onCanStateChange = this::onCanStateChange;
    private CanObservable.CanPacketListener onReceiveCanPacket = this::onReceiveCanPacket;
    private UsageStatistics.UsageStatisticsListener onPacketStatisticUpdate = this::onPacketStatisticUpdate;

    private UsageStatistics packetStatistic = new UsageStatistics(1000, 60);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_can_data, container, false);

        this.packetListViewContainer = rootView.findViewById(R.id.packetListViewContainer);
        this.packetListView = rootView.findViewById(R.id.packetListView);

        return rootView;
    }

    @Override
    protected void initDataItems(@NonNull DataItemConsumer consumer, @NonNull Context context) {
        consumer.put(R.string.car_status_packets, context.getResources().getString(R.string.status_unavailable));
    }

    @Override
    public void onStart(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureSubscriptionConsumer consumer) {
        consumer.subscribeFeature(this.onCanStateChange, CanObservable.class);
        this.packetStatistic.addListener(this.onPacketStatisticUpdate);
    }

    @Override
    public void onStop(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureUnsubscriptionConsumer consumer) {
        consumer.unsubscribeFeature(this.onCanStateChange, CanObservable.class);
        this.packetStatistic.removeListener(this.onPacketStatisticUpdate);
    }

    @Override
    public void onResume() {
        super.onResume();

        this.packetListView.startLiveMode();
    }

    @Override
    public void onPause() {
        super.onPause();

        this.packetListView.stopLiveMode();
    }

    private void onPacketStatisticUpdate(int count, UsageStatistics statistics) {
        String value = UnitValues.getStatisticString(count, Math.round(statistics.getAverage()), R.string.unit_packets_per_second, getContext());
        this.updateDataItem(R.string.car_status_packets, value);
    }

    private void onReceiveCanPacket(@NonNull CanPacket packet) {
        this.packetStatistic.count();
        if (this.getActivity() != null) {
            this.getActivity().runOnUiThread(() -> {
                if (this.packetListView.updatePacket(packet)) {
                    this.packetListView.invalidate();
                }
            });
        }
    }

    private void onCanStateChange(@NonNull CanObservable feature, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            feature.addListener(this.onReceiveCanPacket);
        } else {
            feature.removeListener(this.onReceiveCanPacket);
            if (getContext() != null) {
                this.updateDataItem(R.string.car_status_packets, getContext().getResources().getString(R.string.status_unavailable));
            }
        }
    }

}
