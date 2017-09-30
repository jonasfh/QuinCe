package uk.ac.exeter.QuinCe.data.QC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.config.ColumnConfig;
import uk.ac.exeter.QCRoutines.data.DataRecordException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.ParameterException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class QCDB {

	private static final int FIELD_ROW_NUMBER = 1;
	
	private static final int FIELD_INTAKE_TEMP_1_USED = 2;
	
	private static final int FIELD_INTAKE_TEMP_2_USED = 3;
	
	private static final int FIELD_INTAKE_TEMP_3_USED = 4;
	
	private static final int FIELD_SALINITY_1_USED = 5;
	
	private static final int FIELD_SALINITY_2_USED = 6;
	
	private static final int FIELD_SALINITY_3_USED = 7;
	
	private static final int FIELD_EQT_1_USED = 8;
	
	private static final int FIELD_EQT_2_USED = 9;

	private static final int FIELD_EQT_3_USED = 10;

	private static final int FIELD_EQP_1_USED = 11;
	
	private static final int FIELD_EQP_2_USED = 12;

	private static final int FIELD_EQP_3_USED = 13;

	private static final int FIELD_QC_FLAG = 14;
	
	private static final int FIELD_QC_COMMENT = 15;
	
	private static final int FIELD_WOCE_FLAG = 16;
	
	private static final int FIELD_WOCE_COMMENT = 17;
	
	private static final int FIRST_DATA_FIELD = 18;

	private static final String CLEAR_QC_STATEMENT = "DELETE FROM qc WHERE data_file_id = ?";
	
	private static final String GET_QC_RECORDS_STATEMENT = "SELECT r.row, "
			+ "q.intake_temp_1_used, q.intake_temp_2_used, q.intake_temp_3_used, "
			+ "q.salinity_1_used, q.salinity_2_used, q.salinity_3_used, "
			+ "q.eqt_1_used, q.eqt_2_used, q.eqt_3_used, "
			+ "q.eqp_1_used, q.eqp_2_used, q.eqp_3_used, "
			+ "q.qc_flag, q.qc_message, q.woce_flag, q.woce_message, "
			+ "r.co2_type, r.date_time, r.longitude, "
			+ "r.latitude, r.intake_temp_1, r.intake_temp_2, r.intake_temp_3, "
			+ "r.salinity_1, r.salinity_2, r.salinity_3, r.eqt_1, r.eqt_2, r.eqt_3, r.eqp_1, r.eqp_2, r.eqp_3, "
			+ "r.xh2o, r.atmospheric_pressure, r.co2, "
			+ "d.mean_intake_temp, d.mean_salinity, d.mean_eqt, d.mean_eqp, d.true_xh2o, d.dried_co2, "
			+ "d.calibrated_co2, d.pco2_te_dry, d.ph2o, d.pco2_te_wet, d.fco2_te, d.fco2 "
			+ "FROM raw_data as r "
			+ "INNER JOIN data_reduction as d ON r.data_file_id = d.data_file_id AND r.row = d.row "
			+ "INNER JOIN qc as q ON d.data_file_id  = q.data_file_id AND d.row = q.row "
			+ "WHERE r.data_file_id = ? AND q.woce_flag != " + Flag.VALUE_IGNORED + " AND q.woce_flag != " + Flag.VALUE_BAD + " " 
			+ "AND q.woce_flag != " + Flag.VALUE_FATAL + " ORDER BY r.row ASC";
	
	private static final String GET_NO_DATA_QC_RECORDS_STATEMENT = "SELECT row, "
			+ "intake_temp_1_used, intake_temp_2_used, intake_temp_3_used, "
			+ "salinity_1_used, salinity_2_used, salinity_3_used, "
			+ "eqt_1_used, eqt_2_used, eqt_3_used, "
			+ "eqp_1_used, eqp_2_used, eqp_3_used, "
			+ "qc_flag, qc_message, woce_flag, woce_message "
			+ "FROM qc WHERE data_file_id = ? ORDER BY row ASC";
	
	private static final String ADD_QC_RECORD_STATEMENT = "INSERT INTO qc (data_file_id, row, "
			+ "intake_temp_1_used, intake_temp_2_used, intake_temp_3_used, "
			+ "salinity_1_used, salinity_2_used, salinity_3_used, "
			+ "eqt_1_used, eqt_2_used, eqt_3_used, "
			+ "eqp_1_used, eqp_2_used, eqp_3_used, "
			+ "qc_flag, qc_message, woce_flag, woce_message) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String SET_QC_RESULT_STATEMENT = "UPDATE qc SET "
			+ "intake_temp_1_used = ?, intake_temp_2_used = ?, intake_temp_3_used = ?, "
			+ "salinity_1_used = ?, salinity_2_used = ?, salinity_3_used = ?, "
			+ "eqt_1_used = ?, eqt_2_used = ?, eqt_3_used = ?, "
			+ "eqp_1_used = ?, eqp_2_used = ?, eqp_3_used = ?, "
			+ "qc_flag = ?, qc_message = ?,"
			+ "woce_flag = ?, woce_message = ? WHERE data_file_id = ? AND row = ?";
	
	private static final String SET_QC_FLAG_STATEMENT = "UPDATE qc SET "
			+ "qc_flag = ?, qc_message = ?,"
			+ "woce_flag = ?, woce_message = ? WHERE data_file_id = ? AND row = ?";
	
	private static final String GET_QC_MESSAGES_QUERY = "SELECT qc_message FROM qc WHERE data_file_id = ? AND row = ?";

	private static final String GET_QC_FLAG_QUERY = "SELECT qc_flag FROM qc WHERE data_file_id = ? AND row = ?";
	
	private static final String GET_WOCE_FLAG_QUERY = "SELECT woce_flag FROM qc WHERE data_file_id = ? AND row = ?";
	
	private static final String CLEAR_QC_FLAGS_BY_FLAG_STATEMENT = "UPDATE qc SET qc_flag = " + Flag.VALUE_NOT_SET
			+ ", qc_message = NULL, woce_flag = " + Flag.VALUE_NOT_SET + ", woce_message = NULL "
			+ "WHERE data_file_id = ? AND woce_flag = ? AND woce_message = ?";
	
	private static final String CLEAR_QC_FLAGS_BY_ROW_STATEMENT = "UPDATE qc SET qc_flag = " + Flag.VALUE_NOT_SET
			+ ", qc_message = NULL, woce_flag = " + Flag.VALUE_NOT_SET + ", woce_message = NULL "
			+ "WHERE data_file_id = ? AND row = ?";
	
	private static final String GET_QC_FLAGS_QUERY = "SELECT row, qc_flag, qc_message FROM qc WHERE data_file_id = ? AND row IN (%%ROWS%%)";
	
	private static final String SET_WOCE_DETAILS_STATEMENT = "UPDATE qc SET woce_flag = ?, woce_message = ? WHERE data_file_id = ? AND row = ?";
	
	private static final String SET_WOCE_FLAG_STATEMENT = "UPDATE qc SET woce_flag = ?, woce_message = ? WHERE data_file_id = ? AND row IN (%%ROWS%%)";
	
	public static void clearQCData(DataSource dataSource, long fileId) throws DatabaseException {
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			clearQCData(conn, fileId);
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while clearing out previous data", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	public static void clearQCData(Connection conn, long fileId) throws DatabaseException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(CLEAR_QC_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.execute();
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("An error occurred while clearing out previous data", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	/**
	 * Build and retrieve a complete set of QC data for a given data file.
	 * This method assumes that no QC records have yet been created, and creates them.
	 * @param dataSource A data source
	 * @param fileId The data file ID
	 * @param instrument The instrument to which the data file belongs
	 * @return The list of QC records ready to be processed
	 * @throws DatabaseException If the QC records cannot be retrieved, or cannot be created.
	 */
	public static List<QCRecord> getQCRecords(DataSource dataSource, ColumnConfig columnConfig, long fileId, Instrument instrument) throws DatabaseException {

		Connection conn = null;
		List<QCRecord> result = null;
		
		try {
			conn = dataSource.getConnection();
			result = getQCRecords(conn, columnConfig, fileId, instrument);
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving QC records", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}

		return result;
	}
	
	/**
	 * Build and retrieve a complete set of QC data for a given data file.
	 * @param conn A database connection
	 * @param fileId The data file ID
	 * @param instrument The instrument to which the data file belongs
	 * @return The list of QC records ready to be processed
	 * @throws DatabaseException If the QC records cannot be retrieved, or cannot be created.
	 */
	public static List<QCRecord> getQCRecords(Connection conn, ColumnConfig columnConfig, long fileId, Instrument instrument) throws DatabaseException {
		
		
		PreparedStatement readStatement = null;
		ResultSet records = null;
		List<QCRecord> qcRecords = new ArrayList<QCRecord>();
		
		try {
			readStatement = conn.prepareStatement(GET_QC_RECORDS_STATEMENT);
			readStatement.setLong(1,  fileId);
			
			records = readStatement.executeQuery();
			
			while (records.next()) {
				
				// Extract the row number and QC flags/comments
				int rowNumber = records.getInt(FIELD_ROW_NUMBER);
				
				boolean intakeTemp1Used = records.getBoolean(FIELD_INTAKE_TEMP_1_USED);
				boolean intakeTemp2Used = records.getBoolean(FIELD_INTAKE_TEMP_2_USED);
				boolean intakeTemp3Used = records.getBoolean(FIELD_INTAKE_TEMP_3_USED);
				boolean salinity1Used = records.getBoolean(FIELD_SALINITY_1_USED);
				boolean salinity2Used = records.getBoolean(FIELD_SALINITY_2_USED);
				boolean salinity3Used = records.getBoolean(FIELD_SALINITY_3_USED);
				boolean eqt1Used = records.getBoolean(FIELD_EQT_1_USED);
				boolean eqt2Used = records.getBoolean(FIELD_EQT_2_USED);
				boolean eqt3Used = records.getBoolean(FIELD_EQT_3_USED);
				boolean eqp1Used = records.getBoolean(FIELD_EQP_1_USED);
				boolean eqp2Used = records.getBoolean(FIELD_EQP_2_USED);
				boolean eqp3Used = records.getBoolean(FIELD_EQP_3_USED);

				Flag qcFlag = new Flag(records.getInt(FIELD_QC_FLAG));
				List<Message> qcComments = RebuildCode.getMessagesFromRebuildCodes(records.getString(FIELD_QC_COMMENT));
				Flag woceFlag = new Flag(records.getInt(FIELD_WOCE_FLAG));
				String woceComment = records.getString(FIELD_WOCE_COMMENT);

				// The remainder of the fields are data fields for the QC record
				List<String> recordData = new ArrayList<String>();
				recordData.add(null); // Field indices are 1-based
				for (int i = FIRST_DATA_FIELD; i <= FIRST_DATA_FIELD + columnConfig.getColumnCount() - 1; i++) {
					recordData.add(records.getString(i));
				}
				
				qcRecords.add(new QCRecord(fileId, instrument, columnConfig, rowNumber, recordData,
						intakeTemp1Used, intakeTemp2Used, intakeTemp3Used, salinity1Used, salinity2Used, salinity3Used,
						eqt1Used, eqt2Used, eqt3Used, eqp1Used, eqp2Used, eqp3Used,
						qcFlag, qcComments, woceFlag, woceComment));
			}

		} catch (SQLException|DataRecordException|MessageException|InvalidFlagException e) {
			throw new DatabaseException("An error occurred while retrieving records for QC", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(readStatement);
		}
		
		return qcRecords;
	}
	
	public static Map<Integer, NoDataQCRecord> getNoDataQCRecords(Connection conn, ColumnConfig columnConfig, long fileId, Instrument instrument) throws DatabaseException {
		
		
		PreparedStatement readStatement = null;
		ResultSet records = null;
		Map<Integer, NoDataQCRecord> qcRecords = new TreeMap<Integer, NoDataQCRecord>();
		int rowNumber = -1;
		
		try {
			readStatement = conn.prepareStatement(GET_NO_DATA_QC_RECORDS_STATEMENT);
			readStatement.setLong(1,  fileId);
			
			records = readStatement.executeQuery();
			
			while (records.next()) {
				
				// Extract the row number and QC flags/comments
				rowNumber = records.getInt(FIELD_ROW_NUMBER);
				
				boolean intakeTemp1Used = records.getBoolean(FIELD_INTAKE_TEMP_1_USED);
				boolean intakeTemp2Used = records.getBoolean(FIELD_INTAKE_TEMP_2_USED);
				boolean intakeTemp3Used = records.getBoolean(FIELD_INTAKE_TEMP_3_USED);
				boolean salinity1Used = records.getBoolean(FIELD_SALINITY_1_USED);
				boolean salinity2Used = records.getBoolean(FIELD_SALINITY_2_USED);
				boolean salinity3Used = records.getBoolean(FIELD_SALINITY_3_USED);
				boolean eqt1Used = records.getBoolean(FIELD_EQT_1_USED);
				boolean eqt2Used = records.getBoolean(FIELD_EQT_2_USED);
				boolean eqt3Used = records.getBoolean(FIELD_EQT_3_USED);
				boolean eqp1Used = records.getBoolean(FIELD_EQP_1_USED);
				boolean eqp2Used = records.getBoolean(FIELD_EQP_2_USED);
				boolean eqp3Used = records.getBoolean(FIELD_EQP_3_USED);

				Flag qcFlag = new Flag(records.getInt(FIELD_QC_FLAG));
				List<Message> qcComments = RebuildCode.getMessagesFromRebuildCodes(records.getString(FIELD_QC_COMMENT));
				Flag woceFlag = new Flag(records.getInt(FIELD_WOCE_FLAG));
				String woceComment = records.getString(FIELD_WOCE_COMMENT);

				qcRecords.put(rowNumber, new NoDataQCRecord(fileId, instrument, columnConfig, rowNumber,
						intakeTemp1Used, intakeTemp2Used, intakeTemp3Used, salinity1Used, salinity2Used, salinity3Used,
						eqt1Used, eqt2Used, eqt3Used, eqp1Used, eqp2Used, eqp3Used,
						qcFlag, qcComments, woceFlag, woceComment));
			}

		} catch (SQLException|DataRecordException|MessageException|InvalidFlagException e) {
			throw new DatabaseException("An error occurred while retrieving records for QC (row " + rowNumber + ")", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(readStatement);
		}
		
		return qcRecords;
	}

	public static void createQCRecord(Connection conn, long fileId, int row, Instrument instrument) throws DatabaseException {

		// TODO Rewrite
		
		/*
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(ADD_QC_RECORD_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			stmt.setBoolean(3, instrument.hasIntakeTemp1());
			stmt.setBoolean(4, instrument.hasIntakeTemp2());
			stmt.setBoolean(5, instrument.hasIntakeTemp3());
			stmt.setBoolean(6, instrument.hasSalinity1());
			stmt.setBoolean(7, instrument.hasSalinity2());
			stmt.setBoolean(8, instrument.hasSalinity3());
			stmt.setBoolean(9, instrument.hasEqt1());
			stmt.setBoolean(10, instrument.hasEqt2());
			stmt.setBoolean(11, instrument.hasEqt3());
			stmt.setBoolean(12, instrument.hasEqp1());
			stmt.setBoolean(13, instrument.hasEqp2());
			stmt.setBoolean(14, instrument.hasEqp3());
			stmt.setInt(15, Flag.NOT_SET.getFlagValue());
			stmt.setString(16, "");
			stmt.setInt(17, Flag.NOT_SET.getFlagValue());
			stmt.setString(18, "");
			
			stmt.execute();

		} catch (SQLException e) {
			throw new DatabaseException("Error while creating QC record for file " + fileId + ", row " + row, e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
		
		*/
		
	}
	
	public static void setQC(Connection conn, long fileId, QCRecord record) throws DatabaseException, MessageException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(SET_QC_RESULT_STATEMENT);
			
			stmt.setBoolean(1, record.getIntakeTemp1Used());
			stmt.setBoolean(2, record.getIntakeTemp2Used());
			stmt.setBoolean(3, record.getIntakeTemp3Used());
			stmt.setBoolean(4, record.getSalinity1Used());
			stmt.setBoolean(5, record.getSalinity2Used());
			stmt.setBoolean(6, record.getSalinity3Used());
			stmt.setBoolean(7, record.getEqt1Used());
			stmt.setBoolean(8, record.getEqt2Used());
			stmt.setBoolean(9, record.getEqt3Used());
			stmt.setBoolean(10, record.getEqp1Used());
			stmt.setBoolean(11, record.getEqp2Used());
			stmt.setBoolean(12, record.getEqp3Used());
			stmt.setInt(13, record.getQCFlag().getFlagValue());
			stmt.setString(14, RebuildCode.getRebuildCodes(record.getMessages()));
			stmt.setInt(15, record.getWoceFlag().getFlagValue());
			stmt.setString(16, record.getWoceComment());
			stmt.setLong(17, fileId);
			stmt.setInt(18, record.getLineNumber());
			
			stmt.execute();
			
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while storing the QC result", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	public static void setQCFlag(Connection conn, long fileId, int row, Flag qcFlag, Message qcMessage) throws DatabaseException {
		setQCFlag(conn, fileId, row, qcFlag, qcMessage, qcFlag, qcMessage.getShortMessage());
	}
	
	public static void setQCFlag(Connection conn, long fileId, int row, Flag qcFlag, Message qcMessage, Flag woceFlag, String woceMessage) throws DatabaseException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(SET_QC_FLAG_STATEMENT);
			
			stmt.setInt(1, qcFlag.getFlagValue());
			stmt.setString(2, qcMessage.getRebuildCode().getCode());
			stmt.setInt(3, woceFlag.getFlagValue());
			stmt.setString(4, woceMessage);
			stmt.setLong(5, fileId);
			stmt.setInt(6, row);
			
			stmt.execute();
		} catch (SQLException|MessageException e) {
			throw new DatabaseException("An error occurred while setting the QC flags", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	public static void resetQCFlagsByWoceFlag(Connection conn, long fileId, Flag woceSearchFlag, String woceSearchMessage) throws DatabaseException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(CLEAR_QC_FLAGS_BY_FLAG_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, woceSearchFlag.getFlagValue());
			stmt.setString(3, woceSearchMessage);
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while clearing the QC flags", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	public static void resetQCFlagsByRow(Connection conn, long fileId, int row) throws DatabaseException {
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(CLEAR_QC_FLAGS_BY_ROW_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while clearing the QC flags", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	public static List<Message> getQCMessages(Connection conn, long fileId, int row) throws MessageException, DatabaseException, RecordNotFoundException {
		
		PreparedStatement stmt = null;
		ResultSet record = null;
		List<Message> result = null;
		
		try {
			stmt = conn.prepareStatement(GET_QC_MESSAGES_QUERY);
			
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			
			record = stmt.executeQuery();
			
			if (!record.next()) {
				throw new RecordNotFoundException("Could not find QC record for file " + fileId + ", row " + row);
			} else {
				result = RebuildCode.getMessagesFromRebuildCodes(record.getString(1));
			}
			
			return result;
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while retrieving QC messages", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
	}

	public static Flag getQCFlag(Connection conn, long fileId, int row) throws MessageException, DatabaseException, RecordNotFoundException {
		return getFlag(conn, GET_QC_FLAG_QUERY, fileId, row);
	}
	
	public static Flag getWoceFlag(Connection conn, long fileId, int row) throws MessageException, DatabaseException, RecordNotFoundException {
		return getFlag(conn, GET_WOCE_FLAG_QUERY, fileId, row);
	}
	
	/**
	 * Accept the QC flags suggested by the automatic QC. Simply copies
	 * the flags and message into the WOCE fields
	 * @param dataSource A data source
	 * @param fileId The data file's database ID
	 * @param rows The rows for which the flags should be accepted, as a comma-separated list of row numbers
	 * @throws ParameterException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public static void acceptQCFlags(DataSource dataSource, long fileId, String rows) throws ParameterException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(rows, "rows");
		MissingParam.checkListOfIntegers(rows, "rows");
		
		String queryString = GET_QC_FLAGS_QUERY;
		queryString = queryString.replaceAll("%%ROWS%%", rows);
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet resultSet = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			stmt = conn.prepareStatement(queryString);
			stmt.setLong(1, fileId);
			resultSet = stmt.executeQuery();
			
			while (resultSet.next()) {
				
				int row = resultSet.getInt(1);
				int qcFlag = resultSet.getInt(2);
				List<Message> messageList = RebuildCode.getMessagesFromRebuildCodes(resultSet.getString(3));
				
				setWoceDetails(conn, fileId, row, qcFlag, messageList);
			}
			
			conn.commit();
			
		} catch (SQLException|MessageException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("An error occurred while accepting QC flags", e);
		} finally {
			DatabaseUtils.closeResultSets(resultSet);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	public static void setWoceDetails(Connection conn, long fileId, int row, int flag, List<Message> messages) throws DatabaseException {
		
		PreparedStatement stmt = null;
		
		StringBuilder messageString = new StringBuilder();
		Iterator<Message> i = messages.iterator();
		while (i.hasNext()) {
			Message message = i.next();
			messageString.append(message.getShortMessage());
			if (i.hasNext()) {
				messageString.append(';');
			}
		}
		
		try {
			stmt = conn.prepareStatement(SET_WOCE_DETAILS_STATEMENT);
			stmt.setInt(1, flag);
			stmt.setString(2, messageString.toString());
			stmt.setLong(3, fileId);
			stmt.setInt(4, row);
			
			stmt.execute();
			
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while setting the WOCE details");
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
		
	}
	
	private static Flag getFlag(Connection conn, String query, long fileId, int row) throws MessageException, DatabaseException, RecordNotFoundException {
		
		PreparedStatement stmt = null;
		ResultSet record = null;
		Flag result = null;
		
		try {
			stmt = conn.prepareStatement(query);
			
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			
			record = stmt.executeQuery();
			
			if (!record.next()) {
				throw new RecordNotFoundException("Could not find QC record for file " + fileId + ", row " + row);
			} else {
				result = new Flag(record.getInt(1));
			}
			
			return result;
		} catch (SQLException|InvalidFlagException e) {
			throw new DatabaseException("An error occurred while retrieving QC messages", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	public static void setWoceFlags(DataSource dataSource, long fileId, String rows, int flag, String comment) throws ParameterException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(rows, "rows");
		MissingParam.checkListOfIntegers(rows, "rows");
		if (!Flag.isValidFlagValue(flag)) {
			throw new ParameterException("flag", "Invalid flag value");
		}
		
		if (flag != Flag.VALUE_GOOD) {
			MissingParam.checkMissing(comment, "comment");
		}
		
		String queryString = SET_WOCE_FLAG_STATEMENT;
		queryString = queryString.replaceAll("%%ROWS%%", rows);
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = dataSource.getConnection();
			
			stmt = conn.prepareStatement(queryString);
			stmt.setInt(1, flag);
			stmt.setString(2, comment);
			stmt.setLong(3, fileId);
			
			stmt.execute();
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while setting WOCE flags", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
	}
}