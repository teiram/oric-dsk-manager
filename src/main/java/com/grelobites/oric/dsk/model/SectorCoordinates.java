package com.grelobites.oric.dsk.model;

public class SectorCoordinates {
    private int track;
    private int sector;

    public static class Builder {
        private int track;
        private int sector;

        public Builder withTrack(int track) {
            this.track = track;
            return this;
        }

        public Builder withSector(int sector) {
            this.sector = sector;
            return this;
        }

        public SectorCoordinates build() {
            return new SectorCoordinates(track, sector);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public SectorCoordinates(int track, int sector) {
        this.track = track;
        this.sector = sector;
    }

    public int getTrack() {
        return track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public int getSector() {
        return sector;
    }

    public void setSector(int sector) {
        this.sector = sector;
    }

    public boolean isValid() {
        return !(track == 0 && sector == 0);
    }

    @Override
    public String toString() {
        return "SectorCoordinates{" +
                "track=" + track +
                ", sector=" + sector +
                '}';
    }
}
