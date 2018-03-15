package com.grelobites.oric.dsk.util;

import com.grelobites.oric.dsk.model.*;
import com.grelobites.oric.dsk.sedoric.DskHeader;

import java.io.IOException;
import java.io.InputStream;

public class PlainDskReader implements DskReader {

    @Override
    public Disk fromDsk(InputStream stream, DskHeader header) throws IOException {
        DiskGeometry geometry = SimpleDiskGeometry.newBuilder()
                .withSideCount(header.getSides())
                .withTrackCount(header.getTracks())
                .withTrackGeometry(TrackGeometry.newBuilder()
                        .withSectorSize(256)
                        .withSectorCount(header.getSectors())
                        .build())
                .build();

        Track[] tracks = new Track[geometry.getTrackCount()];
        for (int i = 0; i < geometry.getTrackCount(); i++) {
            TrackGeometry trackGeometry = geometry.getTrackGeometry(i);
            byte[][] data = new byte[trackGeometry.getSectorCount()][trackGeometry.getSectorSize()];
            for (int j = 0; j < trackGeometry.getSectorCount(); j++) {
                stream.read(data[j]);
            }
            tracks[i] = new Track(data);
        }
        return new Disk(tracks, geometry);
    }


}
