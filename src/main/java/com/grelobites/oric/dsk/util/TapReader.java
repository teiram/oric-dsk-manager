package com.grelobites.oric.dsk.util;

import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class TapReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapReader.class);
    private static final String DEFAULT_TAPNAME = "NONAMED";
    private InputStream stream;
    private int currentValue;

    public TapReader(InputStream stream) {
        this.stream = stream;
        this.currentValue = 0;
    }

    public boolean hasNext() throws IOException {
        int value;
        do {
            value = stream.read();
            if (value == -1) {
                LOGGER.info("Reached tap stream EOF");
                return false;
            }
        } while (value != 0x24);
        return true;
    }

    private static Pair<String, String> decodeName(byte[] header, byte[] name) {
        LOGGER.debug("decodeName {}, {}", Util.dumpAsHexString(header),
                Util.dumpAsHexString(name));
        int index = 0;
        while (index < name.length) {
            if (name[index] == 0) {
                break;
            }
            index++;
        }
        String tapName = new String(Arrays.copyOfRange(name, 0, index));
        if (tapName.length() == 0) {
            tapName = DEFAULT_TAPNAME;
        }

        String tapExtension = (header[3] != 0) ? "COM" :
                (header[2] != 0) ? "BIN": "BAS";
        return new Pair<>(tapName, tapExtension);
    }

    private static byte[] nameFromStream(InputStream stream) throws IOException {
        byte name[] = new byte[17];
        for (int i = 0; i < name.length; i++) {
            int value = stream.read();
            if (value == -1) {
                throw new IOException("Stream exhausted");
            }
            name[i] = (byte) value;
            if (value == 0) break;
        }
        return name;
    }

    public SedoricArchive getNext() throws IOException {
        byte header[] = Util.fromInputStream(stream, 9);
        int start = (header[6] & 0xFF) << 8 | (header[7] & 0xFF);
        int end = (header[4] & 0xFF) << 8 | (header[5] & 0xFF);

        LOGGER.debug("Calculated start: {}, end: {}", start, end);
        Pair<String, String> name = decodeName(header, nameFromStream(stream));
        byte[] data = Util.fromInputStream(stream, end + 1 - start);
        SedoricArchive archive = new SedoricArchive(name.left(), name.right(), data);
        archive.setLoadAddress(start);
        archive.setExecutableAttribute(header[3] != 0);
        archive.setBlockAttribute(header[2] != 0);
        if (archive.isExecutableAttribute()) {
            archive.setExecAddress(archive.getLoadAddress());
        }
        LOGGER.debug("Created Archive from TAP " + archive);
        return archive;
    }
}
