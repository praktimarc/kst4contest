package kst4contest.utils;

import kst4contest.controller.DBController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * This class has utility methods to handle application files inside the home directory.
 */
public class ApplicationFileUtils {

    /**
     * Gets the path of a file inside the home directory of the user.
     * @param applicationName Name off the application which is used for the hidden directory
     * @param fileName Name of the file.
     * @return The full path inside the user home directory.
     */
    public static String getFilePath(final String applicationName, final String fileName) {
        return Path.of(System.getProperty("user.home"),
                "." + applicationName,
                fileName).toString();
    }

    /**
     * Copies a resource to the application folder if required.
     * @param applicationName Name of the application.
     * @param resourcePath Path of the resource.
     * @param fileName Name of the target file.
     */
    public static void copyResourceIfRequired(final String applicationName, final String resourcePath, final String fileName) {
        File file = new File(getFilePath(applicationName, fileName));

        if (file.exists()) {
            return;
        }

        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        copyResourceToFile(resourcePath, file.getPath());
    }

    /**
     * Copies a given resource at a resourcePath to the file path.
     * @param resourcePath
     * @param filePath
     */
    public static void copyResourceToFile(String resourcePath, String filePath) {
        try (InputStream resourceStream = ApplicationFileUtils.class.getResourceAsStream(resourcePath);
             FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {

            if (resourceStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            resourceStream.transferTo(fileOutputStream);
        } catch (IOException ex) {
            System.err.println("Exception when copying Application file: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

}
