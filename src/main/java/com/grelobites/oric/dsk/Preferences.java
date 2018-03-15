package com.grelobites.oric.dsk;

import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Preferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    private static final String TRACKS_PROPERTY = "tracks";
    private static final String SECTORS_PER_TRACK_PROPERTY = "sectorsPerTrack";
    private static final String SIDES_PROPERTY = "sides";

    private IntegerProperty tracks;
    private IntegerProperty sectorsPerTrack;
    private IntegerProperty sides;

    private BooleanProperty validPreferences;

    private static Preferences INSTANCE;

    private Preferences() {
        this.tracks = new SimpleIntegerProperty(Constants.DEFAULT_NUM_TRACKS);
        this.sectorsPerTrack = new SimpleIntegerProperty(Constants.DEFAULT_NUM_SECTORS_PER_TRACK);
        this.sides = new SimpleIntegerProperty(Constants.DEFAULT_NUM_SIDES);
    }

    public static Preferences getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    synchronized private static Preferences newInstance() {
        return setFromPreferences(new Preferences());
    }


    public static java.util.prefs.Preferences getApplicationPreferences() {
        return java.util.prefs.Preferences.userNodeForPackage(Preferences.class);
    }

    private static Preferences setFromPreferences(Preferences preferences) {
        java.util.prefs.Preferences p = getApplicationPreferences();
        preferences.setTracks(Integer.parseInt(p.get(TRACKS_PROPERTY,
                Integer.toString(Constants.DEFAULT_NUM_TRACKS))));
        preferences.setSectorsPerTrack(Integer.parseInt(p.get(SECTORS_PER_TRACK_PROPERTY,
                Integer.toString(Constants.DEFAULT_NUM_SECTORS_PER_TRACK))));
        preferences.setSides(Integer.parseInt(p.get(TRACKS_PROPERTY,
                Integer.toString(Constants.DEFAULT_NUM_SIDES))));
        return preferences;
    }

    public static void persistConfigurationValue(String key, String value) {
        LOGGER.debug("persistConfigurationValue " + key + ", " + value);
        java.util.prefs.Preferences p = getApplicationPreferences();
        if (value != null) {
            p.put(key, value);
        } else {
            p.remove(key);
        }
    }

    public int getTracks() {
        return tracks.get();
    }

    public IntegerProperty tracksProperty() {
        return tracks;
    }

    public void setTracks(int tracks) {
        persistConfigurationValue(TRACKS_PROPERTY, Integer.toString(tracks));
        this.tracks.set(tracks);
    }

    public int getSectorsPerTrack() {
        return sectorsPerTrack.get();
    }

    public IntegerProperty sectorsPerTrackProperty() {
        return sectorsPerTrack;
    }

    public void setSectorsPerTrack(int sectorsPerTrack) {
        persistConfigurationValue(SECTORS_PER_TRACK_PROPERTY, Integer.toString(sectorsPerTrack));
        this.sectorsPerTrack.set(sectorsPerTrack);
    }

    public int getSides() {
        return sides.get();
    }

    public IntegerProperty sidesProperty() {
        return sides;
    }

    public void setSides(int sides) {
        persistConfigurationValue(SIDES_PROPERTY, Integer.toString(sides));
        this.sides.set(sides);
    }

    public boolean isValidPreferences() {
        return validPreferences.get();
    }

    public BooleanProperty validPreferencesProperty() {
        return validPreferences;
    }

    public void setValidPreferences(boolean validPreferences) {
        this.validPreferences.set(validPreferences);
    }
}
