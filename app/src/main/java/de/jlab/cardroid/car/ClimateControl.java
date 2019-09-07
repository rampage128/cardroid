package de.jlab.cardroid.car;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

public class ClimateControl extends CarSystem {

    public static final String IS_AC_ON                 = "is_ac_on";
    public static final String IS_AUTO                  = "is_auto";
    public static final String IS_RECIRCULATION         = "is_recirculation";
    public static final String IS_WINDSHIELD_HEATING    = "is_windshield_heating";
    public static final String IS_REAR_WINDOW_HEATING   = "is_rear_window_heating";
    public static final String IS_DUCT_FEET             = "is_duct_feet";
    public static final String IS_DUCT_FACE             = "is_duct_face";
    public static final String IS_DUCT_WINDSHIELD       = "is_duct_windshield";
    public static final String FAN_LEVEL                = "fan_level";
    public static final String TEMPERATURE              = "temperature";

    @Override
    protected void updateDataFromPacket(CarSystemSerialPacket packet) {
        updateProperty(FAN_LEVEL, packet.readByte(1));
        updateProperty(TEMPERATURE, packet.readByte(2) / 2f);

        updateProperty(IS_AUTO, packet.readFlag(0, 6));
        updateProperty(IS_AC_ON, packet.readFlag(0, 7));
        updateProperty(IS_RECIRCULATION, packet.readFlag(0, 0));
        updateProperty(IS_WINDSHIELD_HEATING, packet.readFlag(0, 2));
        updateProperty(IS_REAR_WINDOW_HEATING, packet.readFlag(0, 1));

        updateProperty(IS_DUCT_WINDSHIELD, packet.readFlag(0, 5));
        updateProperty(IS_DUCT_FACE, packet.readFlag(0, 4));
        updateProperty(IS_DUCT_FEET, packet.readFlag(0, 3));
    }

    @Override
    protected void registerProperties() {
        registerProperty(FAN_LEVEL, 0);
        registerProperty(TEMPERATURE, 0);

        registerProperty(IS_AUTO, false);
        registerProperty(IS_AC_ON, false);
        registerProperty(IS_RECIRCULATION, false);
        registerProperty(IS_WINDSHIELD_HEATING, false);
        registerProperty(IS_REAR_WINDOW_HEATING, false);

        registerProperty(IS_DUCT_WINDSHIELD, false);
        registerProperty(IS_DUCT_FACE, false);
        registerProperty(IS_DUCT_FEET, false);
    }

}

