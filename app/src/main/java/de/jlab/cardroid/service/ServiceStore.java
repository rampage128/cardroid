package de.jlab.cardroid.service;

import android.app.Service;

import java.util.ArrayList;

import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDeviceHandler;
import de.jlab.cardroid.errors.ErrorService;
import de.jlab.cardroid.gps.GpsService;
import de.jlab.cardroid.rules.RuleService;

public class ServiceStore {

    public static ArrayList<Class<? extends Service>> servicesForDevice(DeviceHandler device) {
        ArrayList<Class<? extends Service>> services = new ArrayList<>();
        if (GpsUsbDeviceHandler.class.isInstance(device)) {
            services.add(GpsService.class);
        }
        services.add(ErrorService.class);
        return services;
    }
}
