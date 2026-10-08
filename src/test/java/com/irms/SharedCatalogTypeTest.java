package com.irms;

import com.irms.model.SharedCatalogType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SharedCatalogTypeTest {

    @Test
    public void mapsEverySupportedCodeToItsCatalogType() {
        for (SharedCatalogType type : SharedCatalogType.values()) {
            assertEquals(type, SharedCatalogType.fromCode(type.getCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownCatalogTypes() {
        SharedCatalogType.fromCode("UNSUPPORTED");
    }
}
