package com.grelobites.oric.dsk.oricdos;

import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;

public class OricDosSystemSector {
    private static final SectorCoordinates SYSTEM_SECTOR =
            new SectorCoordinates(0, 1);

    private String name;
    private int freeSectors;
    private int busySectors;
    private SectorCoordinates directoryCoordinates;

    public static OricDosSystemSector fromDisk(Disk disk) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(
                disk.getSector(SYSTEM_SECTOR));
        OricDosSystemSector systemSector = new OricDosSystemSector();
        buffer.position(18);
        systemSector.directoryCoordinates = SectorCoordinates.newBuilder()
               .withSector(Util.asUnsignedByte(buffer.get()))
                .withTrack(Util.asUnsignedByte(buffer.get()))
                .build();

        systemSector.freeSectors = Util.asUnsignedShort(buffer.getShort());
        systemSector.busySectors = Util.asUnsignedShort(buffer.getShort());
        byte[] nameArray = new byte[21];
        buffer.get(nameArray);
        systemSector.name = new String(nameArray).trim();

        return systemSector;
    }

    public String getName() {
        return name;
    }

    public int getFreeSectors() {
        return freeSectors;
    }

    public int getBusySectors() {
        return busySectors;
    }

    public SectorCoordinates getDirectoryCoordinates() {
        return directoryCoordinates;
    }

    @Override
    public String toString() {
        return "OricDosSystemSector{" +
                "name='" + name + '\'' +
                ", freeSectors=" + freeSectors +
                ", busySectors=" + busySectors +
                ", directoryCoordinates=" + directoryCoordinates +
                '}';
    }
}
