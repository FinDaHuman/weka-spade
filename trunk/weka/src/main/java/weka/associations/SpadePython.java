package weka.associations;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.converters.ArffSaver;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

public class SpadePython extends AbstractAssociator implements OptionHandler, Serializable {

    private static final long serialVersionUID = 1L;
    private String resultString = "No associations built yet.";
    
    // Default support is 2
    private int m_minSupport = 2;

    public void setMinSupport(int minSup) {
        m_minSupport = minSup;
    }

    public int getMinSupport() {
        return m_minSupport;
    }
    
    public String minSupportTipText() {
        return "The minimum support count required for a sequential pattern to be considered frequent.";
    }
    
    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> result = new Vector<Option>();
        result.addElement(new Option("\tThe minimum support count (default: 2).", "C", 1, "-C <min support>"));
        result.addAll(java.util.Collections.list(super.listOptions()));
        return result.elements();
    }

    @Override
    public String[] getOptions() {
        Vector<String> options = new Vector<String>();
        options.add("-C");
        options.add("" + getMinSupport());
        java.util.Collections.addAll(options, super.getOptions());
        return options.toArray(new String[0]);
    }

    @Override
    public void setOptions(String[] options) throws Exception {
        String supString = Utils.getOption('C', options);
        if (supString.length() != 0) {
            setMinSupport(Integer.parseInt(supString));
        } else {
            setMinSupport(2);
        }
        super.setOptions(options);
    }

    @Override
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();
        // Allow numeric attributes for IDs and Nominal for items
        result.enable(Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capability.MISSING_VALUES); // just in case
        result.enable(Capability.NO_CLASS);
        return result;
    }

    @Override
    public void buildAssociations(Instances data) throws Exception {
        // 1. Check capabilities
        getCapabilities().testWithFail(data);
        
        // 2. Export data to temp ARFF
        File tempFile = File.createTempFile("weka_spade_", ".arff");
        tempFile.deleteOnExit();
        
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(tempFile);
        saver.writeBatch();
        
        // 3. Call Python script
        String pythonScript = "d:/Vs Code/Weka_SPADE_Demo/weka/spade.py";
        ProcessBuilder pb = new ProcessBuilder("python", pythonScript, tempFile.getAbsolutePath(), String.valueOf(m_minSupport));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        // 4. Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        p.waitFor();
        
        if (output.length() > 0) {
            this.resultString = output.toString();
        } else {
            this.resultString = "No output from Python script.";
        }
    }
    
    @Override
    public String toString() {
        return resultString;
    }

    public String globalInfo() {
        return "Executes SPADE via an external Python script.";
    }

    public static void main(String[] args) {
        runAssociator(new SpadePython(), args);
    }
}
