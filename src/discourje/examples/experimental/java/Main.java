package discourje.examples.experimental.java;

public class Main {

    public static void main(String[] args) {

//        ClojureCoreAsync.Chan ch = ClojureCoreAsync.chan(1);
//
//        new Thread(() -> System.out.println(ch.recv())).start();
//
//        ch.send("foo");
//        System.out.println("sent");
//        ch.send("foo");
//        System.out.println("sent");


//        System.out.println();

//        DiscourjeCoreAsync.monitor("discourje.examples.experimental.java.test", "n");

//        System.out.println("loading done");

//        var m = DiscourjeCoreAsync.monitor("(--> alice bob String)");
//        var c = DiscourjeCoreAsync.chan(3, "alice", "bob", m);

var c = ClojureCoreAsync.chan(3);

        c.send("foo");
        c.send("bar");
        System.out.println(c.recv());

    }
}
