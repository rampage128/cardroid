package de.jlab.cardroid;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.usb.CarSystemSerialPacket;
import de.jlab.cardroid.usb.CarduinoManager;
import de.jlab.cardroid.usb.SerialDataPacket;
import de.jlab.cardroid.usb.SerialPacket;
import de.jlab.cardroid.usb.SerialReader;

public class MainService extends Service {
    private static final String LOG_TAG = "MainService";

    private OverlayWindow overlayWindow;
    private Handler uiHandler;

    private CarduinoManager carduino;

    private SerialReader.SerialPacketListener listener = new SerialReader.SerialPacketListener() {
        @Override
        public void onReceivePackets(ArrayList<SerialPacket> packets) {
            Log.d(LOG_TAG, "Received " + packets.size() + " packets.");

            for (final SerialPacket packet : packets) {
                byte[] payload = ((SerialDataPacket)packet).getPayload();
                Log.d(LOG_TAG, new String(payload) + ", " + Integer.toBinaryString(payload[0] & 0xFF) + ", " + bytesToHex(payload) + " (" + payload.length + ")");

                if (packet instanceof CarSystemSerialPacket) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            overlayWindow.updateFromPacket((CarSystemSerialPacket)packet);
                        }
                    });
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
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        uiHandler = new Handler();

        this.carduino = new CarduinoManager(this);
        SerialReader serialReader = new SerialReader();
        serialReader.addListener(this.listener);
        this.carduino.addListener(serialReader);

        this.overlayWindow = new OverlayWindow(this);
        this.overlayWindow.create();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Service start requested.");

        this.carduino.connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.overlayWindow.destroy();
        this.carduino.disconnect();
    }

    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }
}
