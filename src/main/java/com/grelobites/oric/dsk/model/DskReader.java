package com.grelobites.oric.dsk.model;

import com.grelobites.oric.dsk.sedoric.DskHeader;

import java.io.IOException;
import java.io.InputStream;

public interface DskReader {
    Disk fromDsk(InputStream stream, DskHeader header) throws IOException;
}
