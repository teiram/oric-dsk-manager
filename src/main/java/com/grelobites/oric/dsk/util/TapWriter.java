package com.grelobites.oric.dsk.util;

import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TapWriter {
    private static final int HEADER_LENGTH = 13;
    private static final int MAX_NAME_LENGTH = 16;
    private static final byte ONE_VALUE = Integer.valueOf(0x80).byteValue();
    private static final byte ZERO_VALUE = Integer.valueOf(0).byteValue();
    private static final byte SYNC_VALUE = Integer.valueOf(0x16).byteValue();
    private static final byte START_VALUE = Integer.valueOf(0x24).byteValue();

    private static final Logger LOGGER = LoggerFactory.getLogger(TapWriter.class);
    private OutputStream stream;
    private int currentValue;

    public TapWriter(OutputStream stream) {
        this.stream = stream;
        this.currentValue = 0;
    }

    public void write(SedoricArchive archive) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH)
                .order(ByteOrder.BIG_ENDIAN);
        buffer.put(SYNC_VALUE);
        buffer.put(SYNC_VALUE);
        buffer.put(SYNC_VALUE);
        buffer.put(START_VALUE);
        buffer.put(ZERO_VALUE);
        buffer.put(ZERO_VALUE);
        buffer.put(archive.getBlockAttribute() ? ONE_VALUE : ZERO_VALUE);
        buffer.put(archive.getExecutableAttribute() ? ONE_VALUE : ZERO_VALUE);
        buffer.putShort(Integer.valueOf(archive.getSize() + archive.getLoadAddress() - 1).shortValue());
        buffer.putShort(Integer.valueOf(archive.getLoadAddress()).shortValue());
        buffer.put(ZERO_VALUE);
        stream.write(buffer.array()
        );
        stream.write(archive.getName()
                .substring(0, Math.min(MAX_NAME_LENGTH, archive.getName().length())).getBytes());
        stream.write(ZERO_VALUE);
        stream.write(archive.getData(), 0, archive.getSize());
    }

}
