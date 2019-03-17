package de.jlab.cardroid.usb.carduino;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import de.jlab.cardroid.car.Car;
import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.car.CarSystemFactory;
import de.jlab.cardroid.car.ClimateControl;
import de.jlab.cardroid.car.ManageableCarSystem;
import de.jlab.cardroid.car.RemoteControl;
import de.jlab.cardroid.car.UnknownCarSystemException;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.rules.Event;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.usb.SerialConnectionManager;
import de.jlab.cardroid.usb.UsageStatistics;
import de.jlab.cardroid.usb.UsbService;
import de.jlab.cardroid.rules.RuleHandler;

public class CarduinoService extends UsbService implements ManageableCarSystem.CarSystemEventListener {
    private static final String LOG_TAG = "CarduinoService";

    private OverlayWindow overlayWindow;

    private SerialConnectionManager connectionManager;
    private SerialReader serialReader;
    private Car car;

    private RuleHandler ruleHandler;

    private ArrayList<PacketHandler> packetHandlers = new ArrayList<>();

    public class MainServiceBinder extends UsbServiceBinder {
        public void addBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            CarduinoService.this.connectionManager.addBandwidthStatisticsListener(listener);
        }

        public void removeBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            CarduinoService.this.connectionManager.removeBandwidthStatisticsListener(listener);
        }

        public void addPacketStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            CarduinoService.this.serialReader.addPacketStatisticListener(listener);
        }

        public void removePacketStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            CarduinoService.this.serialReader.removePacketStatisticListener(listener);
        }

        public void addSerialPacketListener(SerialReader.SerialPacketListener listener) {
            CarduinoService.this.serialReader.addListener(listener);
        }

        public void removeSerialPacketListener(SerialReader.SerialPacketListener listener) {
            CarduinoService.this.serialReader.removeListener(listener);
        }

        public void startCanSniffer() {
            CarduinoService.this.connectionManager.send(SerialPacketFactory.serialize(MetaEvent.serialize(MetaEvent.START_SNIFFING, null)));
        }

        public void stopCanSniffer() {
            CarduinoService.this.connectionManager.send(SerialPacketFactory.serialize(MetaEvent.serialize(MetaEvent.STOP_SNIFFING, null)));
        }

        public void updateRule(RuleDefinition ruleDefinition) {
            CarduinoService.this.ruleHandler.updateRuleDefinition(ruleDefinition);
        }

        public int getBaudRate() {
            return CarduinoService.this.connectionManager.getBaudRate();
        }

        public void requestBaudRate(int baudRate) {
            CarduinoService.this.requestCarduinoBaudRate(baudRate);
        }

        public Car getCar() {
            return CarduinoService.this.car;
        }
    }

    private final IBinder binder = new MainServiceBinder();

    private SerialReader.SerialPacketListener listener = new SerialReader.SerialPacketListener() {
        @Override
        public void onReceivePackets(ArrayList<SerialPacket> packets) {
            for (final SerialPacket packet : packets) {
                if (packet instanceof MetaSerialPacket) {
                    MetaSerialPacket metaPacket = (MetaSerialPacket)packet;
                    if (packet.getId() == 0x01) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CarduinoService.this);
                        int baudRate = Integer.valueOf(prefs.getString("car_baud_rate", "115200"));
                        CarduinoService.this.requestCarduinoBaudRate(baudRate);
                    }
                    else if (packet.getId() == 0x02) {
                        int baudRate = (int)metaPacket.readDWord(0);
                        Log.d(LOG_TAG, "Adapting baudRate to " + baudRate);
                        CarduinoService.this.connectionManager.setBaudRate(baudRate);
                    }
                }
                try {
                    car.updateFromSerialPacket(packet);
                } catch (UnknownCarSystemException e) {
                    Log.e(LOG_TAG, "Cannot update car system", e);
                }

                for (PacketHandler handler : packetHandlers) {
                    if (handler.shouldHandlePacket(packet)) {
                        handler.handleSerialPacket(packet);
                    }
                }
            }
        }
    };

    public void requestCarduinoBaudRate(int baudRate) {
        Log.d(LOG_TAG, "Requesting baudRate " + baudRate);
        byte[] payload = ByteBuffer.allocate(4).putInt(baudRate).array();
        this.connectionManager.send(SerialPacketFactory.serialize(MetaEvent.serialize(MetaEvent.CHANGE_BAUD_RATE, payload)));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.car = new Car();

        ClimateControl climateControl = (ClimateControl)this.car.getCarSystem(CarSystemFactory.CLIMATE_CONTROL);
        climateControl.addEventListener(this);

        this.overlayWindow = new OverlayWindow(this, climateControl);

        this.serialReader = new SerialReader();
        this.serialReader.addListener(this.listener);

        this.connectionManager = new SerialConnectionManager(this);
        this.connectionManager.addConnectionListener(this.serialReader);

        this.packetHandlers.clear();

        try {
            this.ruleHandler = new getRulesTask(getApplication()).execute().get();
            this.packetHandlers.add(this.ruleHandler);
        } catch (ExecutionException e) {} catch (InterruptedException e) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.packetHandlers.clear();
        this.serialReader.removeListener(this.listener);
        this.connectionManager.removeConnectionListener(this.serialReader);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);

        if (result == START_STICKY) {
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
        }

        return result;
    }

    @Override
    protected boolean isConnected() {
        return this.connectionManager != null && this.connectionManager.isConnected();
    }

    protected boolean connectDevice(UsbDevice device) {
        this.connectionManager.connect(device, 115200);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CarduinoService.this);
        if (prefs.getBoolean("overlay_active", false)) {
            this.overlayWindow.create();
        }

        this.ruleHandler.triggerRule(4);

        return true;
    }

    protected void disconnectDevice() {
        this.ruleHandler.triggerRule(5);

        this.connectionManager.disconnect();
        this.overlayWindow.destroy();
    }

    @Override
    public void onTrigger(SerialCarButtonEventPacket packet) {
        this.connectionManager.send(SerialPacketFactory.serialize(packet));
    }

    public interface PacketHandler<T extends SerialPacket> {

        void handleSerialPacket(@NonNull T packet);

        boolean shouldHandlePacket(@NonNull SerialPacket packet);

    }

    private static class getRulesTask extends AsyncTask<Void, Void, RuleHandler> {

        private Application application;

        public getRulesTask(Application application) {
            this.application = application;
        }

        @Override
        protected RuleHandler doInBackground(Void... voids) {
            EventRepository eventRepo = new EventRepository(this.application);
            List<RuleDefinition> rules = eventRepo.getAllRules();

            RuleHandler ruleHandler = new RuleHandler(this.application);
            ruleHandler.updateRuleDefinitions(rules);
            return ruleHandler;
        }
    }
}
