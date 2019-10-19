package discourje.examples.tacas2020;

import discourje.java.Chan;
import discourje.java.ClojureCoreAsync;
import discourje.java.DiscourjeCoreAsync;

public class Benchmarks {

    public static Lib LIB = Lib.DISCOURJE;
    public static int K = 2;
    public static long TIME = 10;

    public static void useClojure() {
        LIB = Lib.CLOJURE;
    }

    public static void useDiscourje() {
        LIB = Lib.DISCOURJE;
    }

    public static void setK(int k) {
        K = k;
    }

    public static void setTime(long time) {
        TIME = time;
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

    public static Chan newChan(int n, String sender, String receiver, DiscourjeCoreAsync.Monitor m) {
        switch (Benchmarks.LIB) {
            case CLOJURE:
                return ClojureCoreAsync.chan(n);
            case DISCOURJE:
                return DiscourjeCoreAsync.chan(n, sender, receiver, m);
            default:
                throw new RuntimeException();
        }
    }

    public static DiscourjeCoreAsync.Monitor newMonitorOrNull(String ns, String name, int k) {
        switch (Benchmarks.LIB) {
            case CLOJURE:
                return null;
            case DISCOURJE:
                return DiscourjeCoreAsync.monitor(ns, name, k);
            default:
                throw new RuntimeException();
        }
    }

    public enum Lib {CLOJURE, DISCOURJE}
}
