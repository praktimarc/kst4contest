package kst4contest.controller;

import kst4contest.ApplicationConstants;
import kst4contest.model.OperatorProfile;
import kst4contest.utils.ApplicationFileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes the operator profile registry.
 *
 * <p>The registry file is created lazily. As long as an installation has only the
 * historic flat layout, no registry exists and none is written, so a single operator
 * installation behaves exactly as before. The file appears when the second profile is
 * created; at that moment the root profile is materialised as well.</p>
 *
 * <p>A missing, unreadable or malformed registry is never fatal. It is logged and
 * treated like an installation without additional profiles.</p>
 */
public class OperatorProfileStore {

    private static final Logger LOGGER = Logger.getLogger(OperatorProfileStore.class.getName());

    /**
     * Name of the registry file inside the application directory.
     */
    public static final String PROFILES_REGISTRY_FILE = "profiles.xml";

    private static final String TAG_ROOT = "praktiKSTProfiles";
    private static final String TAG_REGISTRY_VERSION = "registryVersion";
    private static final String TAG_LAST_USED_PROFILE_ID = "lastUsedProfileId";
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_PROFILE_ID = "profileId";
    private static final String TAG_DISPLAY_NAME = "displayName";
    private static final String TAG_ROOT_PROFILE = "rootProfile";
    private static final String TAG_SHARED_WORKED_DATABASE = "sharedWorkedDatabase";
    private static final String TAG_LAST_USED_EPOCH_MS = "lastUsedEpochMs";

    private static final int REGISTRY_VERSION = 1;

    private final String registryFilePath;

    /**
     * Creates a store working on the registry of the current installation.
     */
    public OperatorProfileStore() {
        this(ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME, PROFILES_REGISTRY_FILE));
    }

    /**
     * Creates a store working on an explicit registry file.
     *
     * @param registryFilePath absolute path of the registry file
     */
    public OperatorProfileStore(final String registryFilePath) {
        this.registryFilePath = registryFilePath;
    }

    /**
     * Returns whether a registry file exists at all.
     *
     * @return true if the installation already has more than the historic flat layout
     */
    public boolean isRegistryPresent() {
        return new File(registryFilePath).isFile();
    }

    /**
     * Builds the in-memory descriptor of the historic flat installation.
     *
     * <p>Nothing is written. This keeps a single operator installation untouched.</p>
     *
     * @return the implicit root profile
     */
    public OperatorProfile buildImplicitRootProfile() {
        return OperatorProfilePaths.buildRootProfile("Default");
    }

    /**
     * Reads all stored profiles.
     *
     * @return the stored profiles, or an empty list when no usable registry exists
     */
    public List<OperatorProfile> loadProfiles() {

        List<OperatorProfile> loadedProfiles = new ArrayList<>();
        Document document = readRegistryDocument();

        if (document == null) {
            return loadedProfiles;
        }

        NodeList profileNodes = document.getElementsByTagName(TAG_PROFILE);

        for (int profileIndex = 0; profileIndex < profileNodes.getLength(); profileIndex++) {
            Node currentNode = profileNodes.item(profileIndex);

            if (currentNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element profileElement = (Element) currentNode;
            String profileId = readText(profileElement, TAG_PROFILE_ID);

            if (profileId == null || profileId.isBlank()) {
                LOGGER.log(Level.WARNING, "Skipping operator profile entry without an identifier");
                continue;
            }

            OperatorProfile loadedProfile = new OperatorProfile();
            loadedProfile.setProfileId(profileId.trim());
            loadedProfile.setDisplayName(readText(profileElement, TAG_DISPLAY_NAME));
            loadedProfile.setRootProfile(readBoolean(profileElement, TAG_ROOT_PROFILE, false));
            loadedProfile.setSharedWorkedDatabase(
                    readBoolean(profileElement, TAG_SHARED_WORKED_DATABASE, true));
            loadedProfile.setLastUsedEpochMs(readLong(profileElement, TAG_LAST_USED_EPOCH_MS));

            if (loadedProfile.getDisplayName() == null || loadedProfile.getDisplayName().isBlank()) {
                loadedProfile.setDisplayName(loadedProfile.getProfileId());
            }

            // The root profile always uses the common station database, because its
            // database is the historic flat file itself.
            if (loadedProfile.isRootProfile()) {
                loadedProfile.setSharedWorkedDatabase(true);
            }

            loadedProfiles.add(loadedProfile);
        }

        return loadedProfiles;
    }

    /**
     * Reads the identifier of the profile that was activated last.
     *
     * @return the identifier, or empty when unknown
     */
    public Optional<String> loadLastUsedProfileId() {

        Document document = readRegistryDocument();

        if (document == null) {
            return Optional.empty();
        }

        Element rootElement = document.getDocumentElement();

        if (rootElement == null) {
            return Optional.empty();
        }

        String lastUsedProfileId = readText(rootElement, TAG_LAST_USED_PROFILE_ID);

        if (lastUsedProfileId == null || lastUsedProfileId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(lastUsedProfileId.trim());
    }

    /**
     * Writes the complete registry.
     *
     * @param profiles          profiles to store
     * @param lastUsedProfileId identifier of the profile that was activated last, may be null
     * @return true if the registry was written
     */
    public boolean saveProfiles(final List<OperatorProfile> profiles, final String lastUsedProfileId) {

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            Element rootElement = document.createElement(TAG_ROOT);
            document.appendChild(rootElement);

            appendTextElement(document, rootElement, TAG_REGISTRY_VERSION, String.valueOf(REGISTRY_VERSION));

            if (lastUsedProfileId != null && !lastUsedProfileId.isBlank()) {
                appendTextElement(document, rootElement, TAG_LAST_USED_PROFILE_ID, lastUsedProfileId);
            }

            for (OperatorProfile currentProfile : profiles) {
                Element profileElement = document.createElement(TAG_PROFILE);
                rootElement.appendChild(profileElement);

                appendTextElement(document, profileElement, TAG_PROFILE_ID, currentProfile.getProfileId());
                appendTextElement(document, profileElement, TAG_DISPLAY_NAME, currentProfile.getDisplayName());
                appendTextElement(document, profileElement, TAG_ROOT_PROFILE,
                        String.valueOf(currentProfile.isRootProfile()));
                appendTextElement(document, profileElement, TAG_SHARED_WORKED_DATABASE,
                        String.valueOf(currentProfile.isRootProfile() || currentProfile.isSharedWorkedDatabase()));
                appendTextElement(document, profileElement, TAG_LAST_USED_EPOCH_MS,
                        String.valueOf(currentProfile.getLastUsedEpochMs()));
            }

            return writeDocumentAtomically(document);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not write the operator profile registry", e);
            return false;
        }
    }

    /**
     * Records that a profile has been activated.
     *
     * <p>Does nothing when no registry exists, so a single operator installation is not
     * turned into a multi profile installation by merely starting the application.</p>
     *
     * @param profileId identifier of the activated profile
     * @return true if the registry was updated
     */
    public boolean recordLastUsed(final String profileId) {

        if (!isRegistryPresent()) {
            return false;
        }

        List<OperatorProfile> storedProfiles = loadProfiles();

        if (storedProfiles.isEmpty()) {
            return false;
        }

        for (OperatorProfile currentProfile : storedProfiles) {
            if (currentProfile.getProfileId().equalsIgnoreCase(profileId)) {
                currentProfile.setLastUsedEpochMs(System.currentTimeMillis());
            }
        }

        return saveProfiles(storedProfiles, profileId);
    }

    /**
     * Returns the absolute path of the registry file.
     *
     * @return absolute registry path
     */
    public String getRegistryFilePath() {
        return registryFilePath;
    }

    private Document readRegistryDocument() {

        File registryFile = new File(registryFilePath);

        if (!registryFile.isFile()) {
            return null;
        }

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            return documentBuilderFactory.newDocumentBuilder().parse(registryFile);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Could not read the operator profile registry, continuing without additional profiles", e);
            return null;
        }
    }

    private static String readText(final Element parentElement, final String tagName) {

        NodeList matchingNodes = parentElement.getElementsByTagName(tagName);

        if (matchingNodes.getLength() == 0) {
            return null;
        }

        return matchingNodes.item(0).getTextContent();
    }

    private static boolean readBoolean(final Element parentElement,
            final String tagName,
            final boolean defaultValue) {

        String rawValue = readText(parentElement, tagName);

        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(rawValue.trim());
    }

    private static long readLong(final Element parentElement, final String tagName) {

        String rawValue = readText(parentElement, tagName);

        if (rawValue == null || rawValue.isBlank()) {
            return 0L;
        }

        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static void appendTextElement(final Document document,
            final Element parentElement,
            final String tagName,
            final String textContent) {

        Element createdElement = document.createElement(tagName);
        createdElement.setTextContent(textContent == null ? "" : textContent);
        parentElement.appendChild(createdElement);
    }

    /**
     * Writes the registry through a temporary file so a crash can never leave a
     * half-written registry behind. This mirrors the established preferences writer.
     *
     * @param document document to write
     * @return true if the registry file was replaced
     */
    private boolean writeDocumentAtomically(final Document document) {

        Path targetPath = Path.of(registryFilePath).toAbsolutePath();
        Path parentDirectory = targetPath.getParent();

        try {
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            Path temporaryPath = Files.createTempFile(
                    parentDirectory, PROFILES_REGISTRY_FILE, ".tmp");

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            try (OutputStream outputStream = Files.newOutputStream(temporaryPath)) {
                transformer.transform(new DOMSource(document), new StreamResult(outputStream));
            }

            try {
                Files.move(temporaryPath, targetPath,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException atomicMoveUnsupported) {
                Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not store the operator profile registry", e);
            return false;
        }
    }
}
