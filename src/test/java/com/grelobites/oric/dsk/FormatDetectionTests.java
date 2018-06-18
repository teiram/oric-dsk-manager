package com.grelobites.oric.dsk;


import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.util.DskUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class FormatDetectionTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormatDetectionTests.class);

    @Test
    public void checkFormatDetectors() throws IOException {
        InputStream diskStream = FormatDetectionTests.class.getResourceAsStream("/oricdos.dsk");
        Disk disk = DskUtil.diskFromDskStream(diskStream);

        LOGGER.info("Format is {}", DskUtil.getDiskFormat(disk));

    }
}
