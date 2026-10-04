package kst4contest.view.map;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Compatibility offline-first provider.
 *
 * This class is currently not part of the active StationMap pipeline anymore,
 * but it is kept compilable so the source tree stays consistent while the
 * Copernicus provider is the primary offline implementation.
 */
public final class OfflineDemTerrainProfileProvider implements TerrainProfileProvider {

    private final Supplier<String> demRootDirectorySupplier;
    private final Supplier<DemDataset> datasetSupplier;
    private final OfflineDemManager offlineDemManager;

    public OfflineDemTerrainProfileProvider(Supplier<String> demRootDirectorySupplier,
                                            Supplier<DemDataset> datasetSupplier,
                                            OfflineDemManager offlineDemManager) {
        this.demRootDirectorySupplier = Objects.requireNonNull(demRootDirectorySupplier, "demRootDirectorySupplier");
        this.datasetSupplier = Objects.requireNonNull(datasetSupplier, "datasetSupplier");
        this.offlineDemManager = Objects.requireNonNull(offlineDemManager, "offlineDemManager");
    }

    @Override
    public TerrainProfileData loadProfile(TerrainProfileRequest request) {
        DemDataset dataset = datasetSupplier.get();
        if (dataset == null) {
            dataset = DemDataset.COPERNICUS_GLO_30;
        }

        OfflineDemManager.OfflineDemIndex index =
                offlineDemManager.inspectAndIndex(demRootDirectorySupplier.get(), dataset);

        if (!index.usable()) {
            return TerrainProfileData.empty(dataset.displayName() + " offline DEM unavailable");
        }

        return TerrainProfileData.empty(dataset.displayName() + " offline DEM indexed but not used by this compatibility provider");
    }
}