package de.jlab.cardroid.car.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.can.CanDeviceHandler;
import de.jlab.cardroid.devices.serial.can.CanPacket;
import de.jlab.cardroid.R;
import de.jlab.cardroid.utils.UsageStatistics;

/**
 * @deprecated This is obsolete. We can add a button to CarMonitorActivity to toggle sniffing in packet view
 */
@Deprecated
public final class CanSnifferActivity extends AppCompatActivity implements CanDeviceHandler.CanPacketListener {

    private TextView bandwidthStatText;
    private TextView packetStatText;
    private CanView canView;

    private CanDataProvider can = null;

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

    private ServiceConnection deviceServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DeviceService.DeviceServiceBinder binder = (DeviceService.DeviceServiceBinder)service;
            CanSnifferActivity.this.can = binder.getDeviceProvider(CanDataProvider.class);
            if (CanSnifferActivity.this.can != null) {
                CanSnifferActivity.this.can.startCanSniffer();
                CanSnifferActivity.this.can.addExternalListener(CanSnifferActivity.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            CanSnifferActivity.this.can = null;
        }
    };

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
        this.canView.stopLiveMode();

        if (this.can != null) {
            this.can.stopCanSniffer();
            this.can.removeExternalListener(this);
        }
        this.getApplicationContext().unbindService(this.deviceServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.getApplicationContext().bindService(new Intent(this.getApplicationContext(), DeviceService.class), this.deviceServiceConnection, Context.BIND_AUTO_CREATE);

        this.canView.startLiveMode();
    }
}
