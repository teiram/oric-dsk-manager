package com.grelobites.oric.dsk.model;

public class TrackGeometry {
    private int sectorSize;
    private int sectorCount;
    private int trackLead;
    private int gap2;
    private int gap3;
    private int side;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private TrackGeometry trackGeometry = new TrackGeometry();

        public Builder withSectorSize(int sectorSize) {
            trackGeometry.setSectorSize(sectorSize);
            return this;
        }

        public Builder withSectorCount(int sectorCount) {
            trackGeometry.setSectorCount(sectorCount);
            return this;
        }

        public Builder withTrackLead(int trackLead) {
            trackGeometry.setTrackLead(trackLead);
            return this;
        }

        public Builder withGap2(int gap2) {
            trackGeometry.setGap2(gap2);
            return this;
        }

        public Builder withGap3(int gap3) {
            trackGeometry.setGap3(gap3);
            return this;
        }

        public Builder withSide(int side) {
            trackGeometry.setSide(side);
            return this;
        }

        public TrackGeometry build() {
            return trackGeometry;
        }
    }

    public int getSectorSize() {
        return sectorSize;
    }

    public void setSectorSize(int sectorSize) {
        this.sectorSize = sectorSize;
    }

    public int getSectorCount() {
        return sectorCount;
    }

    public void setSectorCount(int sectorCount) {
        this.sectorCount = sectorCount;
    }

    public int getTrackLead() {
        return trackLead;
    }

    public void setTrackLead(int trackLead) {
        this.trackLead = trackLead;
    }

    public int getGap2() {
        return gap2;
    }

    public void setGap2(int gap2) {
        this.gap2 = gap2;
    }

    public int getGap3() {
        return gap3;
    }

    public void setGap3(int gap3) {
        this.gap3 = gap3;
    }

    public int getSide() {
        return side;
    }

    public void setSide(int side) {
        this.side = side;
    }

    @Override
    public String toString() {
        return "TrackGeometry{" +
                "sectorSize=" + sectorSize +
                ", sectorCount=" + sectorCount +
                ", trackLead=" + trackLead +
                ", gap2=" + gap2 +
                ", gap3=" + gap3 +
                ", side=" + side +
                '}';
    }
}
