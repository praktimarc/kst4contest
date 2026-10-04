package kst4contest.view.map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Offline terrain provider for locally extracted Copernicus GLO-30 DGED DEM tiles.
 *
 * <p>Assumptions of this reader:
 * <ul>
 *     <li>local tiles already exist on disk</li>
 *     <li>official DGED GeoTIFF filenames are used</li>
 *     <li>tiles represent 1° x 1° geocells</li>
 *     <li>the raster uses RasterPixelIsPoint semantics</li>
 * </ul>
 *
 * <p>The active improvement step uses great-circle interpolation for the
 * sampled path points. This avoids the path distortion of simple linear
 * latitude/longitude interpolation on longer Europe-wide paths.</p>
 */
public final class CopernicusGlo30TerrainProfileProvider implements TerrainProfileProvider {

    private static final String SOURCE_NAME = "Copernicus GLO-30 offline DEM";
    private static final double NODATA_VALUE = -32767.0;
    private static final int MAX_LOADED_TILES = 8;

    private final Supplier<String> demRootDirectorySupplier;
    private final OfflineDemManager offlineDemManager;

    private final Map<Path, LoadedTile> loadedTileCache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Path, LoadedTile> eldest) {
                    return size() > MAX_LOADED_TILES;
                }
            };

    public CopernicusGlo30TerrainProfileProvider(Supplier<String> demRootDirectorySupplier,
                                                 OfflineDemManager offlineDemManager) {
        this.demRootDirectorySupplier = Objects.requireNonNull(demRootDirectorySupplier, "demRootDirectorySupplier");
        this.offlineDemManager = Objects.requireNonNull(offlineDemManager, "offlineDemManager");
    }

    @Override
    public TerrainProfileData loadProfile(TerrainProfileRequest request) {
        if (request == null || !request.hasUsableEndpoints() || request.requestedSampleCount() < 2) {
            return TerrainProfileData.empty(SOURCE_NAME);
        }

        OfflineDemManager.OfflineDemIndex demIndex =
                offlineDemManager.inspectAndIndex(demRootDirectorySupplier.get(), DemDataset.COPERNICUS_GLO_30);

        if (!demIndex.usable()) {
            return TerrainProfileData.empty(SOURCE_NAME + " unavailable");
        }

        int sampleCount = Math.max(2, request.requestedSampleCount());
        List<PathProfilePoint> points = new ArrayList<>(sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            double t = sampleCount == 1 ? 0.0 : (double) i / (double) (sampleCount - 1);

            PathGeometryUtils.GeoPoint interpolatedPoint =
                    PathGeometryUtils.interpolateGreatCirclePoint(
                            request.fromLatitudeDeg(),
                            request.fromLongitudeDeg(),
                            request.toLatitudeDeg(),
                            request.toLongitudeDeg(),
                            t
                    );

            double latitudeDeg = interpolatedPoint.latitudeDeg();
            double longitudeDeg = interpolatedPoint.longitudeDeg();
            double distanceKm = request.totalDistanceKm() * t;

            if (!Double.isFinite(latitudeDeg) || !Double.isFinite(longitudeDeg)) {
                return TerrainProfileData.empty(SOURCE_NAME + " path interpolation failed");
            }

            Path tilePath = demIndex.findTilePath(latitudeDeg, longitudeDeg);
            if (tilePath == null) {
                return TerrainProfileData.empty(String.format(
                        Locale.US,
                        "%s missing required tile(s) near %.5f / %.5f",
                        SOURCE_NAME,
                        latitudeDeg,
                        longitudeDeg
                ));
            }

            LoadedTile loadedTile = getOrLoadTile(tilePath);
            if (loadedTile == null) {
                return TerrainProfileData.empty(SOURCE_NAME + " tile read failed");
            }

            double elevationMeters = sampleElevationMeters(loadedTile, latitudeDeg, longitudeDeg);
            if (!Double.isFinite(elevationMeters)) {
                return TerrainProfileData.empty(String.format(
                        Locale.US,
                        "%s contains no-data sample(s) near %.5f / %.5f",
                        SOURCE_NAME,
                        latitudeDeg,
                        longitudeDeg
                ));
            }

            points.add(new PathProfilePoint(
                    distanceKm,
                    latitudeDeg,
                    longitudeDeg,
                    elevationMeters
            ));
        }

        return new TerrainProfileData(points, SOURCE_NAME, false);
    }

    private synchronized LoadedTile getOrLoadTile(Path tilePath) {
        LoadedTile cachedTile = loadedTileCache.get(tilePath);
        if (cachedTile != null) {
            return cachedTile;
        }

        LoadedTile loadedTile = loadTile(tilePath);
        if (loadedTile != null) {
            loadedTileCache.put(tilePath, loadedTile);
        }

        return loadedTile;
    }

    private LoadedTile loadTile(Path tilePath) {
        if (tilePath == null || !Files.isRegularFile(tilePath)) {
            return null;
        }

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(tilePath.toFile())) {
            if (imageInputStream == null) {
                return null;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader imageReader = readers.next();
            try {
                imageReader.setInput(imageInputStream, true, true);
                Raster raster = imageReader.readRaster(0, null);

                if (raster == null || raster.getWidth() < 2 || raster.getHeight() < 2) {
                    return null;
                }

                return new LoadedTile(
                        tilePath,
                        raster,
                        raster.getWidth(),
                        raster.getHeight(),
                        parseSouthDeg(tilePath.getFileName().toString()),
                        parseWestDeg(tilePath.getFileName().toString())
                );
            } finally {
                imageReader.dispose();
            }
        } catch (IOException exception) {
            System.err.println("[StationMap] Could not read DEM tile " + tilePath + ": " + exception.getMessage());
            return null;
        }
    }

    private int parseSouthDeg(String filename) {
        ParsedTileKey key = ParsedTileKey.fromFilename(filename);
        return key == null ? 0 : key.southDeg();
    }

    private int parseWestDeg(String filename) {
        ParsedTileKey key = ParsedTileKey.fromFilename(filename);
        return key == null ? 0 : key.westDeg();
    }

    /**
     * Samples one DEM tile using bilinear interpolation.
     *
     * <p>The current reader assumes 1° x 1° geocells and derives raster
     * coordinates directly from the sample latitude/longitude.</p>
     *
     * @param tile loaded DEM tile
     * @param latitudeDeg sample latitude in degrees
     * @param longitudeDeg sample longitude in degrees
     * @return interpolated elevation in meters or NaN
     */
    private double sampleElevationMeters(LoadedTile tile, double latitudeDeg, double longitudeDeg) {
        if (tile == null) {
            return Double.NaN;
        }

        double x = (longitudeDeg - tile.westDeg()) * (tile.width() - 1);
        double y = ((tile.southDeg() + 1.0) - latitudeDeg) * (tile.height() - 1);

        x = clamp(x, 0.0, tile.width() - 1.0);
        y = clamp(y, 0.0, tile.height() - 1.0);

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = Math.min(x0 + 1, tile.width() - 1);
        int y1 = Math.min(y0 + 1, tile.height() - 1);

        double q11 = readSample(tile.raster(), x0, y0);
        double q21 = readSample(tile.raster(), x1, y0);
        double q12 = readSample(tile.raster(), x0, y1);
        double q22 = readSample(tile.raster(), x1, y1);

        if (!Double.isFinite(q11) || !Double.isFinite(q21) || !Double.isFinite(q12) || !Double.isFinite(q22)) {
            double nearest = readSample(tile.raster(), (int) Math.round(x), (int) Math.round(y));
            return Double.isFinite(nearest) ? nearest : Double.NaN;
        }

        double dx = x - x0;
        double dy = y - y0;

        double top = q11 + (q21 - q11) * dx;
        double bottom = q12 + (q22 - q12) * dx;

        return top + (bottom - top) * dy;
    }

    private double readSample(Raster raster, int x, int y) {
        double value = raster.getSampleDouble(x, y, 0);
        if (!Double.isFinite(value) || value <= NODATA_VALUE) {
            return Double.NaN;
        }
        return value;
    }

    private double clamp(double value, double minValue, double maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private record LoadedTile(
            Path path,
            Raster raster,
            int width,
            int height,
            int southDeg,
            int westDeg
    ) {
    }

    private record ParsedTileKey(int southDeg, int westDeg) {

        private static final java.util.regex.Pattern TILE_PATTERN =
                java.util.regex.Pattern.compile("(?i)^Copernicus_[A-Z]{3}_10_([NS])(\\d{2})_(\\d{2})_([EW])(\\d{3})_(\\d{2})_DEM\\.tif$");

        static ParsedTileKey fromFilename(String filename) {
            if (filename == null) {
                return null;
            }

            var matcher = TILE_PATTERN.matcher(filename);
            if (!matcher.matches()) {
                return null;
            }

            int south = signed(matcher.group(1), matcher.group(2));
            int west = signed(matcher.group(4), matcher.group(5));

            return new ParsedTileKey(south, west);
        }

        private static int signed(String direction, String degrees) {
            int value = Integer.parseInt(degrees);
            if ("S".equalsIgnoreCase(direction) || "W".equalsIgnoreCase(direction)) {
                return -value;
            }
            return value;
        }
    }
}