package com.grelobites.oric.dsk.model;

public enum DiskEncodingInfo {
    SECTORS_15_17(72, 34, 50),
    SECTORS_18(12, 34, 46),
    SECTORS_19(36, 34, 20);

    private final int gap1;
    private final int gap2;
    private final int gap3;

    DiskEncodingInfo(int gap1, int gap2, int gap3) {
        this.gap1 = gap1;
        this.gap2 = gap2;
        this.gap3 = gap3;
    }

    public int gap1() {
        return gap1;
    }

    public int gap2() {
        return gap2;
    }

    public int gap3() {
        return gap3;
    }

    public static DiskEncodingInfo forSectorCount(int sectorCount) {
        switch (sectorCount) {
            case 15:
            case 16:
            case 17:
                return SECTORS_15_17;
            case 18:
                return SECTORS_18;
            case 19:
                return SECTORS_19;
            default:
                throw new IllegalArgumentException("No DiskEncodingInfo for the given geometry");
        }
    }
}
