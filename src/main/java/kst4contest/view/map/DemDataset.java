package kst4contest.view.map;

import java.util.Locale;

/**
 * Supported offline DEM datasets.
 *
 * First real offline target:
 * Copernicus DEM GLO-30 DGED GeoTIFF.
 */
public enum DemDataset {

    COPERNICUS_GLO_30("copernicus_glo_30", "Copernicus DEM GLO-30");

    private final String id;
    private final String displayName;

    DemDataset(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static DemDataset fromId(String id) {
        if (id == null || id.isBlank()) {
            return COPERNICUS_GLO_30;
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (DemDataset dataset : values()) {
            if (dataset.id.equals(normalized)) {
                return dataset;
            }
        }

        return COPERNICUS_GLO_30;
    }
}