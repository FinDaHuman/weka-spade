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
 * Spade.java
 * Copyright (C) 2024
 */

package weka.associations;

import weka.associations.spade.Element;
import weka.associations.spade.EquivalenceClass;
import weka.associations.spade.IdList;
import weka.associations.spade.Sequence;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * <!-- globalinfo-start -->
 * Class implementing the SPADE algorithm for mining frequent sequential
 * patterns using vertical ID-List format and equivalence classes.<br/>
 * The attribute identifying the distinct data sequences contained in the
 * data set can be specified by the respective option.<br/>
 * <br/>
 * SPADE uses a vertical database representation where each item is
 * associated with an ID-List of (SID, EID) pairs. It decomposes the
 * search space into independent equivalence classes based on common
 * prefixes and uses efficient temporal joins to find frequent sequences.<br/>
 * <br/>
 * For further information see:<br/>
 * <br/>
 * Mohammed J. Zaki (2001). SPADE: An Efficient Algorithm for Mining
 * Frequent Sequences. Machine Learning Journal, 42(1-2):31-60.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Zaki2001,
 *    author = {Mohammed J. Zaki},
 *    journal = {Machine Learning},
 *    number = {1-2},
 *    pages = {31-60},
 *    title = {SPADE: An Efficient Algorithm for Mining Frequent Sequences},
 *    volume = {42},
 *    year = {2001}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 *
 * <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -D
 *  If set, algorithm is run in debug mode and
 *  may output additional info to the console</pre>
 *
 * <pre> -S &lt;minimum support threshold&gt;
 *  The minimum support threshold (0..1).
 *  (default: 0.5)</pre>
 *
 * <pre> -I &lt;attribute number representing the data sequence ID&gt;
 *  The attribute number representing the data sequence ID (1-based).
 *  (default: 1)</pre>
 *
 * <!-- options-end -->
 *
 * @author Weka SPADE Implementation
 * @version $Revision$
 */
public class Spade
  extends AbstractAssociator
  implements OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = 8819753748033714861L;

  /** The minimum support threshold (as a fraction 0..1) */
  protected double m_MinSupport = 0.5;

  /** The attribute index for the data sequence ID (0-based internally) */
  protected int m_DataSeqID = 0;

  /** Maximum pattern length (k in k-sequence) to prevent combinatorial explosion */
  protected int m_MaxPatternLength = 10;

  /** Whether to run in debug mode */
  protected boolean m_Debug = false;

  /** The original data set */
  protected Instances m_OriginalDataSet;

  /** All discovered frequent sequences grouped by length */
  protected Map<Integer, List<Sequence>> m_FrequentSequences;

  /** Total number of distinct sequences in the data */
  protected int m_TotalSequences;

  /** Total number of frequent sequences found */
  protected int m_TotalFrequentSequences;

  /** Algorithm execution time in milliseconds */
  protected long m_ElapsedTime;

  /**
   * Constructor.
   */
  public Spade() {
    resetOptions();
  }

  /**
   * Returns global information about the algorithm.
   *
   * @return the global information
   */
  public String globalInfo() {
    return
        "Class implementing the SPADE algorithm for mining frequent "
      + "sequential patterns using vertical ID-List format and "
      + "equivalence classes.\n"
      + "The attribute identifying the distinct data sequences contained "
      + "in the data set can be specified by the respective option.\n\n"
      + "SPADE uses a vertical database representation where each item is "
      + "associated with an ID-List of (SID, EID) pairs. It decomposes the "
      + "search space into independent equivalence classes based on common "
      + "prefixes and uses efficient temporal joins to find frequent "
      + "sequences.\n\n"
      + "For further information see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns TechnicalInformation about the SPADE paper.
   *
   * @return the TechnicalInformation
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation paper = new TechnicalInformation(Type.ARTICLE);

    paper.setValue(Field.AUTHOR, "Mohammed J. Zaki");
    paper.setValue(Field.TITLE,
        "SPADE: An Efficient Algorithm for Mining Frequent Sequences");
    paper.setValue(Field.JOURNAL, "Machine Learning");
    paper.setValue(Field.VOLUME, "42");
    paper.setValue(Field.NUMBER, "1-2");
    paper.setValue(Field.PAGES, "31-60");
    paper.setValue(Field.YEAR, "2001");

    return paper;
  }

  /**
   * Returns an enumeration of the available options.
   *
   * @return the available options
   */
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
        "\tIf set, algorithm is run in debug mode and\n"
        + "\tmay output additional info to the console",
        "D", 0, "-D"));

    result.addElement(new Option(
        "\tThe minimum support threshold (0..1).\n"
        + "\t(default: 0.5)",
        "S", 1, "-S <minimum support threshold>"));

    result.addElement(new Option(
        "\tThe attribute number representing the data sequence ID (1-based).\n"
        + "\t(default: 1)",
        "I", 1, "-I <attribute number representing the data sequence ID>"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   *
   * <!-- options-start -->
   * Valid options are: <p/>
   *
   * <pre> -D
   *  If set, algorithm is run in debug mode and
   *  may output additional info to the console</pre>
   *
   * <pre> -S &lt;minimum support threshold&gt;
   *  The minimum support threshold (0..1).
   *  (default: 0.5)</pre>
   *
   * <pre> -I &lt;attribute number representing the data sequence ID&gt;
   *  The attribute number representing the data sequence ID (1-based).
   *  (default: 1)</pre>
   *
   * <!-- options-end -->
   *
   * @param options the Array containing the options
   * @throws Exception if an option is not valid
   */
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    resetOptions();

    setDebug(Utils.getFlag('D', options));

    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0) {
      setMinSupport(Double.parseDouble(tmpStr));
    }

    tmpStr = Utils.getOption('I', options);
    if (tmpStr.length() != 0) {
      setDataSeqID(Integer.parseInt(tmpStr));
    }
  }

  /**
   * Returns an Array containing the current options settings.
   *
   * @return the Array containing the settings
   */
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    if (getDebug()) {
      result.add("-D");
    }

    result.add("-S");
    result.add("" + getMinSupport());

    result.add("-I");
    result.add("" + getDataSeqID());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Resets the algorithm's options to default values.
   */
  protected void resetOptions() {
    m_MinSupport = 0.5;
    m_DataSeqID = 0;
    m_MaxPatternLength = 10;
    m_Debug = false;
  }

  /**
   * Returns the Capabilities of this algorithm.
   *
   * @return the Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * The main entry point. Extracts all frequent sequential patterns from
   * the given data set using the SPADE algorithm.
   *
   * @param data the training data set
   * @throws Exception if the algorithm fails
   */
  public void buildAssociations(Instances data) throws Exception {
    // Check capabilities
    getCapabilities().testWithFail(data);

    long startTime = System.currentTimeMillis();

    m_OriginalDataSet = new Instances(data);
    m_FrequentSequences = new LinkedHashMap<Integer, List<Sequence>>();
    m_TotalFrequentSequences = 0;

    // Step 1: Convert horizontal DB to vertical format (ID-Lists)
    int seqIdAttr = m_DataSeqID;
    if (seqIdAttr < 0 || seqIdAttr >= data.numAttributes()) {
      throw new Exception("Invalid sequence ID attribute index: "
          + (seqIdAttr + 1) + ". Must be between 1 and "
          + data.numAttributes());
    }

    // Extract data items and build vertical DB
    Map<String, IdList> verticalDB = buildVerticalDB(data, seqIdAttr);

    // Count total distinct sequences depending on data format
    Set<Integer> distinctSids = new HashSet<Integer>();
    if (data.attribute(seqIdAttr).isRelationValued()) {
      distinctSids = null; // We will use numInstances directly
      m_TotalSequences = data.numInstances();
    } else {
      for (int i = 0; i < data.numInstances(); i++) {
        if (!data.instance(i).isMissing(seqIdAttr)) {
          distinctSids.add((int) data.instance(i).value(seqIdAttr));
        }
      }
      m_TotalSequences = distinctSids.size();
    }

    long minSupportCount = Math.round(m_MinSupport * m_TotalSequences);
    if (minSupportCount < 1) minSupportCount = 1;

    if (m_Debug) {
      System.out.println("SPADE: Total sequences = " + m_TotalSequences);
      System.out.println("SPADE: Min support count = " + minSupportCount);
      System.out.println("SPADE: Unique items = " + verticalDB.size());
    }

    // Step 2: Find frequent 1-sequences
    List<Sequence> freq1Sequences = new ArrayList<Sequence>();
    for (Map.Entry<String, IdList> entry : verticalDB.entrySet()) {
      if (entry.getValue().getSupport() >= minSupportCount) {
        Sequence seq = new Sequence(entry.getKey());
        seq.setIdList(entry.getValue());
        freq1Sequences.add(seq);
      }
    }

    // Sort for deterministic output
    Collections.sort(freq1Sequences, new java.util.Comparator<Sequence>() {
      public int compare(Sequence a, Sequence b) {
        return a.getLastItem().compareTo(b.getLastItem());
      }
    });

    if (freq1Sequences.isEmpty()) {
      m_ElapsedTime = System.currentTimeMillis() - startTime;
      return;
    }

    m_FrequentSequences.put(1, freq1Sequences);
    m_TotalFrequentSequences += freq1Sequences.size();

    if (m_Debug) {
      System.out.println("SPADE: Frequent 1-sequences = "
          + freq1Sequences.size());
    }

    // Step 3: Find frequent 2-sequences and build equivalence classes
    List<Sequence> freq2Sequences = new ArrayList<Sequence>();
    List<EquivalenceClass> eqClasses = new ArrayList<EquivalenceClass>();

    for (int i = 0; i < freq1Sequences.size(); i++) {
      Sequence si = freq1Sequences.get(i);
      EquivalenceClass eqClass = new EquivalenceClass(si);

      for (int j = i; j < freq1Sequences.size(); j++) {
        Sequence sj = freq1Sequences.get(j);

        // Sequence extension: si -> sj (temporal join)
        IdList seqJoin = si.getIdList().temporalJoin(sj.getIdList());
        if (seqJoin.getSupport() >= minSupportCount) {
          Sequence newSeq = si.sequenceExtend(sj.getLastItem());
          newSeq.setIdList(seqJoin);
          freq2Sequences.add(newSeq);
          eqClass.addMember(newSeq);
        }

        // Reverse sequence extension: sj -> si (only if i != j)
        if (i != j) {
          IdList reverseJoin = sj.getIdList().temporalJoin(si.getIdList());
          if (reverseJoin.getSupport() >= minSupportCount) {
            Sequence newSeq = sj.sequenceExtend(si.getLastItem());
            newSeq.setIdList(reverseJoin);
            freq2Sequences.add(newSeq);
            // This belongs to sj's equivalence class, will be handled later
          }
        }

        // Itemset extension: (si, sj) in same event (only if i < j)
        if (i < j) {
          IdList eqJoin = si.getIdList().equalityJoin(sj.getIdList());
          if (eqJoin.getSupport() >= minSupportCount) {
            Sequence newSeq = si.itemsetExtend(sj.getLastItem());
            newSeq.setIdList(eqJoin);
            freq2Sequences.add(newSeq);
            eqClass.addMember(newSeq);
          }
        }
      }

      if (eqClass.size() > 0) {
        eqClasses.add(eqClass);
      }
    }

    if (!freq2Sequences.isEmpty()) {
      m_FrequentSequences.put(2, freq2Sequences);
      m_TotalFrequentSequences += freq2Sequences.size();
    }

    if (m_Debug) {
      System.out.println("SPADE: Frequent 2-sequences = "
          + freq2Sequences.size());
      System.out.println("SPADE: Equivalence classes = "
          + eqClasses.size());
    }

    // Step 4: Recursively enumerate frequent k-sequences (k >= 3)
    List<Sequence> longerSequences = new ArrayList<Sequence>();
    for (EquivalenceClass eqClass : eqClasses) {
      eqClass.enumerateFrequentSequences(minSupportCount,
          longerSequences, m_Debug, m_MaxPatternLength);
    }

    // Group longer sequences by length
    for (Sequence seq : longerSequences) {
      int k = seq.itemCount();
      if (!m_FrequentSequences.containsKey(k)) {
        m_FrequentSequences.put(k, new ArrayList<Sequence>());
      }
      m_FrequentSequences.get(k).add(seq);
      m_TotalFrequentSequences++;
    }

    m_ElapsedTime = System.currentTimeMillis() - startTime;

    if (m_Debug) {
      System.out.println("SPADE: Total frequent sequences = "
          + m_TotalFrequentSequences);
      System.out.println("SPADE: Elapsed time = " + m_ElapsedTime + " ms");
    }
  }

  /**
   * Builds the vertical database (ID-Lists) from horizontal data.
   *
   * Each item (attribute_name=value) gets an ID-List of (SID, EID) pairs.
   * SID = sequence ID (from the designated attribute)
   * EID = row number within the sequence (event/timestamp order)
   *
   * @param data the input data
   * @param seqIdAttr the attribute index for sequence ID
   * @return map from item name to IdList
   */
  private Map<String, IdList> buildVerticalDB(Instances data, int seqIdAttr) {
    Map<String, IdList> verticalDB = new LinkedHashMap<String, IdList>();
    boolean isRelationalSequence = data.attribute(seqIdAttr).isRelationValued();

    if (isRelationalSequence) {
      // Weka's native sequence format: each top-level instance is one sequence.
      // The relational attribute contains the instances representing events.
      for (int sid = 0; sid < data.numInstances(); sid++) {
        Instance inst = data.instance(sid);
        if (inst.isMissing(seqIdAttr)) continue;
        
        Instances sequenceData = inst.relationalValue(seqIdAttr);
        // Each instance inside sequenceData represents an Event
        for (int eid = 0; eid < sequenceData.numInstances(); eid++) {
          Instance eventInst = sequenceData.instance(eid);
          // Items are attributes inside this event instance
          for (int a = 0; a < sequenceData.numAttributes(); a++) {
            if (eventInst.isMissing(a)) continue;
            Attribute attr = sequenceData.attribute(a);
            String itemName = attr.name() + "=";
            if (attr.isNominal() || attr.isString()) {
              itemName += eventInst.stringValue(a);
            } else {
              itemName += Utils.doubleToString(eventInst.value(a), 4);
            }
            
            IdList idList = verticalDB.get(itemName);
            if (idList == null) {
              idList = new IdList();
              verticalDB.put(itemName, idList);
            }
            idList.addEntry(sid, eid);
          }
        }
      }
    } else {
      // Horizontal flat format: requires an explicit seqID and tracks event appearances.
      Map<Integer, Integer> seqEventCounter = new HashMap<Integer, Integer>();
      for (int i = 0; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);
        if (inst.isMissing(seqIdAttr)) continue;
        int sid = (int) inst.value(seqIdAttr);

        // Increment event counter for this sequence
        Integer eventCount = seqEventCounter.get(sid);
        if (eventCount == null) {
          eventCount = 0;
        }
        int eid = eventCount;
        seqEventCounter.put(sid, eventCount + 1);

        // For each attribute (except seqID), create item entries
        for (int a = 0; a < data.numAttributes(); a++) {
          if (a == seqIdAttr) continue;
          if (inst.isMissing(a)) continue;

          Attribute attr = data.attribute(a);
          String itemName = attr.name() + "=";
          if (attr.isNominal() || attr.isString()) {
            itemName += inst.stringValue(a);
          } else {
            itemName += Utils.doubleToString(inst.value(a), 4);
          }

          IdList idList = verticalDB.get(itemName);
          if (idList == null) {
            idList = new IdList();
            verticalDB.put(itemName, idList);
          }
          idList.addEntry(sid, eid);
        }
      }
    }

    return verticalDB;
  }

  /**
   * Returns the result information as a String for display in the GUI.
   *
   * @return the result string
   */
  @Override
  public String toString() {
    if (m_FrequentSequences == null) {
      return "SPADE: No model built yet.";
    }

    StringBuilder result = new StringBuilder();

    result.append("SPADE - Sequential PAttern Discovery using Equivalence classes\n");
    result.append("===============================================================\n\n");
    result.append("Minimum support: " + m_MinSupport
        + " (" + Math.round(m_MinSupport * m_TotalSequences) + " sequences)\n");
    result.append("Total data sequences: " + m_TotalSequences + "\n");
    result.append("Total frequent sequences found: "
        + m_TotalFrequentSequences + "\n");
    result.append("Elapsed time: " + m_ElapsedTime + " ms\n\n");

    result.append("Frequent Sequential Patterns:\n");
    result.append("-----------------------------\n\n");

    // Sort keys
    List<Integer> lengths = new ArrayList<Integer>(m_FrequentSequences.keySet());
    Collections.sort(lengths);

    for (int k : lengths) {
      List<Sequence> sequences = m_FrequentSequences.get(k);
      result.append("-- " + k + "-sequences (count: " + sequences.size() + ")\n\n");
      for (Sequence seq : sequences) {
        result.append(seq.toStringWithSupport(m_TotalSequences) + "\n");
      }
      result.append("\n");
    }

    return result.toString();
  }

  // ---- Bean property methods for Weka GUI ----

  /**
   * Returns the tip text for the minSupport property.
   *
   * @return the tip text
   */
  public String minSupportTipText() {
    return "Minimum support threshold (fraction between 0 and 1).";
  }

  /**
   * Returns the minimum support threshold.
   *
   * @return the minimum support threshold
   */
  public double getMinSupport() {
    return m_MinSupport;
  }

  /**
   * Sets the minimum support threshold.
   *
   * @param value the minimum support threshold (0..1)
   */
  public void setMinSupport(double value) {
    m_MinSupport = value;
  }

  /**
   * Returns the tip text for the dataSeqID property.
   *
   * @return the tip text
   */
  public String dataSeqIDTipText() {
    return "The attribute number representing the data sequence ID (1-based).";
  }

  /**
   * Returns the data sequence ID attribute index (1-based for display).
   *
   * @return the data sequence ID (1-based)
   */
  public int getDataSeqID() {
    return m_DataSeqID + 1;
  }

  /**
   * Sets the data sequence ID attribute index (1-based from GUI).
   *
   * @param value the data sequence ID (1-based)
   */
  public void setDataSeqID(int value) {
    m_DataSeqID = value - 1;
  }

  /**
   * Returns the tip text for the debug property.
   *
   * @return the tip text
   */
  public String debugTipText() {
    return "If set to true, algorithm may output additional info to the console.";
  }

  /**
   * Set debugging mode.
   *
   * @param value true if debug output should be printed
   */
  public void setDebug(boolean value) {
    m_Debug = value;
  }

  /**
   * Get whether debugging is turned on.
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {
    return m_Debug;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method.
   *
   * @param args commandline options, use -h for help
   */
  public static void main(String[] args) {
    runAssociator(new Spade(), args);
  }
}
