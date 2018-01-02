package de.jlab.cardroid.usb.carduino;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.jlab.cardroid.car.Car;
import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.car.CarSystemFactory;
import de.jlab.cardroid.car.ClimateControl;
import de.jlab.cardroid.car.ManageableCarSystem;
import de.jlab.cardroid.car.RemoteControl;
import de.jlab.cardroid.car.UnknownCarSystemException;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.usb.SerialConnectionManager;
import de.jlab.cardroid.usb.UsageStatistics;
import de.jlab.cardroid.usb.UsbService;

public class CarduinoService extends UsbService implements ManageableCarSystem.CarSystemEventListener {
    private static final String LOG_TAG = "CarduinoService";

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
                    // TODO find some action for phone key (maybe mute?!)
                    break;
                case 42: // VOICE
                    Intent voiceIntent =
                            new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
                    voiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    CarduinoService.this.startActivity(voiceIntent);
                    break;
            }
        }

        private void adjustVolume(int direction) {
            AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

            if (audioManager != null) {
                audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
            }
        }

        private void sendMediaEvent(int keyCode) {
            AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

            if (audioManager != null) {
                KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                audioManager.dispatchMediaKeyEvent(downEvent);

                KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                audioManager.dispatchMediaKeyEvent(upEvent);
            }
        }
    }

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

        public void requestBaudRate(int baudRate) {
            CarduinoService.this.requestCarduinoBaudRate(baudRate);
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
                        int baudRate = Integer.valueOf(prefs.getString("usb_baud_rate", "115200"));
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

        RemoteControl remoteControl = (RemoteControl)this.car.getCarSystem(CarSystemFactory.REMOTE_CONTROL);
        remoteControl.addChangeListener(new RemoteControlChangeListener());

        this.overlayWindow = new OverlayWindow(this, climateControl);

        this.serialReader = new SerialReader();
        this.serialReader.addListener(this.listener);

        this.connectionManager = new SerialConnectionManager(this);
        this.connectionManager.addConnectionListener(this.serialReader);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
        return true;
    }

    protected void disconnectDevice() {
        this.connectionManager.disconnect();
        this.overlayWindow.destroy();
    }

    @Override
    public void onTrigger(SerialCarButtonEventPacket packet) {
        this.connectionManager.send(SerialPacketFactory.serialize(packet));
    }
}
