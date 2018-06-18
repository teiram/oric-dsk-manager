package com.grelobites.oric.dsk.model;

public enum DiskFormat {
    SEDORIC("SEDORIC"),
    ORICDOS("Oric DOS"),
    UNKNOWN("UNKNOWN");

    public String name;

    DiskFormat(String name) {
        this.name = name;
    }

    public static DiskFormat byName(String name) {
        for (DiskFormat format : DiskFormat.values()) {
            if (format.name.equals(name)) {
                return format;
            }
        }
        return null;
    }
}
