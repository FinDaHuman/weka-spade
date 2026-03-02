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
 * Element.java
 * Copyright (C) 2024
 */

package weka.associations.spade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an element (itemset) within a sequence in the SPADE algorithm.
 * An element is a set of items that occur at the same timestamp/event
 * within a sequence.
 *
 * @author Weka SPADE Implementation
 */
public class Element implements Serializable, Comparable<Element> {

  /** for serialization */
  private static final long serialVersionUID = 2L;

  /** The sorted list of items in this element */
  private List<String> m_Items;

  /**
   * Constructor. Creates an empty element.
   */
  public Element() {
    m_Items = new ArrayList<String>();
  }

  /**
   * Constructor. Creates an element with a single item.
   *
   * @param item the item to add
   */
  public Element(String item) {
    m_Items = new ArrayList<String>();
    m_Items.add(item);
  }

  /**
   * Constructor. Creates an element from a list of items.
   *
   * @param items the list of items
   */
  public Element(List<String> items) {
    m_Items = new ArrayList<String>(items);
    Collections.sort(m_Items);
  }

  /**
   * Adds an item to this element, maintaining sorted order.
   *
   * @param item the item to add
   */
  public void addItem(String item) {
    m_Items.add(item);
    Collections.sort(m_Items);
  }

  /**
   * Returns the list of items in this element.
   *
   * @return the items
   */
  public List<String> getItems() {
    return m_Items;
  }

  /**
   * Returns the number of items in this element.
   *
   * @return the size
   */
  public int size() {
    return m_Items.size();
  }

  /**
   * Returns the item at the given index.
   *
   * @param index the index
   * @return the item
   */
  public String getItem(int index) {
    return m_Items.get(index);
  }

  /**
   * Checks if this element contains a given item.
   *
   * @param item the item to check
   * @return true if the element contains the item
   */
  public boolean containsItem(String item) {
    return m_Items.contains(item);
  }

  /**
   * Creates a copy of this element.
   *
   * @return a new Element with the same items
   */
  public Element copy() {
    return new Element(new ArrayList<String>(m_Items));
  }

  @Override
  public int compareTo(Element other) {
    int minLen = Math.min(this.size(), other.size());
    for (int i = 0; i < minLen; i++) {
      int cmp = this.m_Items.get(i).compareTo(other.m_Items.get(i));
      if (cmp != 0) return cmp;
    }
    return Integer.compare(this.size(), other.size());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Element)) return false;
    Element other = (Element) obj;
    return m_Items.equals(other.m_Items);
  }

  @Override
  public int hashCode() {
    return m_Items.hashCode();
  }

  /**
   * Returns a string representation of this element.
   * Single item: "item", multiple items: "(item1, item2)"
   *
   * @return the string representation
   */
  @Override
  public String toString() {
    if (m_Items.size() == 1) {
      return m_Items.get(0);
    }
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (int i = 0; i < m_Items.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append(m_Items.get(i));
    }
    sb.append(")");
    return sb.toString();
  }
}
