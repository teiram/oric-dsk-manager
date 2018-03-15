package com.grelobites.oric.dsk.model;

public class Track {
    private byte[][] data;

    public Track(byte[][] data) {
        this.data = data;
    }

    public Track(int sectorCount, int sectorSize) {
        data = new byte[sectorCount][];
        for (int i = 0; i < sectorCount; i++) {
            data[i] = new byte[sectorSize];
        }
    }

    public byte[] getSector(int sector) {
        return data[sector];
    }
}
