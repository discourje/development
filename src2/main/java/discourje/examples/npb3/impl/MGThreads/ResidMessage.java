package discourje.examples.npb3.impl.MGThreads;

public class ResidMessage {

    public final boolean visr;
    public final int wstart;
    public final int wend;
    public final int n1;
    public final int n2;
    public final int n3;
    public final int off;

    public ResidMessage(boolean visr, int wstart, int wend, int n1, int n2, int n3, int off) {
        this.visr = visr;
        this.wstart = wstart;
        this.wend = wend;
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;
        this.off = off;
    }
}
