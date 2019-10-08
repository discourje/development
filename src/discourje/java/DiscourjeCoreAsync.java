package discourje.java;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class DiscourjeCoreAsync {

    private static final String ns = "discourje.core.async";

    private static IFn fnRequire;
    private static IFn fnChan;
    private static IFn fnSend;
    private static IFn fnRecv;
    private static IFn fnClose;
    private static IFn fnMoni;
    private static IFn fnSpec;

    static {
        //var start = System.currentTimeMillis();

        fnRequire = Clojure.var("clojure.core", "require");
        fnRequire.invoke(Clojure.read("[discourje.core.async :refer :all]"));
        fnRequire.invoke(Clojure.read("[discourje.java.settings :refer :all]"));

        fnChan = Clojure.var(ns, "chan");
        fnSend = Clojure.var(ns, ">!!");
        fnRecv = Clojure.var(ns, "<!!");
        fnClose = Clojure.var(ns, "close-channel!");
        fnMoni = Clojure.var(ns, "moni");
        fnSpec = Clojure.var(ns, "spec");

        //var finish = System.currentTimeMillis();
        //System.err.println(finish - start);
    }

    public static Chan chan(int n, String sender, String receiver, Monitor monitor) {
        return new Chan(fnChan.invoke(n, sender, receiver, monitor.o), fnSend, fnRecv, fnClose);
    }

    public static Monitor monitor(String code) {
        var o = fnMoni.invoke(fnSpec.invoke(Clojure.read(code)));
        return new Monitor(o);
    }

    public static Monitor monitor(String ns, String name, Object... args) {
        fnRequire.invoke(Clojure.read("[" + ns + " :refer :all]"));
        var fn = Clojure.var(ns, name);

        final Object o;
        switch (args.length) {
            case 0:
                o = fnMoni.invoke(fnSpec.invoke(fn.invoke()));
                break;
            case 1:
                o = fnMoni.invoke(fnSpec.invoke(fn.invoke(args[0])));
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
