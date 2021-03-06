package uk.ac.exeter.QuinCe.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.json.JSONArray;

/**
 * A structure for instrument variables, used when
 * selecting variables for plots and maps
 * @author Steve Jones
 *
 */
public class VariableList extends ArrayList<VariableGroup> {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -1121458147055907068L;

  /**
   * The shortcut lookup table
   */
  private Map<Integer, Variable> lookup;

  /**
   * The last ID used in the tree
   */
  private int lastId;

  /**
   * Create a new tree
   */
  protected VariableList() {
    super();
    lookup = new HashMap<Integer, Variable>();
    lastId = -1;
  }

  /**
   * Add a new variable
   * @param groupName The name of the group to which the variable belongs
   * @param variable The variable
   */
  public void addVariable(String groupName, Variable variable) {

    lastId++;
    variable.setId(lastId);

    VariableGroup group = getGroup(groupName);

    if (null == group) {
      group = new VariableGroup(groupName);
      add(group);
    }

    group.add(variable);

    lookup.put(variable.getId(), variable);
  }

  /**
   * Retrieve a variable using its ID
   * @param id The ID
   * @return The variable
   */
  protected Variable getVariable(int id) {
    return lookup.get(id);
  }

  /**
   * Get the named variable group
   * @param groupName The group name
   * @return The group, or {@code null} if it does not exist
   */
  public VariableGroup getGroup(String groupName) {
    VariableGroup result = null;

    for (VariableGroup group : this) {
      if (group.getName().equals(groupName)) {
        result = group;
        break;
      }
    }

    return result;
  }

  /**
   * Get the variable with the given label
   * @param label The label
   * @return The matching variable object, or {@code null} if no match is found
   */
  public Variable getVariableWithLabel(String label) {
    Variable result = null;

    for (VariableGroup group : this) {

      for (Variable variable : group.getVariables()) {
        if (variable.getLabel().equals(label)) {
          result = variable;
          break;
        }
      }


      if (null != result) {
        break;
      }
    }

    return result;
  }

  /**
   * Get a list of variables with the specified IDs
   * @param ids The variable IDs
   * @return The variables
   */
  public List<Variable> getVariables(List<Integer> ids) {

    List<Variable> result = new ArrayList<Variable>();

    if (null != ids) {
      for (int id : ids) {
        result.add(getVariable(id));
      }
    }

    return result;
  }

  /**
   * Get the set of groups and the ids of the variables they contain
   * in JSON format
   * @return The group details
   */
  protected String getGroupsJson() {
    JSONArray json = new JSONArray();

    for (VariableGroup group : this) {
      JSONArray groupJson = new JSONArray();
      for (Variable variable : group.getVariables()) {
        groupJson.put(variable.getId());
      }
      json.put(groupJson);
    }

    return json.toString();
  }

  /**
   * Get the names of all the variable groups as a JSON string
   * @return The group names
   */
  protected String getGroupNamesJson() {
    JSONArray json = new JSONArray();

    for (VariableGroup group : this) {
      json.put(group.getName());
    }

    return json.toString();
  }

  /**
   * Get the total number of variables in the list
   * @return The number of variables
   */
  public int getVariableCount() {
    int count = 0;

    for (VariableGroup group : this) {
      count += group.getSize();
    }

    return count;
  }

  /**
   * Determine whether or not the named variable group
   * is present in the list
   * @param groupName The group name
   * @return {@code true} if the group is defined in this list; {@code false} otherwise
   */
  public boolean containsGroup(String groupName) {
    boolean result = false;

    for (VariableGroup group : this) {
      if (group.getName().equals(groupName)) {
        result = true;
        break;
      }
    }

    return result;
  }
}
