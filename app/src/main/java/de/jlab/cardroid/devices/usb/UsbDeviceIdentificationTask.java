package de.jlab.cardroid.devices.usb;

import android.hardware.usb.UsbDevice;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;

public final class UsbDeviceIdentificationTask {

    private ArrayList<UsbDeviceDetector> detectors = new ArrayList<>();
    private DeviceService service;
    private UsbDeviceDetector.DetectionObserver observer;
    private MasterDetectionObserver masterObserver = new MasterDetectionObserver();

    private int activeDetectorIndex = 0;
    private UsbDevice activeDevice;

    public UsbDeviceIdentificationTask(@NonNull DeviceService service, @NonNull UsbDeviceDetector.DetectionObserver observer, @NonNull UsbDeviceDetector ...detectors) {
        this.detectors.addAll(Arrays.asList(detectors));
        this.service = service;
        this.observer = observer;
    }

    public void identify(@NonNull UsbDevice device) {
        this.activeDevice = device;
        UsbDeviceDetector detector = this.detectors.get(this.activeDetectorIndex);
        detector.setObserver(this.masterObserver);
        detector.identify(device, this.service);
    }

    private void identifyAgain() {
        this.activeDetectorIndex++;

        if (this.activeDetectorIndex < this.detectors.size()) {
            this.identify(this.activeDevice);
        } else {
            this.observer.detectionFailed();
            this.endIdentification();
        }
    }

    private void endIdentification() {
        this.activeDetectorIndex = 0;
        this.activeDevice = null;
    }

    private class MasterDetectionObserver implements UsbDeviceDetector.DetectionObserver {
        @Override
        public void deviceDetected(@NonNull DeviceHandler handler) {
            UsbDeviceIdentificationTask.this.observer.deviceDetected(handler);
            UsbDeviceIdentificationTask.this.endIdentification();
        }

        @Override
        public void detectionFailed() {
            UsbDeviceIdentificationTask.this.identifyAgain();
        }
    }

}
