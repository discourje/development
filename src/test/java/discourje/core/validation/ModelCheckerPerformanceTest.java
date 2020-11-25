package discourje.core.validation;

import discourje.core.lts.LTS;
import org.junit.jupiter.api.Test;

class ModelCheckerPerformanceTest<Spec> extends AbstractModelCheckerTest<Spec> {

    @Test
    public void testPerformanceOnLargeLts() {
        System.out.println("Timings in seconds:");

        long t0 = System.currentTimeMillis();
        LTS<Spec> lts = getLTS("huge-lts");
        lts.expandRecursively();
        long t1 = System.currentTimeMillis();
        int ltsSize = lts.getStates().size();
        System.out.println("LTS size (#states):      " + ltsSize );
        System.out.println("LTS creation:            " + (t1 - t0)/1000.0 );

        ModelChecker modelChecker = new ModelChecker(lts);
        long t2 = System.currentTimeMillis();
        System.out.println("Abstract model creation: " + (t2 - t1)/1000.0 );

        modelChecker.checkModel();
        long t3 = System.currentTimeMillis();
        System.out.println("Labelling:               " + (t3 - t2)/1000.0 );
    }

}