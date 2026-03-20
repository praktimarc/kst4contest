package kst4contest.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatMember;
import kst4contest.utils.ApplicationFileUtils;

public class DBController {

	/**
	 * Name of the database file.
	 */
	public static final String DATABASE_FILE = "praktiKST.db";

	/**
	 * Resource path for the database.
	 */
	public static final String DATABASE_RESOURCE = "/praktiKST.db";

	/**
	 * Number of milliseconds after which worked/not-QRV data is considered outdated
	 * and therefore automatically reset.
	 */
	private static final long WORKED_DATA_EXPIRATION_IN_MILLISECONDS = 65L * 60L * 60L * 1000L;

	private static final DBController dbcontroller = new DBController();
	private static Connection connection;
	private static String DB_PATH = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, DATABASE_FILE);

	public DBController() {
		initDBConnection();
	}

	public static DBController getInstance() {
		return dbcontroller;
	}

	/**
	 * Closes the database connection if it is still open.
	 */
	public synchronized void closeDBConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initializes the SQLite connection and ensures that an existing legacy database
	 * is upgraded before normal runtime access starts.
	 */
	private synchronized void initDBConnection() {

		System.out.println("DBH: initiate new db connection");

		try {
			ApplicationFileUtils.copyResourceIfRequired(
					ApplicationConstants.APPLICATION_NAME,
					DATABASE_RESOURCE,
					DATABASE_FILE
			);

			if (connection != null && !connection.isClosed()) {
				return;
			}

			System.out.println("Creating Connection to Database...");

			DB_PATH = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, DATABASE_FILE);
			connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

			System.out.println("[DBH, Info]: Path = " + DB_PATH);

			if (!connection.isClosed()) {
				System.out.println("...Connection established");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					if (connection != null && !connection.isClosed()) {
						connection.close();

						if (connection.isClosed()) {
							System.out.println("Connection to Database closed");
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});

		ensureChatMemberTableCompatibility();
	}

	/**
	 * Ensures that the ChatMember table exists, that all required columns are
	 * available for newer software versions, that existing old callsign keys are
	 * normalized to callsignRaw semantics and that outdated worked data is removed.
	 */
	private synchronized void ensureChatMemberTableCompatibility() {
		createChatMemberTableIfRequired();
		versionUpdateOfDBCheckAndChangeV11ToV12();
		versionUpdateOfDBCheckAndChangeV12ToV13();
		normalizeStoredCallsignsToRawCallsigns();
		resetExpiredWorkedDataIfRequired();
	}

	/**
	 * Creates the ChatMember table if it does not exist yet. This keeps the program
	 * robust even if the resource database was missing or replaced by a user.
	 */
	private synchronized void createChatMemberTableIfRequired() {

		String createTableSql =
				"CREATE TABLE IF NOT EXISTS ChatMember ("
						+ "callsign TEXT NOT NULL PRIMARY KEY, "
						+ "qra TEXT, "
						+ "name TEXT, "
						+ "lastActivityDateTime TEXT, "
						+ "worked BOOLEAN DEFAULT 0, "
						+ "worked144 BOOLEAN DEFAULT 0, "
						+ "worked432 BOOLEAN DEFAULT 0, "
						+ "worked1240 BOOLEAN DEFAULT 0, "
						+ "worked2300 BOOLEAN DEFAULT 0, "
						+ "worked3400 BOOLEAN DEFAULT 0, "
						+ "worked5600 BOOLEAN DEFAULT 0, "
						+ "worked10G BOOLEAN DEFAULT 0, "
						+ "notQRV144 BOOLEAN DEFAULT 0, "
						+ "notQRV432 BOOLEAN DEFAULT 0, "
						+ "notQRV1240 BOOLEAN DEFAULT 0, "
						+ "notQRV2300 BOOLEAN DEFAULT 0, "
						+ "notQRV3400 BOOLEAN DEFAULT 0, "
						+ "notQRV5600 BOOLEAN DEFAULT 0, "
						+ "notQRV10G BOOLEAN DEFAULT 0, "
						+ "lastFlagsChangeEpochMs INTEGER DEFAULT 0"
						+ ");";

		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(createTableSql);
		} catch (SQLException e) {
			throw new RuntimeException("[DBH, ERROR:] Could not create ChatMember table", e);
		}
	}

	/**
	 * Updates old v1.1 databases to the v1.2 schema by adding the not-QRV fields if
	 * they are missing.
	 */
	public synchronized void versionUpdateOfDBCheckAndChangeV11ToV12() {

		try {
			ensureColumnExists("ChatMember", "notQRV144", "BOOLEAN DEFAULT 0");
			ensureColumnExists("ChatMember", "notQRV432", "BOOLEAN DEFAULT 0");
			ensureColumnExists("ChatMember", "notQRV1240", "BOOLEAN DEFAULT 0");
			ensureColumnExists("ChatMember", "notQRV2300", "BOOLEAN DEFAULT 0");
			ensureColumnExists("ChatMember", "notQRV3400", "BOOLEAN DEFAULT 0");
			ensureColumnExists("ChatMember", "notQRV5600", "BOOLEAN DEFAULT 0");
			ensureColumnExists("ChatMember", "notQRV10G", "BOOLEAN DEFAULT 0");
		} catch (SQLException e) {
			throw new RuntimeException("[DBH, ERROR:] Could not migrate database from v1.1 to v1.2", e);
		}
	}

	/**
	 * Updates old v1.2 databases to the v1.3 schema by adding a timestamp column
	 * which is used for automatic worked-data expiration.
	 */
	public synchronized void versionUpdateOfDBCheckAndChangeV12ToV13() {

		try {
			ensureColumnExists("ChatMember", "lastFlagsChangeEpochMs", "INTEGER DEFAULT 0");
		} catch (SQLException e) {
			throw new RuntimeException("[DBH, ERROR:] Could not migrate database from v1.2 to v1.3", e);
		}
	}

	/**
	 * Adds a missing column to an existing table. This method is used for safe schema
	 * upgrades on customer systems which still contain older database files.
	 *
	 * @param tableName        table to inspect
	 * @param columnName       column which must exist
	 * @param columnDefinition SQL definition used for ALTER TABLE
	 * @throws SQLException if the metadata lookup or ALTER TABLE fails
	 */
	private synchronized void ensureColumnExists(String tableName, String columnName, String columnDefinition) throws SQLException {

		if (helper_checkIfColumnExists(tableName, columnName)) {
			return;
		}

		System.out.println("DBH, Info: adding missing column " + columnName + " to table " + tableName);

		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("ALTER TABLE " + tableName + " ADD " + columnName + " " + columnDefinition + ";");
		}
	}

	/**
	 * Checks via PRAGMA metadata whether a certain column is already available in the
	 * database.
	 *
	 * @param tableName  table to inspect
	 * @param columnName column to look for
	 * @return true if the column exists already
	 * @throws SQLException if the metadata query fails
	 */
	private synchronized boolean helper_checkIfColumnExists(String tableName, String columnName) throws SQLException {

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ");")) {

			while (resultSet.next()) {
				if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Rebuilds the ChatMember table so that every stored row uses the normalized raw
	 * callsign as primary key. Old keys like "EA5/G8MBI/P" or "OK2M-70" are merged to
	 * "G8MBI" and "OK2M". This prevents duplicate logical stations and fixes legacy
	 * databases created by earlier software versions.
	 */
	private synchronized void normalizeStoredCallsignsToRawCallsigns() {

		Map<String, ChatMember> normalizedChatMembersByRawCallsign = new LinkedHashMap<>();

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SELECT * FROM ChatMember ORDER BY callsign ASC;")) {

			while (resultSet.next()) {
				ChatMember currentChatMemberFromDatabase = helper_buildChatMemberFromResultSet(resultSet);
				String currentRawCallsign = currentChatMemberFromDatabase.getCallSignRaw();

				if (currentRawCallsign == null || currentRawCallsign.isBlank()) {
					continue;
				}

				ChatMember existingNormalizedChatMember = normalizedChatMembersByRawCallsign.get(currentRawCallsign);

				if (existingNormalizedChatMember == null) {
					normalizedChatMembersByRawCallsign.put(currentRawCallsign, currentChatMemberFromDatabase);
				} else {
					helper_mergeChatMemberDatabaseState(existingNormalizedChatMember, currentChatMemberFromDatabase);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("[DBH, ERROR:] Could not normalize stored callsigns", e);
		}

		try (Statement deleteStatement = connection.createStatement()) {
			deleteStatement.executeUpdate("DELETE FROM ChatMember;");
		} catch (SQLException e) {
			throw new RuntimeException("[DBH, ERROR:] Could not clear ChatMember table for callsign normalization", e);
		}

		for (ChatMember normalizedChatMember : normalizedChatMembersByRawCallsign.values()) {
			helper_upsertCompleteChatMemberRow(normalizedChatMember);
		}
	}

	/**
	 * Merges the database state of two rows that represent the same normalized raw
	 * callsign. Worked and not-QRV information is combined conservatively via logical
	 * OR so that no positive state is lost during migration.
	 *
	 * @param targetChatMember target row that remains after merge
	 * @param sourceChatMember source row that is merged into the target row
	 */
	private synchronized void helper_mergeChatMemberDatabaseState(ChatMember targetChatMember, ChatMember sourceChatMember) {

		if ((targetChatMember.getQra() == null || targetChatMember.getQra().isBlank())
				&& sourceChatMember.getQra() != null && !sourceChatMember.getQra().isBlank()) {
			targetChatMember.setQra(sourceChatMember.getQra());
		}

		if ((targetChatMember.getName() == null || targetChatMember.getName().isBlank())
				&& sourceChatMember.getName() != null && !sourceChatMember.getName().isBlank()) {
			targetChatMember.setName(sourceChatMember.getName());
		}

		if (targetChatMember.getLastActivity() == null && sourceChatMember.getLastActivity() != null) {
			targetChatMember.setLastActivity(sourceChatMember.getLastActivity());
		}

		targetChatMember.setWorked(targetChatMember.isWorked() || sourceChatMember.isWorked());
		targetChatMember.setWorked144(targetChatMember.isWorked144() || sourceChatMember.isWorked144());
		targetChatMember.setWorked432(targetChatMember.isWorked432() || sourceChatMember.isWorked432());
		targetChatMember.setWorked1240(targetChatMember.isWorked1240() || sourceChatMember.isWorked1240());
		targetChatMember.setWorked2300(targetChatMember.isWorked2300() || sourceChatMember.isWorked2300());
		targetChatMember.setWorked3400(targetChatMember.isWorked3400() || sourceChatMember.isWorked3400());
		targetChatMember.setWorked5600(targetChatMember.isWorked5600() || sourceChatMember.isWorked5600());
		targetChatMember.setWorked10G(targetChatMember.isWorked10G() || sourceChatMember.isWorked10G());

		targetChatMember.setQrv144(targetChatMember.isQrv144() && sourceChatMember.isQrv144());
		targetChatMember.setQrv432(targetChatMember.isQrv432() && sourceChatMember.isQrv432());
		targetChatMember.setQrv1240(targetChatMember.isQrv1240() && sourceChatMember.isQrv1240());
		targetChatMember.setQrv2300(targetChatMember.isQrv2300() && sourceChatMember.isQrv2300());
		targetChatMember.setQrv3400(targetChatMember.isQrv3400() && sourceChatMember.isQrv3400());
		targetChatMember.setQrv5600(targetChatMember.isQrv5600() && sourceChatMember.isQrv5600());
		targetChatMember.setQrv10G(targetChatMember.isQrv10G() && sourceChatMember.isQrv10G());

		targetChatMember.setLastFlagsChangeEpochMs(
				Math.max(targetChatMember.getLastFlagsChangeEpochMs(), sourceChatMember.getLastFlagsChangeEpochMs()));
	}

	/**
	 * Removes outdated worked and not-QRV flags if their last change timestamp is
	 * older than the configured contest lifetime.
	 */
	public synchronized void resetExpiredWorkedDataIfRequired() {

		long expirationThresholdEpochMs = System.currentTimeMillis() - WORKED_DATA_EXPIRATION_IN_MILLISECONDS;

		String resetExpiredDataSql =
				"UPDATE ChatMember SET "
						+ "worked = 0, "
						+ "worked144 = 0, "
						+ "worked432 = 0, "
						+ "worked1240 = 0, "
						+ "worked2300 = 0, "
						+ "worked3400 = 0, "
						+ "worked5600 = 0, "
						+ "worked10G = 0, "
						+ "notQRV144 = 0, "
						+ "notQRV432 = 0, "
						+ "notQRV1240 = 0, "
						+ "notQRV2300 = 0, "
						+ "notQRV3400 = 0, "
						+ "notQRV5600 = 0, "
						+ "notQRV10G = 0, "
						+ "lastFlagsChangeEpochMs = 0 "
						+ "WHERE lastFlagsChangeEpochMs > 0 AND lastFlagsChangeEpochMs < ?;";

		try (PreparedStatement preparedStatement = connection.prepareStatement(resetExpiredDataSql)) {
			preparedStatement.setLong(1, expirationThresholdEpochMs);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("[DBH, ERROR:] Could not reset expired worked data", e);
		}
	}

	/**
	 * Stores a chatmember with its metadata in the database. The unique key is always
	 * the normalized raw callsign. Existing worked/not-QRV flags are preserved on
	 * conflicts so that a normal member refresh does not delete contest state.
	 *
	 * @param chatMemberToStore chatmember to insert or update
	 * @throws SQLException if the database write fails
	 */
	public synchronized void storeChatMember(ChatMember chatMemberToStore) throws SQLException {

		if (chatMemberToStore == null || chatMemberToStore.getCallSignRaw() == null || chatMemberToStore.getCallSignRaw().isBlank()) {
			return;
		}

		String insertOrUpdateSql =
				"INSERT INTO ChatMember ("
						+ "callsign, qra, name, lastActivityDateTime, worked, worked144, worked432, worked1240, worked2300, worked3400, worked5600, worked10G, "
						+ "notQRV144, notQRV432, notQRV1240, notQRV2300, notQRV3400, notQRV5600, notQRV10G, lastFlagsChangeEpochMs"
						+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
						+ "ON CONFLICT(callsign) DO UPDATE SET "
						+ "qra = excluded.qra, "
						+ "name = excluded.name, "
						+ "lastActivityDateTime = excluded.lastActivityDateTime;";

		long resolvedLastFlagsChangeEpochMs = helper_resolveLastFlagsChangeEpochMsForStore(chatMemberToStore);

		try (PreparedStatement preparedStatement = connection.prepareStatement(insertOrUpdateSql)) {
			preparedStatement.setString(1, chatMemberToStore.getCallSignRaw());
			preparedStatement.setString(2, chatMemberToStore.getQra());
			preparedStatement.setString(3, chatMemberToStore.getName());
			preparedStatement.setString(4, chatMemberToStore.getLastActivity() == null ? null : chatMemberToStore.getLastActivity().toString());
			preparedStatement.setInt(5, helper_booleanIntConverter(chatMemberToStore.isWorked()));
			preparedStatement.setInt(6, helper_booleanIntConverter(chatMemberToStore.isWorked144()));
			preparedStatement.setInt(7, helper_booleanIntConverter(chatMemberToStore.isWorked432()));
			preparedStatement.setInt(8, helper_booleanIntConverter(chatMemberToStore.isWorked1240()));
			preparedStatement.setInt(9, helper_booleanIntConverter(chatMemberToStore.isWorked2300()));
			preparedStatement.setInt(10, helper_booleanIntConverter(chatMemberToStore.isWorked3400()));
			preparedStatement.setInt(11, helper_booleanIntConverter(chatMemberToStore.isWorked5600()));
			preparedStatement.setInt(12, helper_booleanIntConverter(chatMemberToStore.isWorked10G()));
			preparedStatement.setInt(13, helper_booleanIntConverter(!chatMemberToStore.isQrv144()));
			preparedStatement.setInt(14, helper_booleanIntConverter(!chatMemberToStore.isQrv432()));
			preparedStatement.setInt(15, helper_booleanIntConverter(!chatMemberToStore.isQrv1240()));
			preparedStatement.setInt(16, helper_booleanIntConverter(!chatMemberToStore.isQrv2300()));
			preparedStatement.setInt(17, helper_booleanIntConverter(!chatMemberToStore.isQrv3400()));
			preparedStatement.setInt(18, helper_booleanIntConverter(!chatMemberToStore.isQrv5600()));
			preparedStatement.setInt(19, helper_booleanIntConverter(!chatMemberToStore.isQrv10G()));
			preparedStatement.setLong(20, resolvedLastFlagsChangeEpochMs);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Chatmember could not been stored.");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Fetches all stored chatmember rows from the database and returns them in a map
	 * keyed by the normalized raw callsign.
	 *
	 * @return map of raw callsign to chatmember database state
	 * @throws SQLException if the database read fails
	 */
	public synchronized HashMap<String, ChatMember> fetchChatMemberWkdDataFromDB() throws SQLException {

		resetExpiredWorkedDataIfRequired();

		HashMap<String, ChatMember> fetchedWorkedData = new LinkedHashMap<>();

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SELECT * FROM ChatMember ORDER BY callsign ASC;")) {

			while (resultSet.next()) {
				ChatMember updatedWorkedData = helper_buildChatMemberFromResultSet(resultSet);
				fetchedWorkedData.put(updatedWorkedData.getCallSignRaw(), updatedWorkedData);
			}

			return fetchedWorkedData;
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't handle DB-Query");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Fetches the worked and not-QRV state for a single chatmember from the database.
	 * The lookup is always performed with the normalized raw callsign.
	 *
	 * @param checkForThis chatmember instance that should receive the stored flags
	 * @return the same chatmember instance with updated flags
	 */
	public synchronized ChatMember fetchChatMemberWkdDataForOnlyOneCallsignFromDB(ChatMember checkForThis) {

		if (checkForThis == null || checkForThis.getCallSignRaw() == null || checkForThis.getCallSignRaw().isBlank()) {
			return checkForThis;
		}

		resetExpiredWorkedDataIfRequired();

		String selectSingleChatMemberSql = "SELECT * FROM ChatMember WHERE callsign = ?;";

		try (PreparedStatement preparedStatement = connection.prepareStatement(selectSingleChatMemberSql)) {
			preparedStatement.setString(1, checkForThis.getCallSignRaw());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					helper_copyWorkedAndQrvFlags(helper_buildChatMemberFromResultSet(resultSet), checkForThis);
				}
			}

			return checkForThis;
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't handle DB-Query");
			e.printStackTrace();
			return checkForThis;
		}
	}

	/**
	 * Removes all worked and not-QRV information from the database. The callsign rows
	 * remain in place so that the table view still shows known stations afterwards.
	 *
	 * @return number of affected database rows, or -1 on error
	 */
	public synchronized int resetWorkedDataInDB() {

		String resetAllWorkedDataSql =
				"UPDATE ChatMember SET "
						+ "worked = 0, worked144 = 0, worked432 = 0, worked1240 = 0, worked2300 = 0, worked3400 = 0, worked5600 = 0, worked10G = 0, "
						+ "notQRV144 = 0, notQRV432 = 0, notQRV1240 = 0, notQRV2300 = 0, notQRV3400 = 0, notQRV5600 = 0, notQRV10G = 0, "
						+ "lastFlagsChangeEpochMs = 0;";

		try (Statement statement = connection.createStatement()) {
			return statement.executeUpdate(resetAllWorkedDataSql);
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't reset the worked data");
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Updates the worked-information of a chatmember row. The method only succeeds if
	 * a row with the normalized raw callsign exists already.
	 *
	 * @param chatMemberToStore chatmember that contains the worked band information
	 * @return true if an existing row was updated, otherwise false
	 * @throws SQLException if the database write fails
	 */
	public synchronized boolean updateWkdInfoOnChatMember(ChatMember chatMemberToStore) throws SQLException {

		if (chatMemberToStore == null || chatMemberToStore.getCallSignRaw() == null || chatMemberToStore.getCallSignRaw().isBlank()) {
			return false;
		}

		String workedBandColumnName = helper_resolveWorkedBandColumnName(chatMemberToStore);

		if (workedBandColumnName == null) {
			System.out.println("[DBCtrl, Error]: unknown at which band the qso had been!");
			return false;
		}

		String updateWorkedSql =
				"UPDATE ChatMember SET worked = 1, " + workedBandColumnName + " = 1, lastFlagsChangeEpochMs = ? WHERE callsign = ?;";

		try (PreparedStatement preparedStatement = connection.prepareStatement(updateWorkedSql)) {
			preparedStatement.setLong(1, System.currentTimeMillis());
			preparedStatement.setString(2, chatMemberToStore.getCallSignRaw());

			int affectedRows = preparedStatement.executeUpdate();
			return affectedRows > 0;
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't handle DB-Query");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Updates all not-QRV flags for a chatmember row. The method uses the normalized
	 * raw callsign and updates the timestamp so that automatic contest cleanup can
	 * reset the flags later.
	 *
	 * @param chatMemberToStore chatmember with the not-QRV information to persist
	 * @return true if an existing row was updated or inserted
	 * @throws SQLException if the database write fails
	 */
	public synchronized boolean updateNotQRVInfoOnChatMember(ChatMember chatMemberToStore) throws SQLException {

		if (chatMemberToStore == null || chatMemberToStore.getCallSignRaw() == null || chatMemberToStore.getCallSignRaw().isBlank()) {
			return false;
		}

		String updateNotQrvSql =
				"UPDATE ChatMember SET "
						+ "notQRV144 = ?, "
						+ "notQRV432 = ?, "
						+ "notQRV1240 = ?, "
						+ "notQRV2300 = ?, "
						+ "notQRV3400 = ?, "
						+ "notQRV5600 = ?, "
						+ "notQRV10G = ?, "
						+ "lastFlagsChangeEpochMs = ? "
						+ "WHERE callsign = ?;";

		try (PreparedStatement preparedStatement = connection.prepareStatement(updateNotQrvSql)) {
			preparedStatement.setInt(1, helper_booleanIntConverter(!chatMemberToStore.isQrv144()));
			preparedStatement.setInt(2, helper_booleanIntConverter(!chatMemberToStore.isQrv432()));
			preparedStatement.setInt(3, helper_booleanIntConverter(!chatMemberToStore.isQrv1240()));
			preparedStatement.setInt(4, helper_booleanIntConverter(!chatMemberToStore.isQrv2300()));
			preparedStatement.setInt(5, helper_booleanIntConverter(!chatMemberToStore.isQrv3400()));
			preparedStatement.setInt(6, helper_booleanIntConverter(!chatMemberToStore.isQrv5600()));
			preparedStatement.setInt(7, helper_booleanIntConverter(!chatMemberToStore.isQrv10G()));
			preparedStatement.setLong(8, System.currentTimeMillis());
			preparedStatement.setString(9, chatMemberToStore.getCallSignRaw());

			int affectedRows = preparedStatement.executeUpdate();

			if (affectedRows == 0) {
				chatMemberToStore.setLastFlagsChangeEpochMs(System.currentTimeMillis());
				storeChatMember(chatMemberToStore);
			}

			return true;
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't handle DB-Query");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Writes a complete ChatMember row including worked state and timestamp back into
	 * the database. This helper is used during migration when the complete table is
	 * rebuilt with normalized raw callsign keys.
	 *
	 * @param chatMemberToStore chatmember state to write completely
	 */
	private synchronized void helper_upsertCompleteChatMemberRow(ChatMember chatMemberToStore) {

		String upsertCompleteRowSql =
				"INSERT INTO ChatMember ("
						+ "callsign, qra, name, lastActivityDateTime, worked, worked144, worked432, worked1240, worked2300, worked3400, worked5600, worked10G, "
						+ "notQRV144, notQRV432, notQRV1240, notQRV2300, notQRV3400, notQRV5600, notQRV10G, lastFlagsChangeEpochMs"
						+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
						+ "ON CONFLICT(callsign) DO UPDATE SET "
						+ "qra = excluded.qra, "
						+ "name = excluded.name, "
						+ "lastActivityDateTime = excluded.lastActivityDateTime, "
						+ "worked = excluded.worked, "
						+ "worked144 = excluded.worked144, "
						+ "worked432 = excluded.worked432, "
						+ "worked1240 = excluded.worked1240, "
						+ "worked2300 = excluded.worked2300, "
						+ "worked3400 = excluded.worked3400, "
						+ "worked5600 = excluded.worked5600, "
						+ "worked10G = excluded.worked10G, "
						+ "notQRV144 = excluded.notQRV144, "
						+ "notQRV432 = excluded.notQRV432, "
						+ "notQRV1240 = excluded.notQRV1240, "
						+ "notQRV2300 = excluded.notQRV2300, "
						+ "notQRV3400 = excluded.notQRV3400, "
						+ "notQRV5600 = excluded.notQRV5600, "
						+ "notQRV10G = excluded.notQRV10G, "
						+ "lastFlagsChangeEpochMs = excluded.lastFlagsChangeEpochMs;";

		try (PreparedStatement preparedStatement = connection.prepareStatement(upsertCompleteRowSql)) {
			preparedStatement.setString(1, chatMemberToStore.getCallSignRaw());
			preparedStatement.setString(2, chatMemberToStore.getQra());
			preparedStatement.setString(3, chatMemberToStore.getName());
			preparedStatement.setString(4, chatMemberToStore.getLastActivity() == null ? null : chatMemberToStore.getLastActivity().toString());
			preparedStatement.setInt(5, helper_booleanIntConverter(chatMemberToStore.isWorked()));
			preparedStatement.setInt(6, helper_booleanIntConverter(chatMemberToStore.isWorked144()));
			preparedStatement.setInt(7, helper_booleanIntConverter(chatMemberToStore.isWorked432()));
			preparedStatement.setInt(8, helper_booleanIntConverter(chatMemberToStore.isWorked1240()));
			preparedStatement.setInt(9, helper_booleanIntConverter(chatMemberToStore.isWorked2300()));
			preparedStatement.setInt(10, helper_booleanIntConverter(chatMemberToStore.isWorked3400()));
			preparedStatement.setInt(11, helper_booleanIntConverter(chatMemberToStore.isWorked5600()));
			preparedStatement.setInt(12, helper_booleanIntConverter(chatMemberToStore.isWorked10G()));
			preparedStatement.setInt(13, helper_booleanIntConverter(!chatMemberToStore.isQrv144()));
			preparedStatement.setInt(14, helper_booleanIntConverter(!chatMemberToStore.isQrv432()));
			preparedStatement.setInt(15, helper_booleanIntConverter(!chatMemberToStore.isQrv1240()));
			preparedStatement.setInt(16, helper_booleanIntConverter(!chatMemberToStore.isQrv2300()));
			preparedStatement.setInt(17, helper_booleanIntConverter(!chatMemberToStore.isQrv3400()));
			preparedStatement.setInt(18, helper_booleanIntConverter(!chatMemberToStore.isQrv5600()));
			preparedStatement.setInt(19, helper_booleanIntConverter(!chatMemberToStore.isQrv10G()));
			preparedStatement.setLong(20, chatMemberToStore.getLastFlagsChangeEpochMs());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("[DBH, ERROR:] Could not rebuild normalized ChatMember row", e);
		}
	}

	/**
	 * Builds a ChatMember object from a database result row.
	 *
	 * @param resultSet current database row
	 * @return chatmember filled with worked/not-QRV database state
	 * @throws SQLException if a column cannot be read
	 */
	private synchronized ChatMember helper_buildChatMemberFromResultSet(ResultSet resultSet) throws SQLException {

		ChatMember builtChatMember = new ChatMember();

		builtChatMember.setCallSign(resultSet.getString("callsign"));
		builtChatMember.setQra(resultSet.getString("qra"));
		builtChatMember.setName(resultSet.getString("name"));
		builtChatMember.setWorked(helper_IntToBooleanConverter(resultSet.getInt("worked")));
		builtChatMember.setWorked144(helper_IntToBooleanConverter(resultSet.getInt("worked144")));
		builtChatMember.setWorked432(helper_IntToBooleanConverter(resultSet.getInt("worked432")));
		builtChatMember.setWorked1240(helper_IntToBooleanConverter(resultSet.getInt("worked1240")));
		builtChatMember.setWorked2300(helper_IntToBooleanConverter(resultSet.getInt("worked2300")));
		builtChatMember.setWorked3400(helper_IntToBooleanConverter(resultSet.getInt("worked3400")));
		builtChatMember.setWorked5600(helper_IntToBooleanConverter(resultSet.getInt("worked5600")));
		builtChatMember.setWorked10G(helper_IntToBooleanConverter(resultSet.getInt("worked10G")));
		builtChatMember.setQrv144(!helper_IntToBooleanConverter(resultSet.getInt("notQRV144")));
		builtChatMember.setQrv432(!helper_IntToBooleanConverter(resultSet.getInt("notQRV432")));
		builtChatMember.setQrv1240(!helper_IntToBooleanConverter(resultSet.getInt("notQRV1240")));
		builtChatMember.setQrv2300(!helper_IntToBooleanConverter(resultSet.getInt("notQRV2300")));
		builtChatMember.setQrv3400(!helper_IntToBooleanConverter(resultSet.getInt("notQRV3400")));
		builtChatMember.setQrv5600(!helper_IntToBooleanConverter(resultSet.getInt("notQRV5600")));
		builtChatMember.setQrv10G(!helper_IntToBooleanConverter(resultSet.getInt("notQRV10G")));
		builtChatMember.setLastFlagsChangeEpochMs(resultSet.getLong("lastFlagsChangeEpochMs"));

		return builtChatMember;
	}

	/**
	 * Copies the worked and not-QRV state from one ChatMember object to another one.
	 *
	 * @param sourceChatMember source state
	 * @param targetChatMember target object that should receive the state
	 */
	private synchronized void helper_copyWorkedAndQrvFlags(ChatMember sourceChatMember, ChatMember targetChatMember) {

		targetChatMember.setWorked(sourceChatMember.isWorked());
		targetChatMember.setWorked144(sourceChatMember.isWorked144());
		targetChatMember.setWorked432(sourceChatMember.isWorked432());
		targetChatMember.setWorked1240(sourceChatMember.isWorked1240());
		targetChatMember.setWorked2300(sourceChatMember.isWorked2300());
		targetChatMember.setWorked3400(sourceChatMember.isWorked3400());
		targetChatMember.setWorked5600(sourceChatMember.isWorked5600());
		targetChatMember.setWorked10G(sourceChatMember.isWorked10G());
		targetChatMember.setQrv144(sourceChatMember.isQrv144());
		targetChatMember.setQrv432(sourceChatMember.isQrv432());
		targetChatMember.setQrv1240(sourceChatMember.isQrv1240());
		targetChatMember.setQrv2300(sourceChatMember.isQrv2300());
		targetChatMember.setQrv3400(sourceChatMember.isQrv3400());
		targetChatMember.setQrv5600(sourceChatMember.isQrv5600());
		targetChatMember.setQrv10G(sourceChatMember.isQrv10G());
		targetChatMember.setLastFlagsChangeEpochMs(sourceChatMember.getLastFlagsChangeEpochMs());
	}

	/**
	 * Determines which worked-band column must be updated for a given chatmember.
	 *
	 * @param chatMemberToStore worked chatmember update
	 * @return database column name, or null if no worked band is marked
	 */
	private synchronized String helper_resolveWorkedBandColumnName(ChatMember chatMemberToStore) {

		if (chatMemberToStore.isWorked144()) {
			return "worked144";
		} else if (chatMemberToStore.isWorked432()) {
			return "worked432";
		} else if (chatMemberToStore.isWorked1240()) {
			return "worked1240";
		} else if (chatMemberToStore.isWorked2300()) {
			return "worked2300";
		} else if (chatMemberToStore.isWorked3400()) {
			return "worked3400";
		} else if (chatMemberToStore.isWorked5600()) {
			return "worked5600";
		} else if (chatMemberToStore.isWorked10G()) {
			return "worked10G";
		}

		return null;
	}

	/**
	 * Resolves the timestamp that should be written into the timestamp column when a
	 * complete row is inserted. Normal member metadata rows keep the value 0, while
	 * rows with existing worked/not-QRV state receive the current timestamp.
	 *
	 * @param chatMemberToStore row that is about to be inserted
	 * @return timestamp to write into the database
	 */
	private synchronized long helper_resolveLastFlagsChangeEpochMsForStore(ChatMember chatMemberToStore) {

		if (chatMemberToStore.getLastFlagsChangeEpochMs() > 0) {
			return chatMemberToStore.getLastFlagsChangeEpochMs();
		}

		if (helper_hasAnyWorkedOrNotQrvState(chatMemberToStore)) {
			return System.currentTimeMillis();
		}

		return 0L;
	}

	/**
	 * Checks whether a ChatMember row currently contains any persisted worked or
	 * not-QRV information.
	 *
	 * @param chatMemberToStore chatmember to inspect
	 * @return true if any database-relevant state is set
	 */
	private synchronized boolean helper_hasAnyWorkedOrNotQrvState(ChatMember chatMemberToStore) {
		return chatMemberToStore.isWorked()
				|| chatMemberToStore.isWorked144()
				|| chatMemberToStore.isWorked432()
				|| chatMemberToStore.isWorked1240()
				|| chatMemberToStore.isWorked2300()
				|| chatMemberToStore.isWorked3400()
				|| chatMemberToStore.isWorked5600()
				|| chatMemberToStore.isWorked10G()
				|| !chatMemberToStore.isQrv144()
				|| !chatMemberToStore.isQrv432()
				|| !chatMemberToStore.isQrv1240()
				|| !chatMemberToStore.isQrv2300()
				|| !chatMemberToStore.isQrv3400()
				|| !chatMemberToStore.isQrv5600()
				|| !chatMemberToStore.isQrv10G();
	}

	/**
	 * Converts a boolean value into the integer representation used in the SQLite
	 * table.
	 *
	 * @param convertToInt boolean value
	 * @return 1 for true, 0 for false
	 */
	private int helper_booleanIntConverter(boolean convertToInt) {

		if (convertToInt) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Converts the integer representation from SQLite into a boolean value.
	 *
	 * @param valueFromDBField integer value from the database
	 * @return true if the value is not zero
	 */
	private boolean helper_IntToBooleanConverter(int valueFromDBField) {

		if (valueFromDBField != 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Small manual test entry point for local experiments on the database helper.
	 *
	 * @param args CLI arguments
	 * @throws SQLException if a database operation fails
	 */
	public static void main(String[] args) throws SQLException {
		DBController dbc = DBController.getInstance();

		ChatMember dummy = new ChatMember();
		dummy.setCallSign("DM5M");
		dummy.setQra("jo51ij");
		dummy.setName("Team Test");
		dummy.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
		dummy.setWorked5600(true);

		// dbc.storeChatMember(dummy);
		// dbc.updateWkdInfoOnChatMember(dummy);
	}
}
