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

public class SpadeInternalAlgorithmTest {
    private Spade spade;

    @BeforeEach
    public void setUp() {
        spade = new Spade();
        try {
            spade.setOptions(new String[]{"-S", "0.5", "-I", "1"});
        } catch (Exception e) {
            fail("Failed to set options: " + e.getMessage());
        }
    }

    @Test
    public void testEqualityVsTemporalJoin() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B"});
        // Seq 0: A and B occur *at the exact same time* (same event)
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0, 1}); // A, B
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{0, 1}); // A, B
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 1.0);
        
        // A -> B is invalid. B -> A is invalid.
        assertFalse(actual.contains(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B"))), "Temporal join A->B is invalid if they only co-occur simultaneously");
        assertFalse(actual.contains(SpadeTestUtils.seq(SpadeTestUtils.itemStr("B"), SpadeTestUtils.itemStr("A"))), "Temporal join B->A is invalid if they only co-occur simultaneously");
        
        // Only itemset (A, B) should exist.
        assertTrue(actual.contains(SpadeTestUtils.itemsetSeq(SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B"))), "Itemset (A, B) must exist");
    }

    @Test
    public void testAntiDoubleCount() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A"});
        // Add A multiple times to the same sequence (seq 0) - testing EID inflation
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0});

        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{0}); // A in seq 1
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.0);
        
        Sequence aSeq = SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"));
        assertTrue(actual.contains(aSeq));
        assertEquals(2, spade.getSupport(aSeq), "Support should strictly count distinct Sequence IDs");
    }

    @Test
    public void testDeepJoinLogic() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B", "C", "D", "E", "F"});
        for (int sid = 0; sid < 5; sid++) {
            for (int item = 0; item < 6; item++) {
                SpadeTestUtils.addRelationalEvent(data, sid, new int[]{item}); 
            }
        }
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 1.0);
        
        Sequence fullSeq = SpadeTestUtils.seq(
            SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B"),
            SpadeTestUtils.itemStr("C"), SpadeTestUtils.itemStr("D"),
            SpadeTestUtils.itemStr("E"), SpadeTestUtils.itemStr("F")
        );
        assertTrue(actual.contains(fullSeq), "Must correctly perform depth-6 join and identify length-6 pattern");
        assertEquals(5, spade.getSupport(fullSeq), "Should have full support of 5");
    }
}
