package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Defines an individual sensor type for an instrument
 * @author Steve Jones
 */
public class SensorType {

  /**
   * The name of the sensor type
   */
  private String name;

  /**
   * Specifies whether or not the sensor is required in the instrument.
   *
   * <p>
   *   Setting a {@link #requiredGroup} overrides the behaviour of this flag.
   * </p>
   */
  private boolean required;

  /**
   * Specifies whether or not sensors assigned to this sensor type
   * can be given names
   */
  private boolean named;

  /**
   * The name of the group of sensors to which this sensor belongs.
   *
   * <p>
   *   Some sensors can be defined together in groups, where at least
   *   one sensor in the group must be present. This names the group -
   *   any sensors with the same name will be in the same group.
   * </p>
   *
   * <p>
   *   Setting a value in the {@code requiredGroup} overrides the behaviour
   *   of the {@link #required} flag.
   * </p>
   */
  private String requiredGroup = null;

  /**
   * Specifies the name of another sensor that must also be present if
   * this sensor is present.
   *
   * <p>
   *   Some sensor values depend on the value of another sensor in the instrument
   *   for the necessary calculations to be performed. For example, a differential
   *   pressure sensor requires an absolute atmospheric pressure sensor in order for
   *   the true pressure to be calculated.
   * </p>
   */
  private String dependsOn = null;

  /**
   * Specifies a question to ask that determines whether the {@link #dependsOn} criterion
   * should be honoured. This question will yield a {@code boolean} result. If the result
   * is {@code true}, then the {@link #dependsOn} criterion will be enforced. If false, it will not.
   * If the question is empty or null, then the criterion will always be enforced.
   */
  private String dependsQuestion = null;

  /**
   * Indicates whether multiple instances of a sensor can be configured for an instrument
   */
  private boolean many = true;

  /**
   * Indicates whether multiple sensor values will be averaged
   */
  private boolean averaged = true;

  /**
   * Indicates whether or not sensors can be post-calibrated by QuinCe
   */
  private boolean postCalibrated = true;

  /**
   * Indicates whether or not this is a core sensor (if so, a Run Type must
   * be assigned from the same file).
   */
  private boolean coreSensor = false;

  /**
   * Indicates whether or not this sensor is used in calculations.
   * Other sensors are for diagnostic purposes only
   */
  private boolean usedInCalculation = true;

  /**
   * Indicates whether or not this is a diagnostic sensor
   */
  private boolean diagnostic = false;

  /**
   * Indicates whether or not this sensor's data is calibrated
   * using data
   */
  private boolean externalStandards = false;

  /**
   * Simple constructor - sets all values
   * @param name The name of the sensor type
   * @param required Whether or not the sensor type is required
   * @param named Whether or not sensors can be named
   * @param requiredGroup The Required Group that this sensor type belongs to
   * @param dependsOn The name of another sensor type that this sensor type depends on
   * @param dependsQuestion The question that determines whether the {@link #dependsOn} criterion will be honoured
   * @param many Whether or not multiple instances of the sensor are allowed
   * @param averaged Whether or not values from multiple sensors will be averaged
   * @param postCalibrated Whether or not values from the sensor need a calibration to be applied
   * @param coreSensor Whether or not this is a core sensor
   * @param usedInCaclulation Whether or not the sensor is used during data reduction
   * @param diagnostic Whether or not this is a diagnostic sensor
   * @param externalStandards Whether or not this sensor is calibrated using external standards
   */
  protected SensorType(String name, boolean required, boolean named, String requiredGroup, String dependsOn, String dependsQuestion, boolean many, boolean averaged,
      boolean postCalibrated, boolean coreSensor, boolean usedInCaclulation, boolean diagnostic, boolean externalStandards) {
    this.name = name;
    this.required = required;
    this.named = named;

    if (null != requiredGroup && requiredGroup.trim().length() > 0) {
      this.requiredGroup = requiredGroup;
    }
    if (null != dependsOn && dependsOn.trim().length() > 0) {
      this.dependsOn = dependsOn;
    }

    if (null != dependsQuestion && dependsQuestion.trim().length() > 0) {
      this.dependsQuestion = dependsQuestion;
    }

    this.many = many;
    this.averaged = averaged;
    this.postCalibrated = postCalibrated;
    this.coreSensor = coreSensor;
    this.usedInCalculation = usedInCaclulation;
    this.diagnostic = diagnostic;
    this.externalStandards = externalStandards;
  }

  /**
   * Get the name of this sensor type
   * @return The name of the sensor type
   */
  public String getName() {
    return name;
  }

  /**
   * Get the name of the sensor type that must exist in conjunction
   * with this sensor type
   * @return The name of the sensor type that this sensor type depends on
   */
  public String getDependsOn() {
    return dependsOn;
  }

  /**
   * Get the flag indicating whether or not this sensor
   * type is required.
   * @return The required flag
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * Determine whether or not sensors assigned to this type can be named
   * @return {@code true} if the sensor can be named; {@code false} otherwise
   */
  public boolean canBeNamed() {
    return named;
  }

  /**
   * Get the name of the Required Group
   * that this sensor type belongs to
   * @return The Required Group
   */
  public String getRequiredGroup() {
    return requiredGroup;
  }

  /**
   * Gets the question that determines whether the {@link #dependsOn}
   * criterion will be honoured.
   * @return The question
   * @see #dependsQuestion
   */
  public String getDependsQuestion() {
    return dependsQuestion;
  }

  /**
   * Determine whether or not multiple instances of this sensor are allowed.
   * @return {@code true} if multiple instances are allowed; {@code false} if only one instance is allowed
   */
  public boolean canHaveMany() {
    return many;
  }

  /**
   * Determine whether or not this is a diagnostic sensor
   * @return {@code true} if this is a diagnostic sensor; {@code false} if it is not.
   */
  public boolean isDiagnostic() {
    return diagnostic;
  }

  /**
   * Equality is based on the sensor name only
   */
  @Override
  public boolean equals(Object o) {
    boolean equal = false;

    if (null != o && o instanceof SensorType) {
      equal = ((SensorType) o).name.equalsIgnoreCase(this.name);
    }

    return equal;
  }

  /**
   * Determines whether or not this sensor type has a Depends Question
   * @return {@code true} if there is a Depends Question; {@code false} if there is not
   */
  public boolean hasDependsQuestion() {
    return (dependsQuestion != null);
  }

  /**
   * Determines whether or not multiple sensor values will be averaged
   * @return {@code true} if the values will be averaged; {@code false} if they will not
   */
  public boolean isAveraged() {
    return averaged;
  }

  /**
   * Determines whether sensors of this type can be post-calibrated by QuinCe
   * @return {@code true} if the sensors can be post-calibrated; {@code false} if they cannot
   */
  public boolean canBePostCalibrated() {
    return postCalibrated;
  }

  /**
   * Determines whether sensors of this type are Core sensors, meaning that a Run Type
   * must be present in the same file.
   * @return {@code true} if this is a core sensor; {@code false} if it is not
   */
  public boolean isCoreSensor() {
    return coreSensor;
  }

  /**
   * Determines whether or not sensors of this type are used in calculations,
   * or used only for diagnostic purposes
   * @return {@code true} if the sensors are used in calculations; {@code false} otherwise.
   */
  public boolean isUsedInCalculation() {
    return usedInCalculation;
  }

  /**
   * Get the database field name for this sensor type
   *
   * <p>
   *   The database field name is the sensor type's name
   *   converted to lower case and with spaces replaced by
   *   underscores. Brackets and other odd characters that
   *   upset MySQL are removed.
   * </p>
   *
   * <p>
   *   Sensors that are not used in calculations are not
   *   stored in conventional database fields. For those
   *   sensors, this method returns {@code null}.
   * </p>
   *
   * @return The database field name
   */
  public String getDatabaseFieldName() {
    String result = null;
    if (usedInCalculation) {
      result = DatabaseUtils.getDatabaseFieldName(name);
    }

    return result;
  }

  /**
   * Determine whether or not this sensor is calibrated using external standards
   * @return {@code true} if the sensor is calibrated from external standards; {@code false} if it is not.
   */
  public boolean hasExternalStandards() {
    return externalStandards;
  }

  /**
   * Get a sensor type for the Run Type
   * @return the SensorType
   */
  public static SensorType getRunTypeSensorType() {
    return new SensorType("Run Type", true, false, null, null, null, false,
        false, false, false, false, false, false);
  }

  /**
   * Create a diagnostic sensor type with the given name.
   * The name automatically has 'Diagnostic' prepended to it.
   * @param name The sensor type name
   * @return The diagnostic sensor type
   */
  protected static SensorType makeDiagnosticSensorType(String name) {
    return new SensorType("Diagnostic: " + name, false, true, null, null, null, true, false,
        false, false, false, true, false);
  }
}
