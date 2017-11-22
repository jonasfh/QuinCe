package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.Properties;

import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Object to represent a data set
 * @author Steve Jones
 *
 */
public class DataSet {

	/**
	 * The numeric value for the data extraction status
	 */
	public static final int STATUS_WAITING = 0;
	
	/**
	 * The string for the data extraction status
	 */
	public static final String STATUS_WAITING_NAME = "Waiting";
	
	/**
	 * The numeric value for the data extraction status
	 */
	public static final int STATUS_DATA_EXTRACTION = 1;
	
	/**
	 * The string for the data extraction status
	 */
	public static final String STATUS_DATA_EXTRACTION_NAME = "Data extraction";
	
	/**
	 * The numeric value for the data extraction status
	 */
	public static final int STATUS_WAITING_FOR_CALCULATION = 2;
	
	/**
	 * The string for the data extraction status
	 */
	public static final String STATUS_WAITING_FOR_CALCULATION_NAME = "Waiting for calculation";
	
	/**
	 * The database ID
	 */
	private long id = DatabaseUtils.NO_DATABASE_RECORD;
	
	/**
	 * The database ID of the instrument to which this data set belongs
	 */
	private long instrumentId;
	
	/**
	 * The data set name
	 */
	private String name;
	
	/**
	 * The start date of the data set
	 */
	private LocalDateTime start;
	
	/**
	 * The end date of the data set
	 */
	private LocalDateTime end;
	
	/**
	 * Additional properties of the data set
	 */
	private Properties properties;
	
	/**
	 * The time when the data file was last touched
	 */
	private LocalDateTime lastTouched;
	
	/**
	 * The data set's status
	 */
	private int status = STATUS_WAITING;
	
	/**
	 * Constructor for all fields
	 * @param id The data set's database ID
	 * @param instrumentId The database ID of the instrument to which the data set belongs
	 * @param name The name
	 * @param start The start date
	 * @param end The end date
	 * @param status The current status
	 * @param properties The additional properties
	 * @param lastTouched The date that the data set was last accessed
	 */
	protected DataSet(long id, long instrumentId, String name, LocalDateTime start, LocalDateTime end, int status, Properties properties, LocalDateTime lastTouched) {
		this.id = id;
		this.instrumentId = instrumentId;
		this.name = name;
		this.start = start;
		this.end = end;
		this.status = status;
		this.properties = properties;
		this.lastTouched = lastTouched;
	}
	
	/**
	 * Constructor for a new, empty data set
	 * @param instrumentId The database ID of the instrument to which the data set belongs
	 */
	public DataSet(long instrumentId) {
		this.instrumentId = instrumentId;
	}
	
	/**
	 * Get the data set's status
	 * @return The status
	 */
	public int getStatus() {
		return status;
	}
	
	/**
	 * Get the human-readable status of the data set
	 * @return The status
	 */
	public String getStatusName() {
		return getStatusName(status);
	}
	
	/**
	 * Get the human-readable name of a given data set status
	 * @param statusValue The status value
	 * @return The status name
	 */
	public static String getStatusName(int statusValue) {
		String result;
		
		switch (statusValue) {
		case STATUS_WAITING: {
			result =STATUS_WAITING_NAME;
			break;
		}
		case STATUS_DATA_EXTRACTION: {
			result = STATUS_DATA_EXTRACTION_NAME;
			break;
		}
		case STATUS_WAITING_FOR_CALCULATION: {
			result = STATUS_WAITING_FOR_CALCULATION_NAME;
			break;
		}
		default: {
			result = "UNKNOWN";
		}
		}
		
		return result;
	}

	/**
	 * Get the data set's database ID
	 * @return The database ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Set the data set's database ID
	 * @param id The database ID
	 */
	protected void setId(long id) {
		this.id = id;
	}

	/**
	 * Get the database ID of the instrument to which this data set belongs
	 * @return The instrument's database ID
	 */
	public long getInstrumentId() {
		return instrumentId;
	}

	/**
	 * Get the name of the data set
	 * @return The data set name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the data set
	 * @param name The data set name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the start date of the data set
	 * @return The start date
	 */
	public LocalDateTime getStart() {
		return start;
	}

	/**
	 * Set the start date of the data set
	 * @param start The start date
	 */
	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	/**
	 * Get the end date of the data set
	 * @return The end date
	 */
	public LocalDateTime getEnd() {
		return end;
	}

	/**
	 * Set the end date of the data set
	 * @param end The end date
	 */
	public void setEnd(LocalDateTime end) {
		this.end = end;
	}

	/**
	 * Get the date that the data set was last accessed
	 * @return The last access date
	 */
	public LocalDateTime getLastTouched() {
		return lastTouched;
	}

	/**
	 * Set the last access date of the data set to the current time
	 */
	public void touch() {
		this.lastTouched = DateTimeUtils.longToDate(System.currentTimeMillis());
	}

	/**
	 * Set the data set's status
	 * @param status The status
	 * @throws InvalidDataSetStatusException If the status is invalid
	 */
	protected void setStatus(int status) throws InvalidDataSetStatusException {
		if (!validateStatus(status)) {
			throw new InvalidDataSetStatusException(status);
		}
		
		this.status = status;
	}
	
	/**
	 * Set a property on the data set.
	 * @param key The key
	 * @param value The value
	 * @see Properties#setProperty(String, String)
	 */
	public void setProperty(String key, String value) {
		if (null == properties) {
			properties = new Properties();
		}
		
		properties.setProperty(key, value);
	}
	
	/**
	 * Get a property from the data set
	 * @param key The key
	 * @return The value
	 * @see Properties#getProperty(String)
	 */
	public String getProperty(String key) {
		String result = null;
		
		if (null != properties) {
			result = properties.getProperty(key);
		}
		
		return result;
	}
	
	/**
	 * Determine whether or not a given status value is valid
	 * @param status The status to be checked
	 * @return {@code true} if the status is valid; {@code false} if it is not
	 */
	public static boolean validateStatus(int status) {
		return (status == STATUS_WAITING || status == STATUS_DATA_EXTRACTION || status == STATUS_WAITING_FOR_CALCULATION);
	}
}
