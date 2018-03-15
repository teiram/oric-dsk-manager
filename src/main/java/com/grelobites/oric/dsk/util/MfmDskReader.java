package com.grelobites.oric.dsk.util;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.*;
import com.grelobites.oric.dsk.sedoric.DskHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MfmDskReader implements DskReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MfmDskReader.class);

    private enum State {
        TRACK_LEAD,
        METADATA_ZEROES_LEAD,
        A1_METADATA_MARK,
        FE_MARK,
        GAP2,
        DATA_ZEROES_LEAD,
        A1_DATA_MARK,
        FB_MARK,
        GAP3
    }

    private static byte[][] orderedTrackSectors(Map<Integer, byte[]> data) {
        List<byte[]> orderedSectors = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_SECTORS_PER_TRACK; i++) {
            if (data.containsKey(i)) {
                orderedSectors.add(data.get(i));
            }
        }
        return orderedSectors.toArray(new byte[0][0]);
    }

    @Override
    public Disk fromDsk(InputStream stream, DskHeader header) throws IOException {
        ComplexDiskGeometry geometry = ComplexDiskGeometry.newBuilder()
                .withSideCount(header.getSides())
                .withTrackCount(header.getTracks())
                .withGeometryId(header.getGeometry())
                .build();
        LOGGER.debug("DSK header: " + header);
        Track[] tracks = new Track[geometry.getTrackCount() * geometry.getSideCount()];
        for (int i = 0; i < tracks.length; i++) {
            ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(stream, Constants.MFM_TRACK_SIZE))
                    .order(ByteOrder.LITTLE_ENDIAN);
            Map<Integer, byte[]> trackData = new HashMap<>();
            State state = State.TRACK_LEAD;
            int counter = 0;
            int trackLeadSize = 0;
            int gap2 = 0;
            int gap3 = 0;
            int trackId = 0;
            int sectorId = 0;
            int sectorSize = 0;
            int side = 0;
            while (buffer.position() < Constants.MFM_TRACK_SIZE) {
                int value = Util.asUnsignedByte(buffer.get());
                switch (state) {
                    case TRACK_LEAD:
                        if (value == 0) {
                            state = State.METADATA_ZEROES_LEAD;
                            trackLeadSize = counter;
                            counter = 0;
                        } else if (value != 0x4E) {
                            LOGGER.warn("Unexpected value during track lead " + Util.toHexString(value));
                        }
                        counter++;
                        break;
                    case METADATA_ZEROES_LEAD:
                        if (value == 0xA1) {
                            state = State.A1_METADATA_MARK;
                            if (counter != 12) {
                                LOGGER.warn("Unexpected size of metadata lead " + counter);
                            }
                            counter = 1;
                        } else if (value == 0) {
                            counter++;
                        } else {
                            LOGGER.warn("Unexpected value during zeroes leading at counter "
                                    + counter + ": " + Util.toHexString(value));
                        }
                        break;
                    case A1_METADATA_MARK:
                        if (value == 0xFE) {
                            if (counter != 3) {
                                LOGGER.warn("Unexpected A1 metadata size " + counter);
                            }
                            counter = 1;
                            state = State.FE_MARK;
                        } else if (value == 0xA1) {
                            counter++;
                        }
                        break;
                    case FE_MARK:
                        trackId = value;
                        side = Util.asUnsignedByte(buffer.get());
                        sectorId = Util.asUnsignedByte(buffer.get());
                        sectorSize = 128 << Util.asUnsignedByte(buffer.get());
                        LOGGER.debug("Sector metadata{trackId: " + trackId
                                + ", side: " + side + ", sectorId: " + sectorId
                                + ", sectorSize: " + sectorSize + "}");
                        buffer.position( ((Buffer) buffer).position() + 2); //Skip CRC
                        state = State.GAP2;
                        counter = 0;
                        break;
                    case GAP2:
                        if (value == 0) {
                            state = State.DATA_ZEROES_LEAD;
                            gap2 = counter;
                        } else if (value != 0x22 && value != 0x4e) {
                            LOGGER.warn("Unexpected value during gap2 " + value);
                        }
                        counter++;
                        break;
                    case DATA_ZEROES_LEAD:
                        if (value == 0xA1) {
                            state = State.A1_DATA_MARK;
                            counter = 1;
                        } else if (value == 0) {
                            counter++;
                        } else {
                            LOGGER.warn("Unexpected value during zeroes leading at "
                                    + counter + ": " + Util.toHexString(value));
                        }
                        break;
                    case A1_DATA_MARK:
                        if (value == 0xFB) {
                            if (counter != 3) {
                                LOGGER.warn("Unexpected A1 data size " + counter);
                            }
                            counter = 1;
                            state = State.FB_MARK;
                        } else if (value == 0xA1) {
                            counter++;
                        }
                        break;
                    case FB_MARK:
                        buffer.position(((Buffer) buffer).position() - 1);
                        byte[] sectorData = new byte[sectorSize];
                        buffer.get(sectorData);
                        trackData.put(sectorId, sectorData);
                        //Skip CRC
                        buffer.position(((Buffer) buffer).position() + 2);
                        state = State.GAP3;
                        counter = 0;
                        break;
                    case GAP3:
                        if (value == 0) {
                            state = State.METADATA_ZEROES_LEAD;
                            gap3 = counter;
                            counter = 0;
                        } else if (value != 0x4E) {
                            LOGGER.warn("Unexpected value during gap3 " + value);
                        }
                        counter++;
                        break;
                }
            }
            tracks[i] = new Track(orderedTrackSectors(trackData));
            TrackGeometry trackGeometry = TrackGeometry.newBuilder()
                    .withTrackLead(trackLeadSize)
                    .withSectorCount(trackData.size())
                    .withSectorSize(sectorSize)
                    .withGap2(gap2)
                    .withGap3(gap3)
                    .build();
            LOGGER.debug("Added track geometry " + trackGeometry);
            geometry.setTrackGeometry(i, trackGeometry);
        }
        return new Disk(tracks, geometry);
    }

}
