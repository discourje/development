package discourje.java;

import clojure.lang.IFn;

public class Chan {

    private final Object o; // the actual channel object
    private final IFn fnSend;
    private final IFn fnRecv;
    private final IFn fnClose;

    public Chan(Object o, IFn fnSend, IFn fnRecv, IFn fnClose) {
        this.o = o;
        this.fnSend = fnSend;
        this.fnRecv = fnRecv;
        this.fnClose = fnClose;
    }

    public void send(Object message) {
        fnSend.invoke(o, message);
    }

    public Object recv() {
        return fnRecv.invoke(o);
    }

    public void close() {
        fnClose.invoke(o);
    }
}
