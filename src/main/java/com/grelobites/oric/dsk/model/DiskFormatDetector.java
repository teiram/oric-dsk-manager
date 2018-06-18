package com.grelobites.oric.dsk.model;

public enum DiskFormatDetector {
    DETECTOR0(0, 1, 0x40, 8),
    DETECTOR1(4, 17, 0x40, 8),
    DETECTOR2(2, 11, 0xC8, 8),
    DETECTOR3(0, 2, 0x22, 8);

    public int track;
    public int sector;
    public int offset;
    public int size;

    DiskFormatDetector(int track, int sector, int offset, int size) {
        this.track = track;
        this.sector = sector;
        this.offset = offset;
        this.size = size;
    }

}
