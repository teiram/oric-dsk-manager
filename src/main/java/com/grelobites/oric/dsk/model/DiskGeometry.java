package com.grelobites.oric.dsk.model;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.IntegerProperty;

public interface DiskGeometry {
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
