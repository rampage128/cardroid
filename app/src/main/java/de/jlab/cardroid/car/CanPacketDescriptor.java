package de.jlab.cardroid.car;

import android.util.Log;
import android.util.SparseIntArray;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public final class CanPacketDescriptor implements CanObservable.CanPacketListener {

    private long canId;
    private byte byteMask = 0x00;
    private ArrayList<CanValue> values = new ArrayList<>();
    private SparseIntArray bitOffsets = new SparseIntArray();
    private int compressedPacketLength = 0;

    public CanPacketDescriptor(long canId) {
        this.canId = canId;
    }

    public void addCanValue(@NonNull CanValue value) {
        this.values.add(value);

        int startByteIndex = (int)Math.floor((value.getBitIndex() + 1) / 8f);
        int endByteIndex = startByteIndex + (int)Math.floor((value.getBitLength() - 1) / 8);

        this.recalculateByteMask(startByteIndex, endByteIndex);
        this.compressedPacketLength = getByteCountUntil(8);
        this.recalculateOffsets();
    }

    private void recalculateByteMask(int startByteIndex, int endByteIndex) {
        for (int i = startByteIndex; i <= endByteIndex; i++) {
            this.byteMask |= 1 << (7 - i);
        }

        StringBuilder sb = new StringBuilder("Recalculated byte mask for " + String.format("%02x", this.getCanId()) + " (" + startByteIndex + " -> " + endByteIndex + "): ");
        for (int i = 0; i < 8; i++) {
            sb.append((this.byteMask >> (7 - i)) & 0x01);
        }
        Log.e(this.getClass().getSimpleName(), sb.toString());
    }

    private void recalculateOffsets() {
        Log.e(this.getClass().getSimpleName(), "Recalculating bit offsets for: " + String.format("%02x", this.getCanId()));
        for (int i = 0; i < this.values.size(); i++) {
            CanValue value = this.values.get(i);
            int bitIndex = value.getBitIndex();
            int startByteIndex = (int)Math.floor((bitIndex + 1) / 8f);
            int byteCount = getUnusedByteCountUntil(startByteIndex);
            int byteOffset = byteCount * -1;
            int bitOffset = (byteOffset * 8);
            this.bitOffsets.put(value.getBitIndex(), bitOffset);
            Log.e(this.getClass().getSimpleName(), "Recalculating bit offset: " + bitIndex + " " + bitOffset + " = " + (bitIndex + bitOffset) + " (start byte: " + startByteIndex + ", unused bytes before: " + byteCount + ", byte offset: " + byteOffset + ")");
        }
    }

    private int getByteCountUntil(int endByteIndex) {
        int byteCount = 0;
        for (int i = 0; i < endByteIndex; i++) {
            byteCount += (this.byteMask >> (7 - i)) & 0x01;
        }
        return byteCount;
    }

    private int getUnusedByteCountUntil(int endByteIndex) {
        int byteCount = 0;
        for (int i = 0; i < endByteIndex; i++) {
            byteCount += (this.byteMask >> (7 - i)) & 0x01 ^ 0x01;
        }
        return byteCount;
    }

    public long getCanId() {
        return this.canId;
    }

    public byte getByteMask() {
        return this.byteMask;
    }

    @Override
    public void onReceive(CanPacket packet) {
        for (int i = 0; i < this.values.size(); i++) {
            CanValue value = this.values.get(i);
            int bitOffset = 0;
            if (this.compressedPacketLength == packet.getDataLength()) {
                bitOffset = this.bitOffsets.get(value.getBitIndex());
            }
            value.updateFromCanPacket(packet, bitOffset);
        }
    }

}
