package com.grelobites.oric.dsk.model;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class SimpleDiskGeometry implements DiskGeometry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDiskGeometry.class);

    private IntegerProperty trackCount;
    private IntegerProperty sideCount;
    private IntegerProperty sectorCount;
    private TrackGeometry[] trackGeometries;
    private IntegerBinding capacityBinding;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Integer trackCount;
        private Integer sideCount;
        private TrackGeometry trackGeometry;

        public Builder withTrackGeometry(TrackGeometry trackGeometry) {
            this.trackGeometry = trackGeometry;
            return this;
        }

        public Builder withTrackCount(int trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Builder withSideCount(int sideCount) {
            this.sideCount = sideCount;
            return this;
        }

        public SimpleDiskGeometry build() {
            SimpleDiskGeometry geometry = new SimpleDiskGeometry();
            geometry.setSideCount(sideCount);
            geometry.setTrackCount(trackCount);
            geometry.setSectorCount(trackGeometry.getSectorCount());
            geometry.setTrackGeometry(trackGeometry);

            return geometry;
        }
    }

    public SimpleDiskGeometry() {
        this.trackCount = new SimpleIntegerProperty();
        this.sideCount = new SimpleIntegerProperty();
        this.sectorCount = new SimpleIntegerProperty();
        this.capacityBinding = Bindings.createIntegerBinding(() -> capacity(),
                trackCount, sideCount, sectorCount);
    }

    @Override
    public IntegerBinding capacityBinding() {
        return capacityBinding;
    }

    @Override
    public int getTrackCount() {
        return trackCount.get();
    }

    public IntegerProperty trackCountProperty() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount.set(trackCount);
    }

    @Override
    public int getSideCount() {
        return sideCount.get();
    }

    public IntegerProperty sideCountProperty() {
        return sideCount;
    }

    public void setSideCount(int sideCount) {
        this.sideCount.set(sideCount);
    }

    @Override
    public TrackGeometry getTrackGeometry(int track) {
        return trackGeometries[track / getTrackCount()];
    }

    public void setTrackGeometry(TrackGeometry trackGeometry) {
        trackGeometries = new TrackGeometry[getSideCount()];
        for (int i = 0; i < getSideCount(); i++) {
            trackGeometries[i] = TrackGeometry.newBuilder()
                    .withSectorCount(trackGeometry.getSectorCount())
                    .withSectorSize(trackGeometry.getSectorSize())
                    .withTrackLead(trackGeometry.getTrackLead())
                    .withGap2(trackGeometry.getGap2())
                    .withGap3(trackGeometry.getGap3())
                    .withSide(i).build();
        }
        setSectorCount(trackGeometry.getSectorCount());
    }

    @Override
    public boolean hasSectorCount() {
        return true;
    }

    @Override
    public IntegerProperty sectorCountProperty() {
        return sectorCount;
    }

    public void setSectorCount(int sectorCount) {
        this.sectorCount.set(sectorCount);
    }

    @Override
    public int getSectorCount() {
        return sectorCount.get();
    }

    public int capacity() {
        return getSectorCount() * getTrackCount() * getSideCount() *
                trackGeometries[0].getSectorSize();
    }

    @Override
    public String toString() {
        return "SimpleDiskGeometry{" +
                "trackCount=" + trackCount +
                ", sideCount=" + sideCount +
                ", sectorCount=" + sectorCount +
                ", trackGeometries=" + Arrays.toString(trackGeometries) +
                '}';
    }
}
