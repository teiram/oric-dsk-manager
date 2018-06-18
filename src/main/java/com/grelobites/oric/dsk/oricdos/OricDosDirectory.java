package com.grelobites.oric.dsk.oricdos;

import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.DiskGeometry;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import com.grelobites.oric.dsk.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class OricDosDirectory {
    private static final Logger LOGGER = LoggerFactory.getLogger(OricDosDirectory.class);
    private static final int DIRECTORY_SIZE = 16;
    private static final byte DESCRIPTOR_ID = (byte) 0xff;

    private String name;
    private String extension;
    private SectorCoordinates firstSector;
    private SectorCoordinates lastSector;
    private int sectors;
    private int flags;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private OricDosDirectory directory = new OricDosDirectory();

        public Builder withName(String name) {
            directory.setName(name);
            return this;
        }

        public Builder withExtension(String extension) {
            directory.setExtension(extension);
            return this;
        }

        public Builder withFirstSector(SectorCoordinates firstSector) {
            directory.setFirstSector(firstSector);
            return this;
        }

        public Builder withLastSector(SectorCoordinates lastSector) {
            directory.setLastSector(lastSector);
            return this;
        }

        public Builder withSectors(int sectors) {
            directory.setSectors(sectors);
            return this;
        }

        public Builder withFlags(int flags) {
            directory.setFlags(flags);
            return this;
        }

        public OricDosDirectory build() {
            return directory;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }


    public int getSectors() {
        return sectors;
    }

    public void setSectors(int sectors) {
        this.sectors = sectors;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public SectorCoordinates getFirstSector() {
        return firstSector;
    }

    public void setFirstSector(SectorCoordinates firstSector) {
        this.firstSector = firstSector;
    }

    public SectorCoordinates getLastSector() {
        return lastSector;
    }

    public void setLastSector(SectorCoordinates lastSector) {
        this.lastSector = lastSector;
    }

    public Optional<SedoricArchive> getArchive(Disk disk) {
        LOGGER.debug("Getting archive for directory {}", this);
        try {
            byte[] data = disk.getSectorFromEncodedTrack(firstSector);
            int startAddress;
            int endAddress;
            int execAddress;
            int sectorDataBytes;
            int dataOffset;
            if (data[2] == DESCRIPTOR_ID) {
                ByteBuffer buffer = ByteBuffer.wrap(data, 4, 7)
                        .order(ByteOrder.LITTLE_ENDIAN);
                LOGGER.debug("Found a header sector with data {}",
                        Util.dumpAsHexString(buffer.array()));
                startAddress = Util.asUnsignedShort(buffer.getShort());
                endAddress = Util.asUnsignedShort(buffer.getShort());
                execAddress = Util.asUnsignedShort(buffer.getShort());
                sectorDataBytes = Util.asUnsignedByte(buffer.get());
                dataOffset = 11;
                //Handle special execAddress cases
                execAddress = execAddress < 3 ? startAddress : execAddress;
            } else {
                sectorDataBytes = Util.asUnsignedByte(data[2]);
                LOGGER.debug("Found raw sector with {} data bytes",
                        sectorDataBytes);
                startAddress = 0;
                execAddress = 0;
                endAddress = sectors * 253;
                dataOffset = 3;
            }
            LOGGER.debug("File parameters. StartAddress={}, EndAddress={}, ExecAddress={}, SectorDataBytes={}",
                    String.format("0x%04x", startAddress),
                    String.format("0x%04x", endAddress),
                    String.format("0x%04x", execAddress),
                    sectorDataBytes);
            int nextTrack = Util.asUnsignedByte(data[0]);
            int nextSector = Util.asUnsignedByte(data[1]);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            do {
                bos.write(Arrays.copyOfRange(data, dataOffset,
                        dataOffset + sectorDataBytes));
                if (nextSector != 0) {
                    data = disk.getSectorFromEncodedTrack(SectorCoordinates.newBuilder()
                            .withTrack(nextTrack)
                            .withSector(nextSector)
                            .build());
                    dataOffset = 3;
                    nextTrack = Util.asUnsignedByte(data[0]);
                    nextSector = Util.asUnsignedByte(data[1]);
                    sectorDataBytes = Util.asUnsignedByte(data[2]);
                }
            } while (nextSector != 0);

            LOGGER.debug("Got byte array of length {} for directory {}",
                    bos.size(), this);

            SedoricArchive archive = new SedoricArchive(name, extension, bos.toByteArray());
            archive.setSize(bos.size());
            archive.setLoadAddress(startAddress);
            archive.setExecAddress(execAddress);
            archive.setBlockAttribute(false);
            archive.setExecutableAttribute(false);

            return Optional.of(archive);
        } catch (Exception e) {
            LOGGER.warn("Trying to get archive from directory {}", e);
            return Optional.empty();
        }
    }

    private static void addDirectoryEntries(List<OricDosDirectory> list, byte[] sector,
                                            int offset, int size) {
        LOGGER.debug("addDirectoryEntries with offset {} and size {}", offset, size);
        while (offset < size && sector[offset] != 0) {
            LOGGER.debug("Iteration with offset {} for a sector of size {}", offset, sector.length);
            ByteBuffer buffer = ByteBuffer.wrap(sector, offset, DIRECTORY_SIZE);
            byte[] nameBytes = new byte[6];
            byte[] extensionBytes = new byte[3];
            buffer.get(nameBytes);
            if (nameBytes[0] != (byte) 0) {
                buffer.get(extensionBytes);
                OricDosDirectory directory = OricDosDirectory.newBuilder()
                        .withName(new String(nameBytes).trim())
                        .withExtension(new String(extensionBytes).trim())
                        .withSectors(Util.asUnsignedShort(buffer.getShort()))
                        .withFirstSector(SectorCoordinates.newBuilder()
                                .withSector(Util.asUnsignedByte(buffer.get()))
                                .withTrack(Util.asUnsignedByte(buffer.get()))
                                .build())
                        .withLastSector(SectorCoordinates.newBuilder()
                                .withSector(Util.asUnsignedByte(buffer.get()))
                                .withTrack(Util.asUnsignedByte(buffer.get()))
                                .build())
                        .withFlags(Util.asUnsignedByte(buffer.get()))
                        .build();
                LOGGER.debug("Adding directory entry " + directory);
                list.add(directory);
            }
            offset += DIRECTORY_SIZE;
        }
    }

    public static List<OricDosDirectory> fromDisk(Disk disk) throws IOException {
        List<OricDosDirectory> result = new ArrayList<>();
        OricDosSystemSector systemSector = OricDosSystemSector.fromDisk(disk);
        LOGGER.debug("System sector is {}", systemSector);
        int track = systemSector.getDirectoryCoordinates().getTrack();
        int sector = systemSector.getDirectoryCoordinates().getSector();
        final DiskGeometry geometry = disk.getGeometry();
        do {
            byte[] sectorData = disk.getSectorFromEncodedTrack(new SectorCoordinates(track, sector));
            addDirectoryEntries(result, sectorData, 3, geometry
                    .getTrackGeometry(geometry.decodeTrack(track)).getSectorSize());
            LOGGER.debug("Sector {}, {} points to Sector {}, {}", track, sector,
                    sectorData[0] & 0xff, sectorData[1] & 0xff);
            track = sectorData[0] & 0xff;
            sector = sectorData[1] & 0xff;

            LOGGER.debug("File count is {}", result.size());
        } while (track != 0);
        return result;
    }

    @Override
    public String toString() {
        return "OricDosDirectory{" +
                "name='" + name + '\'' +
                ", extension='" + extension + '\'' +
                ", firstSector=" + firstSector +
                ", lastSector=" + lastSector +
                ", sectors=" + sectors +
                ", flags=" + flags +
                '}';
    }
}
