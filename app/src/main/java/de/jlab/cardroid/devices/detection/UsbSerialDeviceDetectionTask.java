package de.jlab.cardroid.devices.detection;

import android.app.Application;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

import de.jlab.cardroid.devices.DeviceConnectionRequest;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.SerialPacket;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialConnection;

public class UsbSerialDeviceDetectionTask {

    private UsbDevice device;
    private DeviceService service;
    private UsbSerialDeviceDetector.SerialMatcher[] matchers;
    private UsbDeviceDetector detector;

    private DetectionSerialReader reader = null;
    private UsbSerialConnection connection = null;
    private Timer timer = null;
    private int currentBaudRateIndex = 0;

    public UsbSerialDeviceDetectionTask(@NonNull UsbDevice device, @NonNull DeviceService service, @NonNull UsbDeviceDetector detector) {
        this.device = device;
        this.service = service;
        this.matchers = matchers;
        this.detector = detector;
        this.matchers = new UsbSerialDeviceDetector.SerialMatcher[] {
            new CarduinoSerialMatcher(),
            new GpsSerialMatcher()
        };
    }

    public void detect(@NonNull UsbManager usb, long timeout, int... baudRates) {
        this.reader = new DetectionSerialReader(device, service.getApplication());
        this.connection = new UsbSerialConnection(device, baudRates[0], usb);
        this.connection.addUsbSerialReader(this.reader);
        boolean connectionSuccess = this.connection.connect();
        if (connectionSuccess) {
            this.currentBaudRateIndex = 0;
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (currentBaudRateIndex < baudRates.length) {
                        int currentBaudRate = baudRates[currentBaudRateIndex];
                        connection.setBaudRate(currentBaudRate);
                        reader.setBaudRate(currentBaudRate);
                        reader.clear();
                        currentBaudRateIndex++;
                    } else {
                        detectionFailed(device);
                    }
                }
            }, timeout, timeout);
        } else {
            this.detectionFailed(this.device);
        }
    }

    public void dispose() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }

        if (this.connection != null) {
            if (this.reader != null) {
                this.connection.removeUsbSerialReader(this.reader);
            }
            this.connection.disconnect();
            this.connection = null;
            this.reader = null;
        }

        this.service = null;
        this.device = null;
        this.detector = null;
    }

    private void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest) {
        this.detector.deviceDetected(connectionRequest);
        this.dispose();
    }

    private void detectionFailed(@NonNull UsbDevice device) {
        this.detector.detectionFailed(device);
        this.dispose();
    }


    private class DetectionSerialReader extends SerialReader {

        private UsbDevice device;
        private Application app;
        private UsbDeviceDetector.DeviceSink onSuccess;

        private int baudRate = 0;

        public DetectionSerialReader(@NonNull UsbDevice device, @NonNull Application app) {
            this.device = device;
            this.app = app;
        }

        public void clear() {
            for (UsbSerialDeviceDetector.SerialMatcher matcher : matchers) {
                matcher.clear();
            }
        }

        public void setBaudRate(int baudRate) {
            this.baudRate = baudRate;
        }

        @NonNull
        @Override
        protected SerialPacket[] createPackets(@NonNull byte[] data) {
            if (baudRate > 0) {
                for (UsbSerialDeviceDetector.SerialMatcher matcher : matchers) {
                    DeviceConnectionRequest connectionRequest = matcher.detect(data, this.device, baudRate, this.app);
                    if (connectionRequest != null) {
                        deviceDetected(connectionRequest);
                    }
                }
            }

            return new SerialPacket[0];
        }
    }

}
