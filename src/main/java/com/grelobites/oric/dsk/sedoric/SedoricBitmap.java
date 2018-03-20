package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.DiskGeometry;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class SedoricBitmap {
    private static final Logger LOGGER = LoggerFactory.getLogger(SedoricBitmap.class);

    private static final int BITMAP_HEADER_LENGTH = 0x10;
    private DiskGeometry diskGeometry;
    private int[] trackOffsets;
    private byte[] bitmap;
    private int freeSectors = 0;

    public SedoricBitmap(DiskGeometry diskGeometry) {
        this.diskGeometry = diskGeometry;
        int trackCount = diskGeometry.getTrackCount() * diskGeometry.getSideCount();
        trackOffsets = new int[trackCount + 1];
        freeSectors = 0;
        for (int i = 0; i < trackCount; i++) {
            trackOffsets[i] = freeSectors;
            freeSectors += diskGeometry.getTrackGeometry(i).getSectorCount();
            LOGGER.debug("Accumulated sectors after track {} are {}",
                    i, freeSectors);
        }
        trackOffsets[trackOffsets.length - 1] = freeSectors;

        LOGGER.debug("Bitmap with {} sectors", freeSectors);
        int bitmapSize = (freeSectors + 0x07) >> 3;
        LOGGER.debug("Creating a bitmap of {} bytes", bitmapSize);
        bitmap = new byte[bitmapSize];
        Arrays.fill(bitmap, 0, bitmapSize, (byte) 0xff);
    }

    public boolean requiresTwoSectors() {
        return bitmap.length > diskGeometry.getTrackGeometry(Constants.SEDORIC_DIRECTORY_TRACK)
                .getSectorSize() - BITMAP_HEADER_LENGTH;
    }

    public SectorCoordinates  allocateSector(SectorCoordinates sectorCoordinates) {
        LOGGER.debug("allocateSector " + sectorCoordinates);
        if (freeSectors > 0) {
            int correctedSector = sectorCoordinates.getSector() - 1;
            int trackOffset = trackOffsets[sectorCoordinates.getTrack()];
            int bitmapOffset = (trackOffset + correctedSector) >> 3;
            int byteOffset = (trackOffset + correctedSector) & 0x07;
            bitmap[bitmapOffset] &= ~(1 << byteOffset);
            freeSectors--;
            return sectorCoordinates;
        } else {
            throw new IllegalStateException("Bitmap space exhausted");
        }
    }

    private SectorCoordinates fromLinearSector(int linearSector) {
        for (int i = 0; i < trackOffsets.length; i++) {
            if (trackOffsets[i + 1] > linearSector) {
                int track = i;
                int sector = (linearSector - trackOffsets[track]);
                sector += 1;
                LOGGER.debug("Free sector found at ({}, {})", track, sector);
                return new SectorCoordinates(track, sector);
            }
        }
        //We shall be in the last track

        throw new IllegalStateException("No free sector found");
    }

    public SectorCoordinates getFreeSector() {
        if (freeSectors > 0) {
            int firstFreeSector = 0;
            while ((bitmap[firstFreeSector >> 3] &
                    (1 << (firstFreeSector & 0x07))) == 0) {
                firstFreeSector++;
            }
            bitmap[firstFreeSector >> 3] &= ~(1 << (firstFreeSector & 0x07));
            LOGGER.debug("Found first free sector at linear offset {}", firstFreeSector);
            freeSectors--;
            return fromLinearSector(firstFreeSector);
        } else {
            throw new IllegalStateException("Bitmap space exhausted");
        }
    }

    public int freeSectors() {
        return freeSectors;
    }

    private void writeBitmapSector(Disk disk, SectorCoordinates coordinates,
                                   int trackCount, int sectorCount,
                                   int fileCount, int directorySectorCount,
                                   int offset, int size) {
        byte[] sectorData = disk.getSector(coordinates);
        Arrays.fill(sectorData, BITMAP_HEADER_LENGTH, sectorData.length, (byte) 0xFF);
        ByteBuffer buffer = ByteBuffer.wrap(sectorData)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0xff)
                .put((byte) 0)
                .putShort((short) freeSectors)
                .putShort((short) fileCount)
                .put((byte) trackCount)
                .put((byte) sectorCount)
                .put((byte) directorySectorCount)
                .put((byte) (trackCount | (diskGeometry.getSideCount() == 2 ?
                        0x80 : 0x00)))
                .putInt(0)
                .putShort((short) 0)
                .put(bitmap, offset, size);
    }

    public void flush(Disk disk, int fileCount, int directorySectorCount) {
        int trackCount = diskGeometry.getTrackCount();
        int sectorSize = diskGeometry.getTrackGeometry(Constants.SEDORIC_DIRECTORY_TRACK).getSectorSize();

        int remaining = bitmap.length - sectorSize - BITMAP_HEADER_LENGTH;
        LOGGER.debug("Inserting first bitmap  with size {}",
                remaining > 0 ? sectorSize - BITMAP_HEADER_LENGTH : bitmap.length);
        writeBitmapSector(disk,
                new SectorCoordinates(Constants.SEDORIC_DIRECTORY_TRACK,
                        Constants.SEDORIC_BITMAP_SECTOR),
                trackCount, diskGeometry.getTrackGeometry(0).getSectorCount(),
                fileCount, directorySectorCount, 0,
                remaining > 0 ? sectorSize - BITMAP_HEADER_LENGTH : bitmap.length);
        if (remaining > 0) {
            LOGGER.debug("Inserting second bitmap from {} with length {}",
                    sectorSize - BITMAP_HEADER_LENGTH,
                    remaining);
            writeBitmapSector(disk,
                    new SectorCoordinates(Constants.SEDORIC_DIRECTORY_TRACK,
                            Constants.SEDORIC_BITMAP_SECTOR + 1),
                    trackCount, diskGeometry.getTrackGeometry(0).getSectorCount(),
                    fileCount, directorySectorCount,
                    sectorSize - BITMAP_HEADER_LENGTH,
                    remaining);
        }
    }
}
