package discourje.examples;

import discourje.core.AsyncJ;
import discourje.core.SpecJ;

public class HelloWorld {

    public static void main(String[] args) {
        AsyncJ.load(AsyncJ.Lib.DCJ);
        var c = AsyncJ.channel();

        if (AsyncJ.dcj()) {
            SpecJ.defRole("::alice");
            SpecJ.defRole("::bob");
            SpecJ.defSession("::hello-world", new String[]{}, "(cat (--> ::alice ::bob) (close ::alice ::bob))");

            var m = AsyncJ.monitor(SpecJ.session("::hello-world", new String[]{}));
            AsyncJ.link(c, SpecJ.role("::alice"), SpecJ.role("::bob"), m);
        }

        new Thread(() -> {
            AsyncJ.send(c, "hello, world");
            AsyncJ.close(c);
        }).start();

        new Thread(() -> {
            System.out.println(AsyncJ.receive(c));
        }).start();
    }
}
