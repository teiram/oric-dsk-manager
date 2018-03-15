package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.SectorCoordinates;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class DescriptorWriter {
    private static final int HEADER_LENGTH = 0x0c;
    private Disk disk;
    private SedoricBitmap bitmap;

    public DescriptorWriter(Disk disk, SedoricBitmap bitmap) {
        this.disk = disk;
        this.bitmap = bitmap;
    }

    private static void writeDescriptorData(byte[] sectorData, SedoricDescriptor descriptor) {
        ByteBuffer buffer = ByteBuffer.wrap(sectorData, 0, HEADER_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(2); //Skip pointer to next sector
        buffer.put((byte) 0xFF)
                .put((byte) ((descriptor.isBlock() ? 0x40: 0x80) |
                        (descriptor.isExecutable() ? 1 : 0)))
                .putShort((short) descriptor.getStartAddress())
                .putShort((short) descriptor.getEndAddress())
                .putShort((short) descriptor.getExecAddress())
                .putShort((short) descriptor.getSectors());
    }

    public void write(SedoricDirectory directory, SedoricDescriptor descriptor) {
        List<SectorCoordinates> sectorList = new ArrayList<>();
        int remaining = descriptor.getEndAddress() - descriptor.getStartAddress();
        int sectors = 0;
        while (remaining > 0) {
            SectorCoordinates position = bitmap.getFreeSector();
            remaining -= disk.getSector(position).length;
            sectorList.add(position);
            sectors++;
        }
        descriptor.setSectors(sectors);
        descriptor.setFileSectors(sectorList);
        int descriptorSectors = 1;
        SectorCoordinates descriptorLocation = bitmap.getFreeSector();
        directory.setDescriptorLocation(descriptorLocation);
        byte[] sectorData = disk.getSector(descriptorLocation);
        writeDescriptorData(sectorData, descriptor);
        int offset = 0x0c;

        for (SectorCoordinates item : sectorList) {
            sectorData[offset++] = (byte) item.getTrack();
            sectorData[offset++] = (byte) item.getSector();
            if (offset >= sectorData.length) {
                offset = 2;
                descriptorSectors++;
                descriptorLocation = bitmap.getFreeSector();
                sectorData[0] = (byte) descriptorLocation.getTrack();
                sectorData[1] = (byte) descriptorLocation.getSector();
                sectorData = disk.getSector(descriptorLocation);
            }
        }
        directory.setSectors(sectors + descriptorSectors);
    }

}
