package com.grelobites.oric.dsk.util;

import com.grelobites.oric.dsk.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    public static String stripSuffix(String value, String suffix) {
        int index;
        if ((index = value.lastIndexOf(suffix)) > -1) {
            return value.substring(0, index);
        } else {
            return value;
        }
    }

    public static String stripSnapshotVersion(String value) {
        return stripSuffix(value, SNAPSHOT_SUFFIX);
    }

    public static Optional<String> getFileExtension(String fileName) {
        int index;
        if ((index = fileName.lastIndexOf('.')) > -1) {
            return Optional.of(fileName.substring(index + 1));
        } else {
            return Optional.empty();
        }
    }

    public static byte[] fromInputStream(InputStream is, int size) throws IOException {
        byte[] result = new byte[size];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(result);
        return result;
    }

    public static byte[] fromInputStream(InputStream is) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int nread;
            while ((nread = is.read(buffer)) != -1) {
                out.write(buffer, 0, nread);
            }
            out.flush();
            out.close();
            return out.toByteArray();
        }
    }

    public static byte[] paddedByteArray(byte[] source, int from, int length, byte filler) {
        LOGGER.debug("paddedByteArray from array of length " + source.length + " from " + from
            + ", length = " + length);
        byte[] result = new byte[length];
        Arrays.fill(result, filler);
        System.arraycopy(source, from, result, 0, Math.min(source.length - from, length));
        return result;
    }

    public static byte[] paddedByteArray(byte[] source, int length, byte filler) {
        return paddedByteArray(source, 0, length, filler);
    }

    public static String spacePadding(int n) {
        return (n == 0 ? "" : String.format("%"+n+"s", " "));
    }

    public static String dumpAsHexString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (byte value: byteArray) {
            sb.append("0x").append(String.format("%02X", value)).append(" ");
        }
        sb.append(" ]");
        return sb.toString();
    }

    public static String toHexString(Integer value) {
        if (value != null) {
            return String.format("0x%04x", value);
        } else {
            return "null";
        }
    }

    public static byte[] pad(byte[] bytes, int i, byte c) {
        if (bytes.length > i) {
            return Arrays.copyOf(bytes, i);
        } else if (bytes.length < i) {
            byte[] newArray = Arrays.copyOf(bytes, i);
            for (int j = bytes.length; j < i; j++) {
                newArray[j] = c;
            }
            return newArray;
        } else {
            return bytes;
        }
    }

    public static int asUnsignedByte(byte value) {
        return (int) value & 0xff;
    }

    public static int asUnsignedShort(short value) {
        return (int) value & 0xffff;
    }

    public static byte[] filledByteArray(int size, byte value) {
        byte[] result = new byte[size];
        Arrays.fill(result, value);
        return result;
    }

    public static Optional<Integer> decodeAddress(String value) {
        try {
            int intValue = Integer.decode(value);
            if (intValue >= 0 && intValue < 0x10000) {
                return Optional.of(intValue);
            }
        } catch (Exception e) {}
        return Optional.empty();
    }

    //Calculates a CRC16 value for the bytes between the current position - length
    //and the current position of the ByteBuffer argument
    public static short crc16(ByteBuffer buffer, int length) {
	    int crc = 0xFFFF;

	    for (int i = buffer.position() - length; i < buffer.position(); i++) {
	        int offset = ((crc >> 8) ^ buffer.get(i)) & 0xff;
		    crc = (crc << 8) ^ Constants.CRCTAB[offset];
	    }
	    return Integer.valueOf(crc).shortValue();
    }
}
