package discourje.examples.npb3.impl.FTThreads;

public class FFTSetVariablesMessage {
    public final int sign1;
    public final boolean tr;
    public final double x1[];
    public final double exp11[];
    public final double exp21[];
    public final double exp31[];

    public FFTSetVariablesMessage(int sign1, boolean tr, double[] x1, double[] exp11, double[] exp21, double[] exp31) {
        this.sign1 = sign1;
        this.tr = tr;
        this.x1 = x1;
        this.exp11 = exp11;
        this.exp21 = exp21;
        this.exp31 = exp31;
    }
}
