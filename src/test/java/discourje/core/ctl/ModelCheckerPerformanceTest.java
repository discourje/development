package discourje.core.ctl;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import discourje.core.lts.LTS;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ModelCheckerPerformanceTest<Spec> extends AbstractModelCheckerTest<Spec> {

    @Test
    @Disabled
    public void testIncreasingLtsSize() throws IOException {
        String java = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");
        println("run #;#states;LTS creation;Model creation;Labelling");
        long i_old = 1;
        long i = 1;
        outer:
        while (true) {
            for (int j = 0; j < 3; j++) {
                long t0 = System.currentTimeMillis();
                String[] command = {java, "-classpath", classpath, "com.intellij.rt.junit.JUnitStarter", "-ideVersion5", "-junit5", "discourje.core.ctl.ModelCheckerPerformanceTest,testLtsForSize"};
                String[] envp = {"discourje.performance.ltssize=" + i};
                Process process = Runtime.getRuntime().exec(command, envp);
                try {
                    process.waitFor(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
                output.lines().forEach(System.out::println);

                BufferedReader outputError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                outputError.lines().forEach(System.err::println);

                if (System.currentTimeMillis() - t0 > (2 * 60_000)) {
                    break outer;
                }
            }
            long tmp = i;
            i = i + i_old;
            i_old = tmp;
        }
    }

    @Test
    public void testLtsForSize() throws FileNotFoundException {
        long t0 = System.currentTimeMillis();
        String sizeString = System.getenv("discourje.performance.ltssize");
        int size = sizeString != null ? Integer.parseInt(sizeString) : 100;
        LTS<Spec> lts = getLargeLTS(size);
        lts.expandRecursively();
        long t1 = System.currentTimeMillis();
        int ltsSize = lts.getStates().size();

        ModelChecker modelChecker = new ModelChecker(lts);
        long t2 = System.currentTimeMillis();

        List<String> result = modelChecker.checkModel();
        System.out.println(result);

        long t3 = System.currentTimeMillis();
        String line = String.format("%s;%s;%s;%s;%s", size, ltsSize, (t1 - t0), (t2 - t1), (t3 - t2));
        println(line);
    }

    private void println(String line) throws FileNotFoundException {
        PrintStream ps = new PrintStream(new FileOutputStream("test.csv", true));
        ps.println(line);
        ps.close();
    }

    LTS<Spec> getLargeLTS(long size) {
        IFn var = Clojure.var("discourje.core.ctl.example-applications", "get-protocol");
        @SuppressWarnings("unchecked")
        LTS<Spec> lts = (LTS<Spec>) var.invoke(size);
        return lts;
    }
}