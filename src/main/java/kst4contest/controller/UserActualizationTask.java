package kst4contest.controller;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserActualizationTask extends TimerTask {

	private static final Logger LOGGER = Logger.getLogger(UserActualizationTask.class.getName());

	private final ChatController client;

	public UserActualizationTask(ChatController client) {
		this.client = client;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("UserActualizationTask");

		try {
			updateWorkedCallSignsFromFile();
		} catch (RuntimeException exception) {
			LOGGER.log(Level.WARNING,
					"Unexpected failure while updating Worked callsigns from the Simplelogfile; "
							+ "the periodic task will continue.",
					exception);
		}
	}

	private void updateWorkedCallSignsFromFile() {
		if (!client.getChatPreferences().isLogsynch_fileBasedWkdCallInterpreterEnabled()) {
			return;
		}

		String configuredFileName = client.getChatPreferences()
				.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly();
		if (configuredFileName == null || configuredFileName.isBlank()) {
			LOGGER.warning("Cannot read the Simplelogfile because no file is selected.");
			return;
		}

		final Path logFile;
		try {
			logFile = Path.of(configuredFileName).toAbsolutePath().normalize();
		} catch (InvalidPathException exception) {
			LOGGER.log(Level.WARNING,
					"Cannot use the configured Simplelogfile path: " + configuredFileName,
					exception);
			return;
		}

		boolean created = createMissingLogFile(logFile);
		if (!Files.isRegularFile(logFile)) {
			return;
		}

		try {
			Set<String> workedBaseCalls = new UCXLogFileToHashsetParser(logFile.toString()).parse();
			client.applySimpleLogWorkedBaseCalls(workedBaseCalls);
			LOGGER.log(Level.FINE,
					"Read {0} unique base callsigns from Simplelogfile {1}.",
					new Object[] { workedBaseCalls.size(), logFile });
		} catch (IOException exception) {
			LOGGER.log(Level.WARNING, "Cannot read Simplelogfile " + logFile, exception);
		}

		if (created) {
			client.notifySimpleLogFileCreated(logFile);
		}
	}

	private boolean createMissingLogFile(Path logFile) {
		if (Files.exists(logFile)) {
			if (!Files.isRegularFile(logFile)) {
				LOGGER.log(Level.WARNING,
						"The selected Simplelogfile path is not a regular file: {0}",
						logFile);
			}
			return false;
		}

		try {
			Files.createFile(logFile);
			LOGGER.log(Level.INFO, "Created missing Simplelogfile {0}.", logFile);
			return true;
		} catch (FileAlreadyExistsException exception) {
			return false;
		} catch (IOException | SecurityException exception) {
			LOGGER.log(Level.WARNING, "Cannot create Simplelogfile " + logFile, exception);
			return false;
		}
	}
}
