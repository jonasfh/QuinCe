package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableList;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class for handling database queries related to the
 * {@code dataset_data} table
 * @author Steve Jones
 *
 */
public class DataSetDataDB {

  /**
   * Query to get all measurements for a data set
   */
  private static final String GET_ALL_MEASUREMENTS_QUERY = "SELECT * FROM dataset_data WHERE dataset_id = ? ORDER BY date ASC";

  /**
   * Query to get all measurements IDs for a data set
   */
  private static final String GET_ALL_MEASUREMENT_IDS_QUERY = "SELECT id FROM dataset_data WHERE dataset_id = ? ORDER BY date ASC";

  /**
   * Query to get a single measurement
   */
  private static final String GET_MEASUREMENT_QUERY = "SELECT * FROM dataset_data WHERE id = ?";

  /**
   * Query to get the geographical bounds of a data set
   */
  private static final String GET_BOUNDS_QUERY = "SELECT"
      + " MIN(longitude), MIN(latitude), MAX(longitude), MAX(latitude)"
      + " FROM dataset_data WHERE dataset_id = ?";

  /**
   * The name of the ID column
   */
  private static final String ID_COL = "id";

  /**
   * The name of the date column
   */
  private static final String DATE_COL = "date";

  /**
   * The name of the longitude column
   */
  private static final String LON_COL = "longitude";

  /**
   * The name of the latitude column
   */
  private static final String LAT_COL = "latitude";

  /**
   * The name of the run type column
   */
  private static final String RUN_TYPE_COL = "run_type";

  /**
   * The name of the diagnostic values column
   */
  private static final String DIAGNOSTIC_COL = "diagnostic_values";

  /**
   * The name of the dataset ID column
   */
  private static final String DATASET_COL = "dataset_id";

  /**
   * Store a data set record in the database.
   *
   * Measurement and calibration records are automatically detected
   * and stored in the appropriate table.
   *
   * @param conn A database connection
   * @param record The record to be stored
   * @param datasetDataStatement A previously generated statement for inserting a record. Can be null.
   * @return A {@link PreparedStatement} that can be used for storing subsequent records
   * @throws MissingParamException If any required parameters are missing
   * @throws DataSetException If a non-measurement record is supplied
   * @throws DatabaseException If a database error occurs
   * @throws NoSuchCategoryException If the record's Run Type is not recognised
   */
  public static PreparedStatement storeRecord(Connection conn, DataSetRawDataRecord record, PreparedStatement datasetDataStatement) throws MissingParamException, DataSetException, DatabaseException, NoSuchCategoryException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");

    if (!record.isMeasurement()) {
      throw new DataSetException("Record is not a measurement");
    }

    ResultSet createdKeys = null;

    try {
      if (null == datasetDataStatement) {
        datasetDataStatement = createInsertPositionRecordStatement(conn,
            record);
      }

      datasetDataStatement.setLong(1, record.getDatasetId());
      datasetDataStatement.setLong(2, DateTimeUtils.dateToLong(record.getDate()));
      datasetDataStatement.setDouble(3, record.getLongitude());
      datasetDataStatement.setDouble(4, record.getLatitude());
      datasetDataStatement.execute();

      CalculationDB calculationDB = CalculationDBFactory.getCalculationDB();

      createdKeys = datasetDataStatement.getGeneratedKeys();
      // A single key can be generated from this insert
      if (createdKeys.first()) {
        long dataset_data_id = createdKeys.getLong(1);
        storeIntakeRecord(conn, record, dataset_data_id);
        storeEquilibratorRecord(conn, record, dataset_data_id);
        calculationDB.createCalculationRecord(conn, dataset_data_id);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error storing dataset record", e);
    } finally {
      DatabaseUtils.closeResultSets(createdKeys);
    }

    return datasetDataStatement;
  }

  /**
   * Internal helper function to store data in the dataset_data_water_at_intake
   * - table.
   *
   * @param conn
   * @param record
   * @param dataset_data_id
   * @throws MissingParamException
   * @throws DataSetException
   * @throws DatabaseException
   * @throws NoSuchCategoryException
   */
  private static void storeIntakeRecord(Connection conn,
      DataSetRawDataRecord record, long dataset_data_id)
      throws MissingParamException, DataSetException, DatabaseException,
      NoSuchCategoryException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");

    if (!record.isMeasurement()) {
      throw new DataSetException("Record is not a measurement");
    }
    PreparedStatement intakeDataStatement = null;
    try {
      intakeDataStatement = createInsertIntakeRecordStatement(
          conn, record);

      intakeDataStatement.setLong(1, dataset_data_id);
      Double sensorValue = record.getSensorValue("Intake Temperature");
      if (null == sensorValue) {
        intakeDataStatement.setNull(2, Types.DOUBLE);
      } else {
        intakeDataStatement.setDouble(2, sensorValue);
      }

      intakeDataStatement.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing dataset record", e);
    } finally {
      DatabaseUtils.closeStatements(intakeDataStatement);
    }
  }

  /**
   * Internal helper function to store data in the
   * dataset_data_water_at_equilibrator - table.
   *
   * @param conn
   * @param record
   * @param dataset_data_id
   * @throws MissingParamException
   * @throws DataSetException
   * @throws DatabaseException
   * @throws NoSuchCategoryException
   */
  private static void storeEquilibratorRecord(Connection conn,
      DataSetRawDataRecord record, long dataset_data_id)
      throws MissingParamException, DataSetException, DatabaseException,
      NoSuchCategoryException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");
    if (!record.isMeasurement()) {
      throw new DataSetException("Record is not a measurement");
    }
    PreparedStatement equilibratorDataStatement = null;
    try {
      equilibratorDataStatement = createInsertEquilibratorRecordStatement(
          conn, record);
      int currentField = 1;
      equilibratorDataStatement.setLong(currentField++, dataset_data_id);
      equilibratorDataStatement.setLong(currentField++, dataset_data_id);
      equilibratorDataStatement.setString(currentField++, record.getRunType());
      equilibratorDataStatement.setString(currentField++,
          record.getDiagnosticValuesString());

      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
          .getSensorsConfiguration();
      for (SensorType sensorType : sensorConfig.getSensorTypes()) {
        if (sensorType.isUsedInCalculation()
            && !"Intake Temperature".equals(sensorType.getName())) {
          Double sensorValue = record.getSensorValue(sensorType.getName());
          if (null == sensorValue) {
            equilibratorDataStatement.setNull(currentField, Types.DOUBLE);
          } else {
            equilibratorDataStatement.setDouble(currentField, sensorValue);
          }
          currentField++;
        }
      }

      equilibratorDataStatement.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing dataset record", e);
    } finally {
      DatabaseUtils.closeStatements(equilibratorDataStatement);
    }
  }

  /**
   * Get a single measurement from the database
   * @param conn A database connection
   * @param dataSet The data set to which the measurement belongs
   * @param measurementId The measurement's database ID
   * @return The record
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the measurement does not exist
   */
  public static DataSetRawDataRecord getMeasurement(Connection conn, DataSet dataSet, long measurementId) throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    DataSetRawDataRecord result = null;
    Map<String, Integer> baseColumns = new HashMap<String, Integer>();
    Map<Integer, String> sensorColumns = new HashMap<Integer, String>();

    try {
      stmt = conn.prepareStatement(GET_MEASUREMENT_QUERY);
      stmt.setLong(1, measurementId);
      records = stmt.executeQuery();

      if (!records.next()) {
        throw new RecordNotFoundException("Measurement data not found", "dataset_data", measurementId);
      } else {
        result = getRecordFromResultSet(conn, dataSet, records, baseColumns, sensorColumns);
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurement data", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }


  /**
   * Get measurement records for a dataset
   * @param dataSource A data source
   * @param dataSet The data set
   * @param start The first record to retrieve
   * @param length The number of records to retrieve
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(DataSource dataSource, DataSet dataSet, int start, int length) throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      return getMeasurements(conn, dataSet, start, length);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement IDs", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }


  /**
   * Get all measurement records for a dataset
   * @param conn A database connection
   * @param dataSet The data set
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(Connection conn, DataSet dataSet) throws DatabaseException, MissingParamException {
    return getMeasurements(conn, dataSet, -1, -1);
  }

  /**
   * Get all measurement records for a dataset
   * @param conn A database connection
   * @param dataSet The data set
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(DataSource dataSource, DataSet dataSet) throws DatabaseException, MissingParamException {
    return getMeasurements(dataSource, dataSet, -1, -1);
  }

  /**
   * Get measurement records for a dataset
   * @param conn A database connection
   * @param dataSet The data set
   * @param start The first record to retrieve
   * @param length The number of records to retrieve
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(Connection conn, DataSet dataSet, int start, int length) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataSet, "dataSet");

    PreparedStatement stmt = null;
    ResultSet records = null;

    List<DataSetRawDataRecord> result = new ArrayList<DataSetRawDataRecord>();

    Map<String, Integer> baseColumns = new HashMap<String, Integer>();
    Map<Integer, String> sensorColumns = new HashMap<Integer, String>();

    try {
      StringBuilder query = new StringBuilder(GET_ALL_MEASUREMENTS_QUERY);
      if (length > 0) {
        query.append(" LIMIT ");
        query.append(start);
        query.append(',');
        query.append(length);
      }

      stmt = conn.prepareStatement(query.toString());
      stmt.setLong(1, dataSet.getId());

      records = stmt.executeQuery();

      while (records.next()) {
        result.add(getRecordFromResultSet(conn, dataSet, records, baseColumns, sensorColumns));
      }

      return result;

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurements", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Read a record from a ResultSet
   * @param dataSet The data set to which the record belongs
   * @param records The result set
   * @param baseColumns The column indices for the base columns
   * @param sensorColumns The column indices for the sensor columns
   * @return The record
     * @throws MissingParamException If any required parameters are missing
     * @throws SQLException If the record details cannot be extracted
   * @throws DataSetException If the diagnostic values cannot be read
   * @throws InstrumentException If the instrument details cannot be retrieved
   * @throws RecordNotFoundException If the instrument does not exist
   * @throws DatabaseException If a database error occurs
     *
   */
  private static DataSetRawDataRecord getRecordFromResultSet(Connection conn, DataSet dataSet, ResultSet records, Map<String, Integer> baseColumns, Map<Integer, String> sensorColumns) throws MissingParamException, SQLException, DataSetException, DatabaseException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(records, "records");
    MissingParam.checkMissing(baseColumns, "baseColumns", true);
    MissingParam.checkMissing(sensorColumns, "sensorColumns", true);

    DataSetRawDataRecord result = null;

    // Get the column indices if we haven't already got them
    if (baseColumns.size() == 0) {
      ResultSetMetaData rsmd = records.getMetaData();
      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
      SensorAssignments sensorAssignments = instrument.getSensorAssignments();
      calculateColumnIndices(rsmd, sensorAssignments, baseColumns, sensorColumns);
    }

    long id = records.getLong(baseColumns.get(ID_COL));
    LocalDateTime date = DateTimeUtils.longToDate(records.getLong(baseColumns.get(DATE_COL)));
    double longitude = records.getDouble(baseColumns.get(LON_COL));
    double latitude = records.getDouble(baseColumns.get(LAT_COL));
    String runType = records.getString(baseColumns.get(RUN_TYPE_COL));
    RunTypeCategory runTypeCategory = ResourceManager.getInstance().getRunTypeCategoryConfiguration().getCategory(runType);
    String diagnosticValues = records.getString(baseColumns.get(DIAGNOSTIC_COL));

    result = new DataSetRawDataRecord(dataSet, id, date, longitude, latitude, runType, runTypeCategory);
    result.setDiagnosticValues(diagnosticValues);

    for (Map.Entry<Integer, String> entry : sensorColumns.entrySet()) {
      Double value = records.getDouble(entry.getKey());
      if (records.wasNull()) {
        value = null;
      }

      result.setSensorValue(entry.getValue(), value);
    }

    return result;
  }

  /**
   * Calculate all the required column indices for extracting a data set record
   * @param rsmd The metadata of the query that's been run
   * @param baseColumns The mapping of base columns
   * @param sensorColumns The mapping of sensor columns
   * @throws SQLException If the column details cannot be read
   */
  private static void calculateColumnIndices(ResultSetMetaData rsmd, SensorAssignments sensorAssignments, Map<String, Integer> baseColumns, Map<Integer, String> sensorColumns) throws SQLException {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();

    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
      String columnName = rsmd.getColumnName(i);
      switch (columnName) {
      case ID_COL: {
        baseColumns.put(ID_COL, i);
        break;
      }
      case DATE_COL: {
        baseColumns.put(DATE_COL, i);
        break;
      }
      case LON_COL: {
        baseColumns.put(LON_COL, i);
        break;
      }
      case LAT_COL: {
        baseColumns.put(LAT_COL, i);
        break;
      }
      case RUN_TYPE_COL: {
        baseColumns.put(RUN_TYPE_COL, i);
        break;
      }
      case DIAGNOSTIC_COL: {
        baseColumns.put(DIAGNOSTIC_COL, i);
        break;
      }
      case DATASET_COL: {
        // Do nothing
        break;
      }
      default: {
        // This is a sensor field. Get the sensor name from the sensors configuration
        for (SensorType sensorType : sensorConfig.getSensorTypes()) {
          if (sensorType.isUsedInCalculation() && sensorAssignments.get(sensorType).size() > 0) {
            if (columnName.equals(sensorType.getDatabaseFieldName())) {
              sensorColumns.put(i, sensorType.getName());
              break;
            }
          }
        }
      }
      }
    }
  }

  /**
   * Create a statement to insert a new dataset record in the database
   * @param conn A database connection
   * @param record A dataset record
   * @return The statement
   * @throws MissingParamException If any required parameters are missing
   * @throws SQLException If the statement cannot be created
   */
  private static PreparedStatement createInsertPositionRecordStatement(
      Connection conn,
      DataSetRawDataRecord record) throws MissingParamException, SQLException {

    List<String> fieldNames = new ArrayList<String>();

    fieldNames.add("dataset_id");
    fieldNames.add("date");
    fieldNames.add("longitude");
    fieldNames.add("latitude");
    return DatabaseUtils.createInsertStatement(conn, "dataset_data_positions",
        fieldNames, Statement.RETURN_GENERATED_KEYS);
  }

  /**
   * @param conn
   * @param record
   * @return
   * @throws MissingParamException
   * @throws SQLException
   */
  private static PreparedStatement createInsertIntakeRecordStatement(
      Connection conn, DataSetRawDataRecord record)
      throws MissingParamException, SQLException {

    List<String> fieldNames = new ArrayList<String>();

    fieldNames.add("dataset_data_positions_id");
    fieldNames.add("intake_temperature");
    return DatabaseUtils.createInsertStatement(conn,
        "dataset_data_water_at_intake", fieldNames,
        Statement.RETURN_GENERATED_KEYS);
  }

  /**
   * @param conn
   * @param record
   * @return
   * @throws MissingParamException
   * @throws SQLException
   */
  private static PreparedStatement createInsertEquilibratorRecordStatement(
      Connection conn, DataSetRawDataRecord record)
      throws MissingParamException, SQLException {

    List<String> fieldNames = new ArrayList<String>();

    fieldNames.add("dataset_data_positions_id");
    fieldNames.add("shifted_dataset_data_positions_id");
    fieldNames.add("run_type");
    fieldNames.add("diagnostic_values");

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();
    for (SensorType sensorType : sensorConfig.getSensorTypes()) {
      if (sensorType.isUsedInCalculation()
          && !"Intake Temperature".equals(sensorType.getName())) {
        fieldNames.add(sensorType.getDatabaseFieldName());
      }
    }

    return DatabaseUtils.createInsertStatement(conn,
        "dataset_data_water_at_equilibrator", fieldNames,
        Statement.RETURN_GENERATED_KEYS);
  }

  /**
   * Get the IDs of all the measurements for a given data set
   * @param dataSource A data source
   * @param datasetId The dataset ID
   * @return The measurement IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If no measurements are found
   */
  public static List<Long> getMeasurementIds(DataSource dataSource, long datasetId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      return getMeasurementIds(conn, datasetId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement IDs", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Get the IDs of all the measurements for a given data set
   * @param conn A database connection
   * @param datasetId The dataset ID
   * @return The measurement IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If no measurements are found
   */
  public static List<Long> getMeasurementIds(Connection conn, long datasetId) throws MissingParamException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    List<Long> ids = new ArrayList<Long>();

    try {
      stmt = conn.prepareStatement(GET_ALL_MEASUREMENT_IDS_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();

      while(records.next()) {
        ids.add(records.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement IDs", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    if (ids.size() == 0) {
      throw new RecordNotFoundException("No records found for dataset " + datasetId);
    }

    return ids;
  }

  /**
   * Get the list of columns names for a raw dataset record
   * @param dataSource A data source
   * @param dataSet The data set
   * @return The column names
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
     * @throws InstrumentException If the instrument details cannot be retrieved
     * @throws RecordNotFoundException If the instrument for the data set does not exist
   */
  public static List<String> getDatasetDataColumnNames(DataSource dataSource, DataSet dataSet) throws MissingParamException, DatabaseException, InstrumentException, RecordNotFoundException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");

    List<String> result = new ArrayList<String>();

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    Connection conn = null;
    ResultSet columns = null;

    try {
      conn = dataSource.getConnection();

      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      DatabaseMetaData metadata = conn.getMetaData();
      columns = metadata.getColumns(null, null, "dataset_data", null);

      while (columns.next()) {
        String columnName = columns.getString(4);

        switch (columnName) {
        case "id": {
          result.add("ID");
          break;
        }
        case "date": {
          result.add("Date");
          break;
        }
        case "longitude": {
          result.add("Longitude");
          break;
        }
        case "latitude": {
          result.add("Latitude");
          break;
        }
        case "dataset_id":
        case "run_type": {
          // Ignored
          break;
        }
        case "diagnostic_values": {
          // TODO Add these
          break;
        }
        default: {
          // Sensor value columns
          for (SensorType sensorType : sensorConfig.getSensorTypes()) {
            if (sensorAssignments.get(sensorType).size() > 0) {
              if (columnName.equals(sensorType.getDatabaseFieldName())) {
                result.add(sensorType.getName());
                break;
              }
            }
          }

          break;
        }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting column names", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  public static void populateVariableList(DataSource dataSource, DataSet dataSet, VariableList variables) throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");
    MissingParam.checkMissing(variables, "variables", true);

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    Connection conn = null;
    ResultSet columns = null;

    try {
      conn = dataSource.getConnection();

      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      DatabaseMetaData metadata = conn.getMetaData();
      columns = metadata.getColumns(null, null, "dataset_data", null);

      while (columns.next()) {
        String columnName = columns.getString(4);

        switch (columnName) {
        case "id": {
          // Ignored
          break;
        }
        case "date": {
          variables.addVariable("Date/Time", new Variable(Variable.TYPE_BASE, "Date/Time", "date"));
          break;
        }
        case "longitude": {
          variables.addVariable("Longitude", new Variable(Variable.TYPE_BASE, "Longitude", "longitude"));
          break;
        }
        case "latitude": {
          variables.addVariable("Latitude", new Variable(Variable.TYPE_BASE, "Latitude", "latitude"));
          break;
        }
        case "dataset_id":
        case "run_type": {
          // Ignored
          break;
        }
        case "diagnostic_values": {
          // TODO Add these
          break;
        }
        default: {
          // Sensor value columns
          for (SensorType sensorType : sensorConfig.getSensorTypes()) {
            if (sensorAssignments.get(sensorType).size() > 0) {
              if (columnName.equals(sensorType.getDatabaseFieldName())) {
                // TODO Eventually this will use the sensor name as the label, and the sensor type as the group
                variables.addVariable(sensorType.getName(), new Variable(Variable.TYPE_SENSOR, sensorType.getName(), columnName));
                break;
              }
            }
          }

          break;
        }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting column names", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Take a list of fields, and return those which come from the dataset data.
   * Any others will come from calculation data and will be left alone.
   * @param conn A database connection
   * @param dataSet The data set to which the fields belong
   * @param originalFields The list of fields
   * @return The fields that come from dataset data
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
     * @throws RecordNotFoundException If the dataset or its instrument do not exist
   * @throws InstrumentException If the instrument details cannot be retrieved
   */
  public static List<String> extractDatasetFields(Connection conn, DataSet dataSet, List<String> originalFields) throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException {
    List<String> datasetFields = new ArrayList<String>();

    ResourceManager resourceManager = ResourceManager.getInstance();
    SensorsConfiguration sensorConfig = resourceManager.getSensorsConfiguration();

    Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
    SensorAssignments sensorAssignments = instrument.getSensorAssignments();

    for (String originalField : originalFields) {

      switch (originalField) {
      case "id":
      case "date":
      case "longitude":
      case "latitude": {
        datasetFields.add(originalField);
        break;
      }
      case "diagnostic_values": {
        // TODO Handle diagnostic values somehow
        break;
      }
      default: {
        // Sensor value columns
        for (SensorType sensorType : sensorConfig.getSensorTypes()) {
          if (sensorAssignments.get(sensorType).size() > 0) {
            if (originalField.equals(sensorType.getDatabaseFieldName())) {
              // TODO Eventually this will use the sensor name as the label, and the sensor type as the group
              datasetFields.add(originalField);
              break;
            }
          }
        }

        break;
      }
      }
    }

    return datasetFields;
  }

  /**
   * Get the geographical bounds of a data set
   * @param dataSource A data source
   * @param dataset The dataset
   * @return The bounds
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<Double> getDataBounds(DataSource dataSource, DataSet dataset) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataset, "dataset");

    List<Double> result = new ArrayList<Double>(6);

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_BOUNDS_QUERY);
      stmt.setLong(1, dataset.getId());

      records = stmt.executeQuery();

      records.next();
      result.add(records.getDouble(1));
      result.add(records.getDouble(2));
      result.add(records.getDouble(3));
      result.add(records.getDouble(4));

      // Mid point
      result.add((records.getDouble(3) - records.getDouble(1)) / 2 + records.getDouble(1));
      result.add((records.getDouble(4) - records.getDouble(2)) / 2 + records.getDouble(2));

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting dataset bounds", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

}
