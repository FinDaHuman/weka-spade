/*
 * SPADE is considered production-ready ONLY IF all tests in this suite pass with:
 *  - No test failures
 *  - No memory errors
 *  - No nondeterministic output
 *  - No support miscount
 */
package weka.associations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression and Bug-Fix Verification Tests for SPADE.
 */
public class SpadeRegressionTest {

    private Spade spade;

    @BeforeEach
    public void setUp() {
        spade = new Spade();
        try {
            spade.setOptions(new String[]{"-S", "0.5", "-I", "1"});
        } catch (Exception e) {
            fail("Failed to setup SPADE options: " + e.getMessage());
        }
    }

    private Instances createFlatDataset(int numItems) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("SeqID"));
        for (int i = 0; i < numItems; i++) {
            attributes.add(new Attribute("Item" + (char) ('A' + i)));
        }
        return new Instances("RegressionDataset", attributes, 0);
    }
    
    private void addEvent(Instances data, int seqID, int[] itemsPresent) {
        DenseInstance inst = new DenseInstance(data.numAttributes());
        inst.setDataset(data);
        for (int i = 0; i < data.numAttributes(); i++) {
            inst.setMissing(i);
        }
        inst.setValue(0, seqID); // First column is SeqID
        for (int itemIdx : itemsPresent) {
            inst.setValue(itemIdx + 1, 1.0);
        }
        data.add(inst);
    }

    @Test
    public void testEventIDNotTreatedAsItem() throws Exception {
        Instances data = createFlatDataset(2); // A, B
        addEvent(data, 1, new int[]{0}); // A
        addEvent(data, 2, new int[]{0}); // A
        
        spade.setMinSupport(1.0);
        spade.buildAssociations(data);
        
        String result = spade.toString();
        // If eventID/seqID is treated as an item, the model would find "SeqID=1.0000 -> ..."
        assertFalse(result.contains("SeqID="), "Sequence ID or Event ID must not be mined as an item");
    }

    @Test
    public void testSupportDoesNotDoubleCountSameSID() throws Exception {
        Instances data = createFlatDataset(1); // Only A
        // Add A multiple times to the same sequence
        addEvent(data, 1, new int[]{0});
        addEvent(data, 1, new int[]{0});
        addEvent(data, 1, new int[]{0});

        addEvent(data, 2, new int[]{0}); // A
        
        spade.setMinSupport(0.0);
        spade.buildAssociations(data);
        
        String result = spade.toString();
        // There are 2 sequences. Support of A should be max 2, not 4.
        assertTrue(result.contains("ItemA=1") && result.contains("support: 2"), "Support should strictly count distinct Sequence IDs");
        assertFalse(result.contains("support: 3"), "Should not count multiple events in same SID");
        assertFalse(result.contains("support: 4"), "Should not over-count");
    }

    @Test
    public void testJoinDoesNotProduceInvalidTemporalSequences() throws Exception {
        Instances data = createFlatDataset(2); // A, B
        // Seq 1: A and B occur *at the exact same time* (same event)
        addEvent(data, 1, new int[]{0, 1}); // A, B simultaneously
        addEvent(data, 2, new int[]{0, 1}); // A, B simultaneously
        
        spade.setMinSupport(1.0);
        spade.buildAssociations(data);
        
        String result = spade.toString();
        // Temporal sequence A -> B implies A happens *before* B.
        // If they only happen at the same time, A -> B should NOT exist.
        // Only itemset (A, B) should exist.
        
        assertFalse(result.contains("ItemA=1 -> ItemB=1"), "Temporal join A->B is invalid if they only co-occur simultaneously");
        assertFalse(result.contains("ItemB=1 -> ItemA=1"), "Temporal join B->A is invalid if they only co-occur simultaneously");
        
        // Ensure that equality join is found (ItemA ItemB) -> depending on Weka SPADE output format, usually output as "ItemA=1 ItemB=1"
        assertTrue(result.contains("ItemB=1") && result.contains("ItemA=1"), "Itemset (A, B) must exist if they happen at the same time");
    }

    @Test
    public void testLexicographicallySortedItemsAndDeterministicOutput() throws Exception {
        // Run identical tests multiple times to ensure the same String output
        Instances data = createFlatDataset(3); // A, B, C
        addEvent(data, 1, new int[]{2, 1, 0}); // Add items reversed
        addEvent(data, 2, new int[]{1, 2, 0}); 
        addEvent(data, 3, new int[]{0, 1, 2});
        
        spade.setMinSupport(0.5);
        spade.buildAssociations(data);
        String firstRun = spade.toString();
        
        spade = new Spade();
        spade.setOptions(new String[]{"-S", "0.5", "-I", "1"});
        spade.buildAssociations(data);
        String secondRun = spade.toString();
        
        firstRun = firstRun.replaceAll("Elapsed time: \\d+ ms\n", "Elapsed time: X ms\n");
        secondRun = secondRun.replaceAll("Elapsed time: \\d+ ms\n", "Elapsed time: X ms\n");
        
        assertEquals(firstRun, secondRun, "SPADE output must be completely deterministic over repeated runs");
    }
}
