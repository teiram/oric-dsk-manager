package com.grelobites.oric.dsk;

import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import com.grelobites.oric.dsk.sedoric.SedoricDirectory;
import com.grelobites.oric.dsk.util.DskUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class DskImportTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskImportTest.class);

    @Test
    public void importFromOldDsk() throws IOException {
        InputStream diskStream = DskImportTest.class.getResourceAsStream("/xenon1.old.dsk");

        Disk disk = DskUtil.diskFromDskStream(diskStream);
        SedoricDirectory.fromDisk(disk).forEach(d -> {
            LOGGER.info("Read directory " + d);
            SedoricArchive archive = d.getArchive(disk);
            LOGGER.info("Archive is " + archive);
        });

    }

    @Test
    public void importFromNewDsk() throws IOException {
        InputStream diskStream = DskImportTest.class.getResourceAsStream("/BuggyBoy.dsk");
        Disk disk = DskUtil.diskFromDskStream(diskStream);

        SedoricDirectory.fromDisk(disk).forEach(d -> {
            LOGGER.info("Read directory " + d);
            SedoricArchive archive = d.getArchive(disk);
            LOGGER.info("Archive is " + archive);
        });

    }
}
