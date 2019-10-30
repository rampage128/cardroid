package de.jlab.cardroid.devices.detection;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class UsbDeviceDetectionController {

    private Iterator<UsbDeviceDetector> detectors;

    private DeviceService service;

    private UsbDevice activeDevice;
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
        this.activeDevice = device;

        this.detectors = this.filter.getIterator(device);
        if (detectors != null) {
            this.doIdentify();
        } else {
            Log.w(this.getClass().getSimpleName(), "No UsbDeviceDetector available for UsbDevice " + device.getVendorId() + "/" + device.getProductId() + "@" + device.getDeviceName());
            this.onError.run();
            this.endIdentification();
        }
    }

    private void deviceDetected(@NonNull Device device, @Nullable DeviceUid deviceUid) {
        this.sink.deviceDetected(device, deviceUid);
        this.endIdentification();
    }

    private void onDetectorError() {
        this.doIdentify();
    }

    private void doIdentify() {
        if (this.detectors.hasNext()) {
            UsbDeviceDetector detector = this.detectors.next();
            detector.setSink(this::deviceDetected);
            detector.setOnError(this::onDetectorError);
            detector.identify(this.activeDevice, this.service);
        } else {
            this.onError.run();
            this.endIdentification();
        }
    }

    private void endIdentification() {
        this.detectors = null;
        this.activeDevice = null;
    }

}

