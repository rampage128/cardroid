package de.jlab.cardroid.car.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.jlab.cardroid.R;
import de.jlab.cardroid.car.CanInteractable;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.car.CanPacket;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceServiceConnection;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.utils.UsageStatistics;

/**
 * @deprecated This is obsolete. We can add a button to CarMonitorActivity to toggle sniffing in packet view
 */
@Deprecated
public final class CanSnifferActivity extends AppCompatActivity implements CanObservable.CanPacketListener {

    private TextView bandwidthStatText;
    private TextView packetStatText;
    private CanView canView;

    private Device.FeatureChangeObserver<CanInteractable> canInteractableFeatureObserver = this::interactableStateChange;
    private Device.FeatureChangeObserver<CanObservable> canObservableFeatureObserver = this::observableStateChange;

    private DeviceServiceConnection serviceConnection = new DeviceServiceConnection(this::serviceAction);

    private UsageStatistics.UsageStatisticsListener bandWidthStatListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            final int averageValue = Math.round(statistics.getAverage());
            final int averageReliability = Math.round(statistics.getAverageReliability() * 100f);
            final int currentUsage = count > 0 ? Math.round(100f / (115200 * 0.125f) * count) : 0;

            runOnUiThread(() -> bandwidthStatText.setText(getString(R.string.car_stats_bandwidth, count, averageValue, averageReliability, currentUsage)));
        }
    };
    private UsageStatistics.UsageStatisticsListener packetStatListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            final int averageValue = Math.round(statistics.getAverage());
            final int averageReliability = Math.round(statistics.getAverageReliability() * 100f);

            runOnUiThread(() -> packetStatText.setText(getString(R.string.car_stats_packets, count, averageValue, averageReliability)));
        }
    };

    private void serviceAction(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action) {
        if (action == DeviceServiceConnection.Action.BOUND) {
            deviceService.subscribeFeature(this.canInteractableFeatureObserver, CanInteractable.class);
            deviceService.subscribeFeature(this.canObservableFeatureObserver, CanObservable.class);
        } else {
            deviceService.unsubscribeFeature(this.canInteractableFeatureObserver, CanInteractable.class);
            deviceService.unsubscribeFeature(this.canObservableFeatureObserver, CanObservable.class);
        }
    }

    private void interactableStateChange(@NonNull CanInteractable interactable, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            interactable.startSniffer();
        } else {
            interactable.stopSniffer();
        }
    }

    private void observableStateChange(@NonNull CanObservable observable, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            observable.addListener(this);
        } else {
            observable.removeListener(this);
        }
    }

    @Override
    public void onReceive(@NonNull CanPacket packet) {
        this.runOnUiThread(() -> this.canView.updatePacket(packet));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can_sniffer);

        this.bandwidthStatText = findViewById(R.id.bandwidthText);
        this.packetStatText = findViewById(R.id.packetText);
        this.canView = findViewById(R.id.packet_list);

        this.bandwidthStatText.setText(R.string.pref_title_car_bandwidth);
        this.packetStatText.setText(R.string.pref_title_car_packets);

        this.canView.startLiveMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.canView.stopLiveMode();

        this.serviceConnection.unbind(this.getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.serviceConnection.bind(this.getApplicationContext());

        this.canView.startLiveMode();
    }
}
