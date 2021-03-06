package uk.ac.exeter.QuinCe.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QCRoutines.messages.Flag;

/**
 * Miscellaneous string utilities
 * @author Steve Jones
 *
 */
public final class StringUtils {

  /**
   * Private constructor to prevent instantiation
   */
  private StringUtils() {
    // Do nothing
  }

  /**
   * Converts a collection of values to a single string,
   * with a semi-colon delimiter.
   *
   * <b>Note that this does not handle semi-colons within the values themselves.</b>
   *
   * @param list The list to be converted
   * @return The converted list
   */
  public static String collectionToDelimited(Collection<?> list) {
    return collectionToDelimited(list, ";", null);
  }

  /**
   * Converts a collection of values to a single string,
   * with a specified delimiter.
   *
   * <b>Note that this does not handle the case where the delimiter is found within the values themselves.</b>
   *
   * @param list The list to be converted
   * @param delimiter The delimiter to use
   * @return The converted list
   */
  public static String collectionToDelimited(Collection<?> list, String delimiter) {
    return collectionToDelimited(list, delimiter, null);
  }

  /**
   * Convert a collection of objects to a delimited string
   * @param collection The list
   * @param delimiter The delimiter
   * @param surrounder The character to put at the start and end of each entry
   * @return The delimited string
   */
  public static String collectionToDelimited(Collection<?> collection, String delimiter, String surrounder) {

    String result = null;

    if (null != collection) {
      StringBuilder buildResult = new StringBuilder();

      Iterator<?> i = collection.iterator();
      int counter = 0;
      while (i.hasNext()) {
        Object item = i.next();
        counter++;

        if (null != surrounder) {
          buildResult.append(surrounder);
          buildResult.append(item.toString().replace(surrounder, "\\" + surrounder));
          buildResult.append(surrounder);
        } else {
          buildResult.append(item.toString());
        }

        if (counter < (collection.size())) {
          buildResult.append(delimiter);
        }
      }
      result = buildResult.toString();
    }

    return result;
  }

  /**
   * Converts a String containing values separated by semi-colon delimiters
   * into a list of String values
   *
   * <b>Note that this does not handle semi-colons within the values themselves.</b>
   *
   * @param values The String to be converted
   * @return A list of String values
   */
  public static List<String> delimitedToList(String values) {
    return delimitedToList(values, ";");
  }

  /**
   * Converts a String containing values separated by semi-colon delimiters
   * into a list of String values
   *
   * <b>Note that this does not handle semi-colons within the values themselves.</b>
   *
   * @param values The String to be converted
   * @param delimiter The delimiter
   * @return A list of String values
   */
  public static List<String> delimitedToList(String values, String delimiter) {
    List<String> result = null;

    if (null != values) {
      if (values.length() == 0) {
        result = new ArrayList<String>();
      } else {
        result = Arrays.asList(values.split(delimiter, 0));
      }
    }

    return result;
  }

  /**
   * Convert a delimited list of integers into a list of integers
   * @param values The list
   * @return The list as integers
   */
  public static List<Integer> delimitedToIntegerList(String values) {
    return delimitedToIntegerList(values, ";");
  }

    /**
     * Convert a delimited list of integers into a list of integers
     * @param values The list
     * @param delimiter The delimiter
     * @return The list as integers
     */
    public static List<Integer> delimitedToIntegerList(String values, String delimiter) {

    List<Integer> result = null;

    if (values != null) {
      result = new ArrayList<Integer>();

      for (String item : delimitedToList(values, delimiter)) {
        result.add(Integer.parseInt(item));
      }
    }

    return result;
  }

  public static List<Double> delimitedToDoubleList(String values) {
    return delimitedToDoubleList(values, ";");
  }

  /**
   * Convert a delimited list of double into a list of doubles
   * @param values The list
   * @return The list as integers
   */
  public static List<Double> delimitedToDoubleList(String values, String delimiter) {

    List<Double> result = null;

    if (values != null) {
      List<String> stringList = delimitedToList(values, delimiter);
      result = new ArrayList<Double>(stringList.size());

      for (String item: stringList) {
        result.add(Double.parseDouble(item));
      }
    }

    return result;
  }

  /**
   * Convert a comma-separated list of numbers to a list of longs
   * @param values The numbers
   * @return The longs
   */
  public static List<Long> delimitedToLongList(String values) {
    // TODO This is the preferred way of doing this. Make the other methods do the same.

    List<Long> result = null;

    if (values != null) {
      String[] numberList = values.split(",");
      result = new ArrayList<Long>(numberList.length);

      for (String number : numberList) {
        result.add(Long.parseLong(number));
      }
    }

    return result;
  }

  /**
   * Extract the stack trace from an Exception (or other
   * Throwable) as a String.
   * @param e The error
   * @return The stack trace
   */
  public static String stackTraceToString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Determines whether or not a line is a comment, signified by it starting with {@code #} or {@code !} or {@code //}
   * @param line The line to be checked
   * @return {@code true} if the line is a comment; {@code false} otherwise.
   */
  public static boolean isComment(String line) {
    String trimmedLine = line.trim();
    return trimmedLine.length() == 0 || trimmedLine.charAt(0) == '#' || trimmedLine.charAt(0) == '!' || trimmedLine.startsWith("//", 0);
  }

  /**
   * Trims all items in a list of strings. A string that starts with a
   * single backslash has that backslash removed.
   * @param source The strings to be converted
   * @return The converted strings
   */
  public static List<String> trimList(List<String> source) {

    List<String> result = new ArrayList<String>(source.size());

    for (int i = 0; i < source.size(); i++) {
      String trimmedValue = source.get(i).trim();
      if (trimmedValue.startsWith("\\")) {
        trimmedValue = trimmedValue.substring(1);
      }

      result.add(trimmedValue);
    }

    return result;
  }

  /**
   * Determine whether or not a String contains a numeric value
   * @param value The String
   * @return {@code true} if the String contains a number; {@code false} if it does not
   */
  public static boolean isNumeric(String value) {
    boolean result = true;

    if (null == value) {
      result = false;
    } else {
      try {
        Double doubleValue = new Double(value);
        if (doubleValue.isNaN()) {
          result = false;
        }
      } catch (NumberFormatException e) {
        result = false;
      }
    }

    return result;
  }

  /**
   * Determine whether or not a String contains an integer value
   * @param value The String
   * @return {@code true} if the String contains an integer; {@code false} if it does not
   */
  public static boolean isInteger(String value) {
    boolean result = true;

    if (null == value) {
      result = false;
    } else {
      try {
        new Integer(value);
      } catch (NumberFormatException e) {
        result = false;
      }
    }

    return result;
  }

  /**
   * Convert a String-to-String lookup map into a String.
   * <p>
   *   Each map entry is converted to a {@code key=value} pair.
   *   Each entry is separated by a semi-colon.
   * </p>
   * <p>
   *   <b>Note:</b> There is no handling of {@code =} or {@code ;}
   *   in the keys or values.
   * </p>
   * @param map The Map to be converted
   * @return The String representation of the Map
   */
  public static String mapToDelimited(Map<String, String> map) {

    StringBuilder result = new StringBuilder();

    int counter = 0;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      counter++;
      result.append(entry.getKey());
      result.append('=');
      result.append(entry.getValue());

      if (counter < map.size()) {
        result.append(';');
      }
    }

    return result.toString();
  }

  /**
   * Convert a semi-colon-delimited list of {@code key=value} pairs
   * into a Map.
   * @param values The String
   * @return The Map
   * @throws StringFormatException If the String is not formatted correctly
   */
  public static Map<String,String> delimitedToMap(String values) throws StringFormatException {

    Map<String, String> result = new HashMap<String, String>();

    for (String entry : values.split(";", 0)) {

      String[] entrySplit = entry.split("=", 0);
      if (entrySplit.length != 2) {
        throw new StringFormatException("Invalid map format", entry);
      } else {
        result.put(entrySplit[0], entrySplit[1]);
      }
    }

    return result;
  }

  /**
   * Convert a case-insensitive Y/N value to a boolean
   * @param value The value
   * @return The boolean value
   * @throws StringFormatException If the supplied value is not Y or N
   */
  public static boolean parseYNBoolean(String value) throws StringFormatException {
    boolean result;

    switch(value.toUpperCase()) {
    case "Y": {
      result = true;
      break;
    }
    case "N": {
      result = false;
      break;
    }
    default: {
      throw new StringFormatException("Invalid boolean value", value);
    }
    }

    return result;
  }

  /**
   * Convert a Properties object into a JSON string
   * @param properties The properties
   * @return The JSON string
   */
  public static String getPropertiesAsJson(Properties properties) {


    StringBuilder result = new StringBuilder();
    if (null == properties) {
      result.append("null");
    } else {

      result.append('{');

      int propCount = 0;
      for (String prop : properties.stringPropertyNames()) {
        propCount++;
        result.append('"');
        result.append(prop);
        result.append("\":\"");
        result.append(properties.getProperty(prop));
        result.append('"');

        if (propCount < properties.size()) {
          result.append(',');
        }
      }


      result.append('}');
    }

    return result.toString();
  }

  /**
   * Create a {@link Properties} object from a string
   * @param propsString The properties String
   * @return The Properties object
   * @throws IOException If the string cannot be parsed
   */
  public static Properties propertiesFromString(String propsString) throws IOException {
    Properties result = null;

    if (null != propsString && propsString.length() > 0) {
      StringReader reader = new StringReader(propsString);
      Properties props = new Properties();
      props.load(reader);
      return props;
    }

    return result;
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, String value) {
    return makeJsonField(fieldNumber, value, true);
  }

  /**
   * Make a JSON field value from a flag, using the flag's integer value
   * @param fieldNumber The field number
   * @param flag The flag
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, Flag flag) {
    return makeJsonField(fieldNumber, flag.getFlagValue());
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @param asString Indicates whether or not the value should be represented as a String
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, double value, boolean asString) {
    return makeJsonField(fieldNumber, value, asString, -1);
  }

  /**
   * Create a JSON field value formatted with a given number of decimal places
   * @param fieldNumber The field number
   * @param value The field value
   * @param asString Indicates whether or not the value should be represented as a String
   * @param decimalPlaces The number of decimal places
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, double value, boolean asString, int decimalPlaces) {
    String stringValue;

    if (decimalPlaces > -1) {
      stringValue = String.format(Locale.ENGLISH, "%.0" + decimalPlaces + "f", value);
    } else {
      stringValue = String.valueOf(value);
    }

    return makeJsonField(fieldNumber, stringValue, asString);
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @param asString Indicates whether or not the value should be represented as a String
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, boolean value, boolean asString) {
    return makeJsonField(fieldNumber, String.valueOf(value), asString);
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, double value) {
    return makeJsonField(fieldNumber, value, false, -1);
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @param decimalPlaces The number of decimal places
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, double value, int decimalPlaces) {
    return makeJsonField(fieldNumber, value, false, decimalPlaces);
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, boolean value) {
    return makeJsonField(fieldNumber, String.valueOf(value), false);
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @param asString Indicates whether or not the value should be represented as a String
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, long value, boolean asString) {
    return makeJsonField(fieldNumber, String.valueOf(value), asString);
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, long value) {
    return makeJsonField(fieldNumber, String.valueOf(value), false);
  }

  /**
   * Create a JSON field value
   * @param fieldNumber The field number
   * @param value The field value
   * @param asString Indicates whether or not the value should be represented as a String
   * @return The field string
   */
  public static String makeJsonField(int fieldNumber, String value, boolean asString) {
    return makeJsonField(String.valueOf(fieldNumber), value, asString);
  }

  /**
   * Create a JSON field value
   * @param fieldName The field name
   * @param value The field value
   * @param asString Indicates whether or not the value should be represented as a String
   * @return The field string
   */
  public static String makeJsonField(String fieldName, String value, boolean asString) {

    StringBuilder field = new StringBuilder();

    field.append("\"");
    field.append(fieldName);
    field.append("\":");

    if (asString) {
      field.append("\"");
    }

    field.append(value);

    if (asString) {
      field.append("\"");
    }

    return field.toString();
  }

  /**
   * Create a JSON field with a {@code null} value
   * @param fieldNumber The field number
   * @return The JSON field
   */
  public static String makeJsonNull(int fieldNumber) {
    return makeJsonNull(String.valueOf(fieldNumber));
  }

  /**
   * Create a JSON field with a {@code null} value
   * @param fieldName The field name
   * @return The JSON field
   */
  public static String makeJsonNull(String fieldName) {
    StringBuilder field = new StringBuilder();

    field.append("\"");
    field.append(fieldName);
    field.append("\":null");

    return field.toString();
  }

  /**
   * Convert a JSON array of numbers to a list of integers
   * @param jsonArray The JSON array
   * @return The integer list
   */
  public static List<Integer> jsonArrayToIntList(String jsonArray) {
    return delimitedToIntegerList(jsonArray.substring(1, jsonArray.length() - 1), ",");
  }

  /**
   * Convert a list of objects to a JSON array
   * @param list The list
   * @return The JSON array
   */
  public static String intListToJsonArray(List<Integer> list) {
    StringBuilder result = new StringBuilder();
    result.append('[');
    result.append(collectionToDelimited(list, ","));
    result.append(']');
    return result.toString();
  }

  /**
   * Make a valid CSV String from the given text.
   *
   * This always performs three steps:
   * <ul>
   *   <li>Surround the value in quotes</li>
   *   <li>Any " are replaced with "", per the CSV spec</li>
   *   <li>Newlines are replaced with semi-colons</li>
   * </ul>
   *
   * While these are not strictly necessary for all values,
   * they are appropriate for this application and the
   * target audiences of exported CSV files.
   *
   * @param text The value
   * @return The CSV value
   */
  public static String makeCsvString(String text) {
    StringBuilder csv = new StringBuilder();
    csv.append('"');
    csv.append(text.replace("\"", "\"\"").replaceAll("[\\r\\n]+", "; "));
    csv.append('"');

    return csv.toString();
  }
}
