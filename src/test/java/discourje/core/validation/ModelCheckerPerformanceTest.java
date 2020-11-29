package discourje.core.validation;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import discourje.core.lts.LTS;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ModelCheckerPerformanceTest<Spec> extends AbstractModelCheckerTest<Spec> {

    @Test
    public void testIncreasingLtsSize() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        printHeader();
        long i = 1;
        outer:
        while (true) {
            for (int j = 0; j < 3; j++) {
                long finalI = i;
                long t0 = System.currentTimeMillis();
                Future<?> future = executor.submit(() -> testPerformanceOnLargeLts(finalI));
                try {
                    future.get(10, TimeUnit.MINUTES);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    break outer;
                }
                if (System.currentTimeMillis() - t0 > (2 * 60_000)) {
                    break outer;
                }
            }
            i++;
        }
    }

    public void testPerformanceOnLargeLts(long size) {
        long t0 = System.currentTimeMillis();
        LTS<Spec> lts = getHugeLTS(size);
        lts.expandRecursively();
        long t1 = System.currentTimeMillis();
        int ltsSize = lts.getStates().size();

        ModelChecker modelChecker = new ModelChecker(lts);
        long t2 = System.currentTimeMillis();

        modelChecker.checkModel();
        long t3 = System.currentTimeMillis();
        System.out.printf("%s;%s;%s;%s;%s%n", size, ltsSize, (t1 - t0)/1000.0, (t2 - t1)/1000.0, (t3 - t2)/1000.0 );
    }

    private void printHeader() {
        System.out.println("run #;#states;LTS creation;Model creation;Labelling");
    }

    LTS<Spec> getHugeLTS(long size) {
        IFn var = Clojure.var("discourje.core.validation.performance", "get-huge-lts");
        @SuppressWarnings("unchecked")
        LTS<Spec> lts = (LTS<Spec>) var.invoke(size);
        return lts;
    }
}