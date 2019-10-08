package discourje.examples.tacas2020;

public class Benchmarks {

    public static int K = 2;
    public static Lib LIB = Lib.DISCOURJE;

    public static void setK(int k) {
        K = k;
    }

    public static void useClojure() {
        LIB = Lib.CLOJURE;
    }

    public static void useDiscourje() {
        LIB = Lib.DISCOURJE;
    }

    public static void run(long time, Runnable r) {
        time = time * 1000_000_000;

        var start = System.nanoTime();
        var finish = System.nanoTime();
        var n = 0;

        while (finish - start < time) {
            r.run();
            n++;
            finish = System.nanoTime();
        }

        System.err.println((finish - start) + " ns, "
                + n + " runs, "
                + ((finish - start) / n) + " ns/run");
    }

    public enum Lib {CLOJURE, DISCOURJE}
}
