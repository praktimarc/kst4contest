package kst4contest.view.map;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Installs one downloaded terrain package into the local DEM directory.
 *
 * <p>This first vertical slice intentionally extracts raw Copernicus *_DEM.tif
 * files, because the current runtime already knows how to scan and load them.
 * That means we get an end-to-end working package flow quickly without first
 * introducing a second runtime terrain format.</p>
 */
public final class TerrainPackageInstaller {

    private static final String DEFAULT_LOCAL_DEM_ROOT_RELATIVE_DIRECTORY = "dem/copernicus_glo30";

    /**
     * Installs one terrain package into the configured DEM root directory.
     *
     * <p>If no DEM root directory is configured, the default directory below
     * .praktiKST is used automatically.</p>
     *
     * @param packageFile local *.tpak file
     * @param configuredDemRootDirectory user-configured DEM root directory text
     * @return installation result
     */
    public InstallResult installPackage(Path packageFile, String configuredDemRootDirectory) {
        if (packageFile == null || !Files.isRegularFile(packageFile)) {
            return new InstallResult(
                    null,
                    null,
                    0,
                    false,
                    "Terrain package file does not exist."
            );
        }

        try (ZipFile zipFile = new ZipFile(packageFile.toFile())) {
            ZipEntry manifestEntry = zipFile.getEntry("manifest.xml");
            if (manifestEntry == null) {
                return new InstallResult(
                        null,
                        null,
                        0,
                        false,
                        "Terrain package does not contain manifest.xml."
                );
            }

            TerrainPackageManifest terrainPackageManifest;
            try (InputStream manifestInputStream = zipFile.getInputStream(manifestEntry)) {
                terrainPackageManifest = parseManifest(manifestInputStream);
            }

            if (terrainPackageManifest == null || !terrainPackageManifest.hasUsableTiles()) {
                return new InstallResult(
                        terrainPackageManifest,
                        null,
                        0,
                        false,
                        "Terrain package manifest is missing or contains no usable tiles."
                );
            }

            Path demRootDirectory = resolveEffectiveDemRootDirectory(configuredDemRootDirectory);
            Path targetPackageDirectory = demRootDirectory
                    .resolve("packages")
                    .resolve(terrainPackageManifest.packageId());

            Files.createDirectories(targetPackageDirectory);

            int installedTileCount = 0;

            List<? extends ZipEntry> zipEntries = zipFile.stream().toList();
            for (ZipEntry zipEntry : zipEntries) {
                if (zipEntry.isDirectory()) {
                    continue;
                }

                String entryName = zipEntry.getName();
                String fileName = Path.of(entryName).getFileName().toString();

                if ("manifest.xml".equalsIgnoreCase(fileName)) {
                    Path targetFile = targetPackageDirectory.resolve("manifest.xml");
                    extractZipEntry(zipFile, zipEntry, targetFile);
                    continue;
                }

                if (!OfflineDemManager.isSupportedCopernicusGlo30DemFilename(fileName)) {
                    continue;
                }

                Path targetFile = targetPackageDirectory.resolve(fileName);
                extractZipEntry(zipFile, zipEntry, targetFile);
                installedTileCount++;
            }

            if (installedTileCount <= 0) {
                return new InstallResult(
                        terrainPackageManifest,
                        targetPackageDirectory,
                        0,
                        false,
                        "Terrain package did not contain any supported Copernicus *_DEM.tif files."
                );
            }

            return new InstallResult(
                    terrainPackageManifest,
                    targetPackageDirectory,
                    installedTileCount,
                    true,
                    "Installed "
                            + installedTileCount
                            + " DEM tile(s) into:\n"
                            + targetPackageDirectory.toAbsolutePath()
            );
        } catch (Exception exception) {
            return new InstallResult(
                    null,
                    null,
                    0,
                    false,
                    "Terrain package installation failed: " + exception.getMessage()
            );
        }
    }

    private Path resolveEffectiveDemRootDirectory(String configuredDemRootDirectory) {
        if (configuredDemRootDirectory != null && !configuredDemRootDirectory.isBlank()) {
            return Path.of(configuredDemRootDirectory.trim());
        }

        return Path.of(ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME,
                DEFAULT_LOCAL_DEM_ROOT_RELATIVE_DIRECTORY
        ));
    }

    private void extractZipEntry(ZipFile zipFile, ZipEntry zipEntry, Path targetFile) throws Exception {
        Path normalizedTarget = targetFile.normalize();

        if (normalizedTarget.getParent() != null) {
            Files.createDirectories(normalizedTarget.getParent());
        }

        try (InputStream entryInputStream = zipFile.getInputStream(zipEntry)) {
            Files.copy(entryInputStream, normalizedTarget, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private TerrainPackageManifest parseManifest(InputStream inputStream) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(inputStream);

        Element root = document.getDocumentElement();
        if (root == null || !"terrainPackageManifest".equals(root.getTagName())) {
            throw new IllegalArgumentException("Unexpected package manifest root element.");
        }

        int schemaVersion = parseIntAttribute(root, "schemaVersion", 1);
        String packageId = root.getAttribute("packageId");
        int packageVersion = parseIntAttribute(root, "packageVersion", 1);
        String regionType = root.getAttribute("regionType");
        String regionId = root.getAttribute("regionId");

        double minLatitudeDeg = parseDoubleAttribute(root, "minLatitudeDeg", Double.NaN);
        double maxLatitudeDeg = parseDoubleAttribute(root, "maxLatitudeDeg", Double.NaN);
        double minLongitudeDeg = parseDoubleAttribute(root, "minLongitudeDeg", Double.NaN);
        double maxLongitudeDeg = parseDoubleAttribute(root, "maxLongitudeDeg", Double.NaN);

        String primaryDataset = root.getAttribute("primaryDataset");
        String fallbackDataset = root.getAttribute("fallbackDataset");
        String packageBuiltAtUtc = root.getAttribute("packageBuiltAtUtc");
        String packageSha256 = root.getAttribute("packageSha256");

        String sourceAttribution = getDirectChildText(root, "sourceAttribution");
        String derivedProductNotice = getDirectChildText(root, "derivedProductNotice");
        String disclaimerNotice = getDirectChildText(root, "disclaimerNotice");

        List<TerrainTileMetadata> tiles = new ArrayList<>();
        Element tilesElement = getFirstDirectChild(root, "tiles");

        if (tilesElement != null) {
            for (Element tileElement : getDirectChildElements(tilesElement, "tile")) {
                tiles.add(new TerrainTileMetadata(
                        tileElement.getAttribute("tileId"),
                        tileElement.getAttribute("fileName"),
                        parseIntAttribute(tileElement, "southDeg", 0),
                        parseIntAttribute(tileElement, "westDeg", 0),
                        parseIntAttribute(tileElement, "width", 3601),
                        parseIntAttribute(tileElement, "height", 3601),
                        parseIntAttribute(tileElement, "arcSecondResolution", 1),
                        (short) parseIntAttribute(tileElement, "noDataValue", -32768),
                        tileElement.getAttribute("sourceDataset"),
                        tileElement.getAttribute("sha256")
                ));
            }
        }

        return new TerrainPackageManifest(
                schemaVersion,
                packageId,
                packageVersion,
                regionType,
                regionId,
                minLatitudeDeg,
                maxLatitudeDeg,
                minLongitudeDeg,
                maxLongitudeDeg,
                primaryDataset,
                fallbackDataset,
                packageBuiltAtUtc,
                sourceAttribution,
                derivedProductNotice,
                disclaimerNotice,
                packageSha256,
                tiles
        );
    }

    private static List<Element> getDirectChildElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();

        if (parent == null) {
            return result;
        }

        for (int index = 0; index < parent.getChildNodes().getLength(); index++) {
            Node node = parent.getChildNodes().item(index);
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                result.add(element);
            }
        }

        return result;
    }

    private static Element getFirstDirectChild(Element parent, String tagName) {
        for (Element element : getDirectChildElements(parent, tagName)) {
            return element;
        }
        return null;
    }

    private static String getDirectChildText(Element parent, String tagName) {
        Element child = getFirstDirectChild(parent, tagName);
        return child == null ? "" : child.getTextContent().trim();
    }

    private static int parseIntAttribute(Element element, String attributeName, int defaultValue) {
        try {
            return Integer.parseInt(element.getAttribute(attributeName));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static double parseDoubleAttribute(Element element, String attributeName, double defaultValue) {
        try {
            return Double.parseDouble(element.getAttribute(attributeName));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * Result of one terrain package installation.
     *
     * @param terrainPackageManifest parsed manifest, or null
     * @param targetPackageDirectory extraction target directory
     * @param installedTileCount number of extracted DEM GeoTIFF files
     * @param success true if at least one usable tile was installed
     * @param message human-readable result text
     */
    public record InstallResult(
            TerrainPackageManifest terrainPackageManifest,
            Path targetPackageDirectory,
            int installedTileCount,
            boolean success,
            String message
    ) {
    }
}