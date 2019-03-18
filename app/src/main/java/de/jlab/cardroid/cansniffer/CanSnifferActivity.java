package de.jlab.cardroid.cansniffer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import androidx.appcompat.app.AppCompatActivity;
import de.jlab.cardroid.usb.carduino.CarduinoService;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.carduino.SerialCanPacket;
import de.jlab.cardroid.usb.carduino.SerialPacket;
import de.jlab.cardroid.usb.carduino.SerialReader;
import de.jlab.cardroid.usb.UsageStatistics;

public class CanSnifferActivity extends AppCompatActivity implements SerialReader.SerialPacketListener {

    private TextView bandwidthStatText;
    private TextView packetStatText;
    private CanView canView;

    private ConcurrentHashMap<Long, SerialCanPacket> packets = new ConcurrentHashMap<>();
    private Handler updateHandler;
    private Runnable updateRunner = new Runnable() {
        @Override
        public void run() {
            boolean needsRepaint = false;
            for (SerialCanPacket packet : CanSnifferActivity.this.packets.values()) {
                needsRepaint |= CanSnifferActivity.this.canView.updatePacket(packet);
            }
            CanSnifferActivity.this.packets.clear();
            needsRepaint |= CanSnifferActivity.this.canView.flushPackets();

            if (needsRepaint) {
                CanSnifferActivity.this.canView.invalidate();
            }
            CanSnifferActivity.this.updateHandler.postDelayed(this, 50);
        }
    };

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

    private static CarduinoService.MainServiceBinder mainService;

    private ServiceConnection mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mainService = (CarduinoService.MainServiceBinder)service;
            mainService.startCanSniffer();
            mainService.addBandwidthStatisticsListener(bandWidthStatListener);
            mainService.addPacketStatisticsListener(packetStatListener);
            mainService.addSerialPacketListener(CanSnifferActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            mainService = null;
        }
    };

    @Override
    public void onReceivePackets(final ArrayList<SerialPacket> packets) {
        for (SerialPacket packet : packets) {
            if (packet instanceof SerialCanPacket) {
                SerialCanPacket canPacket = (SerialCanPacket)packet;
                this.packets.put(canPacket.getCanId(), canPacket);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can_sniffer);

        this.updateHandler = new Handler();

        this.bandwidthStatText = findViewById(R.id.bandwidthText);
        this.packetStatText = findViewById(R.id.packetText);
        this.canView = findViewById(R.id.packet_list);

        this.bandwidthStatText.setText(R.string.pref_title_car_bandwidth);
        this.packetStatText.setText(R.string.pref_title_car_packets);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.updateHandler.removeCallbacks(this.updateRunner);
        if (mainService != null) {
            mainService.stopCanSniffer();
            mainService.removeBandwidthStatisticsListener(this.bandWidthStatListener);
            mainService.removePacketStatisticsListener(this.packetStatListener);
            mainService.removeSerialPacketListener(CanSnifferActivity.this);
            unbindService(this.mainServiceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, CarduinoService.class), this.mainServiceConnection, Context.BIND_AUTO_CREATE);

        this.updateHandler.postDelayed(this.updateRunner, 50);
    }
}
