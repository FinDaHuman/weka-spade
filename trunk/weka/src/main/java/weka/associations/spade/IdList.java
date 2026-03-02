/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * IdList.java
 * Copyright (C) 2024
 */

package weka.associations.spade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a vertical ID-List used by the SPADE algorithm.
 * Each entry is a pair (SID, EID) where SID is the sequence ID
 * and EID is the event/timestamp ID within that sequence.
 *
 * The ID-List is the core data structure that enables SPADE's
 * efficient temporal join operations.
 *
 * @author Weka SPADE Implementation
 */
public class IdList implements Serializable {

  /** for serialization */
  private static final long serialVersionUID = 1L;

  /** List of SID (sequence ID) values */
  private List<Integer> m_SidList;

  /** List of EID (event ID / timestamp) values */
  private List<Integer> m_EidList;

  /**
   * Constructor. Creates an empty ID-List.
   */
  public IdList() {
    m_SidList = new ArrayList<Integer>();
    m_EidList = new ArrayList<Integer>();
  }

  /**
   * Adds a (SID, EID) pair to this ID-List.
   *
   * @param sid the sequence ID
   * @param eid the event ID (timestamp within the sequence)
   */
  public void addEntry(int sid, int eid) {
    m_SidList.add(sid);
    m_EidList.add(eid);
  }

  /**
   * Returns the number of entries in this ID-List.
   *
   * @return the size of the ID-List
   */
  public int size() {
    return m_SidList.size();
  }

  /**
   * Returns the SID at the given index.
   *
   * @param index the index
   * @return the SID value
   */
  public int getSid(int index) {
    return m_SidList.get(index);
  }

  /**
   * Returns the EID at the given index.
   *
   * @param index the index
   * @return the EID value
   */
  public int getEid(int index) {
    return m_EidList.get(index);
  }

  /**
   * Returns the support of this ID-List, defined as the number
   * of distinct SID values.
   *
   * @return the number of distinct sequences containing this pattern
   */
  public int getSupport() {
    Set<Integer> distinctSids = new HashSet<Integer>(m_SidList);
    return distinctSids.size();
  }

  /**
   * Performs a temporal join for creating sequences where two items
   * appear in the SAME event (itemset extension).
   * Result contains (SID, EID) pairs where both items occur at the same
   * SID and same EID.
   *
   * @param other the other ID-List to join with
   * @return the resulting ID-List after equality join
   */
  public IdList equalityJoin(IdList other) {
    IdList result = new IdList();
    int i = 0, j = 0;

    while (i < this.size() && j < other.size()) {
      int sid1 = this.getSid(i);
      int eid1 = this.getEid(i);
      int sid2 = other.getSid(j);
      int eid2 = other.getEid(j);

      if (sid1 < sid2 || (sid1 == sid2 && eid1 < eid2)) {
        i++;
      } else if (sid1 > sid2 || (sid1 == sid2 && eid1 > eid2)) {
        j++;
      } else {
        // sid1 == sid2 && eid1 == eid2
        result.addEntry(sid1, eid1);
        i++;
        j++;
      }
    }
    return result;
  }

  /**
   * Performs a temporal join for creating sequences where the second item
   * appears AFTER the first item (sequence extension).
   * Result contains (SID, EID) pairs from 'other' where sid matches
   * and other's eid > this eid.
   *
   * @param other the other ID-List to join with
   * @return the resulting ID-List after temporal join
   */
  public IdList temporalJoin(IdList other) {
    IdList result = new IdList();
    int i = 0, j = 0;

    while (i < this.size() && j < other.size()) {
      int sid1 = this.getSid(i);
      int sid2 = other.getSid(j);

      if (sid1 < sid2) {
        i++;
      } else if (sid1 > sid2) {
        j++;
      } else {
        // Same SID: find all pairs where eid2 > eid1
        // Collect all entries from 'other' with this SID
        int jStart = j;
        while (j < other.size() && other.getSid(j) == sid1) {
          j++;
        }
        // For each entry in 'this' with sid1
        int iCur = i;
        while (iCur < this.size() && this.getSid(iCur) == sid1) {
          int eid1 = this.getEid(iCur);
          for (int k = jStart; k < j; k++) {
            int eid2 = other.getEid(k);
            if (eid2 > eid1) {
              result.addEntry(sid1, eid2);
            }
          }
          iCur++;
        }
        // Advance i past this SID group
        i = iCur;
      }
    }
    return result;
  }

  /**
   * Returns a string representation of this ID-List.
   *
   * @return the string representation
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for (int i = 0; i < size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append("(").append(m_SidList.get(i)).append(",").append(m_EidList.get(i)).append(")");
    }
    sb.append("}");
    return sb.toString();
  }
}
