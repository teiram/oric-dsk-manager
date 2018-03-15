package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.util.Util;

import java.nio.ByteBuffer;
import java.util.Optional;

public class DirectoryReader {
    private static final int DIRECTORY_SIZE = 16;
    private Disk disk;
    private SectorCoordinates coordinates;

    private int position;
    private byte[] sectorData;

    public DirectoryReader(Disk disk) {
        this.disk = disk;
        this.coordinates = new SectorCoordinates(Constants.SEDORIC_DIRECTORY_TRACK,
                Constants.SEDORIC_DIRECTORY_SECTOR);
        sectorData = disk.getSector(coordinates);
        position = DIRECTORY_SIZE;
    }

    public Optional<SedoricDirectory> next() {
        if (position == sectorData.length) {
            coordinates = new SectorCoordinates(
                    sectorData[0],
                    sectorData[1]
            );
            position = DIRECTORY_SIZE;
            if (!coordinates.isValid()) {
                return Optional.empty();
            } else {
                sectorData = disk.getSector(coordinates);
            }
        }
        ByteBuffer buffer = ByteBuffer.wrap(sectorData, position, position + DIRECTORY_SIZE);
        byte[] nameBytes = new byte[Constants.SEDORIC_FILENAME_MAXLENGTH];
        byte[] extensionBytes = new byte[Constants.SEDORIC_FILEEXTENSION_MAXLENGTH];
        buffer.get(nameBytes).get(extensionBytes);
        return Optional.of(SedoricDirectory.newBuilder()
                .withName(new String(nameBytes))
                .withExtension(new String(extensionBytes))
                .withDescriptorLocation(new SectorCoordinates(
                        Util.asUnsignedByte(buffer.get()),
                        Util.asUnsignedByte(buffer.get())))
                .withSectors(Util.asUnsignedByte(buffer.get()))
                .withFlags(Util.asUnsignedByte(buffer.get()))
                .build());
    }

}
