package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.model.Archive;


import javafx.beans.property.*;

public class SedoricArchive extends Archive {
    private IntegerProperty loadAddress;
    private IntegerProperty execAddress;
    private BooleanProperty executableAttribute;
    private BooleanProperty blockAttribute;
    private BooleanProperty protectedAttribute;

    public SedoricArchive(String name, String extension, byte[] data) {
        super(name, extension, data);
        this.loadAddress = new SimpleIntegerProperty();
        this.execAddress = new SimpleIntegerProperty();
        this.executableAttribute = new SimpleBooleanProperty(false);
        this.protectedAttribute = new SimpleBooleanProperty(false);
        this.blockAttribute = new SimpleBooleanProperty(false);
    }

    public int getLoadAddress() {
        return loadAddress.get();
    }

    public IntegerProperty loadAddressProperty() {
        return loadAddress;
    }

    public void setLoadAddress(int loadAddress) {
        this.loadAddress.set(loadAddress);
    }

    public int getExecAddress() {
        return execAddress.get();
    }

    public IntegerProperty execAddressProperty() {
        return execAddress;
    }

    public void setExecAddress(int execAddress) {
        this.execAddress.set(execAddress);
    }

    public boolean getExecutableAttribute() {
        return executableAttribute.get();
    }

    public BooleanProperty executableAttributeProperty() {
        return executableAttribute;
    }

    public void setExecutableAttribute(boolean executableAttribute) {
        this.executableAttribute.set(executableAttribute);
    }

    public boolean getBlockAttribute() {
        return blockAttribute.get();
    }

    public BooleanProperty blockAttributeProperty() {
        return blockAttribute;
    }

    public void setBlockAttribute(boolean blockAttribute) {
        this.blockAttribute.set(blockAttribute);
    }

    public boolean isProtectedAttribute() {
        return protectedAttribute.get();
    }

    public BooleanProperty protectedAttributeProperty() {
        return protectedAttribute;
    }

    public void setProtectedAttribute(boolean protectedAttribute) {
        this.protectedAttribute.set(protectedAttribute);
    }

    public boolean isExecutableAttribute() {
        return executableAttribute.get();
    }

    public boolean isBlockAttribute() {
        return blockAttribute.get();
    }

    @Override
    public String toString() {
        return "SedoricArchive{" +
                "loadAddress=" + loadAddress +
                ", execAddress=" + execAddress +
                ", executableAttribute=" + executableAttribute +
                ", blockAttribute=" + blockAttribute +
                ", protectedAttribute=" + protectedAttribute +
                "} " + super.toString();
    }
}
