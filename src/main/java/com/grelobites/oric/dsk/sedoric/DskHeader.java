package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.util.FileType;
import com.grelobites.oric.dsk.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DskHeader {

    private String signature;
    private int tracks;
    private Integer sectors;
    private Integer geometry;
    private int sides;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private DskHeader header = new DskHeader();

        public Builder withSignature(String signature) {
            header.setSignature(signature);
            return this;
        }

        public Builder withTracks(int tracks) {
            header.setTracks(tracks);
            return this;
        }

        public Builder withSectors(int sectors) {
            header.setSectors(sectors);
            return this;
        }

        public Builder withSides(int sides) {
            header.setSides(sides);
            return this;
        }

        public Builder withGeometry(int geometry) {
            header.setGeometry(geometry);
            return this;
        }

        public DskHeader build() {
            return header;
        }

    }

    public static DskHeader fromInputStream(InputStream stream) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(stream, Constants.SECTOR_SIZE))
                .order(ByteOrder.LITTLE_ENDIAN);
        byte[] signatureBytes = new byte[Constants.SIGNATURE_SIZE];
        buffer.get(signatureBytes);
        String signature = new String(signatureBytes);
        Builder builder = newBuilder().withSignature(signature)
                .withSides(buffer.getInt())
                .withTracks(buffer.getInt());
        if (Constants.PLAIN_DSK_SIGNATURE.equals(signature)) {
            builder.withSectors(buffer.getInt());
        } else if (Constants.NEW_DSK_SIGNATURE.equals(signature)) {
            builder.withGeometry(buffer.getInt());
        } else {
            throw new IllegalArgumentException("Invalid signature found: " + signature);
        }
        return builder.build();
    }

    public void dump(OutputStream stream) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.SECTOR_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        String trimmedSignature = signature.substring(0, Constants.SIGNATURE_SIZE);
        buffer.put(trimmedSignature.getBytes())
                .putInt(getSides())
                .putInt(getTracks())
                .putInt(Constants.PLAIN_DSK_SIGNATURE.equals(trimmedSignature) ?
                        getSectors() : getGeometry());
        stream.write(buffer.array());
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getTracks() {
        return tracks;
    }

    public void setTracks(int tracks) {
        this.tracks = tracks;
    }

    public Integer getSectors() {
        return sectors;
    }

    public void setSectors(int sectors) {
        this.sectors = sectors;
    }

    public int getSides() {
        return sides;
    }

    public void setSides(int sides) {
        this.sides = sides;
    }

    public Integer getGeometry() {
        return geometry;
    }

    public void setGeometry(Integer geometry) {
        this.geometry = geometry;
    }

    public FileType getFileType() {
        return Constants.PLAIN_DSK_SIGNATURE.equals(signature) ?
                FileType.PLAINDSK : FileType.MFMDSK;
    }

    @Override
    public String toString() {
        return "DskHeader{" +
                "signature='" + signature + '\'' +
                ", tracks=" + tracks +
                ", sectors=" + sectors +
                ", geometry=" + geometry +
                ", sides=" + sides +
                '}';
    }
}
