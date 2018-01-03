package de.jlab.cardroid.cansniffer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import de.jlab.cardroid.usb.carduino.CarduinoService;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.carduino.SerialCanPacket;
import de.jlab.cardroid.usb.carduino.SerialPacket;
import de.jlab.cardroid.usb.carduino.SerialReader;
import de.jlab.cardroid.usb.UsageStatistics;

public class CanSnifferActivity extends AppCompatActivity implements SerialReader.SerialPacketListener {

    private TextView bandwidthStatText;
    private TextView packetStatText;
    private TextView packetListView;

    private TreeMap<Long, SniffedCanPacket> changedPacketList = new TreeMap<>();
    private TreeMap<Long, SniffedCanPacket> packetList = new TreeMap<>();

    private UsageStatistics.UsageStatisticsListener bandWidthStatListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int averageValue = Math.round(statistics.getAverage());
                    int averageReliability = Math.round(statistics.getAverageReliability() * 100f);
                    int currentUsage = count > 0 ? Math.round(100f / (115200 * 0.125f) * count) : 0;

                    bandwidthStatText.setText(getString(R.string.car_stats_bandwidth, count, averageValue, averageReliability, currentUsage));
                }
            });
        }
    };
    private UsageStatistics.UsageStatisticsListener packetStatListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int averageValue = Math.round(statistics.getAverage());
                    int averageReliability = Math.round(statistics.getAverageReliability() * 100f);

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
    public void onReceivePackets(ArrayList<SerialPacket> packets) {
        for (SerialPacket packet : packets) {
            if (packet instanceof SerialCanPacket) {
                SniffedCanPacket sniffedPacket = new SniffedCanPacket((SerialCanPacket)packet);
                long canId = sniffedPacket.getCanId();
                if (!this.packetList.containsKey(canId) || !this.packetList.get(canId).equals(sniffedPacket)) {
                    this.changedPacketList.put(canId, sniffedPacket);
                }
                this.packetList.put(canId, sniffedPacket);
            }
        }

        final StringBuilder packetTextBuilder = new StringBuilder();
        for(Iterator<Map.Entry<Long, SniffedCanPacket>> it = this.changedPacketList.entrySet().iterator(); it.hasNext(); ) {
            SniffedCanPacket canPacket = it.next().getValue();
            if(canPacket.isExpired(3000)) {
                it.remove();
            }
            else {
                packetTextBuilder.append(getString(R.string.cansniffer_line, Long.toHexString(canPacket.getCanId()), canPacket.getDataHex())).append("\n");
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                packetListView.setText(packetTextBuilder.toString());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can_sniffer);

        this.bandwidthStatText = (TextView)findViewById(R.id.bandwidthText);
        this.packetStatText = (TextView)findViewById(R.id.packetText);
        this.packetListView = (TextView)findViewById(R.id.packetList);

        this.packetListView.setText("");
        this.bandwidthStatText.setText(R.string.pref_title_car_bandwidth);
        this.packetStatText.setText(R.string.pref_title_car_packets);
    }

    @Override
    protected void onPause() {
        super.onPause();
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
    }
}
