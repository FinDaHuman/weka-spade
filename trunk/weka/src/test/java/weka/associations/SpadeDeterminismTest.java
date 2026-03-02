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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SpadeDeterminismTest {
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
    public void testDeterminism() throws Exception {
        // Base logical sequence construction:
        // Seq0: A->B
        // Seq1: B->A
        // Seq2: A->C->D
        // Seq3: B->(C,D) -> A
        List<List<int[]>> logicalSeqs = new ArrayList<>();
        
        List<int[]> seq0 = new ArrayList<>();
        seq0.add(new int[]{0}); seq0.add(new int[]{1});
        logicalSeqs.add(seq0);
        
        List<int[]> seq1 = new ArrayList<>();
        seq1.add(new int[]{1}); seq1.add(new int[]{0});
        logicalSeqs.add(seq1);
        
        List<int[]> seq2 = new ArrayList<>();
        seq2.add(new int[]{0}); seq2.add(new int[]{2}); seq2.add(new int[]{3});
        logicalSeqs.add(seq2);
        
        List<int[]> seq3 = new ArrayList<>();
        seq3.add(new int[]{1}); seq3.add(new int[]{2, 3}); seq3.add(new int[]{0});
        logicalSeqs.add(seq3);
        
        Random rand = new Random(100);
        Set<Sequence> firstRun = null;
        
        for (int i = 0; i < 10; i++) {
            // Shuffle the order in which sequences are inserted into the dataset
            Collections.shuffle(logicalSeqs, rand);
            
            Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B", "C", "D"});
            
            for (int sid = 0; sid < logicalSeqs.size(); sid++) {
                List<int[]> events = logicalSeqs.get(sid);
                for (int[] evt : events) {
                    SpadeTestUtils.addRelationalEvent(data, sid, evt);
                }
            }
            
            // Re-instantiate Spade to wipe cache
            spade = new Spade();
            spade.setOptions(new String[]{"-S", "0.25", "-I", "1"});
            
            Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.25);
            
            if (firstRun == null) {
                firstRun = actual;
            } else {
                SpadeTestUtils.assertPatternSetEquals(firstRun, actual);
            }
        }
    }
}
