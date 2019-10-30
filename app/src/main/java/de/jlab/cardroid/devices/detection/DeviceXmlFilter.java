package de.jlab.cardroid.devices.detection;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import de.jlab.cardroid.R;

public final class DeviceXmlFilter {

    private static final String NODE_DEVICE             = "usb-device";
    private static final String NODE_DETECTOR           = "detector";
    private static final String ATTRIBUTE_VENDOR_ID     = "vendor-id";
    private static final String ATTRIBUTE_PRODUCT_ID    = "product-id";
    private static final String ATTRIBUTE_CLASS         = "class";

    private Filter[] filters;

    public DeviceXmlFilter(@XmlRes int xmlResourceId, @NonNull Context context, @NonNull UsbDeviceDetector... detectors) {
        this.filters = parseFilterFile(xmlResourceId, context, detectors);
    }

    @Nullable
    public Iterator<UsbDeviceDetector> getIterator(@NonNull UsbDevice device) {
        for (Filter filter : this.filters) {
            if (filter.matchesDevice(device)) {
                return filter.getIterator();
            }
        }

        return null;
    }

    private static Filter[] parseFilterFile(@XmlRes int xmlResourceId, @NonNull Context context, @NonNull UsbDeviceDetector... detectors) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.device_filter);

        ArrayList<Filter> filters = new ArrayList<>();

        int eventType = -1;
        Filter currentDeviceInfo = null;
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            try {
                if (parser.getEventType() == XmlResourceParser.START_TAG) {
                    String nodeName = parser.getName();
                    if (NODE_DEVICE.equals(nodeName)) {
                        String vendorIdStr = parser.getAttributeValue(null, ATTRIBUTE_VENDOR_ID);
                        String productIdStr = parser.getAttributeValue(null, ATTRIBUTE_PRODUCT_ID);
                        currentDeviceInfo = new Filter(Integer.parseInt(vendorIdStr), productIdStr != null ? Integer.parseInt(productIdStr) : null);
                        filters.add(currentDeviceInfo);
                    } else if (NODE_DETECTOR.equals(nodeName)) {
                        assert currentDeviceInfo != null;
                        String detectorName = parser.getAttributeValue(null, ATTRIBUTE_CLASS);
                        for (UsbDeviceDetector detector : detectors) {
                            if (detector.getClass().getSimpleName().equalsIgnoreCase(detectorName)) {
                                currentDeviceInfo.addDetector(detector);
                            }
                        }
                    }
                }
                eventType = parser.next();
            } catch (Exception e) {
                Log.e(DeviceXmlFilter.class.getSimpleName(), "Error parsing XML resource " + context.getResources().getResourceEntryName(xmlResourceId), e);
            }
        }

        return filters.toArray(new Filter[0]);
    }

    private static class Filter {
        private Integer vendorId;
        private Integer productId;
        private ArrayList<UsbDeviceDetector> detectors;

        public Filter(@NonNull Integer vendorId, @Nullable Integer productId) {
            this.vendorId = vendorId;
            this.productId = productId;
            this.detectors = new ArrayList<>();
        }

        public void addDetector(@NonNull UsbDeviceDetector detector) {
            this.detectors.add(detector);
        }

        public boolean matchesDevice(@NonNull UsbDevice device) {
            return  this.vendorId.equals(device.getVendorId()) &&
                    (this.productId == null || productId.equals(device.getProductId()));
        }

        @NonNull
        public Iterator<UsbDeviceDetector> getIterator() {
            return this.detectors.iterator();
        }
    }

}
