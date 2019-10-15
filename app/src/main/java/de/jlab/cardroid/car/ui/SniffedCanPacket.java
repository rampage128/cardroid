package de.jlab.cardroid.car.ui;

import java.util.ArrayList;

import de.jlab.cardroid.car.CanPacket;

/**
 * @deprecated The time dependency in this class can be introduces into CanPacket class, rendering this class obsolete.
 */
@Deprecated
public final class SniffedCanPacket {
    private long id;
    private ArrayList<DataByte> payload = new ArrayList<>();
    private long lastChange = 0;
    private long createdOn = 0;

    public SniffedCanPacket(CanPacket packet) {
        this(packet.getCanId(), packet.getData());
    }

    public SniffedCanPacket(long id, byte[] dataBytes) {
        this.id = id;
        this.payload.clear();
        this.update(dataBytes);
        this.createdOn = System.currentTimeMillis();
    }

    public boolean update(byte[] dataBytes) {
        boolean hasChanged = false;
        for (int i = 0; i < dataBytes.length; i++) {
            byte data = dataBytes[i];
            if (this.payload.size() - 1 >= i) {
                if (this.payload.get(i).update(data)) {
                    hasChanged = true;
                }
            }
            else {
                this.payload.add(new DataByte(data));
                hasChanged = true;
            }
        }

        for (int i = dataBytes.length; i < this.payload.size(); i++) {
            this.payload.remove(i);
        }

        if (hasChanged) {
            this.lastChange = System.currentTimeMillis();
        }

        return hasChanged;
    }

    public long getAge() {
        return System.currentTimeMillis() - this.createdOn;
    }

    public boolean isExpired(long timeout) {
        return System.currentTimeMillis() - this.lastChange >= timeout;
    }

    public DataByte getByte(int index) {
        if (index > this.payload.size() - 1) {
            return null;
        }
        return this.payload.get(index);
    }

    public long getId() {
        return this.id;
    }

    public String getIdHex() {
        return String.format("%04X", this.id);
    }

    static class DataByte {
        private long timestamp;
        private byte data = 0x00;

        private String raw = null;
        private String hex = null;
        private String binary = null;

        private boolean hasChanged = false;

        private DataByte(byte data) {
            this.update(data);
        }

        private boolean update(byte data) {
            if (this.data != data || this.raw == null) {
                this.timestamp = System.currentTimeMillis();
                this.data = data;
                this.raw = isPrintable() ? String.valueOf((char)this.data) : String.valueOf('\uFFFD');
                this.hex = String.format("%02X", 0xFF & this.data);
                this.binary = String.format("%8s", Integer.toBinaryString(this.data & 0xFF)).replace(' ', '0');
                this.hasChanged = true;
            }
            return this.hasChanged;
        }

        public boolean isFresh(long timeout) {
            return System.currentTimeMillis() - this.timestamp <= timeout;
        }

        public long getAge() {
            return System.currentTimeMillis() - this.timestamp;
        }

        public String getRaw() {
            return this.raw;
        }

        public String getHex() {
            return this.hex;
        }

        public String getBinary() {
            return this.binary;
        }

        public boolean isPrintable() {
            Character.UnicodeBlock block = Character.UnicodeBlock.of( 0xFF & this.data );
            return (!Character.isISOControl(0xFF & this.data)) &&
            block != null &&
            block != Character.UnicodeBlock.SPECIALS;
        }

        public boolean hasChanged() {
            boolean result = this.hasChanged;
            this.hasChanged = false;
            return result;
        }
    }
}
