import weka.associations.Spade;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import java.util.ArrayList;

public class test_spade_regression {
    public static void main(String[] args) throws Exception {
        Spade spade = new Spade();
        spade.setOptions(new String[]{"-S", "0.0", "-I", "1"});

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("SeqID"));
        attributes.add(new Attribute("ItemA"));
        Instances data = new Instances("RegressionDataset", attributes, 0);

        // Add A multiple times to the same sequence
        addEvent(data, 1, new int[]{0});
        addEvent(data, 1, new int[]{0});
        addEvent(data, 1, new int[]{0});

        addEvent(data, 2, new int[]{0}); // A
        
        spade.buildAssociations(data);
        System.out.println(spade.toString());
    }
    
    private static void addEvent(Instances data, int seqID, int[] itemsPresent) {
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
}
