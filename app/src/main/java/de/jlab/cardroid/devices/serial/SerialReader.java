package de.jlab.cardroid.devices.serial;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.utils.UsageStatistics;

public abstract class SerialReader<PacketType extends SerialPacket> {

    private ArrayList<SerialPacketListener<PacketType>> packetListeners = new ArrayList<>();
    private UsageStatistics packetStatistic = new UsageStatistics(1000, 60);
    private UsageStatistics byteStatistic = new UsageStatistics(1000, 60);

    public void onReceiveData(@NonNull byte[] data) {
        this.byteStatistic.count(data.length);
        PacketType[] packets = this.createPackets(data);
        this.packetStatistic.count(packets.length);
        for (int i = 0; i < this.packetListeners.size(); i++) {
            this.packetListeners.get(i).onReceivePackets(packets);
        }
    }

    public void addSerialPacketListener(@NonNull SerialPacketListener<PacketType> listener) {
        this.packetListeners.add(listener);
    }

    public void removeSerialPacketListener(@NonNull SerialPacketListener<PacketType> listener) {
        this.packetListeners.remove(listener);
    }

    public void addPacketStatisticListener(@NonNull UsageStatistics.UsageStatisticsListener listener) {
        this.packetStatistic.addListener(listener);
    }

    public void removePacketStatisticListener(@NonNull UsageStatistics.UsageStatisticsListener listener) {
        this.packetStatistic.removeListener(listener);
    }

    public void addByteStatisticListener(@NonNull UsageStatistics.UsageStatisticsListener listener) {
        this.byteStatistic.addListener(listener);
    }

    public void removeByteStatisticListener(@NonNull UsageStatistics.UsageStatisticsListener listener) {
        this.byteStatistic.removeListener(listener);
    }

    @NonNull
    protected abstract PacketType[] createPackets(@NonNull byte[] data);

    public interface SerialPacketListener<PacketType extends SerialPacket> {
        void onReceivePackets(@NonNull PacketType[] packets);
    }
}
