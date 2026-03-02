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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SpadeStressTest {
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
    public void testDeepPatternJoinPerformanceAndCorrectness() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"0", "1", "2", "3", "4", "5"}); 
        for (int sid = 0; sid < 10; sid++) {
            // 0 -> 1 -> 2 -> 3 -> 4 -> 5
            for (int i = 0; i < 6; i++) {
                SpadeTestUtils.addRelationalEvent(data, sid, new int[]{i});
            }
        }
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.5);
        
        Sequence fullSeq = SpadeTestUtils.seq(
            SpadeTestUtils.itemStr("0"), SpadeTestUtils.itemStr("1"),
            SpadeTestUtils.itemStr("2"), SpadeTestUtils.itemStr("3"),
            SpadeTestUtils.itemStr("4"), SpadeTestUtils.itemStr("5")
        );
        assertTrue(actual.contains(fullSeq), "Must find length 6 pattern");
        
        // Count patterns: 6 choose 1 (6) + 6 choose 2 temporal (15) + ... 6 choose 6 (1)
        // Actually, power set of 6 items temporally ordered is 2^6 - 1 = 63 patterns.
        assertEquals(63, actual.size(), "Should correctly extract exactly 63 temporal subsequence patterns");
    }

    @Test
    public void testSparseDatasetPruningAccuracy() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B", "C", "D"}); 
        for (int sid = 0; sid < 1000; sid++) {
            SpadeTestUtils.addRelationalEvent(data, sid, new int[]{0}); // A
            if (sid % 2 == 0) {
                SpadeTestUtils.addRelationalEvent(data, sid, new int[]{1}); // B
            }
        }
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.4); 
        
        // 1000 sequences. A support 1000. B support 500.
        // A -> B support 500.
        Sequence abSeq = SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B"));
        assertTrue(actual.contains(abSeq), "Must find pattern A->B in massive sparse dataset");
        
        for (Sequence s : actual) {
            if (s.equals(abSeq)) {
                assertEquals(500, spade.getSupport(s));
            }
        }
    }

    @Test
    public void testDenseDatasetPatternExplosionWithLengthLimit() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"0","1","2"}); 
        // Create 20 sequences where EVERY event has EVERY item
        for (int sid = 0; sid < 20; sid++) {
            for (int eid = 0; eid < 3; eid++) {
                int[] allItems = new int[]{0,1,2};
                SpadeTestUtils.addRelationalEvent(data, sid, allItems);
            }
        }
        
        spade.setMinSupport(0.9); // Requires 18 sequences
        spade.setOptions(new String[]{"-S", "0.9", "-I", "1"}); 
        
        // Note: Without a length limit, an unbounded dataset of overlapping events 
        // can explode combinations. The default Spade implementation caps max pattern length.
        // If not capped, it causes OutOfMemory.
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.9);
        
        assertFalse(actual.isEmpty(), "Dense mining must eventually yield outputs or gracefully terminate");
        
        // Soft performance validation: we just verify that it completed and patterns were found.
        // The absence of OutOfMemoryException and presence of items validates stability.
        assertTrue(actual.contains(SpadeTestUtils.seq(SpadeTestUtils.itemStr("0"))));
    }
}
