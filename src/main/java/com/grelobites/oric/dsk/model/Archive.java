package com.grelobites.oric.dsk.model;

import javafx.beans.Observable;
import javafx.beans.property.*;
import java.util.concurrent.atomic.AtomicLong;

public class Archive {
    private static AtomicLong ID_GENERATOR = new AtomicLong(0);
    private final long id;
    private StringProperty name;
    private StringProperty extension;
    private IntegerProperty size;
    private byte[] data;

    public Archive(String name, String extension, byte[] data) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = new SimpleStringProperty(name);
        this.extension = new SimpleStringProperty(extension);
        this.size = new SimpleIntegerProperty(data.length);
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getExtension() {
        return extension.get();
    }

    public StringProperty extensionProperty() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension.set(extension);
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return size.get();
    }

    public IntegerProperty sizeProperty() {
        return size;
    }

    public void setSize(int size) {
        this.size.set(size);
    }

    public Observable[] getObservables() {
        return new Observable[] {name, extension};
    }

    @Override
    public String toString() {
        return "Archive{" +
                "id=" + id +
                ", name=" + name +
                ", extension=" + extension +
                ", size=" + size +
                ", data.length=" + (data != null ? data.length : "0") +
                '}';
    }
}
