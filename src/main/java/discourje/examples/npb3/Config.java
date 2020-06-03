package discourje.examples.npb3;

import discourje.core.AsyncJ;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class Config {

    public static void clj() {
        AsyncJ.load(AsyncJ.Lib.CLJ);
    }

    public static void dcj() {
        AsyncJ.load(AsyncJ.Lib.DCJ);
    }

    public static void dcjNil() {
        AsyncJ.load(AsyncJ.Lib.DCJ_NIL);
    }

    public static void verbose(Boolean b) {
        if (b != null && b) {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        } else {
            var devNull = new PrintStream(new OutputStream() {
                public void write(int b) {
                }
            });
            System.setOut(devNull);
        }
    }
}
