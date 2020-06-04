package discourje.examples.npb3.impl.MGThreads;

public class RprjMessage {

    public final int wstart;
    public final int wend;
    public final int m1k;
    public final int m2k;
    public final int m3k;
    public final int m1j;
    public final int m2j;
    public final int m3j;
    public final int roff;
    public final int zoff;

    public RprjMessage(int wstart, int wend, int m1k, int m2k, int m3k, int m1j, int m2j, int m3j, int roff, int zoff) {
        this.wstart = wstart;
        this.wend = wend;
        this.m1k = m1k;
        this.m2k = m2k;
        this.m3k = m3k;
        this.m1j = m1j;
        this.m2j = m2j;
        this.m3j = m3j;
        this.roff = roff;
        this.zoff = zoff;
    }
}
