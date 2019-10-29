package de.jlab.cardroid.devices.usb;

import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceService;

public final class UsbDeviceIdentificationTask {

    private ArrayList<UsbDeviceDetector> detectors = new ArrayList<>();
    private DeviceService service;
    private UsbDeviceDetector.DetectionObserver observer;
    private MasterDetectionObserver masterObserver = new MasterDetectionObserver();

    private int activeDetectorIndex = 0;
    private UsbDevice activeDevice;
    private DeviceDetectorFilter filter;

    public UsbDeviceIdentificationTask(@NonNull DeviceService service, @NonNull UsbDeviceDetector.DetectionObserver observer, @NonNull UsbDeviceDetector ...detectors) {
        this.detectors.addAll(Arrays.asList(detectors));
        this.service = service;
        this.observer = observer;
        this.filter = new DeviceDetectorFilter(this.service.getResources().getXml(R.xml.device_filter));
    }

    public void identify(@NonNull UsbDevice device) {
        this.activeDevice = device;

        UsbDeviceDetector detector = this.detectors.get(this.activeDetectorIndex);
        if (this.filter.isValidDetector(device, detector)) {
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
        public void deviceDetected(@NonNull Device handler) {
            UsbDeviceIdentificationTask.this.observer.deviceDetected(handler);
            UsbDeviceIdentificationTask.this.endIdentification();
        }

        @Override
        public void detectionFailed() {
            UsbDeviceIdentificationTask.this.identifyAgain();
        }
    }


    private class DeviceDetectorFilter {
        private XmlResourceParser parser;
        Set<DeviceDetectorInfo> devicesInfo;

        public DeviceDetectorFilter(@NonNull XmlResourceParser parser) {
            this.parser = parser;
            this.devicesInfo = new HashSet<>();
            parseDetectorList();

        }

        private void parseDetectorList() {
            int eventType = -1;
            boolean deviceMatchFound = false;
            DeviceDetectorInfo currentDeviceInfo = null;
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                try {
                    if (parser.getEventType() == XmlResourceParser.START_TAG) {
                        String nodeName = parser.getName();
                        if (nodeName.equals("usb-device")) {
                            String vendorIdStr = parser.getAttributeValue(null, "vendor-id");
                            String productIdStr = parser.getAttributeValue(null, "product-id");
                            currentDeviceInfo = new DeviceDetectorInfo(vendorIdStr, productIdStr);
                            this.devicesInfo.add(currentDeviceInfo);
                        } else if (nodeName.equals("detector")) {
                            currentDeviceInfo.addClassName(parser.getAttributeValue(null, "class"));
                        }
                    }
                    eventType = parser.next();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isValidDetector(@NonNull UsbDevice device, @NonNull UsbDeviceDetector detector) {
            for (DeviceDetectorInfo info: devicesInfo) {
                if (info.matches(device, detector)) {
                    return true;
                }
            }
            return false;
        }

        private class DeviceDetectorInfo {
            String vendorId;
            String productId;
            private Set<String> classNames;
            public DeviceDetectorInfo(@NonNull String vendorId, @Nullable String productId) {
                this.vendorId = vendorId;
                this.productId = productId;
                this.classNames = new HashSet<String>();
            }

            public void addClassName(@NonNull String className) {
                this.classNames.add(className);
            }

            public boolean matches(@NonNull UsbDevice device, @NonNull UsbDeviceDetector detector) {
                boolean match = false;
                if (vendorId.equals(device.getVendorId())) {
                    match = true;
                    if (productId != null) {
                        match = match && productId.equals(device.getProductId());
                    }
                }
                if (match) {
                    return classNames.contains(detector.getClass().getSimpleName());
                }
                return false;
            }
        }
    }



}
