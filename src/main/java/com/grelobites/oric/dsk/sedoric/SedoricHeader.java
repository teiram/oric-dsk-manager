package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public class SedoricHeader {
    private static final String SIGNATURE = "SEDORIC";
    public static final int HEADER_SIZE = 12;

    private int loadAddress;
    private int execAddress;
    private int flags;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private int loadAddress;
        private int execAddress;
        private int flags;

        public Builder withLoadAddress(int loadAddress) {
            this.loadAddress = loadAddress;
            return this;
        }

        public Builder withExecAddress(int execAddress) {
            this.execAddress = execAddress;
            return this;
        }

        public Builder withFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public SedoricHeader build() {
            SedoricHeader header = new SedoricHeader();
            header.setLoadAddress(loadAddress);
            header.setExecAddress(execAddress);
            header.setFlags(flags);
            return header;
        }
    }
    private static byte flagsByte(SedoricArchive archive) {
        return (byte)
                ((archive.isExecutableAttribute() ? 1 : 0) |
                (archive.isBlockAttribute() ? 0x80 : 0x40));
    }

    public int getLoadAddress() {
        return loadAddress;
    }

    public void setLoadAddress(int loadAddress) {
        this.loadAddress = loadAddress;
    }

    public int getExecAddress() {
        return execAddress;
    }

    public void setExecAddress(int execAddress) {
        this.execAddress = execAddress;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean getBlockAttribute() {
        return (flags & 0x80) != 0;
    }

    public boolean getExecutableAttribute() {
        return (flags & 0x01) != 0;
    }

    public static Optional<SedoricHeader> fromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(fis, HEADER_SIZE))
                    .order(ByteOrder.LITTLE_ENDIAN);
            byte[] signatureBytes = new byte[SIGNATURE.length()];
            buffer.get(signatureBytes);
            String signature = new String(signatureBytes);
            if (SIGNATURE.equals(signature)) {
                return Optional.of(newBuilder()
                        .withLoadAddress(buffer.getShort())
                        .withExecAddress(buffer.getShort())
                        .withFlags(buffer.get())
                        .build());
            } else {
                return Optional.empty();
            }

        } catch (IOException ioe) {
            return Optional.empty();
        }
    }

    public static boolean needsHeader(SedoricArchive archive) {
        return archive.getLoadAddress() != 0 ||
                archive.getExecAddress() != 0 ||
                archive.isExecutableAttribute() ||
                archive.isBlockAttribute();
    }

    public static SedoricHeader forArchive(SedoricArchive archive) {
        SedoricHeader header = new SedoricHeader();
        header.setLoadAddress(archive.getLoadAddress());
        header.setExecAddress(archive.getExecAddress());
        header.setFlags(flagsByte(archive));

        return header;
    }

    public byte[] asByteArray() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[HEADER_SIZE])
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(SIGNATURE.getBytes())
                .putShort((short) loadAddress)
                .putShort((short) execAddress)
                .put((byte) flags);
        return buffer.array();
    }
}
