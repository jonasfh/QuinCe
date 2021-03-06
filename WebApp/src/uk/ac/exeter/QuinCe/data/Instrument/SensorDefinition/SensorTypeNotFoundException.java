package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;

/**
 * Exception for named sensor types that can't be found
 * @author Steve Jones
 *
 */
public class SensorTypeNotFoundException extends InstrumentException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 7707562190864340444L;

  /**
   * Constructor
   * @param sensorName The sensor name
   */
  public SensorTypeNotFoundException(String sensorName) {
    super("The sensor type with name '" + sensorName + "' does not exist");
  }

}
