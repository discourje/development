package discourje.examples.experimental.java;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class DiscourjeCoreAsync {

    private static final String nsApi = "discourje.examples.experimental.api";
    private static final String nsDsl = "discourje.examples.experimental.dsl";

    private static IFn fnRequire;
    private static IFn fnEval;
    private static IFn fnChan;
    private static IFn fnSend;
    private static IFn fnRecv;
    private static IFn fnMonitor;

    static {
        var start = System.currentTimeMillis();

        fnRequire = Clojure.var("clojure.core", "require");
        fnRequire.invoke(Clojure.read("discourje.core.async"));
        fnRequire.invoke(Clojure.read("[" + nsApi + " :refer :all]"));
        fnRequire.invoke(Clojure.read("[" + nsDsl + " :refer :all]"));

        fnEval = Clojure.var("clojure.core", "eval");
        fnChan = Clojure.var(nsApi, "chan");
        fnSend = Clojure.var(nsApi, ">!!");
        fnRecv = Clojure.var(nsApi, "<!!");
        fnMonitor = Clojure.var(nsDsl, "monitor");

        var finish = System.currentTimeMillis();
        System.out.println(finish - start);
    }

    public static Chan chan(int n, String sender, String receiver, Monitor monitor) {
        return new Chan(fnChan.invoke(n, sender, receiver, monitor.o), fnSend, fnRecv);
    }

    public static Monitor monitor(String code) {
        var o = fnMonitor.invoke(fnEval.invoke(Clojure.read("(spec " + code + ")")));
        return new Monitor(o);
    }


    public static Monitor monitor(String ns, String name, Object... args) {
        fnRequire.invoke(Clojure.read(ns));
        var fn = Clojure.var(ns, name);

        final Object o;
        switch (args.length) {
            case 0:
                o = fn.invoke();
                break;
            case 1:
                o = fn.invoke(args[0]);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return new Monitor(o);
    }

    public static class Monitor {
        private final Object o;

        private Monitor(Object o) {
            this.o = o;
        }
    }
}
