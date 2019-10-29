package de.jlab.cardroid.car.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import de.jlab.cardroid.R;
import de.jlab.cardroid.car.CanInteractable;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.car.CanPacket;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.utils.UsageStatistics;

/**
 * @deprecated This is obsolete. We can add a button to CarMonitorActivity to toggle sniffing in packet view
 */
@Deprecated
public final class CanSnifferActivity extends AppCompatActivity implements CanObservable.CanPacketListener {

    private TextView bandwidthStatText;
    private TextView packetStatText;
    private CanView canView;

    private ArrayList<CanInteractable> interactables = new ArrayList<>();
    private ArrayList<CanObservable> observables = new ArrayList<>();
    private boolean active = false;
    private FeatureObserver<CanInteractable> canInteractableFeatureObserver = new FeatureObserver<CanInteractable>() {
        @Override
        public void onFeatureAvailable(@NonNull CanInteractable feature) {
            CanSnifferActivity.this.interactables.add(feature);
            CanSnifferActivity.this.checkActiveStatus();
        }

        @Override
        public void onFeatureUnavailable(@NonNull CanInteractable feature) {
            CanSnifferActivity.this.interactables.remove(feature);
            CanSnifferActivity.this.checkActiveStatus();
        }
    };

    private FeatureObserver<CanObservable> canObservableFeatureObserver = new FeatureObserver<CanObservable>() {
        @Override
        public void onFeatureAvailable(@NonNull CanObservable feature) {
            CanSnifferActivity.this.observables.add(feature);
            feature.addListener(CanSnifferActivity.this);
            CanSnifferActivity.this.checkActiveStatus();
        }

        @Override
        public void onFeatureUnavailable(@NonNull CanObservable feature) {
            CanSnifferActivity.this.observables.remove(feature);
            CanSnifferActivity.this.checkActiveStatus();
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DeviceService.DeviceServiceBinder binder = (DeviceService.DeviceServiceBinder)service;
            binder.subscribe(canInteractableFeatureObserver, CanInteractable.class);
            binder.subscribe(canObservableFeatureObserver, CanObservable.class);
        }

        public void onServiceDisconnected(ComponentName className) {
            CanSnifferActivity.this.interactables = new ArrayList<>();
            CanSnifferActivity.this.observables   = new ArrayList<>();
        }
    };

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

    private void checkActiveStatus() {
        if (this.active) {
            for (CanInteractable i: this.interactables) {
                i.startSniffer();
            }
        } else {
            for (CanInteractable i: this.interactables) {
                i.stopSniffer();
            }
        }
    }

    @Override
    public void onReceive(CanPacket packet) {
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
        this.active = false;
        this.checkActiveStatus();
        this.canView.stopLiveMode();

        for (CanObservable o: this.observables) {
            o.removeListener(this);
        }
        this.getApplicationContext().unbindService(this.serviceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.active = true;
        this.getApplicationContext().bindService(new Intent(this.getApplicationContext(), DeviceService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);

        this.canView.startLiveMode();
    }
}
