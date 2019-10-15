package de.jlab.cardroid.devices.usb;

import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import androidx.annotation.NonNull;

import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;

public final class UsbDeviceIdentificationTask {

    private ArrayList<UsbDeviceDetector> detectors = new ArrayList<>();
    private DeviceService service;
    private UsbDeviceDetector.DetectionObserver observer;
    private MasterDetectionObserver masterObserver = new MasterDetectionObserver();

    private int activeDetectorIndex = 0;
    private UsbDevice activeDevice;
    private DeviceDetectorInfo activeDeviceDetectorInfo;

    public UsbDeviceIdentificationTask(@NonNull DeviceService service, @NonNull UsbDeviceDetector.DetectionObserver observer, @NonNull UsbDeviceDetector ...detectors) {
        this.detectors.addAll(Arrays.asList(detectors));
        this.service = service;
        this.observer = observer;
    }

    public void identify(@NonNull UsbDevice device) {
        this.activeDevice = device;
        this.activeDeviceDetectorInfo = new DeviceDetectorInfo(this.activeDevice, this.service.getResources().getXml(R.xml.device_filter));

        UsbDeviceDetector detector = this.detectors.get(this.activeDetectorIndex);
        if (this.activeDeviceDetectorInfo.isValidDetector(detector)) {
            detector.setObserver(this.masterObserver);
            detector.identify(device, this.service);
        } else {
            identifyAgain();
        }
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


    private class DeviceDetectorInfo {
        private UsbDevice device;
        private XmlResourceParser parser;
        private Set<String> classNames;

        public DeviceDetectorInfo(@NonNull UsbDevice device, @NonNull XmlResourceParser parser) {
            this.device = device;
            this.parser = parser;
            parseDetectorList();
        }

        private void parseDetectorList() {
            int eventType = -1;
            boolean deviceMatchFound = false;
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                try {
                    if (parser.getEventType() == XmlResourceParser.START_TAG) {
                        String nodeName = parser.getName();
                        if (nodeName.equals("usb-device")) {
                            boolean match = false;
                            String vendorIdStr = parser.getAttributeIntValue(null, "vendor-id");
                            String productIdStr = parser.getAttributeValue(null, "product-id");
                            if (vendorIdStr != null) {
                                match = device.getVendorId() == Integer.parseInt(vendorIdStr);
                            } else {
                                continue;
                            }
                            if (productIdStr != null) {
                                match = match && device.getProductId() == Integer.parseInt(productIdStr);
                            }
                            if (match) {
                                // parse all the detectors
                            }
                        } else if (deviceMatchFound && nodeName.equals("detector")) {
                            classNames.add(parser.getAttributeValue(null, "class"));
                        }
                        eventType = parser.next();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isValidDetector(UsbDeviceDetector detector) {
            return classNames.contains(detector.getClass().getSimpleName());
        }
    }


}
