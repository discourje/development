package discourje.examples.experimental.java;

import clojure.lang.IFn;

public class Chan {

    private final Object o;
    private final IFn fnSend;
    private final IFn fnRecv;

    public Chan(Object o, IFn fnSend, IFn fnRecv) {
        this.o = o;
        this.fnSend = fnSend;
        this.fnRecv = fnRecv;
    }

    public void send(Object message) {
        fnSend.invoke(o, message);
    }

    public Object recv() {
        return fnRecv.invoke(o);
    }
}
