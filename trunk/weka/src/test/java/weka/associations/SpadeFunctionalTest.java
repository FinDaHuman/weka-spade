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
import weka.associations.spade.Element;
import weka.associations.spade.Sequence;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpadeTestUtils {
    public static Instances createRelationalDataset(String[] itemDomain) {
        ArrayList<Attribute> itemAttrs = new ArrayList<>();
        for (String item : itemDomain) {
            itemAttrs.add(new Attribute("Item" + item));
        }
        Instances sequenceMeta = new Instances("Event", itemAttrs, 0);
        
        ArrayList<Attribute> seqAttrs = new ArrayList<>();
        seqAttrs.add(new Attribute("Events", sequenceMeta));
        
        return new Instances("RelationalDataset", seqAttrs, 0);
    }
    
    public static void addRelationalEvent(Instances data, int sequenceIdx, int[] itemsPresent) {
        while (data.numInstances() <= sequenceIdx) {
            DenseInstance seqInst = new DenseInstance(1);
            seqInst.setDataset(data);
            Instances events = new Instances(data.attribute(0).relation(), 0);
            seqInst.setValue(0, data.attribute(0).addRelation(events));
            data.add(seqInst);
        }
        
        DenseInstance seqInst = (DenseInstance) data.instance(sequenceIdx);
        Instances events = seqInst.relationalValue(0);
        DenseInstance eventInst = new DenseInstance(events.numAttributes());
        eventInst.setDataset(events);
        for (int i = 0; i < events.numAttributes(); i++) {
            eventInst.setMissing(i);
        }
        for (int itemIdx : itemsPresent) {
            eventInst.setValue(itemIdx, 1.0);
        }
        events.add(eventInst);
    }

    public static Set<Sequence> mine(Spade spade, Instances data, double minSup) throws Exception {
        spade.setMinSupport(minSup);
        spade.buildAssociations(data);
        return new HashSet<>(spade.getFrequentSequences());
    }

    public static void assertPatternSetEquals(Set<Sequence> expected, Set<Sequence> actual) {
        assertEquals(expected.size(), actual.size(), "Pattern set sizes differ.\nExpected: " + expected + "\nActual:   " + actual);
        for (Sequence seq : expected) {
            assertTrue(actual.contains(seq), "Expected sequence missing: " + seq + ". Actual patterns: " + actual);
        }
    }

    public static Sequence seq(String... elements) {
        Sequence s = new Sequence();
        for (String e : elements) {
            s.addElement(new Element(e));
        }
        return s;
    }

    public static Sequence itemsetSeq(String... itemsInOneElement) {
        Sequence s = new Sequence();
        Element e = new Element();
        for (String i : itemsInOneElement) e.addItem(i);
        s.addElement(e);
        return s;
    }
    
    public static String itemStr(String letter) {
        return "Item" + letter + "=1";
    }
}

public class SpadeFunctionalTest {
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
    public void test1SequenceMining() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B", "C"});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0}); // A
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{1}); // B
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{0}); // A
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{2}); // C
        
        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.5);
        
        Set<Sequence> expected = new HashSet<>();
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("B")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("C")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("C")));

        SpadeTestUtils.assertPatternSetEquals(expected, actual);
        
        for (Sequence s : actual) {
            if (s.equals(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A")))) {
                assertEquals(2, spade.getSupport(s));
            } else {
                assertEquals(1, spade.getSupport(s));
            }
        }
    }

    @Test
    public void test2SequenceMining() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B"});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{0});
        SpadeTestUtils.addRelationalEvent(data, 0, new int[]{1});
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{0});
        SpadeTestUtils.addRelationalEvent(data, 1, new int[]{1});
        SpadeTestUtils.addRelationalEvent(data, 2, new int[]{1});
        SpadeTestUtils.addRelationalEvent(data, 2, new int[]{0});

        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.66); // 2 out of 3
        
        Set<Sequence> expected = new HashSet<>();
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("B")));
        expected.add(SpadeTestUtils.seq(SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B")));

        SpadeTestUtils.assertPatternSetEquals(expected, actual);
        assertFalse(actual.contains(SpadeTestUtils.seq(SpadeTestUtils.itemStr("B"), SpadeTestUtils.itemStr("A"))));
    }

    @Test
    public void testKSequenceMining() throws Exception {
        Instances data = SpadeTestUtils.createRelationalDataset(new String[]{"A", "B", "C", "D", "E"});
        for (int seq = 0; seq < 3; seq++) {
            for (int item = 0; item < 5; item++) {
                SpadeTestUtils.addRelationalEvent(data, seq, new int[]{item}); 
            }
        }
        SpadeTestUtils.addRelationalEvent(data, 3, new int[]{0}); // noise

        Set<Sequence> actual = SpadeTestUtils.mine(spade, data, 0.75); // 3 out of 4
        
        Sequence fullSeq = SpadeTestUtils.seq(
            SpadeTestUtils.itemStr("A"), SpadeTestUtils.itemStr("B"),
            SpadeTestUtils.itemStr("C"), SpadeTestUtils.itemStr("D"),
            SpadeTestUtils.itemStr("E")
        );
        assertTrue(actual.contains(fullSeq), "Must find 5-sequence A->B->C->D->E");
        assertEquals(3, spade.getSupport(fullSeq));
    }
}
