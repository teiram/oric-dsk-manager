package com.grelobites.oric.dsk.sedoric;

import com.grelobites.oric.dsk.ApplicationContext;
import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.model.DiskGeometry;
import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.util.ArchiveUtil;
import com.grelobites.oric.dsk.util.DskUtil;
import com.grelobites.oric.dsk.util.LocaleUtil;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SedoricFileSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(SedoricFileSystem.class);
    private ApplicationContext context;
    private InvalidationListener diskUsageUpdater = e -> updateDiskUsage();

    private StringProperty name;
    private StringProperty initString;
    private IntegerProperty paperColor;
    private IntegerProperty penColor;
    private BooleanProperty bootable;

    public SedoricFileSystem(ApplicationContext context) {
        this.context = context;
        this.name = new SimpleStringProperty();
        this.initString = new SimpleStringProperty();
        this.paperColor = new SimpleIntegerProperty(Constants.DEFAULT_PAPER_COLOR);
        this.penColor = new SimpleIntegerProperty(Constants.DEFAULT_PEN_COLOR);
        this.bootable = new SimpleBooleanProperty(true);
        context.getArchiveList().addListener(diskUsageUpdater);
        context.diskGeometryProperty().addListener(diskUsageUpdater);
        context.diskGeometryProperty().addListener((observable, oldValue, newValue) -> {
            oldValue.capacityBinding().removeListener(diskUsageUpdater);
            newValue.capacityBinding().addListener(diskUsageUpdater);
        });
        bootable.addListener(diskUsageUpdater);
        context.getDiskGeometry().capacityBinding().addListener(diskUsageUpdater);
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

    public String getInitString() {
        return initString.get();
    }

    public StringProperty initStringProperty() {
        return initString;
    }

    public void setInitString(String initString) {
        this.initString.set(initString);
    }

    public int getPaperColor() {
        return paperColor.get();
    }

    public IntegerProperty paperColorProperty() {
        return paperColor;
    }

    public void setPaperColor(int paperColor) {
        this.paperColor.set(paperColor);
    }

    public int getPenColor() {
        return penColor.get();
    }

    public IntegerProperty penColorProperty() {
        return penColor;
    }

    public void setPenColor(int penColor) {
        this.penColor.set(penColor);
    }

    public boolean getBootable() {
        return bootable.get();
    }

    public BooleanProperty bootableProperty() {
        return bootable;
    }

    public void setBootable(boolean bootable) {
        this.bootable.set(bootable);
    }

    private void updateDiskUsage() {
        int usedBytes = getUsedBytes();
        int totalBytes = getTotalBytes();
        context.setDiskUsage((1.0 * usedBytes) / totalBytes);
        context.setDiskUsageDetail(String.format(LocaleUtil.i18n("dskUsageDetail"),
                totalBytes - usedBytes, totalBytes));
    }

    public void openDsk(InputStream stream) throws IOException {
        Disk disk = DskUtil.diskFromDskStream(stream);

        SedoricDirectory.fromDisk(disk).forEach(d -> {
            LOGGER.info("Read directory " + d);
            SedoricArchive archive = d.getArchive(disk);
            addArchive(ArchiveUtil.updateArchiveName(archive, context));
        });
        SedoricSystemSector systemSector = SedoricSystemSector.fromInputStream(
                new ByteArrayInputStream(disk
                        .getSector(Constants.SEDORIC_DIRECTORY_TRACK,
                                Constants.SEDORIC_SYSTEM_SECTOR)));
        LOGGER.debug("Got system sector " + systemSector);
        name.set(systemSector.getName().trim());
        initString.set(systemSector.getInitString().trim());
        if (systemSector.getPenColor() != null) {
            penColor.set(systemSector.getPenColor());
        }
        if (systemSector.getPaperColor() != null) {
            paperColor.set(systemSector.getPaperColor());
        }

        setDiskGeometry(disk.getGeometry());
    }

    public void setDiskGeometry(DiskGeometry geometry) {
        context.setDiskGeometry(geometry);
    }

    public DiskGeometry getDiskGeometry() {
        return context.getDiskGeometry();
    }

    public void addArchive(SedoricArchive archive) {
        getArchiveList().add(archive);
    }

    public void removeArchive(SedoricArchive archive) {
        getArchiveList().remove(archive);
    }

    public void clear() {
        getArchiveList().clear();
        setName("");
        setInitString("");
    }

    public List<SedoricArchive> getArchiveList() {
        return context.getArchiveList();
    }

    public int getTotalBytes() {
        return getDiskGeometry().capacityBinding().get();
    }

    private int descriptorSectorsForSectorCount(int fileSectors) {
        int descriptorSectors = 1;
        int remainingSectors = fileSectors - (Constants.SECTOR_SIZE - 12) / 2;
        while (remainingSectors > 0) {
            descriptorSectors++;
            remainingSectors -= (Constants.SECTOR_SIZE - 2) / 2;
        }
        return descriptorSectors;
    }

    private  int correctedFileSize(int size) {
        //This is not accurate, because each track could have a different geometry
        //We would need to simulate disk population each time a change is made
        //So let's assume we are using 256 bytes sectors
        int fileSectors = (size + Constants.SECTOR_SIZE - 1) / Constants.SECTOR_SIZE;
        return (fileSectors + descriptorSectorsForSectorCount(fileSectors)) *
                Constants.SECTOR_SIZE;
    }

    private int getDirectoryBytes() {
        int directorySectors = 1;
        int directoryEntriesPerSector = (Constants.SECTOR_SIZE - 16) / 16;
        int remainingFiles = getArchiveList().size() - directoryEntriesPerSector;
        while (remainingFiles > 0) {
            directorySectors++;
            remainingFiles -= (Constants.SECTOR_SIZE - 16) / 16;
        }
        return directorySectors * Constants.SECTOR_SIZE;
    }

    public int getUsedBytes() {
        int bytes =  getArchiveList().stream()
                .mapToInt(a -> correctedFileSize(a.getSize())).sum();
        bytes += getDirectoryBytes();
        if (getBootable()) {
            try {
                bytes += Constants.getSedoricBootStrap().length;
            } catch (IOException ioe) {
                LOGGER.error("Trying to fetch Sedoric Boostrap", ioe);
            }
        }
        return bytes;
    }

    public void exportFileSystem(OutputStream os) throws IOException {
        DiskGeometry geometry = getDiskGeometry();

        SedoricBitmap bitmap = new SedoricBitmap(geometry);
        Disk disk = new Disk(geometry);

        DirectoryWriter directoryWriter = new DirectoryWriter(disk, bitmap);
        DescriptorWriter descriptorWriter = new DescriptorWriter(disk, bitmap);

        bitmap.allocateSector(new SectorCoordinates(Constants.SEDORIC_DIRECTORY_TRACK,
                        Constants.SEDORIC_SYSTEM_SECTOR));
        bitmap.allocateSector(new SectorCoordinates(Constants.SEDORIC_DIRECTORY_TRACK,
                        Constants.SEDORIC_BITMAP_SECTOR));

        if (getBootable()) {
            final byte[] bootstrap = Constants.getSedoricBootStrap();
            int remaining = bootstrap.length;
            LOGGER.debug("Adding Sedoric bootstrap with length {}", bootstrap.length);
            int position = 0;
            while (remaining > 0) {
                SectorCoordinates pos = bitmap.getFreeSector();
                byte[] sectorData = disk.getSector(pos);
                System.arraycopy(bootstrap, position, sectorData, 0,
                        Math.min(remaining, sectorData.length));
                remaining -= sectorData.length;
                position += sectorData.length;
            }
        }

        for (SedoricArchive archive : getArchiveList()) {
            SedoricDirectory directory = new SedoricDirectory();
            directory.setName(archive.getName());
            directory.setExtension(archive.getExtension());
            directory.setFlags(archive.isProtectedAttribute() ? 0x40 : 0);
            SedoricDescriptor descriptor = SedoricDescriptor.newBuilder()
                    .withBlock(archive.isBlockAttribute())
                    .withExecutable(archive.isExecutableAttribute())
                    .withStartAddress(archive.getLoadAddress())
                    .withEndAddress(archive.getLoadAddress() + archive.getSize())
                    .withExecAddress(archive.getExecAddress()).build();
            descriptorWriter.write(directory, descriptor); //Populates the coordinates list and the sectors
            directoryWriter.write(directory); //Once the directory is filled by the descriptorWriter
            LOGGER.debug("For archive {} written directory {} and descriptor {}", archive, directory, descriptor);
            int position = 0;
            int remaining = archive.getSize();
            for (SectorCoordinates pos : descriptor.getFileSectors()) {
                byte[] sectorData = disk.getSector(pos);
                System.arraycopy(archive.getData(), position, sectorData, 0,
                        Math.min(remaining, sectorData.length));
                remaining -= sectorData.length;
                position += sectorData.length;
            }
        }
        bitmap.flush(disk, directoryWriter.directoryCount(), directoryWriter.sectorCount());
        SedoricSystemSector.newBuilder()
                .withName(name.get())
                .withInitString(initString.get())
                .withPaperColor(paperColor.get())
                .withPenColor(penColor.get()).build()
                .dump(disk);
        DskUtil.dumpAsMfm(disk, os);
    }
}

