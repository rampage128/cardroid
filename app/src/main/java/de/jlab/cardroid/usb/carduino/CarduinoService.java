package de.jlab.cardroid.usb.carduino;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.Car;
import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.car.CarSystemEvent;
import de.jlab.cardroid.car.CarSystemFactory;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.rules.RuleHandler;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.usb.SerialConnection;
import de.jlab.cardroid.usb.UsageStatistics;
import de.jlab.cardroid.usb.UsbService;
import de.jlab.cardroid.usb.carduino.serial.ErrorPacketHandler;
import de.jlab.cardroid.usb.carduino.serial.MetaEvent;
import de.jlab.cardroid.usb.carduino.serial.MetaSerialPacket;
import de.jlab.cardroid.usb.carduino.serial.SerialPacket;
import de.jlab.cardroid.usb.carduino.serial.SerialPacketFactory;
import de.jlab.cardroid.usb.carduino.serial.SerialReader;
import de.jlab.cardroid.usb.carduino.ui.ErrorNotifier;

public class CarduinoService extends UsbService implements SerialReader.SerialPacketListener {
    private static final String LOG_TAG = "CarduinoService";

    private OverlayWindow overlayWindow;

    private SerialConnection connectionManager;
    private SerialReader serialReader;
    private Car car;

    private RuleHandler ruleHandler;

    private ArrayList<PacketHandler> packetHandlers = new ArrayList<>();

    private static final byte PROTOCOL_MAJOR = 0x01;

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

    public void requestCarduinoBaudRate(int baudRate) {
        Log.d(LOG_TAG, "Requesting baudRate " + baudRate);
        byte[] payload = ByteBuffer.allocate(4).putInt(baudRate).array();
        this.connectionManager.send(SerialPacketFactory.serialize(MetaEvent.serialize(MetaEvent.CHANGE_BAUD_RATE, payload)));
    }

    public void requestCarduinoConnection() {
        this.connectionManager.send(SerialPacketFactory.serialize(MetaEvent.serialize(MetaEvent.REQUEST_CONNECTION, null)));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();


        // initialize serial connection
        this.serialReader = new SerialReader();
        this.serialReader.addListener(this);

        this.connectionManager = new SerialConnection(this);
        this.connectionManager.addConnectionListener(this.serialReader);


        // intitalize packet handlers
        this.packetHandlers.clear();

        this.car = new Car();
        this.packetHandlers.add(this.car);

        ErrorNotifier errorNotifier = new ErrorNotifier(this);
        ErrorPacketHandler errorHandler = new ErrorPacketHandler();
        errorHandler.addListener(errorNotifier);
        this.packetHandlers.add(errorHandler);

        MetaPacketHandler metaPacketHandler = new MetaPacketHandler(this);
        this.packetHandlers.add(metaPacketHandler);

        try {
            this.ruleHandler = new getRulesTask(getApplication()).execute().get();
            this.packetHandlers.add(this.ruleHandler);
        } catch (ExecutionException e) {} catch (InterruptedException e) {}


        // intitialize overlay window
        this.overlayWindow = new OverlayWindow(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.packetHandlers.clear();
        this.serialReader.removeListener(this);
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

    @Override
    protected boolean isConnected(UsbDevice device) {
        return this.connectionManager != null && this.connectionManager.isConnected(device);
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
    public void onReceivePackets(ArrayList<SerialPacket> packets) {
        for (final SerialPacket packet : packets) {
            for (PacketHandler handler : packetHandlers) {
                if (handler.shouldHandlePacket(packet)) {
                    handler.handleSerialPacket(packet);
                }
            }
        }
    }

    public CarSystem getCarSystem(CarSystemFactory carSystemType) {
        return this.car.getCarSystem(carSystemType);
    }

    public void sendCarduinoEvent(CarSystemEvent event, byte[] payload) {
        SerialPacket packet = CarSystemEvent.serialize(event, payload);
        this.connectionManager.send(SerialPacketFactory.serialize(packet));
    }

    private static class MetaPacketHandler implements PacketHandler<MetaSerialPacket> {

        private CarduinoService service;

        public MetaPacketHandler(CarduinoService service) {
            this.service = service;
        }

        @Override
        public void handleSerialPacket(@NonNull MetaSerialPacket packet) {
            if (packet.getId() == 0x00) {
                int major = packet.readByte(0);
                //int minor = packet.readByte(1);
                //int revision = packet.readByte(2);

                if (major == CarduinoService.PROTOCOL_MAJOR) {
                    service.requestCarduinoConnection();
                }
            }
            else if (packet.getId() == 0x01) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.service);
                int baudRate = Integer.valueOf(prefs.getString("car_baud_rate", "115200"));
                service.requestCarduinoBaudRate(baudRate);
            }
            else if (packet.getId() == 0x02) {
                int baudRate = (int)packet.readDWord(0);
                Log.d(LOG_TAG, "Adapting baudRate to " + baudRate);
                service.connectionManager.setBaudRate(baudRate);
            }
            else if (packet.getId() == 0x03) {
                service.disconnectDevice();
                service.stopSelf();
            }
        }

        @Override
        public boolean shouldHandlePacket(@NonNull SerialPacket packet) {
            return packet instanceof MetaSerialPacket;
        }
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
