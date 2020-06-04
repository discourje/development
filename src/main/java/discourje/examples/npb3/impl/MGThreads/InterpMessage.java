package discourje.examples.npb3.impl.MGThreads;

public class InterpMessage {

    public final int wstart;
    public final int wend;
    public final int mm1;
    public final int mm2;
    public final int mm3;
    public final int n1;
    public final int n2;
    public final int n3;
    public final int zoff;
    public final int uoff;

    public InterpMessage(int wstart, int wend, int mm1, int mm2, int mm3, int n1, int n2, int n3, int zoff, int uoff) {
        this.wstart = wstart;
        this.wend = wend;
        this.mm1 = mm1;
        this.mm2 = mm2;
        this.mm3 = mm3;
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;
        this.zoff = zoff;
        this.uoff = uoff;
    }
}
