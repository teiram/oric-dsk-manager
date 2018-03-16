package com.grelobites.oric.dsk.model;

public class SectorCoordinates {
    private int track;
    private int sector;

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
