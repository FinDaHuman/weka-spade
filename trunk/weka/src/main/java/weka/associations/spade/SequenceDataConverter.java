/*
 * SequenceDataConverter.java
 * Converts CSV and JSON sequential data files to Weka ARFF format
 * for use with the SPADE algorithm.
 */
package weka.associations.spade;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility class that converts CSV and JSON sequential data into
 * Weka Instances (flat ARFF format) suitable for the SPADE algorithm.
 *
 * Supported formats:
 * 1. Horizontal CSV: SID,E1,E2,...,En (cells can be "item1,item2" for multi-item events)
 * 2. JSON: {"sequences": [{"SID":"...", "sequence": [["item1"], ["item2","item3"], ...]}]}
 *
 * Output format: sequenceID (numeric), eventID (numeric), item (nominal)
 *
 * Usage:
 *   java weka.associations.spade.SequenceDataConverter input.csv [output.arff]
 *   java weka.associations.spade.SequenceDataConverter input.json [output.arff]
 */
public class SequenceDataConverter {

    /**
     * A parsed sequence: has an ID and a list of events,
     * where each event is a list of item names.
     */
    private static class ParsedSequence {
        int id;
        List<List<String>> events; // each event = list of items

        ParsedSequence(int id, List<List<String>> events) {
            this.id = id;
            this.events = events;
        }
    }

    /**
     * Auto-detect file format and load as Weka Instances.
     *
     * @param filePath path to CSV or JSON file
     * @return Instances in flat ARFF format (sequenceID, eventID, item)
     * @throws Exception if file cannot be read or parsed
     */
    public static Instances loadFromFile(String filePath) throws Exception {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".json")) {
            return loadFromJSON(filePath);
        } else if (lower.endsWith(".csv")) {
            return loadFromCSV(filePath);
        } else {
            throw new Exception("Unsupported file format: " + filePath
                + "\nSupported formats: .csv, .json");
        }
    }

    /**
     * Parse a horizontal CSV file into Weka Instances.
     *
     * Expected format:
     *   SID,E1,E2,E3,...
     *   S1,itemA,"itemB,itemC",itemD,
     *
     * - First column is the sequence ID (ignored as data, used for ordering)
     * - Remaining columns are events in temporal order
     * - Quoted cells with commas represent multi-item events
     * - Empty cells are skipped (no event)
     *
     * @param filePath path to the CSV file
     * @return Instances in flat ARFF format
     * @throws Exception if file cannot be read or parsed
     */
    public static Instances loadFromCSV(String filePath) throws Exception {
        List<ParsedSequence> sequences = new ArrayList<>();
        Set<String> allItems = new LinkedHashSet<>();

        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new Exception("CSV file is empty: " + filePath);
        }

        // Skip header line
        for (int lineIdx = 1; lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx).trim();
            if (line.isEmpty() || line.endsWith(".")) {
                // Remove trailing dot if present
                line = line.replaceAll("\\.$", "").trim();
                if (line.isEmpty()) continue;
            }

            List<String> fields = parseCSVLine(line);
            if (fields.size() < 2) continue;

            int sid = lineIdx; // Use line index as sequence ID
            List<List<String>> events = new ArrayList<>();

            // fields[0] = SID (skip), fields[1..n] = events
            for (int i = 1; i < fields.size(); i++) {
                String cell = fields.get(i).trim();
                if (cell.isEmpty()) continue;

                // Split cell by comma for multi-item events
                List<String> items = new ArrayList<>();
                for (String item : cell.split(",")) {
                    String trimmed = item.trim();
                    if (!trimmed.isEmpty()) {
                        items.add(trimmed);
                        allItems.add(trimmed);
                    }
                }
                if (!items.isEmpty()) {
                    events.add(items);
                }
            }

            if (!events.isEmpty()) {
                sequences.add(new ParsedSequence(sid, events));
            }
        }

        return buildInstances(sequences, allItems, filePath);
    }

    /**
     * Parse a JSON sequential data file into Weka Instances.
     *
     * Expected format:
     * {
     *   "sequences": [
     *     {
     *       "SID": "S1",
     *       "sequence": [["item1"], ["item2","item3"], ["item4"]]
     *     },
     *     ...
     *   ]
     * }
     *
     * @param filePath path to the JSON file
     * @return Instances in flat ARFF format
     * @throws Exception if file cannot be read or parsed
     */
    public static Instances loadFromJSON(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        // Remove trailing dot if present
        content = content.trim();
        if (content.endsWith(".")) {
            content = content.substring(0, content.length() - 1).trim();
        }

        List<ParsedSequence> sequences = new ArrayList<>();
        Set<String> allItems = new LinkedHashSet<>();

        // Simple JSON parser (no external dependencies)
        // Find "sequences" array
        int seqArrayStart = content.indexOf("[", content.indexOf("\"sequences\""));
        if (seqArrayStart < 0) {
            throw new Exception("JSON must contain a \"sequences\" array");
        }

        // Parse each sequence object
        int sid = 1;
        int pos = seqArrayStart + 1;
        while (pos < content.length()) {
            // Find next sequence object
            int objStart = content.indexOf("{", pos);
            if (objStart < 0) break;

            // Find matching closing brace
            int objEnd = findMatchingBrace(content, objStart);
            if (objEnd < 0) break;

            String seqObj = content.substring(objStart, objEnd + 1);

            // Parse the "sequence" array within this object
            int innerArrayStart = seqObj.indexOf("[", seqObj.indexOf("\"sequence\""));
            if (innerArrayStart < 0) {
                pos = objEnd + 1;
                continue;
            }

            List<List<String>> events = new ArrayList<>();
            // Parse each event array [...]
            int eventPos = innerArrayStart + 1;
            while (eventPos < seqObj.length()) {
                int eventStart = seqObj.indexOf("[", eventPos);
                if (eventStart < 0) break;

                int eventEnd = seqObj.indexOf("]", eventStart);
                if (eventEnd < 0) break;

                String eventContent = seqObj.substring(eventStart + 1, eventEnd).trim();
                if (!eventContent.isEmpty()) {
                    List<String> items = new ArrayList<>();
                    // Parse quoted strings
                    int itemPos = 0;
                    while (itemPos < eventContent.length()) {
                        int quoteStart = eventContent.indexOf("\"", itemPos);
                        if (quoteStart < 0) break;
                        int quoteEnd = eventContent.indexOf("\"", quoteStart + 1);
                        if (quoteEnd < 0) break;
                        String item = eventContent.substring(quoteStart + 1, quoteEnd).trim();
                        if (!item.isEmpty()) {
                            items.add(item);
                            allItems.add(item);
                        }
                        itemPos = quoteEnd + 1;
                    }
                    if (!items.isEmpty()) {
                        events.add(items);
                    }
                }
                eventPos = eventEnd + 1;
            }

            if (!events.isEmpty()) {
                sequences.add(new ParsedSequence(sid, events));
                sid++;
            }

            pos = objEnd + 1;
        }

        return buildInstances(sequences, allItems, filePath);
    }

    /**
     * Build Weka Instances from parsed sequences.
     * Output format: sequenceID (numeric), eventID (numeric), item (nominal)
     */
    private static Instances buildInstances(List<ParsedSequence> sequences,
                                             Set<String> allItems,
                                             String sourceName) {
        // Create nominal attribute for items
        ArrayList<String> itemValues = new ArrayList<>(allItems);
        Collections.sort(itemValues);

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("sequenceID"));
        attributes.add(new Attribute("eventID"));
        attributes.add(new Attribute("item", itemValues));

        // Derive relation name from source file
        String relationName = Paths.get(sourceName).getFileName().toString();
        relationName = relationName.replaceAll("\\.[^.]+$", ""); // remove extension

        Instances data = new Instances(relationName, attributes, 0);

        // Add data rows
        for (ParsedSequence seq : sequences) {
            int eid = 1;
            for (List<String> event : seq.events) {
                for (String item : event) {
                    double[] vals = new double[3];
                    vals[0] = seq.id;
                    vals[1] = eid;
                    vals[2] = itemValues.indexOf(item);
                    data.add(new DenseInstance(1.0, vals));
                }
                eid++;
            }
        }

        return data;
    }

    /**
     * Save Instances as ARFF file.
     */
    public static void saveAsARFF(Instances data, String outputPath) throws Exception {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(outputPath));
        saver.writeBatch();
    }

    // ---- Helper methods ----

    /**
     * Parse a CSV line respecting quoted fields.
     * Handles RFC 4180: fields enclosed in double quotes can contain commas.
     */
    private static List<String> parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        return fields;
    }

    /**
     * Find the matching closing brace for an opening brace.
     */
    private static int findMatchingBrace(String s, int openPos) {
        int depth = 0;
        boolean inString = false;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    /**
     * Main method for standalone conversion.
     * Usage: java weka.associations.spade.SequenceDataConverter <input.csv|json> [output.arff]
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: SequenceDataConverter <input.csv|json> [output.arff]");
            System.out.println("  If output.arff is omitted, prints to stdout.");
            System.exit(1);
        }

        try {
            String inputPath = args[0];
            Instances data = loadFromFile(inputPath);

            if (args.length >= 2) {
                String outputPath = args[1];
                saveAsARFF(data, outputPath);
                System.out.println("Converted " + data.numInstances() + " rows to: " + outputPath);
            } else {
                // Print to stdout
                System.out.println(data.toString());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
