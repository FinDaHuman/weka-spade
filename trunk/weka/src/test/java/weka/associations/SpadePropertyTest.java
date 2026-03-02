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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SpadePropertyTest {
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
    public void testAntiMonotonicityProperty() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B", "C"});
        Random rand = new Random(42);
        
        // Generate random sequences
        for (int sid = 0; sid < 20; sid++) {
            int numEvents = rand.nextInt(5) + 1;
            for (int eid = 0; eid < numEvents; eid++) {
                List<Integer> items = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    if (rand.nextBoolean()) items.add(i);
                }
                if (!items.isEmpty()) {
                    int[] arr = items.stream().mapToInt(i -> i).toArray();
                    SpadeTestUtils.addRelationalEvent(data, sid, arr);
                }
            }
        }
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.05);
        
        for (Sequence child : actual) {
            int childSup = spade.getSupport(child);
            if (child.getElements().size() > 1 && child.isSequenceExtension()) {
                Sequence pre = new Sequence();
                // Subsequence generation (prefix)
                for (int i = 0; i < child.getElements().size() - 1; i++) {
                     pre.addElement(child.getElements().get(i).copy());
                }
                
                if (actual.contains(pre)) {
                    int parentSup = getSupFromSet(actual, pre);
                    assertTrue(parentSup >= childSup, "Anti-monotonicity violated: parent support " + parentSup + " < child support " + childSup);
                }
            }
        }
    }

    private int getSupFromSet(Set<Sequence> set, Sequence target) {
        for (Sequence s : set) {
            if (s.equals(target)) {
                return spade.getSupport(s);
            }
        }
        return -1;
    }
}
