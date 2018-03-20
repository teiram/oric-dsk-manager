package com.grelobites.oric.dsk.util;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.*;
import com.grelobites.oric.dsk.sedoric.DskHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;

public class DskUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskUtil.class);


    public static boolean isOricPlainDskFile(File dskFile) {
        return hasExpectedSignature(dskFile, new String[]{Constants.PLAIN_DSK_SIGNATURE});
    }

    public static boolean isOricMfmDskFile(File dskFile) {
        return hasExpectedSignature(dskFile, new String[]{Constants.NEW_DSK_SIGNATURE});
    }

    private static boolean hasExpectedSignature(DskHeader header, String[] signatures) {
        for (String signature : signatures) {
            if (signature.equals(header.getSignature())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExpectedSignature(File dskFile, String[] signatures) {
        try (FileInputStream fis = new FileInputStream(dskFile)) {
            DskHeader header =
                    DskHeader.fromInputStream(fis);
            return hasExpectedSignature(header, signatures);
        } catch (Exception e) {
            return false;
        }
    }

    public static Disk diskFromDskStream(InputStream stream) throws IOException {
        DskHeader header = DskHeader.fromInputStream(stream);
        LOGGER.debug("Header is " + header);
        if (hasExpectedSignature(header, new String[]{
                Constants.NEW_DSK_SIGNATURE,
                Constants.PLAIN_DSK_SIGNATURE})) {
            return DskReaderFactory.getDskReader(header.getFileType())
                    .fromDsk(stream, header);
        } else {
            throw new IllegalArgumentException("Not a DSK stream");
        }
    }

    private static int encodeSectorSize(int sectorSize) {
        return sectorSize == 256 ? 1 : 2;
    }

    public static void dumpAsMfm(Disk disk, OutputStream os) throws IOException {
        DiskGeometry diskGeometry = disk.getGeometry();
        DskHeader header = DskHeader.newBuilder()
                .withTracks(diskGeometry.getTrackCount())
                .withSides(diskGeometry.getSideCount())
                .withSignature(Constants.NEW_DSK_SIGNATURE)
                .withGeometry(1).build();
        header.dump(os);

        for (int track = 0; track < diskGeometry.getTrackCount() * diskGeometry.getSideCount(); track++) {
            TrackGeometry trackGeometry = diskGeometry.getTrackGeometry(track);
            LOGGER.debug("Dumping track with geometry {}", trackGeometry);
            ByteBuffer buffer = ByteBuffer.wrap(new byte[6400]);
            buffer.put(Util.filledByteArray(trackGeometry.getTrackLead(), (byte) 0x4E));

            for (int sector = 0; sector < trackGeometry.getSectorCount(); sector++) {
                buffer.put(Util.filledByteArray(12, (byte) 0))
                        .put(Util.filledByteArray(3, (byte) 0xA1))
                        .put((byte) 0xFE)
                        .put((byte) (track))
                        .put((byte) trackGeometry.getSide())
                        .put((byte) (sector + 1))
                        .put((byte) encodeSectorSize(trackGeometry.getSectorSize()))
                        .putShort(Util.crc16(buffer, 8))
                        .put(Util.filledByteArray(trackGeometry.getGap2(), (byte) 0x22))
                        //.put(Util.filledByteArray(trackGeometry.getGap2(), (byte) 0x4E))

                        .put(Util.filledByteArray(12, (byte) 0))
                        .put(Util.filledByteArray(3, (byte) 0xA1))
                        .put((byte) 0xFB)
                        .put(disk.getSector(new SectorCoordinates(track, sector + 1)))
                        .putShort(Util.crc16(buffer, trackGeometry.getSectorSize() + 4))
                        .put(Util.filledByteArray(trackGeometry.getGap3(), (byte) 0x4E));
            }
            buffer.put(Util.filledByteArray(6400 - buffer.position(), (byte) 0x4E));
            os.write(buffer.array());
        }

    }
}
