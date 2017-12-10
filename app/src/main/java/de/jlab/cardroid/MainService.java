package de.jlab.cardroid;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.jlab.cardroid.car.Car;
import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.car.CarSystemFactory;
import de.jlab.cardroid.car.ClimateControl;
import de.jlab.cardroid.car.ManageableCarSystem;
import de.jlab.cardroid.car.RemoteControl;
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
    private static final String ACTION_USB_PERMISSION  = "de.jlab.cardroid.USB_PERMISSION";
    private static final String LOG_TAG = "MainService";

    private OverlayWindow overlayWindow;

    private SerialConnectionManager connectionManager;
    private SerialReader serialReader;
    private Car car;

    private class RemoteControlChangeListener implements CarSystem.ChangeListener<RemoteControl> {
        @Override
        public void onChange(RemoteControl system) {
            Log.d(LOG_TAG, "Button pressed " + system.getButtonId());
            switch (system.getButtonId()) {
                case 1: // SOURCE
                    // TODO switch task
                    break;
                case 2: // SOURCE LONG
                    // TODO open task switcher
                    break;
                case 10: // MENU UP
                    sendMediaEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    break;
                case 11: // MENU DOWN
                    sendMediaEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
                    break;
                case 12: // MENU ENTER
                    sendMediaEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    break;
                case 13: // BACK
                    // TODO send back button event to system
                    break;
                case 20: // VOL DOWN
                    adjustVolume(AudioManager.ADJUST_LOWER);
                    break;
                case 21: // VOL UP
                    adjustVolume(AudioManager.ADJUST_RAISE);
                    break;
                case 30: // PHONE
                    // TODO find some action for phone key
                    break;
                case 42: // VOICE
                    Intent voiceIntent =
                            new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
                    voiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    MainService.this.startActivity(voiceIntent);
                    break;
            }
        }

        private void adjustVolume(int direction) {
            AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
            audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
        }

        private void sendMediaEvent(int keyCode) {
            AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

            KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            audioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
            audioManager.dispatchMediaKeyEvent(upEvent);
        }
    }

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

        climateControl.addEventListener(this);

        RemoteControl remoteControl = (RemoteControl)this.car.getCarSystem(CarSystemFactory.REMOTE_CONTROL);
        remoteControl.addChangeListener(new RemoteControlChangeListener());
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Starting main service.");

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

            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device == null) {
                UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
                device = findDevice(usbManager);
            }

            if (device == null) {
                Log.w(LOG_TAG, "No USB device");
                stopSelf();
                return START_NOT_STICKY;
            }
            this.connectionManager.connect(device);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainService.this);
        if (prefs.getBoolean("overlay_active", false)) {
            this.overlayWindow.create();
        }
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

    private UsbDevice findDevice(UsbManager manager) {
        UsbDevice device = null;

        HashMap<String, UsbDevice> usbDevices = new HashMap<>();
        try {
            usbDevices = manager.getDeviceList();
        } catch (Exception e) {
            // Usually there will only be an exception on emulators.
            // But maybe weird device implementations exist aswell?
            Log.e(LOG_TAG, "USB not supported", e);
            return null;
        }

        if (usbDevices.isEmpty()) {
            return null;
        }

        for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
            UsbDevice deviceEntry = device = entry.getValue();
            int deviceVID = device.getVendorId();
            int devicePID = device.getProductId();
            if (deviceVID == 0x1a86 && devicePID == 0x7523) {
                device = deviceEntry;
            }
        }

        return device;
    }
}
