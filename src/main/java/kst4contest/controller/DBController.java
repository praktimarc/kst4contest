package kst4contest.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatMember;
import kst4contest.utils.ApplicationFileUtils;

public class DBController {

	/**
	 * Name of the database file.
	 */
	public static final String DATABASE_FILE = "praktiKST.db";

	/**
	 * Resource path for the database
	 */
	public static final String DATABASE_RESOURCE = "/praktiKST.db";

	private static final DBController dbcontroller = new DBController();
	private static Connection connection;
//    private static final String DB_PATH = System.getProperty("praktiKST.db");
	private static String DB_PATH = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, DATABASE_FILE);



/*
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			System.err.println("Fehler beim Laden des JDBC-Treibers");
			e.printStackTrace();
		}
	}
 */

	public DBController() {
		initDBConnection();
	}

	public static DBController getInstance() {
		return dbcontroller;
	}

	/**
	 * Closes the db connecttion and all statements
	 */
	public void closeDBConnection() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initDBConnection() {

		System.out.println("DBH: initiate new db connection");

		try {
			ApplicationFileUtils.copyResourceIfRequired(
					ApplicationConstants.APPLICATION_NAME,
					DATABASE_RESOURCE,
					DATABASE_FILE
			);
			if (connection != null)
				return;
			System.out.println("Creating Connection to Database...");

			DB_PATH  = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, DATABASE_FILE);
			 connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

			//connection = DriverManager.getConnection("jdbc:sqlite:" + "C:\\Users\\prakt\\.praktiKST\\praktiKST.db");




			System.out.println("[DBH, Info]: Path = " + DB_PATH);

			if (!connection.isClosed())
				System.out.println("...Connection established");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					if (!connection.isClosed() && connection != null) {
						connection.close();
						if (connection.isClosed())
							System.out.println("Connection to Database closed");
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});


	}

//	private void handleDB() {
//		try {
//			Statement stmt = connection.createStatement();
//			stmt.executeUpdate("DROP TABLE IF EXISTS books;");
//			stmt.executeUpdate("CREATE TABLE books (author, title, publication, pages, price);");
//			stmt.execute(
//					"INSERT INTO books (author, title, publication, pages, price) VALUES ('Paulchen Paule', 'Paul der Penner', "
//							+ Date.valueOf("2001-05-06") + ", '1234', '5.67')");
//
//			PreparedStatement ps = connection.prepareStatement("INSERT INTO books VALUES (?, ?, ?, ?, ?);");
//
//			ps.setString(1, "Willi Winzig");
//			ps.setString(2, "Willi's Wille");
//			ps.setDate(3, Date.valueOf("2011-05-16"));
//			ps.setInt(4, 432);
//			ps.setDouble(5, 32.95);
//			ps.addBatch();
//
//			ps.setString(1, "Anton Antonius");
//			ps.setString(2, "Anton's Alarm");
//			ps.setDate(3, Date.valueOf("2009-10-01"));
//			ps.setInt(4, 123);
//			ps.setDouble(5, 98.76);
//			ps.addBatch();
//
//			connection.setAutoCommit(false);
//			ps.executeBatch();
//			connection.setAutoCommit(true);
//
//			ResultSet rs = stmt.executeQuery("SELECT * FROM books;");
//			while (rs.next()) {
//				System.out.println("Autor = " + rs.getString("author"));
//				System.out.println("Titel = " + rs.getString("title"));
//				System.out.println("Erscheinungsdatum = " + rs.getDate("publication"));
//				System.out.println("Seiten = " + rs.getInt("pages"));
//				System.out.println("Preis = " + rs.getDouble("price"));
//			}
//			rs.close();
//			connection.close();
//		} catch (SQLException e) {
//			System.err.println("Couldn't handle DB-Query");
//			e.printStackTrace();
//		}
//	}

	/**************************************************************
	 * 
	 * Stores a chatmember with its data to the database. <br/>
	 * <b>It will not insert a callsign entry, if that exists already but update
	 * locator, name and activity-timer. Callsign is unique and pk!</b> <br/>
	 * <br/>
	 * Structure is like following<br/>
	 * 
	 * "callsign" TEXT NOT NULL UNIQUE,<br/>
	 * "qra" TEXT,<br/>
	 * "name" TEXT,<br/>
	 * "lastActivityDateTime" TEXT,<br/>
	 * "worked" BOOLEAN,<br/>
	 * "worked144" BOOLEAN,<br/>
	 * "worked432" BOOLEAN,<br/>
	 * "worked1240" BOOLEAN,<br/>
	 * "worked2300" BOOLEAN,<br/>
	 * "worked3400" BOOLEAN,<br/>
	 * "worked5600" BOOLEAN,<br/>
	 * "worked10G" BOOLEAN,<br/>
	 * 
	 * @throws SQLException
	 */
	public void storeChatMember(ChatMember chatMemberToStore) throws SQLException {
		try {
			Statement stmt = connection.createStatement();
//			ResultSet rs = stmt.executeQuery(
//					"SELECT * FROM ChatMember where callsign = '" + chatMemberToStore.getCallSign() + "';");

//			if (!rs.next()) {

			PreparedStatement ps = connection.prepareStatement(
					"INSERT OR IGNORE INTO ChatMember VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(callsign) DO UPDATE SET qra = '"
							+ chatMemberToStore.getQra() + "', name = '" + chatMemberToStore.getName()
							+ "', lastActivityDateTime = '" + chatMemberToStore.getLastActivity()
							+ "' where callsign = '" + chatMemberToStore.getCallSign() + "';");

			ps.setString(1, chatMemberToStore.getCallSign()); // primary key! Not null!
			ps.setString(2, chatMemberToStore.getQra());
			ps.setString(3, chatMemberToStore.getName());
			ps.setString(4, chatMemberToStore.getLastActivity().toString());
			ps.setInt(5, helper_booleanIntConverter(chatMemberToStore.isWorked()));
			ps.setInt(6, helper_booleanIntConverter(chatMemberToStore.isWorked144()));
			ps.setInt(7, helper_booleanIntConverter(chatMemberToStore.isWorked432()));
			ps.setInt(8, helper_booleanIntConverter(chatMemberToStore.isWorked1240()));
			ps.setInt(9, helper_booleanIntConverter(chatMemberToStore.isWorked2300()));
			ps.setInt(10, helper_booleanIntConverter(chatMemberToStore.isWorked3400()));
			ps.setInt(11, helper_booleanIntConverter(chatMemberToStore.isWorked5600()));
			ps.setInt(12, helper_booleanIntConverter(chatMemberToStore.isWorked10G()));

			ps.addBatch();

			connection.setAutoCommit(false);
			ps.executeBatch();
			connection.setAutoCommit(true);
			stmt.close();
//			} else {

//				System.out.println("DBC: nothing to do");
			// Will not store the callsign entry in the database, it exists already
//			}

//            ResultSet rs = stmt.executeQuery("SELECT * FROM ChatMember;");
//            rs.close();
//			connection.close();
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Chatmember could not been stored.");
			e.printStackTrace();
			connection.close();
		}
	}

	/**************************************************************
	 * 
	 * This method does a select and build a hashmap of chatmembers whith all worked
	 * band data, which are stored at the database <br/>
	 * <b>Usage: one time after startup, for synching the live list with the stored
	 * list, e.g. after program ended</b> <br/>
	 * <br/>
	 * Structure is like following<br/>
	 * 
	 * "callsign" TEXT NOT NULL UNIQUE,<br/>
	 * "qra" TEXT,<br/>
	 * "name" TEXT,<br/>
	 * "lastActivityDateTime" TEXT,<br/>
	 * "worked" BOOLEAN,<br/>
	 * "worked144" BOOLEAN,<br/>
	 * "worked432" BOOLEAN,<br/>
	 * "worked1240" BOOLEAN,<br/>
	 * "worked2300" BOOLEAN,<br/>
	 * "worked3400" BOOLEAN,<br/>
	 * "worked5600" BOOLEAN,<br/>
	 * "worked10G" BOOLEAN,<br/>
	 * 
	 * @throws SQLException
	 */
	public HashMap<String, ChatMember> fetchChatMemberWkdDataFromDB() throws SQLException {

		HashMap<String, ChatMember> fetchedWorkeddata = new HashMap<>();

		try {
			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * FROM ChatMember;");

			ChatMember updateWkdData;

			while (rs.next()) {

				updateWkdData = new ChatMember();

				updateWkdData.setCallSign(rs.getString("callsign"));
				updateWkdData.setWorked(helper_IntToBooleanConverter(rs.getInt("worked")));
				updateWkdData.setWorked144(helper_IntToBooleanConverter(rs.getInt("worked144")));
				updateWkdData.setWorked432(helper_IntToBooleanConverter(rs.getInt("worked432")));
				updateWkdData.setWorked1240(helper_IntToBooleanConverter(rs.getInt("worked1240")));
				updateWkdData.setWorked2300(helper_IntToBooleanConverter(rs.getInt("worked2300")));
				updateWkdData.setWorked3400(helper_IntToBooleanConverter(rs.getInt("worked3400")));
				updateWkdData.setWorked5600(helper_IntToBooleanConverter(rs.getInt("worked5600")));
				updateWkdData.setWorked10G(helper_IntToBooleanConverter(rs.getInt("worked10G")));

				fetchedWorkeddata.put(updateWkdData.getCallSign(), updateWkdData);

//				System.out.println(
//						"[DBH, Info:] providing callsign wkd info, wkd, 144, 432, ... : " + updateWkdData.toString());

			}
			stmt.close();
			rs.close();

			return fetchedWorkeddata;

//			connection.close();
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't handle DB-Query");
			e.printStackTrace();
			connection.close();
		}
		return fetchedWorkeddata; // TODO: what to do if its empty?

	}

	/**************************************************************
	 * 
	 * This method does a select and build a hashmap of chatmembers whith all worked
	 * band data, which are stored at the database for restore worked state in the
	 * chat after someone disconnected and reconnected.<br/>
	 * <b>Usage: MessagebusManagementThread, every time after a new User connects to
	 * the chat</b> <br/>
	 * <br/>
	 * 
	 * @return a modified version of the chatmember-object, which the method takes
	 * 
	 * @throws SQLException
	 */
	public ChatMember fetchChatMemberWkdDataForOnlyOneCallsignFromDB(ChatMember checkForThis) {

		try {
			Statement stmt = connection.createStatement();

			ResultSet rs = stmt
					.executeQuery("SELECT * FROM ChatMember where callsign = '" + checkForThis.getCallSign() + "' ;");

//			ChatMember updateWkdData;

//			if (!rs.isBeforeFirst()) { //if there are no data to update....

			while (rs.next()) {

//				updateWkdData = new ChatMember();

//				updateWkdData.setCallSign(rs.getString("callsign"));
				checkForThis.setWorked(helper_IntToBooleanConverter(rs.getInt("worked")));
				checkForThis.setWorked144(helper_IntToBooleanConverter(rs.getInt("worked144")));
				checkForThis.setWorked432(helper_IntToBooleanConverter(rs.getInt("worked432")));
				checkForThis.setWorked1240(helper_IntToBooleanConverter(rs.getInt("worked1240")));
				checkForThis.setWorked2300(helper_IntToBooleanConverter(rs.getInt("worked2300")));
				checkForThis.setWorked3400(helper_IntToBooleanConverter(rs.getInt("worked3400")));
				checkForThis.setWorked5600(helper_IntToBooleanConverter(rs.getInt("worked5600")));
				checkForThis.setWorked10G(helper_IntToBooleanConverter(rs.getInt("worked10G")));

				System.out.println(
						"[DBH, Info:] providing callsign wkd info, wkd, 144, 432, ... for UA5 new chatmember : "
								+ checkForThis.toString());

			}
//			}
			rs.close();
			stmt.close();
			
			return checkForThis;

//			connection.close();
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't handle DB-Query");
			e.printStackTrace();

			try {
				connection.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return checkForThis; // TODO: what to do if its empty?

	}

	/**************************************************************
	 * 
	 * This method removes all worked band data for each callsign in the
	 * database.<br/>
	 * <b>Usage: User triggered after User clicked the reset-wkd button, may in each
	 * new contest period</b> <br/>
	 * <br/>
	 * 
	 * @return true if reset was successful
	 * 
	 * @throws SQLException
	 */
	public int resetWorkedDataInDB() {

		try {
			Statement stmt = connection.createStatement();

			int affected = stmt.executeUpdate("update ChatMember set worked = 0, worked144 = 0, worked432 = 0, worked1240 = 0, worked2300 = 0, worked3400 = 0, worked5600 = 0, worked10G = 0;");
			 
			stmt.close();
			
			return affected;

		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't reset the worked data");
			e.printStackTrace();

			try {
				connection.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
				return -1;
			}
			return -1;
		}
//		return checkForThis; // TODO: what to do if its empty?

	}

	/**************************************************************
	 * 
	 * Updates the worked-information for a chatmember at the db <br/>
	 * <b>It will not revert wkd info, only add wkd info for a band (only new worked
	 * bands will be stored, no worked info will be removed).</b><br/>
	 * The wkd fields should be cleaned by the user at the begin of a new
	 * contest<br/>
	 * 
	 * // This will update the worked info on a worked chatmember. The DBHandler
	 * will check, if an entry at the db had been modified. If not, then the worked
	 * station had not been stored yet and have to be stored. The informations have
	 * to be stored, then by calling the store method of the DBHandler (<b>it´s up
	 * to you, guy!</b>)!<br/>
	 * <br/>
	 * 
	 * @return true if an entry had been modified, false if not
	 * 
	 * 
	 *         <br/>
	 *         <br/>
	 *         Structure is like following<br/>
	 * 
	 *         "callsign" TEXT NOT NULL UNIQUE,<br/>
	 *         "qra" TEXT,<br/>
	 *         "name" TEXT,<br/>
	 *         "lastActivityDateTime" TEXT,<br/>
	 *         "worked" BOOLEAN,<br/>
	 *         "worked144" BOOLEAN,<br/>
	 *         "worked432" BOOLEAN,<br/>
	 *         "worked1240" BOOLEAN,<br/>
	 *         "worked2300" BOOLEAN,<br/>
	 *         "worked3400" BOOLEAN,<br/>
	 *         "worked5600" BOOLEAN,<br/>
	 *         "worked10G" BOOLEAN,<br/>
	 * 
	 * @throws SQLException
	 */
	public boolean updateWkdInfoOnChatMember(ChatMember chatMemberToStore) throws SQLException {
		try {
			Statement stmt = connection.createStatement();
//			stmt.close();

			/**
			 * at first, mark the station as worked, always
			 */
			PreparedStatement ps = connection.prepareStatement("UPDATE ChatMember set worked = ? WHERE CallSign = ?");

			ps.setInt(1, 1); // 1st variable will be set
			ps.setString(2, chatMemberToStore.getCallSign());

			ps.addBatch();

			connection.setAutoCommit(false);
			ps.executeBatch();
			connection.setAutoCommit(true);

			/**
			 * Then, handle the update information of received worked udp message
			 */

			String bandVariable = "worked";

			if (chatMemberToStore.isWorked144()) {
				bandVariable = "worked144";
			} else if (chatMemberToStore.isWorked432()) {
				bandVariable = "worked432";
			} else if (chatMemberToStore.isWorked1240()) {
				bandVariable = "worked1240";
			} else if (chatMemberToStore.isWorked2300()) {
				bandVariable = "worked2300";
			} else if (chatMemberToStore.isWorked3400()) {
				bandVariable = "worked3400";
			} else if (chatMemberToStore.isWorked5600()) {
				bandVariable = "worked5600";
			} else if (chatMemberToStore.isWorked10G()) {
				bandVariable = "worked10G";
			} else {
				System.out.println("[DBCtrl, Error]: unknown at which band the qso had been!");
			}

			PreparedStatement ps2 = connection
					.prepareStatement("UPDATE ChatMember set " + bandVariable + " = ? WHERE CallSign = ?");

			ps2.setInt(1, 1); // 1st variable will be set
			ps2.setString(2, chatMemberToStore.getCallSign());

			ps2.addBatch();

			connection.setAutoCommit(false);
			ps2.executeBatch();
			connection.setAutoCommit(true);

			stmt.close();

			
			System.out.println("updated count of cols: " + ps2.getUpdateCount());
			if (ps2.getUpdateCount() != 0) {
				return true;
			} else
				return false; // no entry to update: user had not been stored at a chatmember in table
//			} else {

//				System.out.println("DBC: nothing to do");
			// Will not store the callsign entry in the database, it exists already
//			}

//            ResultSet rs = stmt.executeQuery("SELECT * FROM ChatMember;");
//            rs.close();
//			connection.close();
			
		} catch (SQLException e) {
			System.err.println("[DBH, ERROR:] Couldn't handle DB-Query");
			e.printStackTrace();
			connection.close();
			return false;
		}
	}

	private int helper_booleanIntConverter(boolean convertToInt) {

		if (convertToInt) {
			return 1;
		} else
			return 0;

	}

	private boolean helper_IntToBooleanConverter(int valueFromDBField) {

		if (valueFromDBField != 0) {
			return true;
		} else
			return false;

	}

	public static void main(String[] args) throws SQLException {
		DBController dbc = DBController.getInstance();
//        dbc.initDBConnection();

		ChatMember dummy = new ChatMember();
		dummy.setCallSign("DM5M");
		dummy.setQra("jo51ij");
		dummy.setName("Team Test");
		dummy.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
		dummy.setWorked5600(true);
//		dummy.setWorked432(true);

//		dbc.storeChatMember(dummy);

		dbc.updateWkdInfoOnChatMember(dummy);

//        dbc.handleDB();
	}
}
