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
    private UsbDeviceDetector.DeviceDrain drain;

    public UsbDeviceDetectionController(@NonNull DeviceService service, @NonNull UsbDeviceDetector.DeviceSink sink, @NonNull UsbDeviceDetector.DeviceDrain drain, @NonNull UsbDeviceDetector ...detectors) {
        this.service = service;
        this.sink = sink;
        this.drain = drain;
        this.filter = new DeviceXmlFilter(R.xml.device_filter, service, detectors);
    }

    public void identify(@NonNull UsbDevice device) {
        Iterator<UsbDeviceDetector> detectors = this.filter.getIterator(device);

        if (detectors != null) {
            Detection detection = new Detection(service, this.sink, this.drain, detectors);
            detection.run(device);
        } else {
            Log.w(this.getClass().getSimpleName(), "No UsbDeviceDetector available for UsbDevice " + device.getVendorId() + "/" + device.getProductId() + "@" + device.getDeviceName());
            this.drain.deviceDetectionFailed(device);
        }
    }

    private static class Detection {

        private Iterator<UsbDeviceDetector> detectors;
        private UsbDeviceDetector.DeviceSink sink;
        private UsbDeviceDetector.DeviceDrain drain;
        private DeviceService service;

        public Detection(@NonNull DeviceService service, @NonNull UsbDeviceDetector.DeviceSink sink, @NonNull UsbDeviceDetector.DeviceDrain drain, @NonNull Iterator<UsbDeviceDetector> detectors) {
            this.sink = sink;
            this.drain = drain;
            this.detectors = detectors;
            this.service = service;
        }

        public void run(@NonNull UsbDevice device) {
            if (this.detectors != null && this.detectors.hasNext()) {
                this.detect(device, this.detectors.next());
            } else {
                this.deviceDetectionFailed(device);
            }
        }

        private void detect(@NonNull UsbDevice device, @NonNull UsbDeviceDetector detector) {
            detector.setSink(this::deviceDetected);
            detector.setOnError(this::onDetectorError);
            detector.identify(device, this.service);
        }

        private void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest) {
            this.sink.deviceDetected(connectionRequest);
            this.dispose();
        }

        private void deviceDetectionFailed(@NonNull UsbDevice device) {
            this.drain.deviceDetectionFailed(device);
            this.dispose();
        }

        private void onDetectorError(@NonNull UsbDevice device) {
            this.run(device);
        }

        private void dispose() {
            this.service = null;
            this.detectors = null;
        }

    }

}

