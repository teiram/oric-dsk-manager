package com.grelobites.oric.dsk.model;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ComplexDiskGeometry implements DiskGeometry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexDiskGeometry.class);

    private IntegerProperty trackCount;
    private IntegerProperty sideCount;
    private IntegerBinding capacityBinding;
    private int geometryId;
    private Map<Integer, TrackGeometry> trackGeometries = new HashMap<>();

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        ComplexDiskGeometry diskGeometry = new ComplexDiskGeometry();

        public Builder withTrackGeometry(Integer track, TrackGeometry trackGeometry) {
            diskGeometry.getTrackGeometries().put(track, trackGeometry);
            return this;
        }

        public Builder withTrackCount(int trackCount) {
            diskGeometry.setTrackCount(trackCount);
            return this;
        }

        public Builder withSideCount(int sideCount) {
            diskGeometry.setSideCount(sideCount);
            return this;
        }

        public Builder withGeometryId(int geometryId) {
            diskGeometry.setGeometryId(geometryId);
            return this;
        }
        public ComplexDiskGeometry build() {
            return diskGeometry;
        }
    }

    public ComplexDiskGeometry() {
        this.trackCount = new SimpleIntegerProperty();
        this.sideCount = new SimpleIntegerProperty();
        this.capacityBinding = Bindings.createIntegerBinding(() -> capacity(),
                trackCount, sideCount);
    }

    @Override
    public IntegerBinding capacityBinding() {
        return capacityBinding;
    }

    @Override
    public int getTrackCount() {
        return trackCount.get();
    }

    @Override
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

    @Override
    public int getSectorCount() {
        throw new IllegalArgumentException("SectorCount not supported");
    }

    @Override
    public IntegerProperty sideCountProperty() {
        return sideCount;
    }

    @Override
    public IntegerProperty sectorCountProperty() {
        throw new IllegalArgumentException("Sector count not supported");
    }

    public void setSideCount(int sideCount) {
        this.sideCount.set(sideCount);
    }

    public int getGeometryId() {
        return geometryId;
    }

    public void setGeometryId(int geometryId) {
        this.geometryId = geometryId;
    }

    public Map<Integer, TrackGeometry> getTrackGeometries() {
        return trackGeometries;
    }

    public void setTrackGeometries(Map<Integer, TrackGeometry> trackGeometries) {
        this.trackGeometries = trackGeometries;
    }

    public TrackGeometry getTrackGeometry(int track) {
        return trackGeometries.get(track);
    }

    @Override
    public boolean hasSectorCount() {
        return false;
    }

    public void setTrackGeometry(int track, TrackGeometry trackGeometry) {
        trackGeometries.put(track, trackGeometry);
    }

    public int capacity() {
        return getTrackGeometries().values().stream()
                .map(i -> i.getSectorCount() * i.getSectorSize())
                .mapToInt(i -> i).sum();
    }

    @Override
    public String toString() {
        return "ComplexDiskGeometry{" +
                "trackCount=" + trackCount +
                ", sideCount=" + sideCount +
                ", geometryId=" + geometryId +
                ", trackGeometries=" + trackGeometries +
                '}';
    }
}
