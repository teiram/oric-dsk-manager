package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DirectoryWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryWriter.class);
    private static final int DIRECTORY_SIZE = 16;
    private Disk disk;
    private SedoricBitmap bitmap;
    private SectorCoordinates currentSector;
    private int position;
    private byte[] sectorData;
    private int sectorCount = 0;
    private int directoryCount = 0;

    public DirectoryWriter(Disk disk, SedoricBitmap bitmap) {
        this.disk = disk;
        this.bitmap = bitmap;
        this.position = DIRECTORY_SIZE;
        this.currentSector = new SectorCoordinates(Constants.SEDORIC_DIRECTORY_TRACK,
                Constants.SEDORIC_DIRECTORY_SECTOR);
        bitmap.allocateSector(currentSector);
        sectorData = disk.getSector(currentSector);
        sectorData[2] = (byte) position;
        sectorCount = 1;
    }

    public void write(SedoricDirectory directory) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        directory.dump(stream);
        System.arraycopy(stream.toByteArray(), 0, sectorData, position, DIRECTORY_SIZE);
        position += DIRECTORY_SIZE;
        sectorData[2] = (byte) position;
        directoryCount++;
        if (position >= sectorData.length) {
            LOGGER.debug("Allocating new directory sector");
            currentSector = bitmap.getFreeSector();
            sectorData[0] = (byte) currentSector.getTrack();
            sectorData[1] = (byte) currentSector.getSector();
            sectorData = disk.getSector(currentSector);
            position = DIRECTORY_SIZE;
            sectorCount++;
        }
    }

    public int sectorCount() {
        return sectorCount;
    }

    public int directoryCount() {
        return directoryCount;
    }

}
