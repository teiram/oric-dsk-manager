package com.grelobites.oric.dsk;

import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Preferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    private static final String LAST_USED_DIRECTORY_PROPERTY = "lastUsedDirectory";

    private StringProperty lastUsedDirectory;

    private static Preferences INSTANCE;

    private Preferences() {
        this.lastUsedDirectory = new SimpleStringProperty("");
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


    private static java.util.prefs.Preferences getApplicationPreferences() {
        return java.util.prefs.Preferences.userNodeForPackage(Preferences.class);
    }

    private static Preferences setFromPreferences(Preferences preferences) {
        java.util.prefs.Preferences p = getApplicationPreferences();
        preferences.setLastUsedDirectory(p.get(LAST_USED_DIRECTORY_PROPERTY, ""));
        LOGGER.debug("Preferences loaded as {}", preferences);
        return preferences;
    }

    private static void persistConfigurationValue(String key, String value) {
        LOGGER.debug("persistConfigurationValue " + key + ", " + value);
        java.util.prefs.Preferences p = getApplicationPreferences();
        if (value != null) {
            p.put(key, value);
        } else {
            p.remove(key);
        }
    }

    public String getLastUsedDirectory() {
        return lastUsedDirectory.get();
    }

    public StringProperty lastUsedDirectoryProperty() {
        return lastUsedDirectory;
    }

    public void setLastUsedDirectory(String lastUsedDirectory) {
        persistConfigurationValue(LAST_USED_DIRECTORY_PROPERTY, lastUsedDirectory);
        this.lastUsedDirectory.set(lastUsedDirectory);
    }

    @Override
    public String toString() {
        return "Preferences{" +
                "lastUsedDirectory=" + lastUsedDirectory +
                '}';
    }
}
