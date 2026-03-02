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
 * EquivalenceClass.java
 * Copyright (C) 2024
 */

package weka.associations.spade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an equivalence class in the SPADE algorithm.
 * An equivalence class groups sequences that share the same prefix.
 * 
 * Each member of the class is an "atom" — a sequence that extends the 
 * common prefix by exactly one item (either via sequence extension or
 * itemset extension).
 *
 * @author Weka SPADE Implementation
 */
public class EquivalenceClass implements Serializable {

  /** for serialization */
  private static final long serialVersionUID = 4L;

  /** The common prefix shared by all members */
  private Sequence m_Prefix;

  /** The member sequences (atoms) in this equivalence class */
  private List<Sequence> m_Members;

  /**
   * Constructor. Creates an equivalence class with the given prefix.
   *
   * @param prefix the common prefix (can be empty for 1-sequences)
   */
  public EquivalenceClass(Sequence prefix) {
    m_Prefix = prefix;
    m_Members = new ArrayList<Sequence>();
  }

  /**
   * Adds a member sequence to this equivalence class.
   *
   * @param seq the sequence to add
   */
  public void addMember(Sequence seq) {
    m_Members.add(seq);
  }

  /**
   * Returns the common prefix.
   *
   * @return the prefix sequence
   */
  public Sequence getPrefix() {
    return m_Prefix;
  }

  /**
   * Returns all member sequences.
   *
   * @return the members
   */
  public List<Sequence> getMembers() {
    return m_Members;
  }

  /**
   * Returns the number of members.
   *
   * @return the size
   */
  public int size() {
    return m_Members.size();
  }

  /**
   * Enumerates frequent sequences from this equivalence class by
   * performing pairwise temporal joins.
   *
   * For each pair (si, sj) of members:
   * - If both are sequence extensions: temporal join → new sequence extension
   * - If si is itemset extension: equality join → new itemset extension
   * - Also compute temporal join for sequence extension
   *
   * @param minSupportCount the minimum support count threshold
   * @param allFrequentSequences output list to collect all frequent sequences
   * @param debug whether to print debug info
   * @param maxPatternLength maximum pattern length (k in k-sequence)
   */
  public void enumerateFrequentSequences(long minSupportCount,
      List<Sequence> allFrequentSequences, boolean debug, int maxPatternLength) {

    // For each pair of atoms in this equivalence class
    for (int i = 0; i < m_Members.size(); i++) {
      Sequence si = m_Members.get(i);
      
      // Check if extending si would exceed max pattern length
      if (si.itemCount() >= maxPatternLength) {
        continue;
      }

      // Create new equivalence class with si as prefix
      EquivalenceClass newEqClass = new EquivalenceClass(si);

      for (int j = i; j < m_Members.size(); j++) {
        Sequence sj = m_Members.get(j);

        String lastItemI = si.getLastItem();
        String lastItemJ = sj.getLastItem();

        if (lastItemI == null || lastItemJ == null) continue;

        // Case 1: Both are atoms — try temporal join (sequence extension)
        if (i != j) {
          // Temporal join: si's idlist ⋈_T sj's idlist → sequence extension
          IdList joinedList = si.getIdList().temporalJoin(sj.getIdList());
          if (joinedList.getSupport() >= minSupportCount) {
            Sequence newSeq = si.sequenceExtend(lastItemJ);
            newSeq.setIdList(joinedList);
            newEqClass.addMember(newSeq);
            allFrequentSequences.add(newSeq);
          }

          // Also try the reverse: sj ⋈_T si
          IdList reverseJoinedList = sj.getIdList().temporalJoin(si.getIdList());
          if (reverseJoinedList.getSupport() >= minSupportCount) {
            Sequence newSeq = sj.sequenceExtend(lastItemI);
            newSeq.setIdList(reverseJoinedList);
            allFrequentSequences.add(newSeq);
          }
        }

        // Case 2: Equality join (itemset extension)
        // Only if lastItemI < lastItemJ (to avoid duplicates)
        if (i != j && lastItemI.compareTo(lastItemJ) < 0) {
          IdList equalJoinedList = si.getIdList().equalityJoin(sj.getIdList());
          if (equalJoinedList.getSupport() >= minSupportCount) {
            Sequence newSeq = si.itemsetExtend(lastItemJ);
            newSeq.setIdList(equalJoinedList);
            newEqClass.addMember(newSeq);
            allFrequentSequences.add(newSeq);
          }
        }

        // Case 3: Self-join for sequence extension (i == j)
        if (i == j) {
          IdList selfJoinList = si.getIdList().temporalJoin(si.getIdList());
          if (selfJoinList.getSupport() >= minSupportCount) {
            Sequence newSeq = si.sequenceExtend(lastItemI);
            newSeq.setIdList(selfJoinList);
            newEqClass.addMember(newSeq);
            allFrequentSequences.add(newSeq);
          }
        }
      }

      // Recursively enumerate from the new equivalence class
      if (newEqClass.size() > 0) {
        newEqClass.enumerateFrequentSequences(minSupportCount,
            allFrequentSequences, debug, maxPatternLength);
      }
    }
  }

  /**
   * Returns a string representation of this equivalence class.
   *
   * @return the string representation
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("EquivalenceClass[prefix=");
    sb.append(m_Prefix != null ? m_Prefix.toString() : "<empty>");
    sb.append(", members=").append(m_Members.size()).append("]");
    return sb.toString();
  }
}
