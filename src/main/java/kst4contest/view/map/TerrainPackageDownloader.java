package kst4contest.view.map;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Downloads terrain package archives (*.tpak) to the local .praktiKST directory.
 *
 * <p>This implementation is intentionally simple and robust:
 * <ul>
 *     <li>download packages by catalog entry</li>
 *     <li>store them below terrain/packages</li>
 *     <li>reuse existing files when the checksum already matches</li>
 * </ul>
 */
public final class TerrainPackageDownloader {

    private static final String LOCAL_TERRAIN_PACKAGES_RELATIVE_DIRECTORY = "terrain/packages";

    private final HttpClient httpClient;

    public TerrainPackageDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads all required package ids that are present in the catalog.
     *
     * @param terrainCatalog current terrain catalog
     * @param requiredPackageIds ordered package ids
     * @return batch download result
     */
    public BatchDownloadResult downloadPackages(TerrainCatalog terrainCatalog, List<String> requiredPackageIds) {
        List<PackageDownloadResult> itemResults = new ArrayList<>();

        if (terrainCatalog == null || requiredPackageIds == null || requiredPackageIds.isEmpty()) {
            return new BatchDownloadResult(List.of(), "No terrain packages were requested.");
        }

        for (String packageId : requiredPackageIds) {
            PackageDownloadResult itemResult = terrainCatalog.findPackageById(packageId)
                    .map(this::downloadPackage)
                    .orElseGet(() -> new PackageDownloadResult(
                            packageId,
                            null,
                            false,
                            false,
                            "Package id is not present in the loaded catalog."
                    ));

            itemResults.add(itemResult);
        }

        return new BatchDownloadResult(itemResults, buildBatchMessage(itemResults));
    }

    /**
     * Downloads one package archive from one catalog entry.
     *
     * @param packageEntry catalog package entry
     * @return download result
     */
    public PackageDownloadResult downloadPackage(TerrainCatalogPackageEntry packageEntry) {
        if (packageEntry == null || !packageEntry.isUsable()) {
            return new PackageDownloadResult(
                    "",
                    null,
                    false,
                    false,
                    "Terrain package entry is missing or incomplete."
            );
        }

        Path packagesDirectory = resolveLocalPackagesDirectory();
        Path localPackageFile = packagesDirectory.resolve(packageEntry.packageId() + ".tpak");

        try {
            Files.createDirectories(packagesDirectory);

            if (Files.isRegularFile(localPackageFile)
                    && !packageEntry.sha256().isBlank()
                    && packageEntry.sha256().equalsIgnoreCase(computeSha256(localPackageFile))) {
                return new PackageDownloadResult(
                        packageEntry.packageId(),
                        localPackageFile,
                        true,
                        false,
                        "Terrain package is already present locally and matches the expected checksum."
                );
            }

            Path tempFile = packagesDirectory.resolve(packageEntry.packageId() + ".download");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(packageEntry.downloadUrl()))
                    .GET()
                    .build();

            HttpResponse<Path> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofFile(tempFile)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                Files.deleteIfExists(tempFile);

                return new PackageDownloadResult(
                        packageEntry.packageId(),
                        localPackageFile,
                        false,
                        false,
                        "Terrain package download failed with HTTP status " + response.statusCode() + "."
                );
            }

            if (!packageEntry.sha256().isBlank()) {
                String actualSha256 = computeSha256(tempFile);
                if (!packageEntry.sha256().equalsIgnoreCase(actualSha256)) {
                    Files.deleteIfExists(tempFile);

                    return new PackageDownloadResult(
                            packageEntry.packageId(),
                            localPackageFile,
                            false,
                            false,
                            "Downloaded terrain package checksum does not match the catalog."
                    );
                }
            }

            Files.move(tempFile, localPackageFile, StandardCopyOption.REPLACE_EXISTING);

            return new PackageDownloadResult(
                    packageEntry.packageId(),
                    localPackageFile,
                    true,
                    true,
                    "Terrain package downloaded successfully."
            );
        } catch (Exception exception) {
            return new PackageDownloadResult(
                    packageEntry.packageId(),
                    localPackageFile,
                    false,
                    false,
                    "Terrain package download failed: " + exception.getMessage()
            );
        }
    }

    private Path resolveLocalPackagesDirectory() {
        return Path.of(ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME,
                LOCAL_TERRAIN_PACKAGES_RELATIVE_DIRECTORY
        ));
    }

    private String computeSha256(Path file) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        }

        return HexFormat.of().formatHex(messageDigest.digest());
    }

    private String buildBatchMessage(List<PackageDownloadResult> itemResults) {
        int successfulCount = 0;
        int downloadedCount = 0;
        int reusedCount = 0;

        for (PackageDownloadResult itemResult : itemResults) {
            if (itemResult.success()) {
                successfulCount++;
            }
            if (itemResult.downloadedNow()) {
                downloadedCount++;
            } else if (itemResult.success()) {
                reusedCount++;
            }
        }

        return "Terrain packages processed: "
                + itemResults.size()
                + ", successful: "
                + successfulCount
                + ", downloaded now: "
                + downloadedCount
                + ", reused locally: "
                + reusedCount
                + ".";
    }

    /**
     * Result of one terrain package download attempt.
     *
     * @param packageId canonical package id
     * @param localPackageFile local target package file
     * @param success true if a usable local package file is available afterward
     * @param downloadedNow true if the package was downloaded in this run
     * @param message human-readable result text
     */
    public record PackageDownloadResult(
            String packageId,
            Path localPackageFile,
            boolean success,
            boolean downloadedNow,
            String message
    ) {
    }

    /**
     * Result of a batch terrain package download attempt.
     *
     * @param itemResults one result per requested package
     * @param message human-readable summary text
     */
    public record BatchDownloadResult(
            List<PackageDownloadResult> itemResults,
            String message
    ) {
        public BatchDownloadResult {
            itemResults = itemResults == null ? List.of() : List.copyOf(itemResults);
        }

        public boolean allSuccessful() {
            return !itemResults.isEmpty() && itemResults.stream().allMatch(PackageDownloadResult::success);
        }

        public List<Path> successfulPackageFiles() {
            return itemResults.stream()
                    .filter(PackageDownloadResult::success)
                    .map(PackageDownloadResult::localPackageFile)
                    .filter(path -> path != null)
                    .toList();
        }
    }
}