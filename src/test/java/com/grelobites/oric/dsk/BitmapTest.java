package com.grelobites.oric.dsk;

import com.grelobites.oric.dsk.model.SectorCoordinates;
import com.grelobites.oric.dsk.sedoric.SedoricBitmap;
import org.junit.Test;


public class BitmapTest {

    @Test
    public void testAllocation() {
        SedoricBitmap bitmap = new SedoricBitmap(Constants.DEFAULT_DISK_GEOMETRY);
        SectorCoordinates allocated = bitmap.allocateSector(new SectorCoordinates(20, 1));
        int freeSectors = bitmap.freeSectors();
        for (int i = 0; i < freeSectors - 1; i++) {
            SectorCoordinates free = bitmap.getFreeSector();
            assert(!(free.getTrack() == allocated.getTrack() &&
                    free.getSector() == allocated.getSector()));
        }
    }
}
