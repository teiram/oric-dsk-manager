package com.grelobites.oric.dsk.model;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.IntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface DiskGeometry {
    Logger LOGGER = LoggerFactory.getLogger(DiskGeometry.class);

    default int decodeTrack(int track) {
        if ((track & 0x80) != 0) {
            LOGGER.debug("On track decoding {} -> {}", track, getTrackCount() + (track & 0x7F));
            return getTrackCount() + (track & 0x7F);
        } else {
            return track;
        }
    }

    default int encodeTrack(int track) {
        if (track >= getTrackCount()) {
            LOGGER.debug("On track encoding {} -> {}", track, 0x80 | (track - getTrackCount()));
            return 0x80 | (track - getTrackCount());
        } else {
            return track;
        }
    }

    int getTrackCount();
    int getSideCount();
    int getSectorCount(); //Optional, only for homogeneous geometries
    IntegerProperty trackCountProperty();
    IntegerProperty sideCountProperty();
    IntegerProperty sectorCountProperty();
    TrackGeometry getTrackGeometry(int track);
    boolean hasSectorCount();
    IntegerBinding capacityBinding();
}
