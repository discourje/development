package discourje.core;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.util.function.Supplier;

public class AsyncJ {

    public enum Lib {
        CLJ("clojure.core.async"),
        DCJ("discourje.core.async"),
        DCJ_NIL("discourje.core.async");

        public final String ns;

        Lib(String ns) {
            this.ns = ns;
        }
    }

    public interface Channel extends Supplier<Object> {
        default Object close() {
            return AsyncJ.close(this);
        }

        default Object send(Object v) {
            return AsyncJ.send(this, v);
        }

        default Object receive() {
            return AsyncJ.receive(this);
        }
    }

    public interface Monitor extends Supplier<Object> {
    }

    private static Lib lib;

    private static IFn fnRequire;
    private static IFn fnEval;

    private static IFn fnChannel;
    private static IFn fnClose;
    private static IFn fnSend;
    private static IFn fnReceive;
    private static IFn fnSelect;

    private static IFn fnMonitor;
    private static IFn fnLink;

    public static void load(Lib lib) {
        System.out.print("Loading AsyncJ (" + lib + ")... ");
        var begin = System.currentTimeMillis();

        fnRequire = Clojure.var("clojure.core", "require");
        fnRequire.invoke(Clojure.read(lib.ns));
        fnEval = Clojure.var("clojure.core", "eval");

        fnChannel = Clojure.var(lib.ns, "chan");
        fnClose = Clojure.var(lib.ns, "close!");
        fnSend = Clojure.var(lib.ns, ">!!");
        fnReceive = Clojure.var(lib.ns, "<!!");
        fnSelect = Clojure.var(lib.ns, "alts!!");

        if (lib == Lib.DCJ) {
            fnMonitor = Clojure.var(lib.ns, "monitor");
            fnLink = Clojure.var(lib.ns, "link");
        }

        AsyncJ.lib = lib;

        var end = System.currentTimeMillis();
        System.out.println("Done (" + (end - begin) + " ms).");
    }

    public static boolean isLoaded() {
        return lib != null;
    }

    public static boolean clj() {
        return lib == Lib.CLJ;
    }

    public static boolean dcj() {
        return lib == Lib.DCJ;
    }

    public static boolean dcjNil() {
        return lib == Lib.DCJ_NIL;
    }

    //
    // CLJ and DCJ
    //

    public static Channel channel() {
        assert isLoaded();
        var o = fnChannel.invoke();
        return () -> o;
    }

    public static Channel channel(SpecJ.Role sender, SpecJ.Role receiver, Monitor monitor) {
        assert isLoaded();
        var c = channel();
        if (dcj()) {
            link(c, sender, receiver, monitor);
        }
        return c;
    }

    public static Channel channel(int n) {
        assert isLoaded();
        var o = fnChannel.invoke(n);
        return () -> o;
    }

    public static Channel channel(int n, SpecJ.Role sender, SpecJ.Role receiver, Monitor monitor) {
        assert isLoaded();
        var c = channel(n);
        if (dcj()) {
            link(c, sender, receiver, monitor);
        }
        return c;
    }

    public static Object close(Channel c) {
        assert isLoaded();
        return fnClose.invoke(c.get());
    }

    public static Object send(Channel c, Object v) {
        assert isLoaded();
        return fnSend.invoke(c.get(), v);
    }

    public static Object receive(Channel c) {
        assert isLoaded();
        return fnReceive.invoke(c.get());
    }

    public static Object select() {
        assert isLoaded();
        throw new UnsupportedOperationException(); // TODO
    }

    //
    // DCJ
    //

    public static Monitor monitor(SpecJ.Session s) {
        assert isLoaded() && (dcj() || dcjNil());
        var o = dcj() ? fnMonitor.invoke(s.get()) : null;
        return () -> o;
    }

    public static void link(Channel c, SpecJ.Role sender, SpecJ.Role receiver, Monitor monitor) {
        assert isLoaded() && (dcj() || dcjNil());
        fnLink.invoke(c.get(),
                dcj() ? sender.get() : null,
                dcj() ? receiver.get() : null,
                dcj() ? monitor.get() : null);
    }
}
