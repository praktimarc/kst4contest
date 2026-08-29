package kst4contest.view.map;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small helper service for preparing and filling the local offline DEM directory.
 *
 * <p>This service only prepares local data for a possible future offline terrain
 * provider:
 * <ul>
 *     <li>create a known default Copernicus DEM root directory below .praktiKST</li>
 *     <li>copy manually selected local *_DEM.tif files into that directory</li>
 * </ul>
 *
 * <p>Importing files does not activate an offline provider or change the terrain
 * calculation chain. The active provider remains Open-Meteo with Copernicus GLO-90
 * data. Valid Copernicus GLO-30 DGED GeoTIFF files are only copied into the prepared
 * directory tree.</p>
 */
public final class OfflineDemImportService {

    /**
     * Default relative DEM directory below the application's hidden home folder.
     */
    public static final String DEFAULT_RELATIVE_COPERNICUS_ROOT_DIRECTORY = "dem/copernicus_glo30";

    /**
     * Resolves the default local Copernicus DEM root directory below .praktiKST.
     *
     * @return default DEM root directory path
     */
    public Path resolveDefaultCopernicusRootDirectory() {
        return Path.of(
                ApplicationFileUtils.getFilePath(
                        ApplicationConstants.APPLICATION_NAME,
                        DEFAULT_RELATIVE_COPERNICUS_ROOT_DIRECTORY
                )
        );
    }

    /**
     * Ensures that the default local Copernicus DEM root directory exists.
     *
     * @return preparation result including the effective target directory
     */
    public ImportResult ensureDefaultCopernicusRootDirectory() {
        Path targetRootDirectory = resolveDefaultCopernicusRootDirectory();
        return ensureTargetDirectoryExists(targetRootDirectory);
    }

    /**
     * Imports manually selected Copernicus DGED GeoTIFF tiles into the configured
     * DEM root directory.
     *
     * <p>If the configured DEM root directory is blank, the default directory
     * below .praktiKST is used automatically.</p>
     *
     * @param selectedFiles selected local files from the file chooser
     * @param configuredDemRootDirectory current configured DEM root directory text
     * @return import result with counts and a user-friendly summary message
     */
    public ImportResult importTiles(List<File> selectedFiles, String configuredDemRootDirectory) {
        Path targetRootDirectory = resolveEffectiveTargetRootDirectory(configuredDemRootDirectory);

        ImportResult directoryPreparationResult = ensureTargetDirectoryExists(targetRootDirectory);
        if (!directoryPreparationResult.success()) {
            return directoryPreparationResult;
        }

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return new ImportResult(
                    targetRootDirectory,
                    true,
                    0,
                    0,
                    List.of(),
                    "No files were selected for import."
            );
        }

        int importedFileCount = 0;
        int skippedFileCount = 0;
        List<String> skippedFilenames = new ArrayList<>();

        for (File selectedFile : selectedFiles) {
            if (selectedFile == null || !selectedFile.isFile() || !selectedFile.canRead()) {
                skippedFileCount++;
                skippedFilenames.add(selectedFile == null ? "<null>" : selectedFile.getName());
                continue;
            }

            if (!OfflineDemManager.isSupportedCopernicusGlo30DemFilename(selectedFile.getName())) {
                skippedFileCount++;
                skippedFilenames.add(selectedFile.getName());
                continue;
            }

            try {
                Files.copy(
                        selectedFile.toPath(),
                        targetRootDirectory.resolve(selectedFile.getName()),
                        StandardCopyOption.REPLACE_EXISTING
                );
                importedFileCount++;
            } catch (Exception exception) {
                skippedFileCount++;
                skippedFilenames.add(selectedFile.getName());
            }
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format(
                Locale.US,
                "Imported %d DEM tile(s) into:%n%s",
                importedFileCount,
                targetRootDirectory.toAbsolutePath()
        ));

        if (skippedFileCount > 0) {
            message.append(String.format(
                    Locale.US,
                    "%n%nSkipped %d file(s) because they were unreadable or did not match the supported Copernicus *_DEM.tif naming scheme.",
                    skippedFileCount
            ));

            int previewCount = Math.min(8, skippedFilenames.size());
            if (previewCount > 0) {
                message.append(String.format(Locale.US, "%n%nSkipped examples:%n"));
                for (int i = 0; i < previewCount; i++) {
                    message.append("- ").append(skippedFilenames.get(i)).append(System.lineSeparator());
                }
            }
        }

        return new ImportResult(
                targetRootDirectory,
                true,
                importedFileCount,
                skippedFileCount,
                List.copyOf(skippedFilenames),
                message.toString().trim()
        );
    }

    private Path resolveEffectiveTargetRootDirectory(String configuredDemRootDirectory) {
        if (configuredDemRootDirectory == null || configuredDemRootDirectory.isBlank()) {
            return resolveDefaultCopernicusRootDirectory();
        }

        return Path.of(configuredDemRootDirectory.trim());
    }

    private ImportResult ensureTargetDirectoryExists(Path targetRootDirectory) {
        if (targetRootDirectory == null) {
            return new ImportResult(
                    null,
                    false,
                    0,
                    0,
                    List.of(),
                    "DEM target directory is undefined."
            );
        }

        try {
            if (Files.exists(targetRootDirectory) && !Files.isDirectory(targetRootDirectory)) {
                return new ImportResult(
                        targetRootDirectory,
                        false,
                        0,
                        0,
                        List.of(),
                        "Configured DEM root path exists but is not a directory:\n"
                                + targetRootDirectory.toAbsolutePath()
                );
            }

            Files.createDirectories(targetRootDirectory);

            return new ImportResult(
                    targetRootDirectory,
                    true,
                    0,
                    0,
                    List.of(),
                    "Using local Copernicus DEM directory:\n"
                            + targetRootDirectory.toAbsolutePath()
                            + "\n\nYou can now import extracted Copernicus *_DEM.tif tiles into this folder."
            );
        } catch (Exception exception) {
            return new ImportResult(
                    targetRootDirectory,
                    false,
                    0,
                    0,
                    List.of(),
                    "Could not create DEM root directory:\n"
                            + targetRootDirectory.toAbsolutePath()
                            + "\n\nReason: "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Immutable result of a DEM directory preparation or tile import action.
     *
     * @param targetRootDirectory effective DEM root directory
     * @param success true if the action succeeded
     * @param importedFileCount number of imported files
     * @param skippedFileCount number of skipped files
     * @param skippedFilenames skipped filenames for diagnostics
     * @param message user-friendly summary message
     */
    public record ImportResult(
            Path targetRootDirectory,
            boolean success,
            int importedFileCount,
            int skippedFileCount,
            List<String> skippedFilenames,
            String message
    ) {
    }
}
