package de.jlab.cardroid.devices.detection;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.SerialPacket;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialConnection;

public final class UsbSerialDeviceDetector extends UsbDeviceDetector {

    private SerialMatcher[] matchers;

    private Timer timer = null;
    private UsbSerialConnection connection = null;
    private int[] baudRates = null;
    private int currentBaudRateIndex = 0;

    private DetectionSerialReader reader;

    public UsbSerialDeviceDetector(SerialMatcher... matchers) {
        this.matchers = matchers;
    }

    @Override
    protected boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        UsbManager usbManager = (UsbManager)service.getApplication().getSystemService(Context.USB_SERVICE);
        this.baudRates = service.getResources().getIntArray(R.array.serial_detection_baud_rates);
        this.connection = new UsbSerialConnection(device, this.baudRates[this.currentBaudRateIndex], usbManager);

        this.reader = new DetectionSerialReader(device, service.getApplication());
        this.connection.addUsbSerialReader(this.reader);
        boolean connectionSuccess = this.connection.connect();
        if (connectionSuccess) {
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    currentBaudRateIndex++;
                    if (currentBaudRateIndex < baudRates.length) {
                        connection.setBaudRate(baudRates[currentBaudRateIndex]);
                        UsbSerialDeviceDetector.this.reader.clear();
                    } else {
                        detectionFailed();
                    }
                }
            }, 1000, 1000);
        }

        return connectionSuccess;
    }

    protected void deviceDetected(@NonNull Device handler, @Nullable DeviceUid predictedDeviceUid) {
        Log.e(this.getClass().getSimpleName(), "Serial device detected " + handler.getClass().getSimpleName());
        this.dispose();
        super.deviceDetected(handler, predictedDeviceUid);
    }

    protected void detectionFailed() {
        Log.e(this.getClass().getSimpleName(), "Serial device detection failed");
        this.dispose();
        super.detectionFailed();
    }

    private void dispose() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
        if (this.connection != null) {
            this.connection.removeUsbSerialReader(this.reader);
            this.connection.disconnect();
        }
        this.connection = null;
        this.reader = null;
        this.currentBaudRateIndex = 0;
        this.baudRates = null;
    }

    private class DetectionSerialReader extends SerialReader {

        private UsbDevice device;
        private Application app;

        public DetectionSerialReader(@NonNull UsbDevice device, @NonNull Application app) {
            this.device = device;
            this.app = app;
        }

        public void clear() {
            for (SerialMatcher matcher : matchers) {
                matcher.clear();
            }
        }

        @NonNull
        @Override
        protected SerialPacket[] createPackets(@NonNull byte[] data) {
            if (UsbSerialDeviceDetector.this.baudRates.length > UsbSerialDeviceDetector.this.currentBaudRateIndex) {
                int baudRate = UsbSerialDeviceDetector.this.baudRates[UsbSerialDeviceDetector.this.currentBaudRateIndex];
                for (SerialMatcher matcher : matchers) {
                    Device device = matcher.detect(data, this.device, baudRate, this.app);
                    if (device != null) {
                        deviceDetected(device, matcher.predictDeviceUid(this.device));
                    }
                }
            }

            return new SerialPacket[0];
        }
    }

    public interface SerialMatcher {
        @Nullable
        Device detect(@NonNull byte[] data, @NonNull UsbDevice device, int baudRate, @NonNull Application app);

        @Nullable
        DeviceUid predictDeviceUid(@NonNull UsbDevice device);

        void clear();
    }

}
