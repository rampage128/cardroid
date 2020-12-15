package de.jlab.cardroid.devices.serial.carduino;

import android.app.Application;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;

public final class CarduinoUidGenerator {

    private static final int ID_LENGTH = 3;
    private static final String ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ID_PATTERN_PART = "[A-Z]{" + ID_LENGTH + "}";
    private static final String ID_PATTERN = "^" + ID_PATTERN_PART + "$";
    private static final String FRAME_ID_PATTERN = new StringBuilder("[\\S\\s]*")
            .append("\\").append((char)CarduinoSerialPacket.HEADER)
            .append((char)CarduinoPacketType.META.getType())
            .append("[^\\}]*(").append(ID_PATTERN_PART).append(")\\").append((char)CarduinoSerialPacket.FOOTER)
            .append("[\\S\\s]*").toString();

    @NonNull
    public static String generateId(@NonNull Application app) {
        DeviceRepository repo = new DeviceRepository(app);
        List<DeviceEntity> deviceEntities = repo.getAllSynchronous();

        String carduinoId = generateId();

        boolean matches = true;
        while (matches) {
            DeviceUid carduinoUid = getUid(carduinoId.getBytes());

            matches = false;
            for (DeviceEntity deviceEntity : deviceEntities) {
                if (deviceEntity.deviceUid.equals(carduinoUid.toString())) {
                    matches = true;
                    carduinoId = generateId();
                    break;
                }
            }
        }

        return carduinoId;
    }

    public static boolean isValidId(@NonNull byte[] carduinoId) {
        return new String(carduinoId).matches(ID_PATTERN);
    }

    public static DeviceUid getUid(@NonNull byte[] carduinoId) {
        String idString = new String(carduinoId);
        if (!isValidId(carduinoId)) {
            throw new IllegalArgumentException("Carduino IDs must match the pattern \"" + ID_PATTERN + "\". Found \"" + idString + "\"");
        }
        return new DeviceUid("c4rdu1n0-" + idString);
    }

    public static DeviceUid getUid(@NonNull String frames) {
        Matcher matcher = Pattern.compile(FRAME_ID_PATTERN).matcher(frames);
        if (matcher.matches() && matcher.groupCount() > 0) {
            String carduinoId = matcher.group(1);
            if (carduinoId != null && isValidId(carduinoId.getBytes())) {
                return getUid(carduinoId.getBytes());
            }
        }

        return null;
    }

    private static String generateId() {
        Random rnd = new Random();
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < ID_LENGTH; i++) {
            int randomCharIndex = (int) (rnd.nextFloat() * ID_CHARS.length());
            id.append(ID_CHARS.charAt(randomCharIndex));
        }

        return id.toString();
    }

}
