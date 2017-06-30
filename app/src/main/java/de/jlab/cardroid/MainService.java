package de.jlab.cardroid;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;

import de.jlab.cardroid.car.Car;
import de.jlab.cardroid.car.CarSystemFactory;
import de.jlab.cardroid.car.ClimateControl;
import de.jlab.cardroid.car.ManageableCarSystem;
import de.jlab.cardroid.car.UnknownCarSystemException;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.usb.MetaEvent;
import de.jlab.cardroid.usb.MetaSerialPacket;
import de.jlab.cardroid.usb.SerialCarButtonEventPacket;
import de.jlab.cardroid.usb.SerialConnectionManager;
import de.jlab.cardroid.usb.SerialPacket;
import de.jlab.cardroid.usb.SerialReader;
import de.jlab.cardroid.usb.UsageStatistics;

public class MainService extends Service implements ManageableCarSystem.CarSystemEventListener {
    private static final String LOG_TAG = "MainService";

    private OverlayWindow overlayWindow;

    private SerialConnectionManager connectionManager;
    private SerialReader serialReader;
    private Car car;

    public class MainServiceBinder extends Binder {
        public void addBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            MainService.this.connectionManager.addBandwidthStatisticsListener(listener);
        }

        public void removeBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            MainService.this.connectionManager.removeBandwidthStatisticsListener(listener);
        }

        public void addPacketStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            MainService.this.serialReader.addPacketStatisticListener(listener);
        }

        public void removePacketStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            MainService.this.serialReader.removePacketStatisticListener(listener);
        }

        public void addSerialPacketListener(SerialReader.SerialPacketListener listener) {
            MainService.this.serialReader.addListener(listener);
        }

        public void removeSerialPacketListener(SerialReader.SerialPacketListener listener) {
            MainService.this.serialReader.removeListener(listener);
        }

        public void startCanSniffer() {
            MainService.this.connectionManager.sendPacket(MetaEvent.serialize(MetaEvent.START_SNIFFING, null));
        }

        public void stopCanSniffer() {
            MainService.this.connectionManager.sendPacket(MetaEvent.serialize(MetaEvent.STOP_SNIFFING, null));
        }

        public void requestBaudRate(int baudRate) {
            MainService.this.connectionManager.requestBaudRate(baudRate);
        }
    };

    private final IBinder binder = new MainServiceBinder();

    private SerialReader.SerialPacketListener listener = new SerialReader.SerialPacketListener() {
        @Override
        public void onReceivePackets(ArrayList<SerialPacket> packets) {
            for (final SerialPacket packet : packets) {
                if (packet instanceof MetaSerialPacket) {
                    MetaSerialPacket metaPacket = (MetaSerialPacket)packet;
                    if (packet.getId() == 0x01) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainService.this);
                        int baudRate = Integer.valueOf(prefs.getString("usb_baud_rate", "115200"));
                        MainService.this.connectionManager.requestBaudRate(baudRate);
                    }
                    else if (packet.getId() == 0x02) {
                        int baudRate = (int)metaPacket.readDWord(0);
                        Log.d(LOG_TAG, "Adapting baudRate to " + baudRate);
                        MainService.this.connectionManager.setBaudRate(baudRate);
                    }
                }
                try {
                    car.updateFromSerialPacket(packet);
                } catch (UnknownCarSystemException e) {
                    Log.e(LOG_TAG, "Cannot update car system", e);
                }
            }
        }

        private final char[] hexArray = "0123456789ABCDEF".toCharArray();
        private String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 3];
            for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 3] = hexArray[v >>> 4];
                hexChars[j * 3 + 1] = hexArray[v & 0x0F];
                hexChars[j * 3 + 2] = ' ';
            }
            return new String(hexChars);
        }
    };

    public MainService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "Creating main service.");

        this.car = new Car();

        this.connectionManager = new SerialConnectionManager(this);
        this.serialReader = new SerialReader();
        this.serialReader.addListener(this.listener);
        this.connectionManager.addConnectionListener(this.serialReader);

        ClimateControl climateControl = (ClimateControl)this.car.getCarSystem(CarSystemFactory.CLIMATE_CONTROL);
        this.overlayWindow = new OverlayWindow(this, climateControl);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("overlay_active", false)) {
            this.overlayWindow.create();
        }

        climateControl.addEventListener(this);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String command = intent.getStringExtra("command");
            if (command != null) {
                if (command.equals("show_overlay")) {
                    Log.d(LOG_TAG, "Showing overlay.");
                    this.overlayWindow.create();
                } else if (command.equals("hide_overlay")) {
                    Log.d(LOG_TAG, "Hiding overlay.");
                    this.overlayWindow.destroy();
                }
            }
        }

        this.connectionManager.connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.overlayWindow.destroy();
        this.connectionManager.disconnect();
    }

    @Override
    public void onTrigger(SerialCarButtonEventPacket packet) {
        this.connectionManager.sendPacket(packet);
    }
}
