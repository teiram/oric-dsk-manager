package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SedoricSystemSector {
    private static final Logger LOGGER = LoggerFactory.getLogger(SedoricSystemSector.class);
    private static final byte HEADER[] = new byte[]{
            (byte) 0xD0, (byte) 0xD0, (byte) 0xD0, (byte) 0xD0,	// drive table
	        0,			                                        // keyboard type
	        100, 0, 10, 0	                                    // RENUM parameters
    };

    private static final byte COLOR_MARKER = (byte) 27;
    private static final int COLOR_POSITION = 9;
    private static final int NAME_POSITION = 13;
    private static final int INIT_STRING_POSITION = 30;
    private static final int ESCAPE_OFFSET = 64;
    private String name;
    private Integer paperColor;
    private Integer penColor;
    private String initString;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private SedoricSystemSector sector = new SedoricSystemSector();

        public Builder withName(String name) {
            sector.setName(name);
            return this;
        }

        public Builder withPaperColor(int paperColor) {
            sector.setPaperColor(paperColor);
            return this;
        }

        public Builder withPenColor(int penColor) {
            sector.setPenColor(penColor);
            return this;
        }

        public Builder withInitString(String initString) {
            sector.setInitString(initString);
            return this;
        }

        public SedoricSystemSector build() {
            return sector;
        }
    }

    private static byte unescape(int value) {
        return Integer.valueOf(value - ESCAPE_OFFSET).byteValue();
    }

    private static byte escape(int value) {
        return Integer.valueOf(value + ESCAPE_OFFSET).byteValue();
    }

    public static SedoricSystemSector fromInputStream(InputStream stream) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(stream, Constants.SECTOR_SIZE))
                .order(ByteOrder.LITTLE_ENDIAN);
        LOGGER.debug("Reading system sector from array of size " + buffer.array().length);
        Builder builder = SedoricSystemSector.newBuilder();

        if (buffer.get(COLOR_POSITION) == COLOR_MARKER) {
            builder.withPaperColor(unescape(buffer.get(COLOR_POSITION + 1)));
            builder.withPenColor(unescape(buffer.get(COLOR_POSITION + 3)));
            buffer.position(NAME_POSITION);
            byte[] name = new byte[17];
            buffer.get(name);
            builder.withName(new String(name));
        } else {
            buffer.position(COLOR_POSITION);
            byte[] name = new byte[21];
            buffer.get(name);
            builder.withName(new String(name));
        }

        buffer.position(INIT_STRING_POSITION);
        byte[] initString = new byte[60];
        buffer.get(initString);
        builder.withInitString(new String(initString));
        return builder.build();
    }

    public void dump(Disk disk) throws IOException {
        final SectorCoordinates systemSector = new SectorCoordinates(
                Constants.SEDORIC_DIRECTORY_TRACK,
                Constants.SEDORIC_SYSTEM_SECTOR);
        ByteBuffer buffer = ByteBuffer.allocate(disk.getGeometry()
                .getTrackGeometry(systemSector.getTrack()).getSectorSize())
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(HEADER);
        boolean hasColors = paperColor != Constants.DEFAULT_PAPER_COLOR;
        if (hasColors) {
            buffer.put(COLOR_MARKER)
                .put(escape(paperColor))
                .put(COLOR_MARKER)
                .put(escape(penColor));
        }
        buffer.put(Util.pad(name.getBytes(), hasColors ? 17: 21, (byte) ' '));
        buffer.put(Util.pad(initString.getBytes(), 60, (byte) ' '));
        byte[] sectorData = disk.getSector(systemSector);
        System.arraycopy(buffer.array(), 0, sectorData, 0, buffer.array().length);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPaperColor() {
        return paperColor;
    }

    public void setPaperColor(int paperColor) {
        this.paperColor = paperColor;
    }

    public Integer getPenColor() {
        return penColor;
    }

    public void setPenColor(int penColor) {
        this.penColor = penColor;
    }

    public String getInitString() {
        return initString;
    }

    public void setInitString(String initString) {
        this.initString = initString;
    }

    @Override
    public String toString() {
        return "SedoricSystemSector{" +
                "name='" + name + '\'' +
                ", paperColor=" + paperColor +
                ", penColor=" + penColor +
                ", initString='" + initString + '\'' +
                '}';
    }
}
