package de.jlab.cardroid.devices.detection;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.util.Iterator;

import androidx.annotation.NonNull;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceConnectionRequest;
import de.jlab.cardroid.devices.DeviceService;

public final class UsbDeviceDetectionController {

    private DeviceService service;

    private DeviceXmlFilter filter;

    private UsbDeviceDetector.DeviceSink sink;
    private Runnable onError;

    public UsbDeviceDetectionController(@NonNull DeviceService service, @NonNull UsbDeviceDetector.DeviceSink sink, @NonNull Runnable onError, @NonNull UsbDeviceDetector ...detectors) {
        this.service = service;
        this.sink = sink;
        this.onError = onError;
        this.filter = new DeviceXmlFilter(R.xml.device_filter, service, detectors);
    }

    public void identify(@NonNull UsbDevice device) {
        Iterator<UsbDeviceDetector> detectors = this.filter.getIterator(device);

        if (detectors != null) {
            Detection detection = new Detection(service, device, this.sink, this.onError, detectors);
            detection.run();
        } else {
            Log.w(this.getClass().getSimpleName(), "No UsbDeviceDetector available for UsbDevice " + device.getVendorId() + "/" + device.getProductId() + "@" + device.getDeviceName());
            this.onError.run();
        }
    }

    private static class Detection {

        private Iterator<UsbDeviceDetector> detectors;
        private UsbDeviceDetector.DeviceSink sink;
        private Runnable onError;
        private UsbDevice device;
        private DeviceService service;

        public Detection(@NonNull DeviceService service, @NonNull UsbDevice device, @NonNull UsbDeviceDetector.DeviceSink sink, @NonNull Runnable onError, @NonNull Iterator<UsbDeviceDetector> detectors) {
            this.device = device;
            this.sink = sink;
            this.onError = onError;
            this.detectors = detectors;
            this.service = service;
        }

        public void run() {
            if (this.detectors != null && this.detectors.hasNext()) {
                this.detect(this.detectors.next());
            } else {
                this.deviceDetectionFailed();
            }
        }

        private void detect(@NonNull UsbDeviceDetector detector) {
            detector.setSink(this::deviceDetected);
            detector.setOnError(this::onDetectorError);
            detector.identify(this.device, service);
        }

        private void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest) {
            this.sink.deviceDetected(connectionRequest);
            this.dispose();
        }

        private void deviceDetectionFailed() {
            this.onError.run();
            this.dispose();
        }

        private void onDetectorError() {
            this.run();
        }

        private void dispose() {
            this.service = null;
            this.detectors = null;
            this.device = null;
        }

    }

}

