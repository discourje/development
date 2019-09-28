package discourje.examples.experimental.java;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class ClojureCoreAsync {

    private static IFn fnRequire;
    private static IFn fnChan;
    private static IFn fnSend;
    private static IFn fnRecv;

    static {
        var start = System.currentTimeMillis();

        fnRequire = Clojure.var("clojure.core", "require");
        fnRequire.invoke(Clojure.read("clojure.core.async"));
        fnChan = Clojure.var("clojure.core.async", "chan");
        fnSend = Clojure.var("clojure.core.async", ">!!");
        fnRecv = Clojure.var("clojure.core.async", "<!!");

        var finish = System.currentTimeMillis();
        //System.out.println(finish - start);
    }

    public static Chan chan(int n) {
        return new Chan(fnChan.invoke(n), fnSend, fnRecv);
    }
}
