package kst4contest.view.map;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads and parses the terrain package catalog.
 *
 * <p>The first implementation intentionally uses XML instead of JSON because:
 * <ul>
 *     <li>the current project already contains XML parsing patterns</li>
 *     <li>no additional JSON dependency is required</li>
 *     <li>we can move faster toward a working package download/install flow</li>
 * </ul>
 *
 * <p>The parsed result still maps into the shared terrain catalog model classes.</p>
 */
public final class TerrainCatalogClient {

    private static final String LOCAL_TERRAIN_CATALOG_RELATIVE_PATH = "terrain/catalog/terrain-catalog-v1.xml";

    private final HttpClient httpClient;

    public TerrainCatalogClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads the remote XML catalog and stores it below the local .praktiKST directory.
     *
     * @param catalogUrl full catalog URL
     * @return download result
     */
    public CatalogDownloadResult downloadCatalog(String catalogUrl) {
        Path localCatalogFile = resolveLocalCatalogFile();

        try {
            Files.createDirectories(localCatalogFile.getParent());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(catalogUrl))
                    .GET()
                    .build();

            HttpResponse<Path> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofFile(localCatalogFile)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new CatalogDownloadResult(
                        localCatalogFile,
                        false,
                        "Catalog download failed with HTTP status " + response.statusCode() + "."
                );
            }

            return new CatalogDownloadResult(
                    localCatalogFile,
                    true,
                    "Catalog downloaded successfully to:\n" + localCatalogFile.toAbsolutePath()
            );
        } catch (Exception exception) {
            return new CatalogDownloadResult(
                    localCatalogFile,
                    false,
                    "Catalog download failed: " + exception.getMessage()
            );
        }
    }

    /**
     * Loads the previously downloaded local catalog file.
     *
     * @return parsed catalog load result
     */
    public CatalogLoadResult loadLocalCatalog() {
        return loadCatalog(resolveLocalCatalogFile());
    }

    /**
     * Loads and parses one XML catalog file.
     *
     * @param catalogFile local catalog XML file
     * @return parsed catalog load result
     */
    public CatalogLoadResult loadCatalog(Path catalogFile) {
        if (catalogFile == null || !Files.isRegularFile(catalogFile)) {
            return new CatalogLoadResult(
                    null,
                    catalogFile,
                    false,
                    "Local terrain catalog file does not exist."
            );
        }

        try {
            Document document = parseXmlDocument(catalogFile.toFile());
            TerrainCatalog terrainCatalog = parseTerrainCatalog(document);

            return new CatalogLoadResult(
                    terrainCatalog,
                    catalogFile,
                    terrainCatalog != null && terrainCatalog.hasPackages(),
                    terrainCatalog != null && terrainCatalog.hasPackages()
                            ? "Terrain catalog loaded successfully."
                            : "Terrain catalog was loaded but contains no packages."
            );
        } catch (Exception exception) {
            return new CatalogLoadResult(
                    null,
                    catalogFile,
                    false,
                    "Could not parse terrain catalog: " + exception.getMessage()
            );
        }
    }

    private Path resolveLocalCatalogFile() {
        return Path.of(ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME,
                LOCAL_TERRAIN_CATALOG_RELATIVE_PATH
        ));
    }

    private Document parseXmlDocument(File xmlFile) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(xmlFile);
    }

    private TerrainCatalog parseTerrainCatalog(Document document) {
        Element root = document.getDocumentElement();
        if (root == null || !"terrainCatalog".equals(root.getTagName())) {
            throw new IllegalArgumentException("Unexpected catalog root element.");
        }

        int schemaVersion = parseIntAttribute(root, "schemaVersion", 1);
        int catalogVersion = parseIntAttribute(root, "catalogVersion", 1);
        String generatedAtUtc = root.getAttribute("generatedAtUtc");
        String regionSet = root.getAttribute("regionSet");
        String packageBaseUrl = root.getAttribute("packageBaseUrl");

        String sourceAttribution = getDirectChildText(root, "sourceAttribution");
        String licenseNotice = getDirectChildText(root, "licenseNotice");
        String disclaimerNotice = getDirectChildText(root, "disclaimerNotice");

        Element packagesElement = getFirstDirectChild(root, "packages");
        List<TerrainCatalogPackageEntry> packageEntries = new ArrayList<>();

        if (packagesElement != null) {
            List<Element> packageElements = getDirectChildElements(packagesElement, "package");

            for (Element packageElement : packageElements) {
                List<String> tileIds = new ArrayList<>();

                Element tileIdsElement = getFirstDirectChild(packageElement, "tileIds");
                if (tileIdsElement != null) {
                    for (Element tileIdElement : getDirectChildElements(tileIdsElement, "tileId")) {
                        tileIds.add(tileIdElement.getTextContent());
                    }
                }

                packageEntries.add(new TerrainCatalogPackageEntry(
                        packageElement.getAttribute("packageId"),
                        packageElement.getAttribute("regionType"),
                        packageElement.getAttribute("regionId"),
                        parseIntAttribute(packageElement, "packageVersion", 1),
                        packageElement.getAttribute("downloadUrl"),
                        parseLongAttribute(packageElement, "sizeBytes", 0L),
                        packageElement.getAttribute("sha256"),
                        parseDoubleAttribute(packageElement, "minLatitudeDeg", Double.NaN),
                        parseDoubleAttribute(packageElement, "maxLatitudeDeg", Double.NaN),
                        parseDoubleAttribute(packageElement, "minLongitudeDeg", Double.NaN),
                        parseDoubleAttribute(packageElement, "maxLongitudeDeg", Double.NaN),
                        tileIds,
                        packageElement.getAttribute("sourceDataset"),
                        getDirectChildText(packageElement, "sourceAttribution"),
                        getDirectChildText(packageElement, "derivedProductNotice"),
                        getDirectChildText(packageElement, "disclaimerNotice")
                ));
            }
        }

        return new TerrainCatalog(
                schemaVersion,
                catalogVersion,
                generatedAtUtc,
                regionSet,
                packageBaseUrl,
                sourceAttribution,
                licenseNotice,
                disclaimerNotice,
                packageEntries
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

    private static long parseLongAttribute(Element element, String attributeName, long defaultValue) {
        try {
            return Long.parseLong(element.getAttribute(attributeName));
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
     * Download result for the XML terrain catalog.
     *
     * @param localCatalogFile target local file path
     * @param success true if the download succeeded
     * @param message human-readable result text
     */
    public record CatalogDownloadResult(
            Path localCatalogFile,
            boolean success,
            String message
    ) {
    }

    /**
     * Parse/load result for one terrain catalog.
     *
     * @param terrainCatalog parsed catalog, or null
     * @param localCatalogFile source file path
     * @param success true if a usable catalog was loaded
     * @param message human-readable result text
     */
    public record CatalogLoadResult(
            TerrainCatalog terrainCatalog,
            Path localCatalogFile,
            boolean success,
            String message
    ) {
    }
}