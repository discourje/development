package discourje.core;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.util.Arrays;
import java.util.function.Supplier;

public class SpecJ {

    public interface Role extends Supplier<Object> {
    }

    public interface Session extends Supplier<Object> {
    }

    private static IFn fnRequire;
    private static IFn fnEval;
    private static IFn fnVec;
    private static IFn fnRead;

    private static IFn fnRole;
    private static IFn fnSession;

    static {
        System.out.print("Loading SpecJ... ");
        var begin = System.currentTimeMillis();
        var ns = "";

        ns = "clojure.core";
        fnRequire = Clojure.var(ns, "require");
        fnEval = Clojure.var(ns, "eval");
        fnVec = Clojure.var(ns, "vec");
        fnRead = Clojure.var(ns, "read-string");

        ns = "discourje.core.spec.ast";
        fnRole = Clojure.var(ns, "role");
        fnSession = Clojure.var(ns, "session");

        var end = System.currentTimeMillis();
        System.out.println("Done (" + (end - begin) + " ms).");
    }

    public static void defRole(String nameKey) {
        fnRequire.invoke(fnRead.invoke("[discourje.core.spec :refer :all]"));
        var s = "(defrole " + nameKey + ")";
        fnEval.invoke(fnRead.invoke(s));
    }

    public static void defSession(String nameKey, String[] vars, String body) {
        fnRequire.invoke(fnRead.invoke("[discourje.core.spec :refer :all]"));
        var s = "(defsession " + nameKey + " " + Arrays.toString(vars) + " " + body + ")";
        fnEval.invoke(fnRead.invoke(s));
    }

    public static Role role(String nameExpr, Object... indexVals) {
        var o = fnRole.invoke(fnRead.invoke(nameExpr), fnVec.invoke(indexVals));
        return () -> o;
    }

    public static Session session(String nameKey, Object[] vals) {
        var o = fnSession.invoke(fnRead.invoke(nameKey), fnVec.invoke(vals));
        return () -> o;
    }

    public static Session parse(String s) {
        fnRequire.invoke(fnRead.invoke("[discourje.core.spec :refer :all]"));
        var o = fnEval.invoke(Clojure.read(s));
        return () -> o;
    }
}
