package de.jlab.cardroid.devices.usb;

import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

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
    private Filter filter;

    public UsbDeviceIdentificationTask(@NonNull DeviceService service, @NonNull UsbDeviceDetector.DetectionObserver observer, @NonNull UsbDeviceDetector ...detectors) {
        this.detectors.addAll(Arrays.asList(detectors));
        this.service = service;
        this.observer = observer;
        this.filter = Filter.get(() -> this.service.getResources().getXml(R.xml.device_filter));
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
    static class Filter {

        private static Filter cachedFilter = null;

        private XmlResourceParser parser;
        Set<DeviceInfo> devicesInfo;

        private Filter(@NonNull XmlResourceParser parser) {
            this.parser = parser;
            this.devicesInfo = new HashSet<>();
            parseDetectorList();

        }

        public static Filter get(Supplier<XmlResourceParser> resourceProvider) {
            if (cachedFilter == null) {
                cachedFilter = new Filter(resourceProvider.get());
            }
            return cachedFilter;
        }

        private void parseDetectorList() {
            int eventType = -1;
            boolean deviceMatchFound = false;
            DeviceInfo currentDeviceInfo = null;
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                try {
                    if (parser.getEventType() == XmlResourceParser.START_TAG) {
                        String nodeName = parser.getName();
                        if (nodeName.equals("usb-device")) {
                            String vendorIdStr = parser.getAttributeValue(null, "vendor-id");
                            String productIdStr = parser.getAttributeValue(null, "product-id");
                            currentDeviceInfo = new DeviceInfo(Integer.parseInt(vendorIdStr), productIdStr != null ? Integer.parseInt(productIdStr) : null);
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
            for (DeviceInfo info: devicesInfo) {
                if (info.matches(device, detector)) {
                    return true;
                }
            }
            return false;
        }

        private class DeviceInfo {
            Integer vendorId;
            Integer productId;
            private Set<String> classNames;
            public DeviceInfo(@NonNull Integer vendorId, @Nullable Integer productId) {
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

