package de.jlab.cardroid.devices.serial.gps;

import android.text.TextUtils;

import java.util.HashMap;

public abstract class NmeaParser {

    private static HashMap<String, NmeaParser> parsers = new HashMap<>();

    public abstract void parseSentence(GpsSerialPacket packet, GpsPosition position);

    public static NmeaParser createFrom(String type) {
        NmeaParser parser = parsers.get(type);
        if (parser == null) {
            switch(type) {
                case "GPGLL":
                    parser = new GpgllParser();
                    break;
                case "GPGGA":
                    parser = new GpggaParser();
                    break;
                case "GPGSA":
                    parser = new GpgsaParser();
                    break;
                case "GPRMC":
                    parser = new GprmcParser();
                    break;
                case "GPGSV":
                    parser = new GpgsvParser();
                    break;
            }
            parsers.put(type, parser);
        }

        return parser;
    }

    static class GpgllParser extends NmeaParser {
        @Override
        public void parseSentence(GpsSerialPacket packet, GpsPosition position) {
            if (packet.length() < 6) {
                return;
            }
            position.updateCoordinates(packet.readLatitude(1, 2), packet.readLongitude(3, 4));
            position.updateTime(packet.readTime(5));
        }
    }

    static class GpggaParser extends NmeaParser {
        @Override
        public void parseSentence(GpsSerialPacket packet, GpsPosition position) {
            if (packet.length() < 12) {
                return;
            }
            position.updateTime(packet.readTime(1));
            position.updateCoordinates(packet.readLatitude(2, 3), packet.readLongitude(4, 5));
            //parseInt(NMEAParser.PROPERTY_QUALITY, tokens[6]);
            position.updateAltitude(packet.readDouble(11, 0d));
        }
    }

    static class GpgsaParser extends NmeaParser {
        @Override
        public void parseSentence(GpsSerialPacket packet, GpsPosition position) {
            if (packet.length() < 18) {
                return;
            }
            position.updateAccuracy(
                    packet.readInt(2, GpsPosition.FIX_NONE),
                    packet.readFloat(15, GpsPosition.MAX_DOP),
                    packet.readFloat(16, GpsPosition.MAX_DOP),
                    packet.readFloat(17, GpsPosition.MAX_DOP)
            );
            // TODO update fix per satellite (3-14 = IDs of SVs used in position fix)
        }
    }

    static class GprmcParser extends NmeaParser {
        @Override
        public void parseSentence(GpsSerialPacket packet, GpsPosition position) {
            if (packet.length() < 9) {
                return;
            }
            position.updateTime(packet.readTime(1));
            position.updateCoordinates(packet.readLatitude(3, 4), packet.readLongitude(5, 6));
            position.updateMotion(packet.readFloat(7, 0), packet.readFloat(8, 0));
        }
    }

    static class GpgsvParser extends NmeaParser {
        @Override
        public void parseSentence(GpsSerialPacket packet, GpsPosition position) {
            if (packet.length() < 8) {
                return;
            }

            for (int i = 1; i <= 4; i++) {
                if (packet.length() > 4 * i + 3) {
                    String snrValue = packet.readString(4 * i);
                    if (!TextUtils.isEmpty(snrValue)) {
                        position.updateSatellite(
                                packet.readInt(4 * i, 0),
                                packet.readFloat(4 * i + 1, 0),
                                packet.readFloat(4 * i + 2, 90),
                                packet.readInt(4 * i + 3, -1)
                        );
                    }
                }
            }
            if (packet.readInt(1, -1) == packet.readInt(2, -2)) {
                position.flushSatellites();
            }
        }
    }

}
