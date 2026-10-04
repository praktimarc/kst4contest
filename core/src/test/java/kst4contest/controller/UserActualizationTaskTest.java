package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActualizationTaskTest {

	@TempDir
	Path temporaryDirectory;

	@Mock
	ChatController controller;

	@Mock
	ChatPreferences preferences;

	@BeforeEach
	void configureControllerPreferences() {
		when(controller.getChatPreferences()).thenReturn(preferences);
	}

	@Test
	void disabledInterpreterDoesNotCreateOrReadFile() {
		when(preferences.isLogsynch_fileBasedWkdCallInterpreterEnabled()).thenReturn(false);

		new UserActualizationTask(controller).run();

		verify(controller, never()).applySimpleLogWorkedBaseCalls(anySet());
		verify(controller, never()).notifySimpleLogFileCreated(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createsMissingFileAndNotifiesOnlyOnce() {
		Path logFile = temporaryDirectory.resolve("created.log").toAbsolutePath().normalize();
		when(preferences.isLogsynch_fileBasedWkdCallInterpreterEnabled()).thenReturn(true);
		when(preferences.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly())
				.thenReturn(logFile.toString());
		UserActualizationTask task = new UserActualizationTask(controller);

		task.run();
		task.run();

		assertTrue(Files.isRegularFile(logFile));
		verify(controller, times(2)).applySimpleLogWorkedBaseCalls(anySet());
		verify(controller, times(1)).notifySimpleLogFileCreated(logFile);
	}

	@Test
	void readFailureIsContainedAndDoesNotUpdateUiState() {
		when(preferences.isLogsynch_fileBasedWkdCallInterpreterEnabled()).thenReturn(true);
		when(preferences.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly())
				.thenReturn(temporaryDirectory.toString());

		assertDoesNotThrow(() -> new UserActualizationTask(controller).run());

		verify(controller, never()).applySimpleLogWorkedBaseCalls(anySet());
		verify(controller, never()).notifySimpleLogFileCreated(org.mockito.ArgumentMatchers.any());
	}
}
