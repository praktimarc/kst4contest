package kst4contest.view.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small manager for offline DEM discovery and indexing.
 *
 * This class intentionally supports both intermediate API shapes that appeared
 * during development:
 *
 * - old style:
 *   inspect(...) -> OfflineDemStatus
 *
 * - new style:
 *   inspectAndIndex(...) -> OfflineDemIndex
 *
 * This keeps the current codebase compilable even if older helper/provider
 * classes are still present in the source tree.
 */
public final class OfflineDemManager {

    /**
     * Matches official DGED DEM filenames like:
     * Copernicus_DSM_10_N50_00_E020_00_DEM.tif
     */
    private static final Pattern COPERNICUS_GLO_30_DEM_FILE_PATTERN =
            Pattern.compile("(?i)^Copernicus_[A-Z]{3}_10_([NS])(\\d{2})_(\\d{2})_([EW])(\\d{3})_(\\d{2})_DEM\\.tif$");

    /**
     * Returns true if the filename matches the currently supported Copernicus
     * GLO-30 DGED GeoTIFF tile naming scheme.
     *
     * <p>This helper is intentionally public so that UI/import helpers can
     * validate manually selected files before copying them into the DEM root.</p>
     *
     * @param filename candidate filename
     * @return true if the filename looks like a supported local DEM tile
     */
    public static boolean isSupportedCopernicusGlo30DemFilename(String filename) {
        return filename != null
                && COPERNICUS_GLO_30_DEM_FILE_PATTERN.matcher(filename).matches();
    }

    private String lastIndexedRootDirectory = null;
    private DemDataset lastIndexedDataset = null;
    private OfflineDemIndex lastIndex = OfflineDemIndex.empty("Offline DEM root directory is not configured.");

    /**
     * Newer API used by the active Copernicus provider.
     */
    public synchronized OfflineDemIndex inspectAndIndex(String demRootDirectory, DemDataset dataset) {
        DemDataset effectiveDataset = dataset == null ? DemDataset.COPERNICUS_GLO_30 : dataset;

        if (demRootDirectory == null || demRootDirectory.isBlank()) {
            lastIndexedRootDirectory = demRootDirectory;
            lastIndexedDataset = effectiveDataset;
            lastIndex = OfflineDemIndex.empty("Offline DEM root directory is not configured.");
            return lastIndex;
        }

        String normalizedRootDirectory = demRootDirectory.trim();

        if (normalizedRootDirectory.equals(lastIndexedRootDirectory) && effectiveDataset == lastIndexedDataset) {
            return lastIndex;
        }

        lastIndexedRootDirectory = normalizedRootDirectory;
        lastIndexedDataset = effectiveDataset;
        lastIndex = buildIndex(normalizedRootDirectory, effectiveDataset);

        return lastIndex;
    }

    /**
     * Older compatibility API still referenced by older intermediate provider code.
     */
    public synchronized OfflineDemStatus inspect(String demRootDirectory, DemDataset dataset) {
        OfflineDemIndex index = inspectAndIndex(demRootDirectory, dataset);

        return new OfflineDemStatus(
                index.dataset(),
                index.rootDirectory(),
                demRootDirectory != null && !demRootDirectory.isBlank(),
                index.usable(),
                index.message()
        );
    }

    private OfflineDemIndex buildIndex(String demRootDirectory, DemDataset dataset) {
        Path rootPath = Paths.get(demRootDirectory);

        if (!Files.exists(rootPath)) {
            return OfflineDemIndex.empty("Configured DEM root directory does not exist.");
        }

        if (!Files.isDirectory(rootPath)) {
            return OfflineDemIndex.empty("Configured DEM root path is not a directory.");
        }

        if (!Files.isReadable(rootPath)) {
            return OfflineDemIndex.empty("Configured DEM root directory is not readable.");
        }

        Map<String, Path> tilePathByGeocellKey = new LinkedHashMap<>();

        try (var pathStream = Files.walk(rootPath)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .forEach(path -> tryRegisterTile(path, tilePathByGeocellKey));
        } catch (IOException exception) {
            return OfflineDemIndex.empty("Scanning DEM root directory failed: " + exception.getMessage());
        }

        if (tilePathByGeocellKey.isEmpty()) {
            return new OfflineDemIndex(
                    dataset,
                    rootPath.toAbsolutePath().toString(),
                    false,
                    Collections.emptyMap(),
                    "No local Copernicus GLO-30 DEM tiles were found below the configured root directory."
            );
        }

        return new OfflineDemIndex(
                dataset,
                rootPath.toAbsolutePath().toString(),
                true,
                Map.copyOf(tilePathByGeocellKey),
                "Found " + tilePathByGeocellKey.size() + " local Copernicus GLO-30 DEM tiles."
        );
    }

    private void tryRegisterTile(Path path, Map<String, Path> tilePathByGeocellKey) {
        String filename = path.getFileName().toString();
        Matcher matcher = COPERNICUS_GLO_30_DEM_FILE_PATTERN.matcher(filename);

        if (!matcher.matches()) {
            return;
        }

        int southDeg = signedDegrees(matcher.group(1), matcher.group(2), matcher.group(3));
        int westDeg = signedDegrees(matcher.group(4), matcher.group(5), matcher.group(6));

        tilePathByGeocellKey.put(toGeocellKey(southDeg, westDeg), path.toAbsolutePath());
    }

    private int signedDegrees(String hemisphereOrDirection, String integerPart, String decimalPart) {
        int sign = ("S".equalsIgnoreCase(hemisphereOrDirection) || "W".equalsIgnoreCase(hemisphereOrDirection)) ? -1 : 1;
        int degrees = Integer.parseInt(integerPart);

        if (!"00".equals(decimalPart)) {
            // The current reader expects whole-degree LL-corner geocells.
            return sign * degrees;
        }

        return sign * degrees;
    }

    private String toGeocellKey(int southDeg, int westDeg) {
        return southDeg + ":" + westDeg;
    }

    /**
     * Newer indexed offline DEM state used by the active Copernicus provider.
     */
    public record OfflineDemIndex(
            DemDataset dataset,
            String rootDirectory,
            boolean usable,
            Map<String, Path> tilePathByGeocellKey,
            String message
    ) {

        public static OfflineDemIndex empty(String message) {
            return new OfflineDemIndex(
                    DemDataset.COPERNICUS_GLO_30,
                    "",
                    false,
                    Collections.emptyMap(),
                    message
            );
        }

        public Path findTilePath(double latitudeDeg, double longitudeDeg) {
            int southDeg = (int) Math.floor(latitudeDeg);
            int westDeg = (int) Math.floor(longitudeDeg);
            return tilePathByGeocellKey.get(southDeg + ":" + westDeg);
        }

        public int availableTileCount() {
            return tilePathByGeocellKey.size();
        }
    }

    /**
     * Older compatibility status shape still referenced by older intermediate code.
     */
    public record OfflineDemStatus(
            DemDataset dataset,
            String demRootDirectory,
            boolean configured,
            boolean usableRootDirectory,
            String message
    ) {
    }
}