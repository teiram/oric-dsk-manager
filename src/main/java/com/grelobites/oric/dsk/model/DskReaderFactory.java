package com.grelobites.oric.dsk.model;

import com.grelobites.oric.dsk.util.PlainDskReader;
import com.grelobites.oric.dsk.util.FileType;
import com.grelobites.oric.dsk.util.MfmDskReader;

public class DskReaderFactory {
    private static PlainDskReader plainDskReader;
    private static MfmDskReader mfmDskReader;

    private static DskReader getMfmDskReader() {
        if (mfmDskReader == null) {
            mfmDskReader = new MfmDskReader();
        }
        return mfmDskReader;
    }

    private static DskReader getPlainDskReader() {
        if (plainDskReader == null) {
            plainDskReader = new PlainDskReader();
        }
        return plainDskReader;
    }

    public static DskReader getDskReader(FileType fileType) {
        switch (fileType) {
            case PLAINDSK:
                return getPlainDskReader();
            case MFMDSK:
                return getMfmDskReader();
            default:
                throw new IllegalArgumentException("Not a DSK type or not supported");
        }

    }
}
