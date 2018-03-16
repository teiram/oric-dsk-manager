package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class SedoricDescriptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SedoricDescriptor.class);
    private static final int HEADER_LENGTH = 12;
    private static final int DESCRIPTOR_SIGNATURE = 0xFF;
    private int startAddress;
    private int endAddress;
    private int execAddress;
    private boolean executable;
    private boolean block;
    private int sectors;

    private List<SectorCoordinates> fileSectors = new ArrayList<>();

    public static Builder newBuilder() {
        return new Builder();
    }

    public void addFileSector(SectorCoordinates sectorCoordinates) {
        fileSectors.add(sectorCoordinates);
    }

    public int getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(int startAddress) {
        this.startAddress = startAddress;
    }

    public int getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(int endAddress) {
        this.endAddress = endAddress;
    }

    public int getExecAddress() {
        return execAddress;
    }

    public void setExecAddress(int execAddress) {
        this.execAddress = execAddress;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    public int getSectors() {
        return sectors;
    }

    public void setSectors(int sectors) {
        this.sectors = sectors;
    }

    public List<SectorCoordinates> getFileSectors() {
        return fileSectors;
    }

    public void setFileSectors(List<SectorCoordinates> fileSectors) {
        this.fileSectors = fileSectors;
    }

    public static class Builder {
        private SedoricDescriptor descriptor = new SedoricDescriptor();

        public Builder withExecutable(boolean executable) {
            descriptor.setExecutable(executable);
            return this;
        }

        public Builder withBlock(boolean block) {
            descriptor.setBlock(block);
            return this;
        }

        public Builder withStartAddress(int startAddress) {
            descriptor.setStartAddress(startAddress);
            return this;
        }

        public Builder withEndAddress(int endAddress) {
            descriptor.setEndAddress(endAddress);
            return this;
        }

        public Builder withExecAddress(int execAddress) {
            descriptor.setExecAddress(execAddress);
            return this;
        }

        public Builder withSectors(int sectors) {
            descriptor.setSectors(sectors);
            return this;
        }

        public SedoricDescriptor build() {
            return descriptor;
        }
    }

    private static SedoricDescriptor fillFileDescriptors(SedoricDescriptor descriptor,
                                                         int descriptorTrack, int descriptorSector,
                                                         Disk disk) {
        int track = descriptorTrack;
        int sector = descriptorSector;
        int offset = HEADER_LENGTH;
        int sectorCount = 0;
        do {
            LOGGER.debug("Searching for descriptors in sector(" + track + ", " + sector
                    + ") @" + offset);
            byte[] sectorData = disk.getSector(track, sector);
            while (offset < Constants.SECTOR_SIZE) {
                SectorCoordinates coordinates = new SectorCoordinates(
                        Util.asUnsignedByte(sectorData[offset++]),
                        Util.asUnsignedByte(sectorData[offset++])
                );
                if (coordinates.isValid()) {
                    descriptor.addFileSector(coordinates);
                    sectorCount++;
                } else {
                    LOGGER.debug("Found invalid file coordinates {} with count {}",
                            coordinates, sectorCount);
                    break;
                }
            }
            track = Util.asUnsignedByte(sectorData[0]);
            sector = Util.asUnsignedByte(sectorData[1]);
            offset = 2;
        } while (track != 0 && sectorCount < descriptor.getSectors());
        return descriptor;
    }

    public static SedoricDescriptor forSector(int descriptorTrack, int descriptorSector, Disk disk) {
        ByteBuffer buffer = ByteBuffer.wrap(disk.getSector(descriptorTrack, descriptorSector),
                2, HEADER_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);
        Builder builder = newBuilder();
        if (Util.asUnsignedByte(buffer.get()) == DESCRIPTOR_SIGNATURE) {
            int flags = Util.asUnsignedByte(buffer.get());
            SedoricDescriptor descriptor = builder.withExecutable((flags & 0x01) != 0)
                    .withBlock((flags & 0x40) != 0)
                    .withStartAddress(Util.asUnsignedShort(buffer.getShort()))
                    .withEndAddress(Util.asUnsignedShort(buffer.getShort()))
                    .withExecAddress(Util.asUnsignedShort(buffer.getShort()))
                    .withSectors(Util.asUnsignedShort(buffer.getShort()))
                    .build();
            return fillFileDescriptors(descriptor, descriptorTrack, descriptorSector, disk);
        } else {
            throw new IllegalArgumentException("Unexpected descriptor signature");
        }
    }

    @Override
    public String toString() {
        return "SedoricDescriptor{" +
                "startAddress=" + startAddress +
                ", endAddress=" + endAddress +
                ", execAddress=" + execAddress +
                ", executable=" + executable +
                ", block=" + block +
                ", sectors=" + sectors +
                ", fileSectors=" + fileSectors +
                '}';
    }
}
