package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.FileUtils;
import uk.ac.exeter.QuinCe.utils.StringFormatException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 *
 * @author Steve Jones
 *
 */
public class SensorsConfiguration {

  /**
   * The number of columns in the configuration file
   */
  private static final int COL_COUNT = 12;

  /**
   * The column containing the sensor type name
   */
  private static final int COL_NAME = 0;

  /**
   * The column defining whether or not the sensor type is required
   */
  private static final int COL_REQUIRED = 1;

  /**
   * The column that indicates whether a sensor can be named
   */
  private static final int COL_NAMED = 2;

  /**
   * The column containing the Required Group for the sensor
   */
  private static final int COL_REQUIRED_GROUP = 3;

  /**
   * The column naming another sensor type that this sensor relies on
   */
  private static final int COL_DEPENDS_ON = 4;

  /**
   * The column specifying whether or not multiple sensors of this type are permitted
   */
  private static final int COL_DEPENDS_QUESTION = 5;

  /**
   * The column specifying whether multiple instances of the sensor are allowed
   */
  private static final int COL_MANY = 6;

  /**
   * The column specifying whether multiple sensors will be averaged
   */
  private static final int COL_AVERAGED = 7;

  /**
   * The column specifying whether a sensor can be post-calibrated by QuinCe
   */
  private static final int COL_POST_CALIBRATED = 8;

  /**
   * The column specifying whether a sensor is a core sensor, and thus
   * requires a Run Type column to be specified in the file
   */
  private static final int COL_CORE = 9;

  /**
   * The column specifying whether a sensor is a core sensor, and thus
   * requires a Run Type column to be specified in the file
   */
  private static final int COL_USED_IN_CALCULATION = 10;

  /**
   * The column specifying whether a sensor's values are calibrated
   * using data
   */
  private static final int COL_EXTERNAL_STANDARDS = 11;

  /**
   * The set of sensors defined for the instrument with
   * the data file columns assigned to them
   */
  private List<SensorType> sensorTypes;

  /**
   * Create an empty sensor configuration (with no assigned columns)
   * based on the specified configuration file
   * @param configFile The configuration file
   * @param diagnosticSensors The names of the diagnostic sensor types that can be used
   * @throws SensorConfigurationException If the configuration is invalid
   */
  public SensorsConfiguration(File configFile, List<String> diagnosticSensors) throws SensorConfigurationException {

    if (!FileUtils.canAccessFile(configFile)) {
      throw new SensorConfigurationException("Cannot access config file '" + configFile.getAbsolutePath() + "'");
    }

    buildSensorTypes(configFile, diagnosticSensors);
  }

  /**
   * Get the list of sensor types in this configuration
   * @return The sensor types
   */
  public List<SensorType> getSensorTypes() {
    return Collections.unmodifiableList(sensorTypes);
  }

  /**
   * Get an empty map of sensor types ready to have columns assigned
   * @return An empty sensor types/assignments map
   */
  public SensorAssignments getNewSensorAssigments() {
    return new SensorAssignments(sensorTypes);
  }

  /**
   * Build the map of sensor configurations from the supplied
   * configuration file. All map entries will contain {@code null}
   * to indicate that no assignments have been made.
   *
   * The diagnostic sensors will be added to the list automatically.
   *
   * @param configFile The configuration file
   * @param diagnosticSensors The names of the diagnostic sensor types that can be used
   * @throws SensorConfigurationException If the configuration is invalid
   */
  private void buildSensorTypes(File configFile, List<String> diagnosticSensors) throws SensorConfigurationException {

    sensorTypes = new ArrayList<SensorType>();
    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new FileReader(configFile));
      String line = reader.readLine();
      int lineCount = 1;

      while (null != line) {
        if (!StringUtils.isComment(line)) {
          List<String> fields = StringUtils.trimList(Arrays.asList(line.split(",", -1)));

          if (fields.size() != COL_COUNT) {
            throw new SensorConfigurationException(lineCount, "Incorrect number of columns");
          } else {
            try {
              String sensorName = fields.get(COL_NAME);
              if (sensorTypeDefined(sensorName)) {
                throw new SensorConfigurationException(lineCount, "Sensor name '" + sensorName + "' is already defined");
              }

              boolean required = StringUtils.parseYNBoolean(fields.get(COL_REQUIRED));
              boolean named = StringUtils.parseYNBoolean(fields.get(COL_NAMED));

              String requiredGroup = fields.get(COL_REQUIRED_GROUP);
              if (requiredGroup.length() == 0) {
                requiredGroup = null;
              }

              String dependsOn = fields.get(COL_DEPENDS_ON);
              if (dependsOn.length() == 0) {
                dependsOn = null;
              }

              String dependsQuestion = fields.get(COL_DEPENDS_QUESTION);

              boolean many = StringUtils.parseYNBoolean(fields.get(COL_MANY));
              boolean averaged = StringUtils.parseYNBoolean(fields.get(COL_AVERAGED));
              boolean postCalibrated = StringUtils.parseYNBoolean(fields.get(COL_POST_CALIBRATED));
              boolean coreSensor = StringUtils.parseYNBoolean(fields.get(COL_CORE));
              boolean usedInCalculation = StringUtils.parseYNBoolean(fields.get(COL_USED_IN_CALCULATION));
              boolean externalStandards = StringUtils.parseYNBoolean(fields.get(COL_EXTERNAL_STANDARDS));

              SensorType sensor = new SensorType(sensorName, required, named, requiredGroup, dependsOn, dependsQuestion, many, averaged, postCalibrated, coreSensor, usedInCalculation, false, externalStandards);

              sensorTypes.add(sensor);

            } catch (StringFormatException e) {
              throw new SensorConfigurationException(lineCount, e.getMessage());
            }
          }
        }

        line = reader.readLine();
        lineCount++;
      }

      checkDependsOnConfiguration();

      // Add the diagnostic sensor types
      for (String diagnosticSensor : diagnosticSensors) {
        sensorTypes.add(SensorType.makeDiagnosticSensorType(diagnosticSensor));
      }

    } catch (IOException e) {
      throw new SensorConfigurationException("Error while reading config file", e);
    } finally {
      if (null != reader) {
        try {
          reader.close();
        } catch (IOException e) {
          // We tried.
        }
      }
    }
  }

  /**
   * Check the sensor types to ensure that all "Depends On" configurations reference
   * sensor types that exist
   * @throws SensorConfigurationException If a "Depends On" references a sensor type that doesn't exist
   */
  private void checkDependsOnConfiguration() throws SensorConfigurationException {
    for (SensorType type: sensorTypes) {
      String dependsOn = type.getDependsOn();
      if (null != dependsOn && !sensorTypeDefined(dependsOn)) {
        throw new SensorConfigurationException("Sensor type '" + type.getName() + "' depends on '" + dependsOn + "', but that sensor type is not configured");
      }
    }
  }

  /**
   * Determine whether or not a sensor with a given name has
   * already been added to the map. The comparison is case insensitive.
   * @param name The sensor name
   * @return {@code true} if the sensor name already exists in the map; {@code false} if it does not.
   */
  private boolean sensorTypeDefined(String name) {
    boolean foundSensor = false;

    for (SensorType sensorType : sensorTypes) {
      if (sensorType.getName().equalsIgnoreCase(name)) {
        foundSensor = true;
        break;
      }
    }

    return foundSensor;
  }

  /**
   * Check a list of sensor names to ensure they are all present in the sensor configuration.
   * Note that this checks both sensor names and Required Group names
   * @param names The names to check
   * @throws SensorConfigurationException If any sensor names are not recognised
   */
  public void validateSensorNames(List<String> names) throws SensorConfigurationException {
    for (String name : names) {
      boolean found = false;

      for (SensorType sensorType : sensorTypes) {
        if (sensorType.getName().equalsIgnoreCase(name) ||
            (null != sensorType.getRequiredGroup() && sensorType.getRequiredGroup().equalsIgnoreCase(name))) {
          found = true;
          break;
        }
      }

      if (!found) {
        throw new SensorConfigurationException("Unrecognised sensor type '" + name + "'");
      }
    }
  }
}
