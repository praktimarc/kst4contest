package kst4contest.view.map;

import kst4contest.locatorUtils.Location;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * High-level orchestration service for terrain package preparation.
 *
 * <p>This is the first end-to-end vertical slice that connects:
 * <ul>
 *     <li>path coverage resolution</li>
 *     <li>catalog download/load</li>
 *     <li>package download</li>
 *     <li>package installation into the local DEM directory</li>
 * </ul>
 *
 * <p>The current implementation intentionally installs Copernicus *_DEM.tif files
 * into the DEM root that is already used by the existing terrain provider chain.
 * That allows us to reach a working automated download flow with very few
 * intermediate refactorings.</p>
 */
public final class TerrainPackageService {

    /**
     * First default catalog URL for the future modular terrain service on hamradioonline.de.
     *
     * <p>This can later move into user preferences without changing the orchestration flow.</p>
     */
    public static final String DEFAULT_TERRAIN_CATALOG_URL =
            "https://terrain.hamradioonline.de/catalog/terrain-catalog-v1.xml";

    private final TerrainCoverageResolver terrainCoverageResolver;
    private final TerrainCatalogClient terrainCatalogClient;
    private final TerrainPackageDownloader terrainPackageDownloader;
    private final TerrainPackageInstaller terrainPackageInstaller;

    public TerrainPackageService() {
        this(
                new TerrainCoverageResolver(),
                new TerrainCatalogClient(),
                new TerrainPackageDownloader(),
                new TerrainPackageInstaller()
        );
    }

    public TerrainPackageService(TerrainCoverageResolver terrainCoverageResolver,
                                 TerrainCatalogClient terrainCatalogClient,
                                 TerrainPackageDownloader terrainPackageDownloader,
                                 TerrainPackageInstaller terrainPackageInstaller) {
        this.terrainCoverageResolver = terrainCoverageResolver;
        this.terrainCatalogClient = terrainCatalogClient;
        this.terrainPackageDownloader = terrainPackageDownloader;
        this.terrainPackageInstaller = terrainPackageInstaller;
    }

    /**
     * Prepares all terrain packages required for the given path using the default catalog URL.
     *
     * @param request path analysis request
     * @param configuredDemRootDirectory currently configured DEM root directory
     * @return combined terrain preparation result
     */
    public TerrainPreparationResult prepareTerrainForPath(PathAnalysisRequest request,
                                                          String configuredDemRootDirectory) {
        return prepareTerrainForPath(
                request,
                DEFAULT_TERRAIN_CATALOG_URL,
                configuredDemRootDirectory
        );
    }

    /**
     * Prepares all terrain packages required for the given path.
     *
     * <p>Workflow:
     * <ol>
     *     <li>resolve required package ids from the path geometry</li>
     *     <li>download the catalog</li>
     *     <li>fallback to the last local catalog if the download fails</li>
     *     <li>download all matching required packages</li>
     *     <li>install all successfully downloaded packages into the DEM root</li>
     * </ol>
     *
     * @param request path analysis request
     * @param catalogUrl terrain catalog URL
     * @param configuredDemRootDirectory currently configured DEM root directory
     * @return combined terrain preparation result
     */
    public TerrainPreparationResult prepareTerrainForPath(PathAnalysisRequest request,
                                                          String catalogUrl,
                                                          String configuredDemRootDirectory) {

        if (request == null || !request.hasUsableHome() || !request.hasUsableTarget()) {
            return TerrainPreparationResult.failure(
                    request,
                    TerrainCoverageResolver.TerrainCoverageSelection.empty(),
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    "Path request is missing or does not contain usable endpoints."
            );
        }

        TerrainCoverageResolver.TerrainCoverageSelection coverageSelection =
                terrainCoverageResolver.resolveCoverageForPath(request);

        if (!coverageSelection.hasCoverage()) {
            return TerrainPreparationResult.failure(
                    request,
                    coverageSelection,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    "No terrain coverage could be resolved for the requested path."
            );
        }

        TerrainCatalogClient.CatalogDownloadResult catalogDownloadResult =
                terrainCatalogClient.downloadCatalog(catalogUrl);

        TerrainCatalogClient.CatalogLoadResult catalogLoadResult =
                terrainCatalogClient.loadLocalCatalog();

        if (!catalogLoadResult.success() || catalogLoadResult.terrainCatalog() == null) {
            StringBuilder message = new StringBuilder();
            message.append("Terrain catalog is not available.");

            if (catalogDownloadResult != null && catalogDownloadResult.message() != null && !catalogDownloadResult.message().isBlank()) {
                message.append("\n\nDownload: ").append(catalogDownloadResult.message());
            }

            if (catalogLoadResult != null && catalogLoadResult.message() != null && !catalogLoadResult.message().isBlank()) {
                message.append("\n\nLoad: ").append(catalogLoadResult.message());
            }

            return TerrainPreparationResult.failure(
                    request,
                    coverageSelection,
                    catalogDownloadResult,
                    catalogLoadResult,
                    null,
                    List.of(),
                    List.of(),
                    message.toString()
            );
        }

        TerrainCatalog terrainCatalog = catalogLoadResult.terrainCatalog();

        List<String> missingPackageIds = coverageSelection.packageIds().stream()
                .filter(packageId -> terrainCatalog.findPackageById(packageId).isEmpty())
                .toList();

        List<String> availablePackageIds = coverageSelection.packageIds().stream()
                .filter(packageId -> terrainCatalog.findPackageById(packageId).isPresent())
                .toList();

        if (availablePackageIds.isEmpty()) {
            return TerrainPreparationResult.failure(
                    request,
                    coverageSelection,
                    catalogDownloadResult,
                    catalogLoadResult,
                    null,
                    missingPackageIds,
                    List.of(),
                    "None of the required terrain packages are present in the loaded catalog."
            );
        }

        TerrainPackageDownloader.BatchDownloadResult batchDownloadResult =
                terrainPackageDownloader.downloadPackages(terrainCatalog, availablePackageIds);

        List<TerrainPackageInstaller.InstallResult> installResults = new ArrayList<>();
        for (Path packageFile : batchDownloadResult.successfulPackageFiles()) {
            installResults.add(
                    terrainPackageInstaller.installPackage(packageFile, configuredDemRootDirectory)
            );
        }

        String message = buildSummaryMessage(
                coverageSelection,
                catalogDownloadResult,
                catalogLoadResult,
                batchDownloadResult,
                missingPackageIds,
                installResults
        );

        boolean success = missingPackageIds.isEmpty()
                && batchDownloadResult != null
                && batchDownloadResult.allSuccessful()
                && installResults.stream().anyMatch(TerrainPackageInstaller.InstallResult::success);

        return new TerrainPreparationResult(
                request,
                coverageSelection,
                catalogDownloadResult,
                catalogLoadResult,
                batchDownloadResult,
                List.copyOf(missingPackageIds),
                List.copyOf(installResults),
                success,
                message
        );
    }

    /**
     * Convenience helper for the likely future main workflow where only locators
     * are available from the chat/channel context.
     *
     * @param ownLocator6 own 6-digit Maidenhead locator
     * @param targetLocator6 target 6-digit Maidenhead locator
     * @param targetCallsignRaw target callsign
     * @param analysisFrequencyMHz analysis frequency in MHz
     * @param ownAntennaHeightMeters own antenna height in meters AGL
     * @param targetAntennaHeightMeters target antenna height in meters AGL
     * @param configuredDemRootDirectory configured DEM root directory
     * @return combined terrain preparation result
     */
    public TerrainPreparationResult prepareTerrainForLocators(String ownLocator6,
                                                              String targetLocator6,
                                                              String targetCallsignRaw,
                                                              double analysisFrequencyMHz,
                                                              double ownAntennaHeightMeters,
                                                              double targetAntennaHeightMeters,
                                                              String configuredDemRootDirectory) {

        String normalizedOwnLocator6 = normalizeLocator6(ownLocator6);
        String normalizedTargetLocator6 = normalizeLocator6(targetLocator6);

        if (normalizedOwnLocator6.length() != 6 || normalizedTargetLocator6.length() != 6) {
            return TerrainPreparationResult.failure(
                    null,
                    TerrainCoverageResolver.TerrainCoverageSelection.empty(),
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    "Own locator or target locator is missing/invalid."
            );
        }

        Location ownLocation = new Location(normalizedOwnLocator6);
        Location targetLocation = new Location(normalizedTargetLocator6);

        PathAnalysisRequest request = new PathAnalysisRequest(
                normalizedOwnLocator6,
                ownLocation.getLatitude().toDegrees(),
                ownLocation.getLongitude().toDegrees(),
                normalizeCallsignRaw(targetCallsignRaw),
                normalizedTargetLocator6,
                targetLocation.getLatitude().toDegrees(),
                targetLocation.getLongitude().toDegrees(),
                Double.isFinite(analysisFrequencyMHz) && analysisFrequencyMHz > 0.0
                        ? analysisFrequencyMHz
                        : PathGeometryUtils.DEFAULT_ANALYSIS_FREQUENCY_MHZ,
                sanitizeAntennaHeightMeters(ownAntennaHeightMeters),
                sanitizeAntennaHeightMeters(targetAntennaHeightMeters)
        );

        return prepareTerrainForPath(request, configuredDemRootDirectory);
    }

    private String buildSummaryMessage(TerrainCoverageResolver.TerrainCoverageSelection coverageSelection,
                                       TerrainCatalogClient.CatalogDownloadResult catalogDownloadResult,
                                       TerrainCatalogClient.CatalogLoadResult catalogLoadResult,
                                       TerrainPackageDownloader.BatchDownloadResult batchDownloadResult,
                                       List<String> missingPackageIds,
                                       List<TerrainPackageInstaller.InstallResult> installResults) {

        int successfulInstallCount = (int) installResults.stream()
                .filter(TerrainPackageInstaller.InstallResult::success)
                .count();

        int installedTileCount = installResults.stream()
                .filter(TerrainPackageInstaller.InstallResult::success)
                .mapToInt(TerrainPackageInstaller.InstallResult::installedTileCount)
                .sum();

        StringBuilder message = new StringBuilder();

        message.append(String.format(
                Locale.US,
                "Required coverage: %d package(s), %d tile(s), %.1f km path, %d coverage samples.",
                coverageSelection.packageIds().size(),
                coverageSelection.tileIds().size(),
                coverageSelection.totalDistanceKm(),
                coverageSelection.coverageSampleCount()
        ));

        if (catalogDownloadResult != null && catalogDownloadResult.message() != null && !catalogDownloadResult.message().isBlank()) {
            message.append("\n\nCatalog download: ").append(catalogDownloadResult.message());
        }

        if (catalogLoadResult != null && catalogLoadResult.message() != null && !catalogLoadResult.message().isBlank()) {
            message.append("\n\nCatalog load: ").append(catalogLoadResult.message());
        }

        if (batchDownloadResult != null && batchDownloadResult.message() != null && !batchDownloadResult.message().isBlank()) {
            message.append("\n\nPackage download: ").append(batchDownloadResult.message());
        }

        message.append(String.format(
                Locale.US,
                "\n\nInstallation: %d package(s) installed successfully, %d DEM tile(s) extracted.",
                successfulInstallCount,
                installedTileCount
        ));

        if (missingPackageIds != null && !missingPackageIds.isEmpty()) {
            message.append("\n\nMissing package ids in catalog:");
            for (String packageId : missingPackageIds) {
                message.append("\n- ").append(packageId);
            }
        }

        List<TerrainPackageInstaller.InstallResult> failedInstalls = installResults.stream()
                .filter(result -> !result.success())
                .toList();

        if (!failedInstalls.isEmpty()) {
            message.append("\n\nFailed installations:");
            for (TerrainPackageInstaller.InstallResult failedInstall : failedInstalls) {
                String packageId = failedInstall.terrainPackageManifest() == null
                        ? "<unknown>"
                        : failedInstall.terrainPackageManifest().packageId();
                message.append("\n- ").append(packageId).append(": ").append(failedInstall.message());
            }
        }

        return message.toString().trim();
    }

    private String normalizeLocator6(String locator6) {
        if (locator6 == null) {
            return "";
        }

        String trimmed = locator6.trim().toUpperCase(Locale.ROOT);
        return trimmed.length() >= 6 ? trimmed.substring(0, 6) : trimmed;
    }

    private String normalizeCallsignRaw(String callSignRaw) {
        return callSignRaw == null ? "" : callSignRaw.trim().toUpperCase(Locale.ROOT);
    }

    private double sanitizeAntennaHeightMeters(double antennaHeightMeters) {
        if (!Double.isFinite(antennaHeightMeters) || antennaHeightMeters < 0.0) {
            return 0.0;
        }
        return antennaHeightMeters;
    }

    /**
     * Combined result of one terrain preparation run.
     *
     * @param request original path request
     * @param coverageSelection resolved required coverage
     * @param catalogDownloadResult catalog download result
     * @param catalogLoadResult catalog load result
     * @param batchDownloadResult package download result
     * @param missingPackageIds required package ids that are not present in the catalog
     * @param installResults installation results for downloaded packages
     * @param success true if full required package coverage was available and at least one package was installed successfully
     * @param message human-readable summary message
     */
    public record TerrainPreparationResult(
            PathAnalysisRequest request,
            TerrainCoverageResolver.TerrainCoverageSelection coverageSelection,
            TerrainCatalogClient.CatalogDownloadResult catalogDownloadResult,
            TerrainCatalogClient.CatalogLoadResult catalogLoadResult,
            TerrainPackageDownloader.BatchDownloadResult batchDownloadResult,
            List<String> missingPackageIds,
            List<TerrainPackageInstaller.InstallResult> installResults,
            boolean success,
            String message
    ) {
        public TerrainPreparationResult {
            missingPackageIds = missingPackageIds == null ? List.of() : List.copyOf(missingPackageIds);
            installResults = installResults == null ? List.of() : List.copyOf(installResults);
        }

        public static TerrainPreparationResult failure(PathAnalysisRequest request,
                                                       TerrainCoverageResolver.TerrainCoverageSelection coverageSelection,
                                                       TerrainCatalogClient.CatalogDownloadResult catalogDownloadResult,
                                                       TerrainCatalogClient.CatalogLoadResult catalogLoadResult,
                                                       TerrainPackageDownloader.BatchDownloadResult batchDownloadResult,
                                                       List<String> missingPackageIds,
                                                       List<TerrainPackageInstaller.InstallResult> installResults,
                                                       String message) {
            return new TerrainPreparationResult(
                    request,
                    coverageSelection,
                    catalogDownloadResult,
                    catalogLoadResult,
                    batchDownloadResult,
                    missingPackageIds,
                    installResults,
                    false,
                    message
            );
        }

        public boolean hasInstalledAnything() {
            return installResults.stream().anyMatch(TerrainPackageInstaller.InstallResult::success);
        }
    }
}