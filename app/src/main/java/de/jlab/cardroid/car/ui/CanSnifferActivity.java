package de.jlab.cardroid.car.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import de.jlab.cardroid.devices.serial.can.CanDeviceHandler;
import de.jlab.cardroid.devices.serial.can.CanPacket;
import de.jlab.cardroid.usb.carduino.CarduinoService;
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

    private UsageStatistics.UsageStatisticsListener bandWidthStatListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            final int averageValue = Math.round(statistics.getAverage());
            final int averageReliability = Math.round(statistics.getAverageReliability() * 100f);
            final int currentUsage = count > 0 ? Math.round(100f / (115200 * 0.125f) * count) : 0;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bandwidthStatText.setText(getString(R.string.car_stats_bandwidth, count, averageValue, averageReliability, currentUsage));
                }
            });
        }
    };
    private UsageStatistics.UsageStatisticsListener packetStatListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            final int averageValue = Math.round(statistics.getAverage());
            final int averageReliability = Math.round(statistics.getAverageReliability() * 100f);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    packetStatText.setText(getString(R.string.car_stats_packets, count, averageValue, averageReliability));
                }
            });
        }
    };


    //private static CarduinoService.MainServiceBinder mainService;

    private ServiceConnection mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            /*
            mainService = (CarduinoService.MainServiceBinder)service;
            mainService.startCanSniffer();
            mainService.addBandwidthStatisticsListener(bandWidthStatListener);
            mainService.addPacketStatisticsListener(packetStatListener);
            mainService.addSerialPacketListener(CanSnifferActivity.this);

             */
        }

        public void onServiceDisconnected(ComponentName className) {
            //mainService = null;
        }
    };

    @Override
    public void onReceive(CanPacket packet) {
        this.runOnUiThread(() -> this.canView.updatePacket((CanPacket)packet));
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
        /*
        if (mainService != null) {
            mainService.stopCanSniffer();
            mainService.removeBandwidthStatisticsListener(this.bandWidthStatListener);
            mainService.removePacketStatisticsListener(this.packetStatListener);
            mainService.removeSerialPacketListener(CanSnifferActivity.this);
            unbindService(this.mainServiceConnection);
        }
         */
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, CarduinoService.class), this.mainServiceConnection, Context.BIND_AUTO_CREATE);

        this.canView.startLiveMode();
    }
}
