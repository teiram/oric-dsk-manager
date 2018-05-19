package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.DiskGeometry;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

    /*
       - Base offset: 0x10 (Sector start)
            - Offset 0x00 - Track of the next directory entry
            - Offset 0x01 - Sector of the next directory entry
       - Each file occupies 0x10 bytes
       - Name               at 0x00 - 0x07
       - Extension          at 0x09 - 0x0b
       - Descriptor track   at 0x0c
       - Descriptor sector  at 0x0d
       - Number of sectors  at 0x0e
       - Status             at 0x0f
     */

public class SedoricDirectory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SedoricDirectory.class);
    private static final int DIRECTORY_SIZE = 16;
    private static final byte PADDING_BYTE = (byte) ' ';

    private String name;
    private String extension;
    private SectorCoordinates descriptorLocation;
    private int sectors;
    private int flags;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private SedoricDirectory directory = new SedoricDirectory();

        public Builder withName(String name) {
            directory.setName(name);
            return this;
        }

        public Builder withExtension(String extension) {
            directory.setExtension(extension);
            return this;
        }

        public Builder withDescriptorLocation(SectorCoordinates descriptorLocation) {
            directory.setDescriptorLocation(descriptorLocation);
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

        public SedoricDirectory build() {
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

    public SectorCoordinates getDescriptorLocation() {
        return descriptorLocation;
    }

    public void setDescriptorLocation(SectorCoordinates descriptorLocation) {
        this.descriptorLocation = descriptorLocation;
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

    public void dump(OutputStream stream) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[DIRECTORY_SIZE])
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Util.paddedByteArray(name.getBytes(), 9, PADDING_BYTE));
        buffer.put(Util.paddedByteArray(extension.getBytes(), 3, PADDING_BYTE));
        buffer.put(Integer.valueOf(descriptorLocation.getTrack()).byteValue());
        buffer.put(Integer.valueOf(descriptorLocation.getSector()).byteValue());
        buffer.put(Integer.valueOf(sectors).byteValue());
        buffer.put(Integer.valueOf(flags).byteValue());
        stream.write(buffer.array());
    }

    public SedoricArchive getArchive(Disk disk) {
        LOGGER.debug("Getting archive for directory {}", this);
        SedoricDescriptor descriptor = SedoricDescriptor.forSector(
                descriptorLocation.getTrack(),
                descriptorLocation.getSector(),
                disk);
        LOGGER.debug("Got descriptor {} with sector count {}",
                descriptor, descriptor.getFileSectors().size());
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        descriptor.getFileSectors().forEach(c -> {
            try {
                data.write(disk.getSectorFromEncodedTrack(new SectorCoordinates(c.getTrack(), c.getSector())));
            } catch (IOException ioe) {
                LOGGER.error("Writing data to file buffer", ioe);
            }
        });
        SedoricArchive archive = new SedoricArchive(name, extension, data.toByteArray());
        archive.setSize(descriptor.getEndAddress() - descriptor.getStartAddress() + 1);
        archive.setLoadAddress(descriptor.getStartAddress());
        archive.setExecAddress(descriptor.getExecAddress());
        archive.setBlockAttribute(descriptor.isBlock());
        archive.setExecutableAttribute(descriptor.isExecutable());

        return archive;
    }

    private static void addDirectoryEntries(List<SedoricDirectory> list, byte[] sector,
                                            int offset, int size) {
        LOGGER.debug("addDirectoryEntries with offset {} and size {}", offset, size);
        while (offset < size && sector[offset] != 0) {
            LOGGER.debug("Iteration with offset {} for a sector of size {}", offset, sector.length);
            ByteBuffer buffer = ByteBuffer.wrap(sector, offset, DIRECTORY_SIZE);
            byte[] nameBytes = new byte[Constants.SEDORIC_FILENAME_MAXLENGTH];
            byte[] extensionBytes = new byte[Constants.SEDORIC_FILEEXTENSION_MAXLENGTH];
            buffer.get(nameBytes).get(extensionBytes);
            SedoricDirectory directory = SedoricDirectory.newBuilder()
                    .withName(new String(nameBytes).trim())
                    .withExtension(new String(extensionBytes).trim())
                    .withDescriptorLocation(new SectorCoordinates(
                            Util.asUnsignedByte(buffer.get()),
                            Util.asUnsignedByte(buffer.get())))
                    .withSectors(Util.asUnsignedByte(buffer.get()))
                    .withFlags(Util.asUnsignedByte(buffer.get()))
                    .build();
            LOGGER.debug("Adding directory entry " + directory);
            list.add(directory);
            offset += DIRECTORY_SIZE;
        }
    }

    public static List<SedoricDirectory> fromDisk(Disk disk) {
        List<SedoricDirectory> result = new ArrayList<>();
        int track = Constants.SEDORIC_DIRECTORY_TRACK;
        int sector = Constants.SEDORIC_DIRECTORY_SECTOR;
        final DiskGeometry geometry = disk.getGeometry();
        do {
            byte[] sectorData = disk.getSectorFromEncodedTrack(new SectorCoordinates(track, sector));
            addDirectoryEntries(result, sectorData, DIRECTORY_SIZE, geometry
                    .getTrackGeometry(geometry.decodeTrack(track)).getSectorSize());
            LOGGER.debug("Sector {}, {} points to Sector {}, {}", track, sector, sectorData[0] & 0xff, sectorData[1] & 0xff);
            track = sectorData[0] & 0xff;
            sector = sectorData[1] & 0xff;

            LOGGER.debug("File count is {}", result.size());
        } while (track != 0);
        return result;
    }

    @Override
    public String toString() {
        return "SedoricDirectory{" +
                "name='" + name + '\'' +
                ", extension='" + extension + '\'' +
                ", descriptorLocation=" + descriptorLocation +
                ", sectors=" + sectors +
                ", flags=" + flags +
                '}';
    }
}
