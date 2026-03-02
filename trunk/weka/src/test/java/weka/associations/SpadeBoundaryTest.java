/*
 * SPADE is production-ready IF AND ONLY IF:
 *  - 100% of tests pass
 *  - No invariant violation
 *  - No nondeterminism
 *  - No support miscount
 *  - No invalid join behavior
 *  - No duplicate patterns
 */
package weka.associations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weka.associations.spade.Sequence;
import weka.core.Instances;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SpadeBoundaryTest {
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

    @Test
    public void testEmptyDataset() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A"});
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.5);
        
        assertTrue(actual.isEmpty(), "Should not find any patterns in an empty dataset");
    }

    @Test
    public void testDatasetWithOneSequence() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B"});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{1});

        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.5);
        
        Set<Sequence> expected = new HashSet<>();
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("B")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B")));
        
        SpadeTestUtils.assertPatternSetEquals(expected, actual);
    }

    @Test
    public void testDatasetWithOneItem() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A"});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0}); // A
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{0}); // A

        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.5);
        
        assertEquals(1, actual.size(), "Only item A should be found");
        Sequence seqA = SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"));
        assertTrue(actual.contains(seqA), "A should be correctly mined");
        assertEquals(2, spade.getSupport(seqA), "Support should be 2");
    }

    @Test
    public void testDatasetNoItemMeetsMinSup() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B", "C"});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0}); // A
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{1}); // B
        SpadeTestUtils.addRelationalEvent(data, 2, new int[]{2}); // C

        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.5); // min_sup = 2
        
        assertTrue(actual.isEmpty(), "No items should be frequent");
    }

    @Test
    public void testMinSupZero() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A"});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0});
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.0);
        
        assertEquals(1, actual.size(), "Must work with sup=0");
        Sequence seqA = SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"));
        assertTrue(actual.contains(seqA));
        assertEquals(1, spade.getSupport(seqA));
    }

    @Test
    public void testMinSupOne() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B"});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0});
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{0});
        // Seq 2 does not have A
        SpadeTestUtils.addRelationalEvent(data, 2, new int[]{1});
        
        // Correct implementation for not having A: add an event with B
        // The fact that we added an event for sequence 2 makes total seq = 3
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 1.0); // requires 3
        
        assertTrue(actual.isEmpty(), "A has support 2/3, not 100%");
    }
}
